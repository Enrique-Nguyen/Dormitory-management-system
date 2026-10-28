package vn.edu.tlu.dms.billing.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.api.UtilityRateDtos;
import vn.edu.tlu.dms.billing.domain.UtilityRate;
import vn.edu.tlu.dms.billing.repository.UtilityRateRepository;
import vn.edu.tlu.dms.billing.service.UtilityRateService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.MeterType;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.SqlPage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/** Giữ lịch sử giá; chỉ phiên bản tương lai chưa hiệu lực mới được cập nhật. */
@Service
@Transactional(readOnly = true)
public class UtilityRateServiceImpl implements UtilityRateService {
    private final UtilityRateRepository repository;
    private final CurrentUser currentUser;
    private final ScopeGuard scopeGuard;
    private final JdbcTemplate jdbc;
    private final SqlPage queries;
    private final AuditService audit;

    public UtilityRateServiceImpl(
            UtilityRateRepository repository,
            CurrentUser currentUser,
            ScopeGuard scopeGuard,
            JdbcTemplate jdbc,
            SqlPage queries,
            AuditService audit) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.scopeGuard = scopeGuard;
        this.jdbc = jdbc;
        this.queries = queries;
        this.audit = audit;
    }

    @Override
    public PageResponse<UtilityRateDtos.Response> searchByPage(SearchRequest input) {
        scopeGuard.requirePermission(Permission.UTILITY_RATE_READ);
        scopeGuard.requireRole(Role.STAFF);
        String meterType = input.meterType();
        if (meterType != null && !meterType.matches(MeterType.PATTERN)) {
            throw BusinessException.invalid(ApiField.METER_TYPE, ValidationMessage.METER_TYPE_INVALID);
        }
        String keyword = "%" + input.q().trim() + "%";
        var args = new ArrayList<Object>();
        args.add(keyword);
        args.add(keyword);
        args.add(keyword);
        String sql =
                "SELECT id,meter_type,unit_price,effective_from FROM utility_rate WHERE (meter_type"
                        + " ILIKE ? OR unit_price::text ILIKE ? OR effective_from::text ILIKE ?)";
        if (meterType != null) {
            sql += " AND meter_type=?";
            args.add(meterType);
        }
        var page =
                queries.search(
                        sql,
                        args,
                        input,
                        Set.of("id", "meterType", "unitPrice", "effectiveFrom"),
                        "effectiveFrom,desc");
        return new PageResponse<>(
                page.content().stream().map(this::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    @Override
    @Transactional
    public UtilityRateDtos.Response saveOrUpdate(UtilityRateDtos.Save input) {
        if (input == null) {
            throw BusinessException.invalid(ApiField.INPUT, ValidationMessage.RATE_INPUT_REQUIRED);
        }
        scopeGuard.requirePermission(
                input.id() == null
                        ? Permission.UTILITY_RATE_CREATE
                        : Permission.UTILITY_RATE_UPDATE);
        scopeGuard.requireRole(Role.STAFF);
        if (input.id() == null) {
            validateValues(input.meterType(), input.unitPrice(), input.effectiveFrom());
            var rate = new UtilityRate();
            rate.setMeterType(input.meterType());
            rate.setUnitPrice(input.unitPrice());
            rate.setEffectiveFrom(input.effectiveFrom());
            rate.setCreatedBy(currentUser.get().id());
            var saved = repository.saveAndFlush(rate);
            audit.record(
                    currentUser.get().id(),
                    AuditAction.RATE_CREATE,
                    EntityType.UTILITY_RATE,
                    saved.getId(),
                    null,
                    input,
                    null);
            return new UtilityRateDtos.Response(
                    saved.getId(),
                    saved.getMeterType(),
                    saved.getUnitPrice(),
                    saved.getEffectiveFrom());
        }
        if (input.id() <= 0) {
            throw BusinessException.invalid(ApiField.ID, ValidationMessage.ID_POSITIVE);
        }
        // Khóa trước khi kiểm người tạo/ngày hiệu lực để tránh ghi đè phiên bản đang đổi.
        var before = queries.one("SELECT * FROM utility_rate WHERE id=? FOR UPDATE", input.id());
        if (!currentUser.get().isAdmin()
                && !currentUser.get().id().equals(before.get("createdBy"))) {
            throw BusinessException.notFound();
        }
        if (!((LocalDate) before.get("effectiveFrom")).isAfter(LocalDate.now())) {
            throw BusinessException.conflict(ErrorCode.RATE_ALREADY_EFFECTIVE);
        }
        String meterType =
                input.meterType() == null ? (String) before.get("meterType") : input.meterType();
        Long unitPrice =
                input.unitPrice() == null
                        ? ((Number) before.get("unitPrice")).longValue()
                        : input.unitPrice();
        LocalDate effectiveFrom =
                input.effectiveFrom() == null
                        ? (LocalDate) before.get("effectiveFrom")
                        : input.effectiveFrom();
        validateValues(meterType, unitPrice, effectiveFrom);
        if (!effectiveFrom.isAfter(LocalDate.now())) {
            throw BusinessException.conflict(ErrorCode.RATE_ALREADY_EFFECTIVE);
        }
        jdbc.update(
                "UPDATE utility_rate SET meter_type=?,unit_price=?,effective_from=? WHERE id=?",
                meterType,
                unitPrice,
                effectiveFrom,
                input.id());
        var after =
                queries.one(
                        "SELECT id,meter_type,unit_price,effective_from FROM utility_rate WHERE"
                                + " id=?",
                        input.id());
        audit.record(
                currentUser.get().id(),
                AuditAction.RATE_UPDATE,
                EntityType.UTILITY_RATE,
                input.id(),
                before,
                after,
                null);
        return toResponse(after);
    }

    private void validateValues(String meterType, Long unitPrice, LocalDate effectiveFrom) {
        if (meterType == null || !meterType.matches(MeterType.PATTERN)) {
            throw BusinessException.invalid(ApiField.METER_TYPE, ValidationMessage.METER_TYPE_REQUIRED);
        }
        if (unitPrice == null || unitPrice <= 0) {
            throw BusinessException.invalid(ApiField.UNIT_PRICE, ValidationMessage.UNIT_PRICE_POSITIVE);
        }
        if (effectiveFrom == null) {
            throw BusinessException.invalid(ApiField.EFFECTIVE_FROM, ValidationMessage.EFFECTIVE_FROM_REQUIRED);
        }
    }

    private UtilityRateDtos.Response toResponse(Map<String, Object> row) {
        return new UtilityRateDtos.Response(
                ((Number) row.get("id")).longValue(),
                (String) row.get("meterType"),
                ((Number) row.get("unitPrice")).longValue(),
                (LocalDate) row.get("effectiveFrom"));
    }
}
