package vn.edu.tlu.dms.notification.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.notification.service.DashboardService;

/**
 * Controller API phục vụ màn hình Staff Dashboard (FR-NOT-05, FR-NOT-06).
 */
@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @RequirePrivilege(Permission.DASHBOARD_READ)
    @GetMapping("/api/staff/dashboard")
    public DashboardDtos.StaffDashboardResponse getStaffDashboard(
            @RequestParam(name = "managementUnitId", required = false) Long managementUnitId) {
        return dashboardService.getSummary(managementUnitId);
    }
}
