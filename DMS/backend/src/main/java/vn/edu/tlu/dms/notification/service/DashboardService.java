package vn.edu.tlu.dms.notification.service;

import vn.edu.tlu.dms.notification.api.DashboardDtos;

/** Dịch vụ tổng hợp chỉ số KPI cho Staff Dashboard (FR-NOT-05, FR-NOT-06). */
public interface DashboardService {

    /** Tổng hợp công suất phòng, công nợ và vận hành theo đơn vị quản lý của cán bộ. */
    DashboardDtos.StaffDashboardResponse getSummary(Long managementUnitId);
}
