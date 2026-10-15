package vn.edu.tlu.dms.common.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.repository.SystemConfigRepository;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.ConfigService;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConfigServiceImpl implements ConfigService {

    private final SystemConfigRepository repository;

    private final CurrentUser currentUser;

    private final ScopeGuard guard;

    private final AuditService audit;

    public ConfigServiceImpl(
            SystemConfigRepository repository,
            CurrentUser currentUser,
            ScopeGuard guard,
            AuditService audit) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.guard = guard;
        this.audit = audit;
    }

    @Override()
    public List<ConfigValue> list() {
        guard.requirePermission(Permission.CONFIG_READ);
        return repository.findAll().stream()
                .map(r -> new ConfigValue(r.getKey(), r.getValue(), r.getDescription()))
                .toList();
    }

    @Override()
    public int positiveInt(String key) {
        return Integer.parseInt(
                repository.findById(key).orElseThrow(BusinessException::notFound).getValue());
    }

    @Transactional
    @Override()
    public ConfigValue update(String key, String value) {
        guard.requirePermission(Permission.CONFIG_UPDATE);
        var row = repository.findById(key).orElseThrow(BusinessException::notFound);
        try {
            if (value == null || Integer.parseInt(value) < 1 || Integer.parseInt(value) > 3650) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.CONFIG_VALUE_INVALID, HttpStatus.BAD_REQUEST);
        }
        String before = row.getValue();
        row.setValue(value);
        row.setUpdatedBy(currentUser.get().id());
        row.setUpdatedAt(OffsetDateTime.now());
        repository.saveAndFlush(row);
        audit.record(
                currentUser.get().id(),
                AuditAction.CONFIG_UPDATE,
                "CONFIG",
                null,
                before,
                value,
                null);
        return new ConfigValue(key, row.getValue(), row.getDescription());
    }
}
