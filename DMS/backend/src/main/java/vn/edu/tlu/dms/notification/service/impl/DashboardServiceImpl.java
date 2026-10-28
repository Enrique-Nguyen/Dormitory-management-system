package vn.edu.tlu.dms.notification.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.service.DebtQuery;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ConfigKey;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceStatus;
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
import vn.edu.tlu.dms.registration.service.RoomApplicationService;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.util.Set;

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
    private final RoomApplicationService applications;

    public DashboardServiceImpl(
            ScopeGuard guard,
            CurrentUser current,
            ConfigService config,
            RoomReferenceService rooms,
            DebtQuery debts,
            ContractService contracts,
            MaintenanceRequestService maintenance,
            RoomApplicationService applications) {
        this.guard = guard;
        this.current = current;
        this.config = config;
        this.rooms = rooms;
        this.debts = debts;
        this.contracts = contracts;
        this.maintenance = maintenance;
        this.applications = applications;
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
        var infrastructure = rooms.getDashboardInfrastructure(unitId);
        var registration = applications.getDashboardSummary(unitId);
        var residency = contracts.getDashboardSummary(unitId);
        var ticketsByStatus = maintenance.countByStatus(unitId);
        Set<String> pendingTicketStatuses =
                Set.of(
                        MaintenanceStatus.OPEN,
                        MaintenanceStatus.ASSIGNED,
                        MaintenanceStatus.IN_PROGRESS,
                        MaintenanceStatus.RESOLVED);
        long ticketsRequiringAction =
                ticketsByStatus.entrySet().stream()
                        .filter(entry -> pendingTicketStatuses.contains(entry.getKey()))
                        .mapToLong(entry -> entry.getValue())
                        .sum();
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
                        ticketsByStatus,
                        ticketsRequiringAction),
                new DashboardDtos.InfrastructureStats(
                        infrastructure.buildings(),
                        infrastructure.roomsByStatus().values().stream()
                                .mapToLong(Long::longValue)
                                .sum(),
                        infrastructure.roomsByStatus()),
                new DashboardDtos.RegistrationStats(
                        registration.applicationsByStatus().values().stream()
                                .mapToLong(Long::longValue)
                                .sum(),
                        registration
                                .applicationsByStatus()
                                .getOrDefault(ApplicationStatus.SUBMITTED, 0L),
                        registration.pendingSummerChoices(),
                        registration.applicationsByStatus()),
                new DashboardDtos.ResidencyStats(
                        residency.contractsByStatus().getOrDefault(ContractStatus.ACTIVE, 0L),
                        residency.contractsByStatus().getOrDefault(ContractStatus.PENDING, 0L),
                        residency.pendingExtensions(),
                        residency.pendingTransfers(),
                        residency.storageRecords(),
                        residency.storedPackages(),
                        residency.contractsByStatus()));
    }
}
