package vn.edu.tlu.dms.registration.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.StayRequestStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.SummerStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.contract.service.ContractCrudService;
import vn.edu.tlu.dms.contract.service.ContractService;
import vn.edu.tlu.dms.registration.api.PendingItemController.Item;
import vn.edu.tlu.dms.registration.api.PendingItemController.Type;
import vn.edu.tlu.dms.registration.service.PendingItemService;
import vn.edu.tlu.dms.registration.service.RoomApplicationService;
import vn.edu.tlu.dms.registration.service.SummerCrudService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tổng hợp hộp việc chờ từ các Service công khai. Mỗi nguồn tự lọc phạm vi đơn vị và quyền đọc;
 * lớp này chỉ gộp, sắp xếp mới nhất trước và phân trang lại, không truy vấn bảng của module khác.
 *
 * <p>Do nhiều nguồn không dùng chung một truy vấn phân trang, mỗi nguồn được đọc theo cửa sổ
 * {@code (page + 1) * size} bản ghi mới nhất (tối đa {@link #MAX_SCAN_PER_SOURCE}) rồi mới gộp.
 */
@Service
@Transactional(readOnly = true)
public class PendingItemServiceImpl implements PendingItemService {

    /** Giới hạn kỹ thuật của một trang nguồn; khớp giới hạn page size của API. */
    private static final int SOURCE_PAGE_SIZE = 100;

    /** Trần bản ghi đọc từ mỗi nguồn để bảo vệ bộ nhớ khi trang yêu cầu quá sâu. */
    private static final int MAX_SCAN_PER_SOURCE = 500;

    private static final Comparator<Item> NEWEST_FIRST =
            Comparator.comparing(
                            Item::submittedAt,
                            Comparator.nullsLast(Comparator.<OffsetDateTime>reverseOrder()))
                    .thenComparing(Item::entityId, Comparator.reverseOrder());

    private final RoomApplicationService applications;

    private final SummerCrudService summers;

    private final ContractService contracts;

    private final ContractCrudService stayRequests;

    private final AccountReferenceService accounts;

    private final ScopeGuard guard;

    private final ObjectMapper mapper;

    public PendingItemServiceImpl(
            RoomApplicationService applications,
            SummerCrudService summers,
            ContractService contracts,
            ContractCrudService stayRequests,
            AccountReferenceService accounts,
            ScopeGuard guard,
            ObjectMapper mapper) {
        this.applications = applications;
        this.summers = summers;
        this.contracts = contracts;
        this.stayRequests = stayRequests;
        this.accounts = accounts;
        this.guard = guard;
        this.mapper = mapper;
    }

    @Override
    public PageResponse<Item> searchByPage(SearchRequest input) {
        guard.requireRole(Role.STAFF);
        var types = resolveTypes(input.type());
        int page = input.page();
        int size = input.size();
        int window = (int) Math.min((long) MAX_SCAN_PER_SOURCE, (long) (page + 1) * size);
        var items = new ArrayList<Item>();
        long total = 0;
        for (Type type : types) {
            var chunk = load(type, input, window);
            items.addAll(chunk.items());
            total += chunk.total();
        }
        items.sort(NEWEST_FIRST);
        long offset = (long) page * size;
        int from = offset > Integer.MAX_VALUE ? items.size() : (int) offset;
        List<Item> content =
                from >= items.size()
                        ? List.of()
                        : List.copyOf(items.subList(from, Math.min(items.size(), from + size)));
        int totalPages = (int) ((total + size - 1) / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    private List<Type> resolveTypes(String requested) {
        if (requested == null || requested.isBlank()) {
            return List.of(Type.values());
        }
        try {
            return List.of(Type.valueOf(requested.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            throw BusinessException.invalid(
                    ApiField.TYPE, ValidationMessage.PENDING_TYPE_INVALID);
        }
    }

    private Chunk load(Type type, SearchRequest input, int window) {
        String status = statusFor(type);
        var items = new ArrayList<Item>();
        long total = 0;
        int page = 0;
        int size = Math.min(SOURCE_PAGE_SIZE, window);
        while (true) {
            var request = searchRequest(page, size, status, input.q());
            var chunk = fetch(type, request);
            if (page == 0) {
                total = chunk.total();
            }
            items.addAll(chunk.items());
            if (items.size() >= window || chunk.items().size() < size) {
                break;
            }
            page++;
            size = SOURCE_PAGE_SIZE;
        }
        if (items.size() > window) {
            items = new ArrayList<>(items.subList(0, window));
        }
        return new Chunk(items, total);
    }

    private Chunk fetch(Type type, SearchRequest request) {
        return switch (type) {
            case APPLICATION -> applicationChunk(request);
            case EXTENSION -> stayChunk(Type.EXTENSION, true, request);
            case TRANSFER -> stayChunk(Type.TRANSFER, false, request);
            case SUMMER_CHOICE -> summerChunk(request);
            case CONTRACT -> contractChunk(request);
        };
    }

    private Chunk applicationChunk(SearchRequest request) {
        var page = applications.searchByPage(request, false);
        var items =
                page.content().stream()
                        .map(
                                application ->
                                        new Item(
                                                Type.APPLICATION.name(),
                                                application.id(),
                                                application.student().id(),
                                                application.student().fullName(),
                                                application.status(),
                                                application.submittedAt()))
                        .toList();
        return new Chunk(items, page.totalElements());
    }

    private Chunk summerChunk(SearchRequest request) {
        var page = summers.searchByPage(request, false);
        var items =
                page.content().stream()
                        .map(
                                row ->
                                        new Item(
                                                Type.SUMMER_CHOICE.name(),
                                                longValue(row.get("id")),
                                                longValue(row.get("studentId")),
                                                string(row.get("fullName")),
                                                string(row.get("status")),
                                                timestamp(row.get("createdAt"))))
                        .toList();
        return new Chunk(items, page.totalElements());
    }

    private Chunk contractChunk(SearchRequest request) {
        var page = mapPage(contracts.searchByPage(request, false));
        var items =
                page.content().stream()
                        .map(
                                row -> {
                                    Long studentId = longValue(row.get("studentId"));
                                    return new Item(
                                            Type.CONTRACT.name(),
                                            longValue(row.get("id")),
                                            studentId,
                                            studentName(studentId),
                                            string(row.get("status")),
                                            null);
                                })
                        .toList();
        return new Chunk(items, page.totalElements());
    }

    private Chunk stayChunk(Type type, boolean extension, SearchRequest request) {
        var page = mapPage(stayRequests.searchStayRequestsByPage(extension, request, false));
        var items =
                page.content().stream()
                        .map(
                                row ->
                                        new Item(
                                                type.name(),
                                                longValue(row.get("id")),
                                                null,
                                                string(row.get("fullName")),
                                                string(row.get("status")),
                                                timestamp(row.get("createdAt"))))
                        .toList();
        return new Chunk(items, page.totalElements());
    }

    /**
     * Chuyển trang DTO của module contract về Map để registration không phụ thuộc package api của
     * module khác (SAD 4.2); lời gọi vẫn đi qua Service công khai.
     */
    private PageResponse<Map<String, Object>> mapPage(Object raw) {
        return mapper.convertValue(
                raw, new TypeReference<PageResponse<Map<String, Object>>>() {});
    }

    private SearchRequest searchRequest(int page, int size, String status, String keyword) {
        var body = new LinkedHashMap<String, Object>();
        body.put(ApiField.PAGE, page);
        body.put(ApiField.SIZE, size);
        body.put(ApiField.STATUS, List.of(status));
        if (keyword != null && !keyword.isBlank()) {
            body.put(ApiField.QUERY, keyword);
        }
        return mapper.convertValue(body, SearchRequest.class);
    }

    private static String statusFor(Type type) {
        return switch (type) {
            case APPLICATION -> ApplicationStatus.SUBMITTED;
            case EXTENSION, TRANSFER -> StayRequestStatus.PENDING;
            case SUMMER_CHOICE -> SummerStatus.PENDING;
            case CONTRACT -> ContractStatus.PENDING;
        };
    }

    private String studentName(Long studentId) {
        return studentId == null ? null : accounts.user(studentId).fullName();
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static OffsetDateTime timestamp(Object value) {
        if (value instanceof OffsetDateTime time) {
            return time;
        }
        if (value instanceof Instant instant) {
            return instant.atZone(ZoneId.of(DmsConstants.APPLICATION_TIME_ZONE))
                    .toOffsetDateTime();
        }
        if (value instanceof LocalDate date) {
            return date.atStartOfDay(ZoneId.of(DmsConstants.APPLICATION_TIME_ZONE))
                    .toOffsetDateTime();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return OffsetDateTime.parse(text);
            } catch (RuntimeException exception) {
                return null;
            }
        }
        return null;
    }

    private record Chunk(List<Item> items, long total) {}
}
