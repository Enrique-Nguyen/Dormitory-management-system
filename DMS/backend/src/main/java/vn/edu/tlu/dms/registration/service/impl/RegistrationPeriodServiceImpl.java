package vn.edu.tlu.dms.registration.service.impl;

import jakarta.persistence.criteria.Predicate;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.RegistrationKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.PageQuery;
import vn.edu.tlu.dms.registration.api.RegistrationPeriodDtos.Input;
import vn.edu.tlu.dms.registration.api.RegistrationPeriodDtos.Quota;
import vn.edu.tlu.dms.registration.api.RegistrationPeriodDtos.Response;
import vn.edu.tlu.dms.registration.domain.RegistrationPeriod;
import vn.edu.tlu.dms.registration.domain.RegistrationPeriodCreatedEvent;
import vn.edu.tlu.dms.registration.domain.RegistrationPeriodUnit;
import vn.edu.tlu.dms.registration.domain.RegistrationQuota;
import vn.edu.tlu.dms.registration.repository.RegistrationPeriodRepository;
import vn.edu.tlu.dms.registration.repository.RegistrationPeriodUnitRepository;
import vn.edu.tlu.dms.registration.repository.RegistrationQuotaRepository;
import vn.edu.tlu.dms.registration.service.RegistrationPeriodService;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class RegistrationPeriodServiceImpl implements RegistrationPeriodService {

    private final RegistrationPeriodRepository periods;

    private final RegistrationPeriodUnitRepository units;

    private final RegistrationQuotaRepository quotas;

    private final RoomReferenceService rooms;

    private final AccountReferenceService accounts;

    private final CurrentUser current;

    private final ScopeGuard guard;

    private final JdbcTemplate jdbc;

    private final ApplicationEventPublisher events;

    private final AuditService audit;

    public RegistrationPeriodServiceImpl(
            RegistrationPeriodRepository periods,
            RegistrationPeriodUnitRepository units,
            RegistrationQuotaRepository quotas,
            RoomReferenceService rooms,
            AccountReferenceService accounts,
            CurrentUser current,
            ScopeGuard guard,
            JdbcTemplate jdbc,
            ApplicationEventPublisher events,
            AuditService audit) {
        this.periods = periods;
        this.units = units;
        this.quotas = quotas;
        this.rooms = rooms;
        this.accounts = accounts;
        this.current = current;
        this.guard = guard;
        this.jdbc = jdbc;
        this.events = events;
        this.audit = audit;
    }

    /**
     * Lọc phạm vi và từ khóa tại CSDL trước cả truy vấn dữ liệu và truy vấn đếm. Danh mục quản lý
     * mở cho STAFF và TECHNICIAN (được cấp REGISTRATION_PERIOD_READ trong ma trận quyền), cả hai
     * chỉ thấy đợt có đơn vị của mình.
     */
    @Override
    public PageResponse<Response> searchByPage(SearchRequest search, boolean openForStudent) {
        guard.requirePermission(Permission.REGISTRATION_PERIOD_READ);
        guard.requireRole(
                openForStudent
                        ? new String[] {Role.STUDENT}
                        : new String[] {Role.STAFF, Role.TECHNICIAN});
        var actor = current.get();
        var pageable =
                PageQuery.of(
                        search.page(),
                        search.size(),
                        search.sort(),
                        Set.of("id", "semesterId", "kind", "name", "openAt", "closeAt"),
                        "openAt,asc");
        OffsetDateTime now = OffsetDateTime.now();
        return PageResponse.from(
                periods.findAll(
                                (r, q, b) -> {
                                    var filters = new ArrayList<Predicate>();
                                    if (!openForStudent && !actor.canReadAllUnits()) {
                                        var sub = q.subquery(Long.class);
                                        var unit = sub.from(RegistrationPeriodUnit.class);
                                        sub.select(unit.get("periodId"))
                                                .where(
                                                        b.equal(
                                                                unit.get("managementUnitId"),
                                                                actor.unitId()));
                                        filters.add(r.get("id").in(sub));
                                    }
                                    if (search.semesterId() != null) {
                                        filters.add(
                                                b.equal(r.get("semesterId"), search.semesterId()));
                                    }
                                    if (search.kind() != null) {
                                        filters.add(b.equal(r.get("kind"), search.kind()));
                                    }
                                    if (!search.q().isBlank()) {
                                        filters.add(
                                                b.like(
                                                        b.lower(r.get("name")),
                                                        "%"
                                                                + search.q()
                                                                        .trim()
                                                                        .toLowerCase(Locale.ROOT)
                                                                + "%"));
                                    }
                                    if (search.name() != null && !search.name().isBlank()) {
                                        filters.add(b.greaterThan(b.locate(b.lower(r.get("name")),
                                                search.name().trim().toLowerCase(Locale.ROOT)), 0));
                                    }
                                    if (search.openAtFrom() != null) {
                                        filters.add(b.greaterThanOrEqualTo(r.get("openAt"), search.openAtFrom()));
                                    }
                                    if (search.openAtTo() != null) {
                                        filters.add(b.lessThanOrEqualTo(r.get("openAt"), search.openAtTo()));
                                    }
                                    if (search.closeAtFrom() != null) {
                                        filters.add(b.greaterThanOrEqualTo(r.get("closeAt"), search.closeAtFrom()));
                                    }
                                    if (search.closeAtTo() != null) {
                                        filters.add(b.lessThanOrEqualTo(r.get("closeAt"), search.closeAtTo()));
                                    }
                                    var isOpen =
                                            b.and(
                                                    b.lessThanOrEqualTo(r.get("openAt"), now),
                                                    b.greaterThan(r.get("closeAt"), now));
                                    if (openForStudent) {
                                        // Lookup cá nhân không thể mở rộng bằng open=false do
                                        // client gửi.
                                        filters.add(isOpen);
                                    } else if (search.open() != null) {
                                        filters.add(search.open() ? isOpen : b.not(isOpen));
                                    }
                                    return b.and(filters.toArray(Predicate[]::new));
                                },
                                pageable)
                        .map(this::toPeriodResponse));
    }

    @Override()
    public Response getRegistrationPeriodById(Long id) {
        guard.requirePermission(Permission.REGISTRATION_PERIOD_READ);
        guard.requireRole(Role.STAFF, Role.TECHNICIAN);
        var period = periods.findById(id).orElseThrow(BusinessException::notFound);
        checkPeriodScope(id);
        return toPeriodResponse(period);
    }

    @Override
    @Transactional
    public Response saveOrUpdate(Input input) {
        Long id = input.id();
        if (id != null && id <= 0) {
            throw BusinessException.invalid(ApiField.ID, ValidationMessage.ID_POSITIVE);
        }
        return id == null ? createRegistrationPeriod(input) : updateRegistrationPeriod(id, input);
    }

    /** Ghi đợt, đơn vị áp dụng và chỉ tiêu trong cùng transaction. */
    private Response createRegistrationPeriod(Input input) {
        guard.requirePermission(Permission.REGISTRATION_PERIOD_CREATE);
        var period = new RegistrationPeriod();
        period.setCreatedBy(current.get().id());
        applyPeriodInput(period, input, true);
        periods.saveAndFlush(period);
        replacePeriodChildren(period.getId(), input);
        var created = toPeriodResponse(period);
        audit.record(
                current.get().id(),
                AuditAction.REGISTRATION_PERIOD_CREATE,
                EntityType.REGISTRATION_PERIOD,
                period.getId(),
                null,
                created,
                null);
        events.publishEvent(
                new RegistrationPeriodCreatedEvent(
                        period.getId(),
                        period.getName(),
                        period.getKind(),
                        input.unitIds(),
                        current.get().id()));
        return created;
    }

    /**
     * Đợt đã mở chỉ được gia hạn closeAt; khóa bản ghi rồi kiểm tra lại để tránh hai request ghi
     * đè.
     */
    private Response updateRegistrationPeriod(Long id, Input input) {
        guard.requirePermission(Permission.REGISTRATION_PERIOD_UPDATE);
        jdbc.query(
                "SELECT id FROM registration_period WHERE id=? FOR UPDATE",
                (r, i) -> r.getLong(1),
                id);
        var period = periods.findById(id).orElseThrow(BusinessException::notFound);
        checkPeriodWriteScope(id);
        var before = toPeriodResponse(period);
        if (!period.getOpenAt().isAfter(OffsetDateTime.now())) {
            if (input.semesterId() != null
                    || input.kind() != null
                    || input.name() != null
                    || input.openAt() != null
                    || input.unitIds() != null
                    || input.quotas() != null) {
                throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
            }
            if (input.closeAt() == null || !input.closeAt().isAfter(period.getCloseAt())) {
                throw BusinessException.invalid(ApiField.CLOSE_AT, ValidationMessage.PERIOD_EXTEND_CLOSE_ONLY);
            }
            period.setCloseAt(input.closeAt());
        } else {
            applyPeriodInput(period, input, false);
            replacePeriodChildren(id, input);
        }
        periods.saveAndFlush(period);
        var after = toPeriodResponse(period);
        audit.record(
                current.get().id(),
                AuditAction.REGISTRATION_PERIOD_UPDATE,
                EntityType.REGISTRATION_PERIOD,
                id,
                before,
                after,
                null);
        return after;
    }

    @Transactional
    @Override()
    public void deleteUnusedPeriod(Long id) {
        guard.requirePermission(Permission.REGISTRATION_PERIOD_DELETE);
        var period = periods.findById(id).orElseThrow(BusinessException::notFound);
        jdbc.queryForObject(
                "SELECT id FROM registration_period WHERE id=? FOR UPDATE", Long.class, id);
        checkPeriodWriteScope(id);
        if (!period.getOpenAt().isAfter(OffsetDateTime.now())
                || Boolean.TRUE.equals(
                        jdbc.queryForObject(
                                "SELECT EXISTS(SELECT 1 FROM room_application WHERE period_id=?)",
                                Boolean.class,
                                id))) {
            throw BusinessException.conflict(ErrorCode.RESOURCE_IN_USE);
        }
        var before = toPeriodResponse(period);
        quotas.deleteAll(quotas.findAll((r, q, b) -> b.equal(r.get("periodId"), id)));
        quotas.flush();
        units.deleteAll(units.findAll((r, q, b) -> b.equal(r.get("periodId"), id)));
        units.flush();
        periods.deleteById(id);
        periods.flush();
        audit.record(
                current.get().id(),
                AuditAction.REGISTRATION_PERIOD_DELETE,
                EntityType.REGISTRATION_PERIOD,
                id,
                before,
                null,
                null);
    }

    private void applyPeriodInput(RegistrationPeriod p, Input input, boolean creating) {
        if (creating
                && (input.semesterId() == null
                        || input.kind() == null
                        || input.name() == null
                        || input.openAt() == null
                        || input.closeAt() == null
                        || input.unitIds() == null)) {
            throw BusinessException.invalid(ApiField.PERIOD, ValidationMessage.PERIOD_INPUT_REQUIRED);
        }
        if (input.semesterId() != null) {
            p.setSemesterId(input.semesterId());
        }
        if (input.kind() != null) {
            p.setKind(input.kind());
        }
        if (input.name() != null) {
            if (input.name().isBlank()) {
                throw BusinessException.invalid(ApiField.NAME, ValidationMessage.NAME_REQUIRED);
            }
            p.setName(input.name().trim());
        }
        if (input.openAt() != null) {
            p.setOpenAt(input.openAt());
        }
        if (input.closeAt() != null) {
            p.setCloseAt(input.closeAt());
        }
        if (!p.getCloseAt().isAfter(p.getOpenAt())) {
            throw BusinessException.invalid(ApiField.CLOSE_AT, ValidationMessage.CLOSE_AFTER_OPEN);
        }
        var semester = rooms.getSemesterReference(p.getSemesterId());
        if (p.getKind().equals(RegistrationKind.SUMMER)
                && !semester.type().equals(SemesterType.SUMMER)) {
            throw BusinessException.invalid(ApiField.SEMESTER_ID, ValidationMessage.SUMMER_PERIOD_SEMESTER);
        }
    }

    private void replacePeriodChildren(Long id, Input input) {
        List<Long> chosenUnits = input.unitIds() == null ? periodUnitIds(id) : input.unitIds();
        if (chosenUnits.isEmpty() || new HashSet<>(chosenUnits).size() != chosenUnits.size()) {
            throw BusinessException.invalid(
                    ApiField.UNIT_IDS, ValidationMessage.UNIT_IDS_INVALID);
        }
        for (Long unitId : chosenUnits) {
            accounts.requireUnit(unitId);
            var actor = current.get();
            if (!actor.isAdmin() && !actor.crossUnitAccess() && !unitId.equals(actor.unitId())) {
                throw BusinessException.forbidden();
            }
        }
        String kind = periods.findById(id).orElseThrow(BusinessException::notFound).getKind();
        var chosenQuotas =
                input.quotas() == null
                        ? quotas.findAll((r, q, b) -> b.equal(r.get("periodId"), id)).stream()
                                .map(r -> new Quota(r.getBuildingId(), r.getQuota()))
                                .toList()
                        : input.quotas();
        if (kind.equals(RegistrationKind.SUMMER) && !chosenQuotas.isEmpty()) {
            throw BusinessException.invalid(ApiField.QUOTAS, ValidationMessage.SUMMER_NO_QUOTA);
        }
        Set<Long> seen = new HashSet<>();
        for (var quota : chosenQuotas) {
            if (!seen.add(quota.buildingId())
                    || !chosenUnits.contains(
                            rooms.getBuildingManagementUnitId(quota.buildingId()))) {
                throw BusinessException.invalid(
                        ApiField.QUOTAS, ValidationMessage.QUOTA_BUILDING_INVALID);
            }
        }
        if (input.unitIds() != null) {
            units.deleteAll(units.findAll((r, q, b) -> b.equal(r.get("periodId"), id)));
            units.flush();
            for (Long unitId : chosenUnits) {
                var row = new RegistrationPeriodUnit();
                row.setPeriodId(id);
                row.setManagementUnitId(unitId);
                units.save(row);
            }
            units.flush();
        }
        if (input.quotas() != null) {
            quotas.deleteAll(quotas.findAll((r, q, b) -> b.equal(r.get("periodId"), id)));
            quotas.flush();
            for (var inputQuota : chosenQuotas) {
                var row = new RegistrationQuota();
                row.setPeriodId(id);
                row.setBuildingId(inputQuota.buildingId());
                row.setQuota(inputQuota.quota());
                quotas.save(row);
            }
            quotas.flush();
        }
    }

    private List<Long> periodUnitIds(Long id) {
        return units.findAll((r, q, b) -> b.equal(r.get("periodId"), id)).stream()
                .map(RegistrationPeriodUnit::getManagementUnitId)
                .toList();
    }

    private void checkPeriodScope(Long id) {
        if (!current.get().canReadAllUnits()
                && !periodUnitIds(id).contains(current.get().unitId())) {
            throw BusinessException.notFound();
        }
    }

    /** ADMIN ghi mọi đơn vị; cán bộ tổng hợp vẫn phải có đơn vị của mình trong đợt cần sửa. */
    private void checkPeriodWriteScope(Long id) {
        var actor = current.get();
        if (actor.isAdmin()) {
            return;
        }
        var unitIds = periodUnitIds(id);
        if (!unitIds.contains(actor.unitId()) || (!actor.crossUnitAccess() && unitIds.size() > 1)) {
            throw BusinessException.notFound();
        }
    }

    private Response toPeriodResponse(RegistrationPeriod p) {
        return new Response(
                p.getId(),
                p.getSemesterId(),
                p.getKind(),
                p.getName(),
                p.getOpenAt(),
                p.getCloseAt(),
                periodUnitIds(p.getId()),
                quotas.findAll((r, q, b) -> b.equal(r.get("periodId"), p.getId())).stream()
                        .map(r -> new Quota(r.getBuildingId(), r.getQuota()))
                        .toList());
    }
}
