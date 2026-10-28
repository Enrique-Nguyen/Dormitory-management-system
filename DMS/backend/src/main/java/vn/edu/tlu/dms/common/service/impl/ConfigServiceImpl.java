package vn.edu.tlu.dms.common.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.domain.SystemConfig;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.repository.SystemConfigRepository;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.ConfigService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

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

    @Override
    public PageResponse<ConfigValue> searchByPage(SearchRequest input) {
        guard.requirePermission(Permission.CONFIG_READ);
        if (input.page() < 0
                || input.size() < 1
                || input.size() > 100
                || (long) input.page() * input.size() > Integer.MAX_VALUE) {
            throw BusinessException.invalid(ApiField.PAGE, ValidationMessage.PAGE_RANGE);
        }
        var orders = new ArrayList<Sort.Order>();
        if (input.sort() != null) {
            for (String item : input.sort()) {
                if (item == null) {
                    throw BusinessException.invalid(
                            ApiField.SORT, ValidationMessage.SORT_FIELD_INVALID);
                }
                String[] parts = item.split(",", -1);
                if (parts.length > 2 || !Set.of("key", "value", "description").contains(parts[0])) {
                    throw BusinessException.invalid(
                            ApiField.SORT, ValidationMessage.SORT_FIELD_INVALID);
                }
                String direction = parts.length == 1 ? "asc" : parts[1];
                if (!Set.of("asc", "desc").contains(direction)) {
                    throw BusinessException.invalid(
                            ApiField.SORT, ValidationMessage.SORT_DIRECTION_INVALID);
                }
                orders.add(new Sort.Order(Sort.Direction.fromString(direction), parts[0]));
            }
        }
        orders.add(Sort.Order.asc("key"));
        var pageable = PageRequest.of(input.page(), input.size(), Sort.by(orders));
        String keyword = "%" + input.q().trim().toLowerCase(Locale.ROOT) + "%";
        Specification<SystemConfig> specification =
                (row, query, builder) ->
                        builder.or(
                                builder.like(builder.lower(row.get("key")), keyword),
                                builder.like(builder.lower(row.get("description")), keyword),
                                builder.like(builder.lower(row.get("value")), keyword));
        return PageResponse.from(
                repository
                        .findAll(specification, pageable)
                        .map(
                                row ->
                                        new ConfigValue(
                                                row.getKey(),
                                                row.getValue(),
                                                row.getDescription())));
    }

    @Override()
    public int positiveInt(String key) {
        try {
            int value = Integer.parseInt(repository.findById(key)
                    .orElseThrow(() -> BusinessException.conflict(ErrorCode.CONFIG_VALUE_INVALID)).getValue());
            if (value < 1 || value > 3650) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException error) {
            throw BusinessException.conflict(ErrorCode.CONFIG_VALUE_INVALID);
        }
    }

    @Transactional
    @Override()
    public ConfigValue saveOrUpdate(String key, String value) {
        guard.requirePermission(Permission.CONFIG_UPDATE);
        if (key == null || key.isBlank()) {
            throw BusinessException.invalid(ApiField.KEY, ValidationMessage.CONFIG_KEY_EXISTING);
        }
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
