package vn.edu.tlu.dms.registration.service;

import jakarta.persistence.criteria.Predicate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.common.support.PageQuery;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Input;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Priority;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Response;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.StudentRef;
import vn.edu.tlu.dms.registration.domain.ApplicationPriority;
import vn.edu.tlu.dms.registration.domain.RegistrationPeriodUnitId;
import vn.edu.tlu.dms.registration.domain.RoomApplication;
import vn.edu.tlu.dms.registration.domain.RoomApplicationPolicy;
import vn.edu.tlu.dms.registration.repository.ApplicationPriorityRepository;
import vn.edu.tlu.dms.registration.repository.PriorityCategoryRepository;
import vn.edu.tlu.dms.registration.repository.RegistrationPeriodRepository;
import vn.edu.tlu.dms.registration.repository.RegistrationPeriodUnitRepository;
import vn.edu.tlu.dms.registration.repository.RoomApplicationRepository;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class RoomApplicationService {

    private final RoomApplicationRepository applications;

    private final RegistrationPeriodRepository periods;

    private final RegistrationPeriodUnitRepository periodUnits;

    private final ApplicationPriorityRepository priorities;

    private final PriorityCategoryRepository categories;

    private final RoomReferenceService rooms;

    private final AccountReferenceService accounts;

    private final CurrentUser current;

    private final ScopeGuard guard;

    private final StatusHistoryService history;

    private final JdbcTemplate jdbc;

    public RoomApplicationService(
            RoomApplicationRepository applications,
            RegistrationPeriodRepository periods,
            RegistrationPeriodUnitRepository periodUnits,
            ApplicationPriorityRepository priorities,
            PriorityCategoryRepository categories,
            RoomReferenceService rooms,
            AccountReferenceService accounts,
            CurrentUser current,
            ScopeGuard guard,
            StatusHistoryService history,
            JdbcTemplate jdbc) {
        this.applications = applications;
        this.periods = periods;
        this.periodUnits = periodUnits;
        this.priorities = priorities;
        this.categories = categories;
        this.rooms = rooms;
        this.accounts = accounts;
        this.current = current;
        this.guard = guard;
        this.history = history;
        this.jdbc = jdbc;
    }

    public PageResponse<Response> searchApplications(
            int page,
            int size,
            List<String> sort,
            Long periodId,
            List<String> statuses,
            boolean mine) {
        return searchApplications(page, size, sort, periodId, statuses, mine, "");
    }

    public PageResponse<Response> searchApplications(
            int page,
            int size,
            List<String> sort,
            Long periodId,
            List<String> statuses,
            boolean mine,
            String keyword) {
        guard.requireRole(mine ? new String[] {"STUDENT"} : new String[] {"STAFF"});
        var actor = current.get();
        var pageable =
                PageQuery.of(
                        page,
                        size,
                        sort,
                        Set.of("id", "priorityScore", "submittedAt", "createdAt"),
                        "createdAt,desc");
        return PageResponse.from(
                applications
                        .findAll(
                                (r, q, b) -> {
                                    var filters = new ArrayList<Predicate>();
                                    if (keyword != null && !keyword.isBlank()) {
                                        var ids =
                                                jdbc.query(
                                                        "SELECT id FROM app_user WHERE full_name"
                                                                + " ILIKE ? OR username ILIKE ?",
                                                        (row, index) -> row.getLong(1),
                                                        "%" + keyword.trim() + "%",
                                                        "%" + keyword.trim() + "%");
                                        filters.add(
                                                ids.isEmpty()
                                                        ? b.disjunction()
                                                        : r.get("studentId").in(ids));
                                    }
                                    if (mine) {
                                        filters.add(b.equal(r.get("studentId"), actor.id()));
                                    } else if (!actor.crossUnitAccess()) {
                                        filters.add(
                                                b.equal(r.get("preferredUnitId"), actor.unitId()));
                                    }
                                    if (periodId != null) {
                                        filters.add(b.equal(r.get("periodId"), periodId));
                                    }
                                    if (statuses != null && !statuses.isEmpty()) {
                                        filters.add(r.get("status").in(statuses));
                                    }
                                    return b.and(filters.toArray(Predicate[]::new));
                                },
                                pageable)
                        .map(a -> toApplicationResponse(a, false)));
    }

    public Response getApplicationById(Long id) {
        var a = loadAccessibleApplication(id, false);
        return toApplicationResponse(a, true);
    }

    /**
     * Sinh viên lấy từ phiên, không nhận studentId/status/priorityScore từ request để tránh giả
     * mạo.
     */
    @Transactional
    public Response createApplication(Input input) {
        guard.requireRole("STUDENT");
        if (input.periodId() == null || input.preferredUnitId() == null) {
            throw BusinessException.invalid("periodId", "Thiếu đợt hoặc đơn vị nguyện vọng");
        }
        rooms.lockStudentProfile(current.get().id());
        if (rooms.hasPendingOrActiveContract(current.get().id())) {
            throw BusinessException.conflict("CONTRACT_EXISTS");
        }
        var a = new RoomApplication();
        a.setStudentId(current.get().id());
        a.setPeriodId(input.periodId());
        a.setPreferredUnitId(input.preferredUnitId());
        a.setPreferredRoomTypeId(input.preferredRoomTypeId());
        a.setNote(input.note());
        a.setStatus("DRAFT");
        a.setPriorityScore(0);
        a.setCreatedAt(OffsetDateTime.now());
        validateApplicationPeriod(a);
        applications.saveAndFlush(a);
        replaceApplicationPriorities(a.getId(), input.priorityCategoryIds());
        history.record("APPLICATION", a.getId(), null, "DRAFT", current.get().id(), null);
        return toApplicationResponse(a, true);
    }

    @Transactional
    public Response updateDraftApplication(Long id, Input input) {
        guard.requireRole("STUDENT");
        var a = loadAccessibleApplication(id, true);
        RoomApplicationPolicy.requireAction(a, current.get(), "edit");
        if (input.periodId() != null && !input.periodId().equals(a.getPeriodId())) {
            throw BusinessException.invalid("periodId", "Không đổi đợt của đơn đã tạo");
        }
        if (input.preferredUnitId() != null) {
            a.setPreferredUnitId(input.preferredUnitId());
        }
        if (input.preferredRoomTypeId() != null) {
            a.setPreferredRoomTypeId(input.preferredRoomTypeId());
        }
        if (input.note() != null) {
            a.setNote(input.note().trim());
        }
        validateApplicationPeriod(a);
        if (input.priorityCategoryIds() != null) {
            replaceApplicationPriorities(id, input.priorityCategoryIds());
        }
        applications.saveAndFlush(a);
        return toApplicationResponse(a, true);
    }

    /** Khóa đơn, kiểm tra Policy và ghi trạng thái + timeline trong cùng transaction. */
    @Transactional
    public Response performApplicationAction(Long id, String action, String reason) {
        var a = loadAccessibleApplication(id, true);
        RoomApplicationPolicy.requireAction(a, current.get(), action);
        if (action.equals("submit")) {
            validateApplicationPeriod(a);
            if (rooms.hasPendingOrActiveContract(a.getStudentId())) {
                throw BusinessException.conflict("CONTRACT_EXISTS");
            }
            a.setPriorityScore(
                    priorities.findAll((r, q, b) -> b.equal(r.get("applicationId"), id)).stream()
                            .filter(p -> !Boolean.FALSE.equals(p.getVerified()))
                            .mapToInt(
                                    p ->
                                            categories
                                                    .findById(p.getCategoryId())
                                                    .orElseThrow(BusinessException::notFound)
                                                    .getWeight())
                            .sum());
            a.setSubmittedAt(OffsetDateTime.now());
        }
        if (action.equals("reject") && (reason == null || reason.trim().length() < 5)) {
            throw BusinessException.invalid("reason", "Cần lý do từ chối");
        }
        String before = a.getStatus();
        a.setStatus(
                switch (action) {
                    case "submit" -> "SUBMITTED";
                    case "approve" -> "APPROVED";
                    case "reject" -> "REJECTED";
                    case "cancel" -> "CANCELLED";
                    default -> throw BusinessException.invalid("action", "Hành động không hỗ trợ");
                });
        a.setStatusReason(reason);
        applications.saveAndFlush(a);
        history.record("APPLICATION", id, before, a.getStatus(), current.get().id(), reason);
        return toApplicationResponse(a, true);
    }

    private RoomApplication loadAccessibleApplication(Long id, boolean writing) {
        guard.requireRole("STUDENT", "STAFF");
        if (writing) {
            jdbc.query(
                    "SELECT id FROM room_application WHERE id=? FOR UPDATE",
                    (r, i) -> r.getLong(1),
                    id);
        }
        var a = applications.findById(id).orElseThrow(BusinessException::notFound);
        var actor = current.get();
        if (actor.is("STUDENT") && !actor.id().equals(a.getStudentId())) {
            throw BusinessException.notFound();
        }
        if (actor.is("STAFF")) {
            if (writing) {
                guard.requireUnit(a.getPreferredUnitId());
            } else {
                guard.requireReadableUnit(a.getPreferredUnitId());
            }
        }
        return a;
    }

    private void validateApplicationPeriod(RoomApplication a) {
        var p = periods.findById(a.getPeriodId()).orElseThrow(BusinessException::notFound);
        var now = OffsetDateTime.now();
        if (!p.getKind().equals("REGULAR")
                || now.isBefore(p.getOpenAt())
                || !now.isBefore(p.getCloseAt())) {
            throw BusinessException.conflict("PERIOD_NOT_OPEN");
        }
        if (!periodUnits.existsById(
                new RegistrationPeriodUnitId(p.getId(), a.getPreferredUnitId()))) {
            throw new BusinessException("UNIT_NOT_IN_PERIOD", 400);
        }
        if (a.getPreferredRoomTypeId() != null) {
            rooms.requireActiveRoomType(a.getPreferredRoomTypeId());
        }
    }

    private void replaceApplicationPriorities(Long id, List<Long> ids) {
        List<Long> selected = ids == null ? List.of() : ids;
        if (new HashSet<>(selected).size() != selected.size()) {
            throw BusinessException.invalid("priorityCategoryIds", "Không khai trùng diện ưu tiên");
        }
        for (Long categoryId : selected) {
            if (!categories.exists(
                    (r, q, b) ->
                            b.and(b.equal(r.get("id"), categoryId), b.isTrue(r.get("active"))))) {
                throw BusinessException.notFound();
            }
        }
        priorities.deleteAll(priorities.findAll((r, q, b) -> b.equal(r.get("applicationId"), id)));
        priorities.flush();
        for (Long categoryId : selected) {
            var row = new ApplicationPriority();
            row.setApplicationId(id);
            row.setCategoryId(categoryId);
            priorities.save(row);
        }
        priorities.flush();
    }

    private Response toApplicationResponse(RoomApplication a, boolean detail) {
        var user = accounts.user(a.getStudentId());
        var priorityRows =
                detail
                        ? priorities
                                .findAll((r, q, b) -> b.equal(r.get("applicationId"), a.getId()))
                                .stream()
                                .map(
                                        p -> {
                                            var category =
                                                    categories
                                                            .findById(p.getCategoryId())
                                                            .orElseThrow(
                                                                    BusinessException::notFound);
                                            return new Priority(
                                                    p.getCategoryId(),
                                                    category.getName(),
                                                    category.getWeight(),
                                                    p.getVerified());
                                        })
                                .toList()
                        : null;
        return new Response(
                a.getId(),
                a.getStatus(),
                new StudentRef(user.id(), user.username(), user.fullName(), user.gender()),
                a.getPeriodId(),
                a.getPreferredUnitId(),
                a.getPreferredRoomTypeId(),
                a.getPriorityScore(),
                a.getNote(),
                a.getStatusReason(),
                a.getSubmittedAt(),
                priorityRows,
                detail ? RoomApplicationPolicy.allowedActions(a, current.get()) : null,
                detail ? history.list("APPLICATION", a.getId()) : null);
    }
}
