package vn.edu.tlu.dms.registration.service.impl;

import jakarta.persistence.criteria.Predicate;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.RegistrationKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
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

    public RegistrationPeriodServiceImpl(
            RegistrationPeriodRepository periods,
            RegistrationPeriodUnitRepository units,
            RegistrationQuotaRepository quotas,
            RoomReferenceService rooms,
            AccountReferenceService accounts,
            CurrentUser current,
            ScopeGuard guard,
            JdbcTemplate jdbc,
            ApplicationEventPublisher events) {
        this.periods = periods;
        this.units = units;
        this.quotas = quotas;
        this.rooms = rooms;
        this.accounts = accounts;
        this.current = current;
        this.guard = guard;
        this.jdbc = jdbc;
        this.events = events;
    }

    /**
     * Lọc phạm vi trong truy vấn trước khi trả danh sách, không nhận đơn vị từ client để mở rộng
     * quyền.
     */
    @Override()
    public List<Response> getRegistrationPeriods(Long semesterId, String kind, Boolean open) {
        guard.requirePermission(Permission.REGISTRATION_PERIOD_READ);
        guard.requireRole(Role.STAFF);
        var actor = current.get();
        OffsetDateTime now = OffsetDateTime.now();
        return periods
                .findAll(
                        (r, q, b) -> {
                            var filters = new ArrayList<Predicate>();
                            if (!actor.canReadAllUnits()) {
                                var sub = q.subquery(Long.class);
                                var unit = sub.from(RegistrationPeriodUnit.class);
                                sub.select(unit.get("periodId"))
                                        .where(
                                                b.equal(
                                                        unit.get("managementUnitId"),
                                                        actor.unitId()));
                                filters.add(r.get("id").in(sub));
                            }
                            if (semesterId != null) {
                                filters.add(b.equal(r.get("semesterId"), semesterId));
                            }
                            if (kind != null) {
                                filters.add(b.equal(r.get("kind"), kind));
                            }
                            if (open != null) {
                                var isOpen =
                                        b.and(
                                                b.lessThanOrEqualTo(r.get("openAt"), now),
                                                b.greaterThan(r.get("closeAt"), now));
                                filters.add(open ? isOpen : b.not(isOpen));
                            }
                            return b.and(filters.toArray(Predicate[]::new));
                        },
                        Sort.by("openAt", "id"))
                .stream()
                .map(this::toPeriodResponse)
                .toList();
    }

    @Override()
    public Response getRegistrationPeriodById(Long id) {
        guard.requirePermission(Permission.REGISTRATION_PERIOD_READ);
        guard.requireRole(Role.STAFF);
        var period = periods.findById(id).orElseThrow(BusinessException::notFound);
        checkPeriodScope(id);
        return toPeriodResponse(period);
    }

    /** Ghi đợt, đơn vị áp dụng và chỉ tiêu trong cùng transaction. */
    @Transactional
    @Override()
    public Response createRegistrationPeriod(Input input) {
        guard.requirePermission(Permission.REGISTRATION_PERIOD_CREATE);
        var period = new RegistrationPeriod();
        period.setCreatedBy(current.get().id());
        applyPeriodInput(period, input, true);
        periods.saveAndFlush(period);
        replacePeriodChildren(period.getId(), input);
        events.publishEvent(
                new RegistrationPeriodCreatedEvent(
                        period.getId(),
                        period.getName(),
                        period.getKind(),
                        input.unitIds(),
                        current.get().id()));
        return toPeriodResponse(period);
    }

    /**
     * Đợt đã mở chỉ được gia hạn closeAt; khóa bản ghi rồi kiểm tra lại để tránh hai request ghi
     * đè.
     */
    @Transactional
    @Override()
    public Response updateRegistrationPeriod(Long id, Input input) {
        guard.requirePermission(Permission.REGISTRATION_PERIOD_UPDATE);
        jdbc.query(
                "SELECT id FROM registration_period WHERE id=? FOR UPDATE",
                (r, i) -> r.getLong(1),
                id);
        var period = periods.findById(id).orElseThrow(BusinessException::notFound);
        checkPeriodWriteScope(id);
        if (!period.getOpenAt().isAfter(OffsetDateTime.now())) {
            if (input.semesterId() != null
                    || input.kind() != null
                    || input.name() != null
                    || input.openAt() != null
                    || input.unitIds() != null
                    || input.quotas() != null) {
                throw BusinessException.conflict("INVALID_STATE_TRANSITION");
            }
            if (input.closeAt() == null || !input.closeAt().isAfter(period.getCloseAt())) {
                throw BusinessException.invalid("closeAt", "Chỉ được gia hạn ngày đóng");
            }
            period.setCloseAt(input.closeAt());
        } else {
            applyPeriodInput(period, input, false);
            replacePeriodChildren(id, input);
        }
        periods.saveAndFlush(period);
        return toPeriodResponse(period);
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
            throw BusinessException.conflict("RESOURCE_IN_USE");
        }
        quotas.deleteAll(quotas.findAll((r, q, b) -> b.equal(r.get("periodId"), id)));
        quotas.flush();
        units.deleteAll(units.findAll((r, q, b) -> b.equal(r.get("periodId"), id)));
        units.flush();
        periods.deleteById(id);
        periods.flush();
    }

    @Override()
    public List<Response> getOpenPeriodsForStudent() {
        guard.requirePermission(Permission.REGISTRATION_PERIOD_READ);
        guard.requireRole(Role.STUDENT);
        var now = OffsetDateTime.now();
        return periods
                .findAll(
                        (r, q, b) ->
                                b.and(
                                        b.lessThanOrEqualTo(r.get("openAt"), now),
                                        b.greaterThan(r.get("closeAt"), now)))
                .stream()
                .map(this::toPeriodResponse)
                .toList();
    }

    private void applyPeriodInput(RegistrationPeriod p, Input input, boolean creating) {
        if (creating
                && (input.semesterId() == null
                        || input.kind() == null
                        || input.name() == null
                        || input.openAt() == null
                        || input.closeAt() == null
                        || input.unitIds() == null)) {
            throw BusinessException.invalid("period", "Thiếu trường bắt buộc của đợt đăng ký");
        }
        if (input.semesterId() != null) {
            p.setSemesterId(input.semesterId());
        }
        if (input.kind() != null) {
            p.setKind(input.kind());
        }
        if (input.name() != null) {
            if (input.name().isBlank()) {
                throw BusinessException.invalid("name", "Không để trống tên");
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
            throw BusinessException.invalid("closeAt", "Ngày đóng phải sau ngày mở");
        }
        var semester = rooms.getSemesterReference(p.getSemesterId());
        if (p.getKind().equals(RegistrationKind.SUMMER)
                && !semester.type().equals(SemesterType.SUMMER)) {
            throw BusinessException.invalid("semesterId", "Đợt hè phải dùng học kỳ hè");
        }
    }

    private void replacePeriodChildren(Long id, Input input) {
        List<Long> chosenUnits = input.unitIds() == null ? periodUnitIds(id) : input.unitIds();
        if (chosenUnits.isEmpty() || new HashSet<>(chosenUnits).size() != chosenUnits.size()) {
            throw BusinessException.invalid(
                    "unitIds", "Danh sách đơn vị phải khác rỗng và không trùng");
        }
        for (Long unitId : chosenUnits) {
            accounts.requireUnit(unitId);
            var actor = current.get();
            if (!actor.isAdmin() && !actor.crossUnitAccess() && !unitId.equals(actor.unitId())) {
                throw new BusinessException("FORBIDDEN", 403);
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
            throw BusinessException.invalid("quotas", "Đợt hè không có chỉ tiêu");
        }
        Set<Long> seen = new HashSet<>();
        for (var quota : chosenQuotas) {
            if (!seen.add(quota.buildingId())
                    || !chosenUnits.contains(
                            rooms.getBuildingManagementUnitId(quota.buildingId()))) {
                throw BusinessException.invalid(
                        "quotas", "Tòa phải thuộc đơn vị áp dụng và không được trùng");
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
