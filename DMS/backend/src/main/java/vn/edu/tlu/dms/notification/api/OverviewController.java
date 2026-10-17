package vn.edu.tlu.dms.notification.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.notification.service.OverviewService;

import java.util.Map;

/** API tra cứu; phạm vi dữ liệu và truy vấn được xử lý trong Service Impl. */
@RestController
public class OverviewController {

    private final OverviewService service;

    public OverviewController(OverviewService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.DEPOSIT_READ)
    @PostMapping("/api/deposits/search-by-page")
    public PageResponse<Map<String, Object>> searchDepositsByPage(
            @Valid @RequestBody SearchRequest input) {
        return service.searchDepositsByPage(input, false);
    }

    @RequirePrivilege(Permission.DEPOSIT_READ)
    @PostMapping("/api/me/deposits/search-by-page")
    public PageResponse<Map<String, Object>> searchMyDepositsByPage(
            @Valid @RequestBody SearchRequest input) {
        return service.searchDepositsByPage(input, true);
    }

    @RequirePrivilege(Permission.DEPOSIT_READ)
    @GetMapping("/api/contracts/{id}/deposit")
    public Map<String, Object> getDeposit(@PathVariable Long id) {
        return service.getDeposit(id);
    }

    @RequirePrivilege(Permission.STORAGE_READ)
    @PostMapping("/api/storage-records/search-by-page")
    public PageResponse<Map<String, Object>> searchStorageByPage(
            @Valid @RequestBody SearchRequest input) {
        return service.searchStorageByPage(input, false);
    }

    @RequirePrivilege(Permission.STORAGE_READ)
    @PostMapping("/api/me/storage-records/search-by-page")
    public PageResponse<Map<String, Object>> searchMyStorageByPage(
            @Valid @RequestBody SearchRequest input) {
        return service.searchStorageByPage(input, true);
    }

    @RequirePrivilege(Permission.ROOM_READ)
    @PostMapping("/api/me/room/search-by-page")
    public PageResponse<Map<String, Object>> searchMyRoomByPage(
            @Valid @RequestBody SearchRequest input) {
        return service.searchMyRoomByPage(input);
    }
}
