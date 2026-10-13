package vn.edu.tlu.dms.common.service;

import jakarta.persistence.Table;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;

import java.util.List;
import java.util.Locale;

/**
 * Khung CRUD chỉ dùng cho danh mục được phép sửa. Dữ liệu có trạng thái nghiệp vụ phải dùng service
 * riêng để bảo toàn hợp đồng, lịch sử phân phòng và giao dịch tiền. Mỗi thao tác ghi là một
 * transaction; saveAndFlush làm lộ lỗi ràng buộc trước khi trả response.
 */
@Transactional(readOnly = true)
public abstract class AbstractCrudService<E, I, R> implements CrudService<I, R> {

    protected final JpaRepository<E, Long> repository;

    private final AuditService auditService;

    private final CurrentUser currentUser;

    protected AbstractCrudService(
            JpaRepository<E, Long> repository, AuditService auditService, CurrentUser currentUser) {
        this.repository = repository;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    protected abstract E newEntity();

    protected abstract void apply(E entity, I input, boolean creating);

    protected abstract R toResponse(E entity);

    protected abstract Long getEntityId(E entity);

    /** Service con bắt buộc khai báo quyền đọc, tránh quên kiểm tra khi mở module mới. */
    protected abstract void checkRead();

    /**
     * Service con bắt buộc kiểm tra quyền ghi; việc ẩn nút trong frontend không thay thế kiểm tra
     * này.
     */
    protected abstract void checkWrite();

    protected void beforeDelete(E entity) {}

    protected E findEntityById(Long id) {
        return repository.findById(id).orElseThrow(BusinessException::notFound);
    }

    @Override
    public List<R> getAll() {
        checkRead();
        return repository.findAll(Sort.by("id")).stream().map(this::toResponse).toList();
    }

    @Override
    public R getById(Long id) {
        checkRead();
        return toResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public R create(I input) {
        checkWrite();
        E entity = newEntity();
        apply(entity, input, true);
        R response = toResponse(repository.saveAndFlush(entity));
        recordCatalogAudit(entity, "CREATE", null, response);
        return response;
    }

    @Override
    @Transactional
    public R update(Long id, I input) {
        checkWrite();
        E entity = findEntityById(id);
        R before = toResponse(entity);
        apply(entity, input, false);
        R response = toResponse(repository.saveAndFlush(entity));
        recordCatalogAudit(entity, "UPDATE", before, response);
        return response;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        checkWrite();
        E entity = findEntityById(id);
        beforeDelete(entity);
        R before = toResponse(entity);
        repository.delete(entity);
        repository.flush();
        recordCatalogAudit(entity, "DELETE", before, null);
    }

    /** Nhật ký và dữ liệu danh mục cùng transaction: không có dữ liệu đổi mà thiếu nhật ký. */
    private void recordCatalogAudit(E entity, String action, Object before, Object after) {
        var table = entity.getClass().getAnnotation(Table.class);
        String entityType = table == null ? entity.getClass().getSimpleName() : table.name();
        auditService.record(
                currentUser.get().id(),
                "CATALOG_" + action,
                entityType.toUpperCase(Locale.ROOT),
                getEntityId(entity),
                before,
                after,
                null);
    }

    protected <T> T requireValue(T value, String field) {
        if (value == null) {
            throw BusinessException.invalid(field, "Trường bắt buộc");
        }
        return value;
    }

    protected String trimRequiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw BusinessException.invalid(field, "Trường bắt buộc");
        }
        return value.trim();
    }
}
