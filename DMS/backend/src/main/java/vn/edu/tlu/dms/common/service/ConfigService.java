package vn.edu.tlu.dms.common.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.repository.SystemConfigRepository;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConfigService {

    private final SystemConfigRepository repository;

    private final CurrentUser currentUser;

    private final ScopeGuard guard;

    private final AuditService audit;

    public ConfigService(
            SystemConfigRepository repository,
            CurrentUser currentUser,
            ScopeGuard guard,
            AuditService audit) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.guard = guard;
        this.audit = audit;
    }

    public record ConfigValue(String key, String value, String description) {}

    public List<ConfigValue> list() {
        guard.requireRole("ADMIN");
        return repository.findAll().stream()
                .map(r -> new ConfigValue(r.getKey(), r.getValue(), r.getDescription()))
                .toList();
    }

    public int positiveInt(String key) {
        return Integer.parseInt(
                repository.findById(key).orElseThrow(BusinessException::notFound).getValue());
    }

    @Transactional
    public ConfigValue update(String key, String value) {
        guard.requireRole("ADMIN");
        var row = repository.findById(key).orElseThrow(BusinessException::notFound);
        try {
            if (value == null || Integer.parseInt(value) < 1 || Integer.parseInt(value) > 3650) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new BusinessException("CONFIG_VALUE_INVALID", 400);
        }
        String before = row.getValue();
        row.setValue(value);
        row.setUpdatedBy(currentUser.get().id());
        row.setUpdatedAt(OffsetDateTime.now());
        repository.saveAndFlush(row);
        audit.record(currentUser.get().id(), "CONFIG_UPDATE", "CONFIG", null, before, value, null);
        return new ConfigValue(key, row.getValue(), row.getDescription());
    }
}
