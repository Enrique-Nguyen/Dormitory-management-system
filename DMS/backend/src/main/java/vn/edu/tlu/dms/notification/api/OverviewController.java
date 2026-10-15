package vn.edu.tlu.dms.notification.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.support.SqlPage;

import java.util.ArrayList;
import java.util.List;

/** Chỉ cung cấp dữ liệu tra cứu cho khung; báo cáo và xử lý tài chính chi tiết để bổ sung sau. */
@RestController
public class OverviewController {

    private final SqlPage queries;

    private final ScopeGuard guard;

    private final CurrentUser current;

    public OverviewController(SqlPage queries, ScopeGuard guard, CurrentUser current) {
        this.queries = queries;
        this.guard = guard;
        this.current = current;
    }

    private String scope(List<Object> args) {
        if (current.get().is(Role.STUDENT)) {
            args.add(current.get().id());
            return "c.student_id=?";
        }
        if (current.get().canReadAllUnits()) {
            return "TRUE";
        }
        args.add(current.get().unitId());
        return "ap.preferred_unit_id=?";
    }

    @RequirePrivilege(Permission.DEPOSIT_READ)
    @GetMapping({"/api/deposits", "/api/me/deposits"})
    public Object searchDeposits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q) {
        guard.requirePermission(Permission.DEPOSIT_READ);
        guard.requireRole(Role.STAFF, Role.STUDENT);
        var args = new ArrayList<Object>();
        String filter = scope(args);
        args.add("%" + q + "%");
        return queries.search(
                "SELECT"
                    + " c.id,c.code,c.student_id,c.status,c.deposit_required,u.full_name,coalesce(v.balance,0)"
                    + " AS balance,coalesce(v.topup_needed,0) AS topup_needed FROM"
                    + " dormitory_contract c JOIN room_application ap ON ap.id=c.application_id"
                    + " JOIN app_user u ON u.id=c.student_id LEFT JOIN v_deposit_balance v ON"
                    + " v.contract_id=c.id WHERE "
                        + filter
                        + " AND u.full_name ILIKE ?",
                args,
                page,
                size);
    }

    @RequirePrivilege(Permission.DEPOSIT_READ)
    @GetMapping("/api/contracts/{id}/deposit")
    public Object getDeposit(@PathVariable Long id) {
        guard.requirePermission(Permission.DEPOSIT_READ);
        guard.requireRole(Role.STAFF, Role.STUDENT);
        var args = new ArrayList<Object>();
        String filter = scope(args);
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

    @RequirePrivilege(Permission.STORAGE_READ)
    @GetMapping({"/api/storage-records", "/api/me/storage-records"})
    public Object searchStorage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q) {
        guard.requirePermission(Permission.STORAGE_READ);
        guard.requireRole(Role.STAFF, Role.STUDENT);
        var args = new ArrayList<Object>();
        String filter = scope(args);
        args.add("%" + q + "%");
        return queries.search(
                "SELECT s.*,c.code AS contract_code,u.full_name FROM storage_record s JOIN"
                        + " dormitory_contract c ON c.id=s.contract_id JOIN room_application ap ON"
                        + " ap.id=c.application_id JOIN app_user u ON u.id=c.student_id WHERE "
                        + filter
                        + " AND u.full_name ILIKE ?",
                args,
                page,
                size);
    }

    @RequirePrivilege(Permission.ROOM_READ)
    @GetMapping("/api/me/room")
    public Object getMyRoom() {
        guard.requirePermission(Permission.ROOM_READ);
        guard.requireRole(Role.STUDENT);
        return queries.list(
                "SELECT room_id AS"
                    + " id,room_code,bed_code,building_code,assignment_kind,price_main,price_summer"
                    + " FROM v_current_stay WHERE student_id=?",
                current.get().id());
    }
}
