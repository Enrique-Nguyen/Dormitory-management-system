package vn.edu.tlu.dms.common.service.impl;

import jakarta.persistence.Table;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.Action;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.CrudService;
import vn.edu.tlu.dms.common.support.PageQuery;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Khung CRUD chỉ dùng cho danh mục được phép sửa. Dữ liệu có trạng thái nghiệp vụ phải dùng service
 * riêng để bảo toàn hợp đồng, lịch sử phân phòng và giao dịch tiền. Mỗi thao tác ghi là một
 * transaction; saveAndFlush làm lộ lỗi ràng buộc trước khi trả response.
 */
@Transactional(readOnly = true)
public abstract class AbstractCrudServiceImpl<E, I, R> implements CrudService<I, R> {

    protected final JpaRepository<E, Long> repository;

    private final JpaSpecificationExecutor<E> searchRepository;

    private final AuditService auditService;

    private final CurrentUser currentUser;

    protected <T extends JpaRepository<E, Long> & JpaSpecificationExecutor<E>>
            AbstractCrudServiceImpl(
                    T repository, AuditService auditService, CurrentUser currentUser) {
        this.repository = repository;
        this.searchRepository = repository;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    protected abstract E newEntity();

    protected abstract void apply(E entity, I input, boolean creating);

    protected abstract R toResponse(E entity);

    protected abstract Long getEntityId(E entity);

    /** ID trong body ghi phẳng; null là tạo mới, ID dương là cập nhật bản ghi đã có. */
    protected abstract Long getInputId(I input);

    /** Danh sách thuộc tính văn bản do server cho phép tìm kiếm; không nhận tên cột từ client. */
    protected abstract Set<String> searchableFields();

    /** Danh sách thuộc tính sắp xếp an toàn, khai báo theo entity trong Service cụ thể. */
    protected abstract Set<String> sortableFields();

    /** Bộ lọc phạm vi bắt buộc được kết hợp trước khi phân trang và tính tổng bản ghi. */
    protected Specification<E> searchScope(SearchRequest search) {
        return (root, query, builder) -> builder.conjunction();
    }

    /** Service con bắt buộc khai báo quyền đọc, tránh quên kiểm tra khi mở module mới. */
    protected abstract void checkRead();

    /**
     * Service con bắt buộc kiểm tra quyền ghi; việc ẩn nút trong frontend không thay thế kiểm tra
     * này.
     */
    protected abstract void checkWrite();

    /** Service con gán mã quyền CRUD cụ thể; giữ hook cũ cho module chưa khai mã quyền. */
    protected void checkPermission(String action) {
        if (Action.READ.equals(action)) {
            checkRead();
        } else {
            checkWrite();
        }
    }

    protected void beforeDelete(E entity) {}

    protected E findEntityById(Long id) {
        return repository.findById(id).orElseThrow(BusinessException::notFound);
    }

    @Override
    public PageResponse<R> searchByPage(SearchRequest search) {
        checkPermission(Action.READ);
        requireValue(search, "search");
        var pageable =
                PageQuery.of(
                        search.page(), search.size(), search.sort(), sortableFields(), "id,asc");
        String keyword = search.q().trim().toLowerCase(Locale.ROOT);
        Specification<E> keywordFilter =
                (root, query, builder) -> {
                    if (keyword.isBlank()) {
                        return builder.conjunction();
                    }
                    var predicates =
                            searchableFields().stream()
                                    .map(
                                            field ->
                                                    builder.like(
                                                            builder.lower(root.get(field)),
                                                            "%" + keyword + "%"))
                                    .toArray(Predicate[]::new);
                    return builder.or(predicates);
                };
        return PageResponse.from(
                searchRepository
                        .findAll(searchScope(search).and(keywordFilter), pageable)
                        .map(this::toResponse));
    }

    @Override
    public List<R> getAll() {
        checkPermission(Action.READ);
        return repository.findAll(Sort.by("id")).stream().map(this::toResponse).toList();
    }

    @Override
    public R getById(Long id) {
        checkPermission(Action.READ);
        return toResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public R saveOrUpdate(I input) {
        requireValue(input, "form");
        Long id = getInputId(input);
        if (id != null && id <= 0) {
            throw BusinessException.invalid(ApiField.ID, ValidationMessage.ID_POSITIVE);
        }
        boolean creating = id == null;
        String action = creating ? Action.CREATE : Action.UPDATE;
        // Annotation ngoài Controller là OR; quyền thật phải kiểm tra theo nhánh ghi ở đây.
        checkPermission(action);
        E entity = creating ? newEntity() : findEntityById(id);
        R before = creating ? null : toResponse(entity);
        apply(entity, input, creating);
        R response = toResponse(repository.saveAndFlush(entity));
        recordCatalogAudit(entity, action, before, response);
        return response;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        checkPermission(Action.DELETE);
        E entity = findEntityById(id);
        beforeDelete(entity);
        R before = toResponse(entity);
        repository.delete(entity);
        repository.flush();
        recordCatalogAudit(entity, Action.DELETE, before, null);
    }

    /** Nhật ký và dữ liệu danh mục cùng transaction: không có dữ liệu đổi mà thiếu nhật ký. */
    private void recordCatalogAudit(E entity, String action, Object before, Object after) {
        var table = entity.getClass().getAnnotation(Table.class);
        String entityType = table == null ? entity.getClass().getSimpleName() : table.name();
        auditService.record(
                currentUser.get().id(),
                AuditAction.CATALOG_PREFIX + action,
                entityType.toUpperCase(Locale.ROOT),
                getEntityId(entity),
                before,
                after,
                null);
    }

    protected <T> T requireValue(T value, String field) {
        if (value == null) {
            throw BusinessException.invalid(field, ValidationMessage.REQUIRED);
        }
        return value;
    }

    protected String trimRequiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw BusinessException.invalid(field, ValidationMessage.REQUIRED);
        }
        return value.trim();
    }
}
