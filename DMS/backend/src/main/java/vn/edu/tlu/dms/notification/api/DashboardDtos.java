package vn.edu.tlu.dms.notification.api;

import java.util.Map;

/** DTO dữ liệu tổng hợp cho Staff Dashboard (FR-NOT-05, FR-NOT-06). */
public final class DashboardDtos {

    private DashboardDtos() {}

    public record StaffDashboardResponse(
            Long managementUnitId,
            String unitName,
            CapacityStats capacity,
            FinancialStats finance,
            OperationsStats operations,
            InfrastructureStats infrastructure,
            RegistrationStats registration,
            ResidencyStats residency) {}

    public record CapacityStats(
            long operationalBeds,
            long occupiedBeds,
            long availableBeds,
            long maintenanceBeds,
            double occupancyRate) {}

    public record FinancialStats(
            long totalUnpaidAmount,
            long unpaidRoomFeeAmount,
            long unpaidUtilityAmount,
            long totalTopupNeededAmount) {}

    public record OperationsStats(
            long roomsMissingLeader,
            long contractsExpiringSoon,
            long contractsOverdueMoveout,
            Map<String, Long> ticketsByStatus,
            long ticketsRequiringAction) {}

    public record InfrastructureStats(
            long buildings, long rooms, Map<String, Long> roomsByStatus) {}

    public record RegistrationStats(
            long totalApplications,
            long pendingApplications,
            long pendingSummerChoices,
            Map<String, Long> applicationsByStatus) {}

    public record ResidencyStats(
            long activeContracts,
            long pendingActivation,
            long pendingExtensions,
            long pendingTransfers,
            long storageRecords,
            long storedPackages,
            Map<String, Long> contractsByStatus) {}
}
