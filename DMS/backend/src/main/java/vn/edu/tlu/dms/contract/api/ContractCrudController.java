package vn.edu.tlu.dms.contract.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.contract.service.ContractCrudService;

/** Chỉ CRUD cơ bản, không chứa các lệnh kích hoạt, thu cọc, phân giường hoặc xét duyệt. */
@RestController
public class ContractCrudController {

    private final ContractCrudService service;

    public ContractCrudController(ContractCrudService service) {
        this.service = service;
    }

    @RequirePrivilege({Permission.CONTRACT_CREATE, Permission.CONTRACT_UPDATE})
    @PostMapping("/api/contracts/save-or-update")
    public Object saveOrUpdateContract(@Valid @RequestBody ContractDtos.Save input) {
        return service.saveOrUpdateContract(input);
    }

    @RequirePrivilege(Permission.CONTRACT_DELETE)
    @DeleteMapping("/api/contracts/{id}")
    public ResponseEntity<Void> deleteContract(@PathVariable Long id) {
        service.deletePendingContract(id);
        return ResponseEntity.noContent().build();
    }

    @RequirePrivilege(Permission.CONTRACT_EXTENSION_READ)
    @PostMapping("/api/extension-requests/search-by-page")
    public Object searchExtensionsByPage(@Valid @RequestBody SearchRequest input) {
        return service.searchStayRequestsByPage(true, input, false);
    }

    @RequirePrivilege(Permission.CONTRACT_EXTENSION_READ)
    @PostMapping("/api/me/extension-requests/search-by-page")
    public Object searchMyExtensionsByPage(@Valid @RequestBody SearchRequest input) {
        return service.searchStayRequestsByPage(true, input, true);
    }

    @RequirePrivilege(Permission.ROOM_TRANSFER_READ)
    @PostMapping("/api/transfer-requests/search-by-page")
    public Object searchTransfersByPage(@Valid @RequestBody SearchRequest input) {
        return service.searchStayRequestsByPage(false, input, false);
    }

    @RequirePrivilege(Permission.ROOM_TRANSFER_READ)
    @PostMapping("/api/me/transfer-requests/search-by-page")
    public Object searchMyTransfersByPage(@Valid @RequestBody SearchRequest input) {
        return service.searchStayRequestsByPage(false, input, true);
    }

    @RequirePrivilege(Permission.CONTRACT_EXTENSION_READ)
    @GetMapping("/api/extension-requests/{id}")
    public ContractDtos.ExtensionDetail getExtension(@PathVariable Long id) {
        return service.getExtensionDetail(id);
    }

    @RequirePrivilege(Permission.CONTRACT_EXTENSION_UPDATE)
    @PostMapping("/api/extension-requests/{id}/approve")
    public ContractDtos.ExtensionDetail approveExtension(@PathVariable Long id) {
        return service.approveExtension(id);
    }

    @RequirePrivilege(Permission.CONTRACT_EXTENSION_UPDATE)
    @PostMapping("/api/extension-requests/{id}/reject")
    public ContractDtos.ExtensionDetail rejectExtension(
            @PathVariable Long id, @RequestBody ContractDtos.ExtensionRejectRequest input) {
        return service.rejectExtension(id, input.reason());
    }

    @RequirePrivilege(Permission.CONTRACT_EXTENSION_DELETE)
    @PostMapping("/api/extension-requests/{id}/cancel")
    public ContractDtos.ExtensionDetail cancelExtension(@PathVariable Long id) {
        return service.cancelExtension(id);
    }

    @RequirePrivilege({Permission.CONTRACT_EXTENSION_CREATE, Permission.CONTRACT_EXTENSION_UPDATE})
    @PostMapping("/api/extension-requests/save-or-update")
    public Object saveOrUpdateExtension(@Valid @RequestBody ContractDtos.StaySave input) {
        return service.saveOrUpdateStayRequest(true, input);
    }

    @RequirePrivilege({Permission.ROOM_TRANSFER_CREATE, Permission.ROOM_TRANSFER_UPDATE})
    @PostMapping("/api/transfer-requests/save-or-update")
    public Object saveOrUpdateTransfer(@Valid @RequestBody ContractDtos.StaySave input) {
        return service.saveOrUpdateStayRequest(false, input);
    }

    @RequirePrivilege(Permission.CONTRACT_EXTENSION_DELETE)
    @DeleteMapping("/api/extension-requests/{id}")
    public ResponseEntity<Void> deleteExtension(@PathVariable Long id) {
        service.deletePendingStayRequest(true, id);
        return ResponseEntity.noContent().build();
    }

    @RequirePrivilege(Permission.ROOM_TRANSFER_DELETE)
    @DeleteMapping("/api/transfer-requests/{id}")
    public ResponseEntity<Void> deleteTransfer(@PathVariable Long id) {
        service.deletePendingStayRequest(false, id);
        return ResponseEntity.noContent().build();
    }
}
