package vn.edu.tlu.dms.notification.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.service.DebtQuery;
import vn.edu.tlu.dms.common.constants.DmsConstants.ConfigKey;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.ConfigService;
import vn.edu.tlu.dms.contract.service.ContractService;
import vn.edu.tlu.dms.maintenance.service.MaintenanceRequestService;
import vn.edu.tlu.dms.notification.api.DashboardDtos;
import vn.edu.tlu.dms.notification.service.DashboardService;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

/** Tổng hợp Dashboard qua Service công khai của từng module theo SAD 4.2. */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {
    private final ScopeGuard guard;
    private final CurrentUser current;
    private final ConfigService config;
    private final RoomReferenceService rooms;
    private final DebtQuery debts;
    private final ContractService contracts;
    private final MaintenanceRequestService maintenance;

    public DashboardServiceImpl(
            ScopeGuard guard,
            CurrentUser current,
            ConfigService config,
            RoomReferenceService rooms,
            DebtQuery debts,
            ContractService contracts,
            MaintenanceRequestService maintenance) {
        this.guard = guard;
        this.current = current;
        this.config = config;
        this.rooms = rooms;
        this.debts = debts;
        this.contracts = contracts;
        this.maintenance = maintenance;
    }

    /** Xác định phạm vi phía server rồi ghép số liệu; quyền tổng hợp chỉ áp dụng cho đọc. */
    @Override
    public DashboardDtos.StaffDashboardResponse getSummary(Long managementUnitId) {
        guard.requirePermission(Permission.DASHBOARD_READ);
        guard.requireRole(Role.STAFF, Role.ADMIN);
        var actor = current.get();
        Long unitId = actor.canReadAllUnits() ? managementUnitId : actor.unitId();
        if (!actor.canReadAllUnits()
                && (unitId == null
                        || (managementUnitId != null && !managementUnitId.equals(unitId)))) {
            throw BusinessException.notFound();
        }
        var capacity = rooms.getDashboardCapacity(unitId);
        var finance = debts.getFinanceSummary(unitId);
        var expiry =
                contracts.getExpirySummary(
                        unitId, config.positiveInt(ConfigKey.CONTRACT_EXPIRING_DAYS));
        double occupancyRate =
                capacity.operationalBeds() == 0
                        ? 0
                        : Math.round(capacity.occupiedBeds() * 1000.0 / capacity.operationalBeds())
                                / 10.0;
        return new DashboardDtos.StaffDashboardResponse(
                unitId,
                capacity.unitName(),
                new DashboardDtos.CapacityStats(
                        capacity.operationalBeds(),
                        capacity.occupiedBeds(),
                        capacity.availableBeds(),
                        capacity.maintenanceBeds(),
                        occupancyRate),
                new DashboardDtos.FinancialStats(
                        finance.unpaidRoomFee() + finance.unpaidUtility(),
                        finance.unpaidRoomFee(),
                        finance.unpaidUtility(),
                        finance.topupNeeded()),
                new DashboardDtos.OperationsStats(
                        capacity.roomsMissingLeader(),
                        expiry.expiringSoon(),
                        expiry.overdueMoveout(),
                        maintenance.countByStatus(unitId)));
    }
}
