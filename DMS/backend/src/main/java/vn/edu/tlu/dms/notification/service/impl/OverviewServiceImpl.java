package vn.edu.tlu.dms.notification.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.notification.service.OverviewService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dữ liệu tra cứu theo phạm vi; không thay đổi số dư cọc, phiếu gửi đồ hoặc lịch sử chỗ ở. */
@Service
@Transactional(readOnly = true)
public class OverviewServiceImpl implements OverviewService {

    private static final Set<String> DEPOSIT_SORT_COLUMNS =
            Set.of(
                    "id",
                    "code",
                    "studentId",
                    "status",
                    "depositRequired",
                    "fullName",
                    "balance",
                    "topupNeeded");

    private static final Set<String> STORAGE_SORT_COLUMNS =
            Set.of(
                    "id",
                    "contractCode",
                    "fullName",
                    "packageCount",
                    "location",
                    "depositedAt",
                    "returnedAt");

    private static final Set<String> MY_ROOM_SORT_COLUMNS =
            Set.of(
                    "id",
                    "roomCode",
                    "bedCode",
                    "buildingCode",
                    "assignmentKind",
                    "priceMain",
                    "priceSummer");

    private final SqlPage queries;
    private final ScopeGuard guard;
    private final CurrentUser current;

    public OverviewServiceImpl(SqlPage queries, ScopeGuard guard, CurrentUser current) {
        this.queries = queries;
        this.guard = guard;
        this.current = current;
    }

    @Override
    public PageResponse<Map<String, Object>> searchDepositsByPage(
            SearchRequest input, boolean mine) {
        guard.requirePermission(Permission.DEPOSIT_READ);
        guard.requireRole(Role.STAFF, Role.STUDENT);
        var args = new ArrayList<Object>();
        String filter = contractScope(args, mine);
        args.add("%" + input.q() + "%");
        String sql =
                "SELECT c.id,c.code,c.student_id,c.status,c.deposit_required,u.full_name,"
                    + " coalesce(v.balance,0) AS balance,coalesce(v.topup_needed,0) AS topup_needed"
                    + " FROM dormitory_contract c JOIN room_application ap ON"
                    + " ap.id=c.application_id JOIN app_user u ON u.id=c.student_id LEFT JOIN"
                    + " v_deposit_balance v ON v.contract_id=c.id WHERE "
                        + filter
                        + " AND u.full_name ILIKE ?";
        return queries.search(sql, args, input, DEPOSIT_SORT_COLUMNS, "id,desc");
    }

    @Override
    public Map<String, Object> getDeposit(Long id) {
        guard.requirePermission(Permission.DEPOSIT_READ);
        guard.requireRole(Role.STAFF, Role.STUDENT);
        var args = new ArrayList<Object>();
        String filter = contractScope(args, false);
        args.add(id);
        var record =
                queries.one(
                        "SELECT c.id,c.code,c.deposit_required,coalesce(v.balance,0) AS balance"
                                + " FROM dormitory_contract c JOIN room_application ap ON"
                                + " ap.id=c.application_id LEFT JOIN v_deposit_balance v ON"
                                + " v.contract_id=c.id WHERE "
                                + filter
                                + " AND c.id=?",
                        args.toArray());
        record.put(
                "transactions",
                queries.list(
                        "SELECT id,type,amount,note,created_at FROM deposit_transaction WHERE"
                                + " contract_id=? ORDER BY id DESC",
                        id));
        return record;
    }

    @Override
    public PageResponse<Map<String, Object>> searchStorageByPage(
            SearchRequest input, boolean mine) {
        guard.requirePermission(Permission.STORAGE_READ);
        guard.requireRole(Role.STAFF, Role.STUDENT);
        var args = new ArrayList<Object>();
        String filter = contractScope(args, mine);
        args.add("%" + input.q() + "%");
        String sql =
                "SELECT s.*,c.code AS contract_code,u.full_name FROM storage_record s JOIN"
                        + " dormitory_contract c ON c.id=s.contract_id JOIN room_application ap ON"
                        + " ap.id=c.application_id JOIN app_user u ON u.id=c.student_id WHERE "
                        + filter
                        + " AND u.full_name ILIKE ?";
        return queries.search(sql, args, input, STORAGE_SORT_COLUMNS, "id,desc");
    }

    @Override
    public PageResponse<Map<String, Object>> searchMyRoomByPage(SearchRequest input) {
        guard.requirePermission(Permission.ROOM_READ);
        guard.requireRole(Role.STUDENT);
        String keyword = "%" + input.q() + "%";
        String sql =
                "SELECT room_id AS id,room_code,bed_code,building_code,assignment_kind,"
                        + " price_main,price_summer FROM v_current_stay WHERE student_id=?"
                        + " AND (room_code ILIKE ? OR bed_code ILIKE ? OR building_code ILIKE ?)";
        return queries.search(
                sql,
                List.of(current.get().id(), keyword, keyword, keyword),
                input,
                MY_ROOM_SORT_COLUMNS,
                "id,desc");
    }

    /** Mọi phạm vi đều do server xác định; cờ tổng hợp không được lấy từ body tìm kiếm. */
    private String contractScope(List<Object> args, boolean mine) {
        var actor = current.get();
        if (mine || actor.is(Role.STUDENT)) {
            args.add(actor.id());
            return "c.student_id=?";
        }
        if (actor.canReadAllUnits()) {
            return "TRUE";
        }
        args.add(actor.unitId());
        return "ap.preferred_unit_id=?";
    }
}
