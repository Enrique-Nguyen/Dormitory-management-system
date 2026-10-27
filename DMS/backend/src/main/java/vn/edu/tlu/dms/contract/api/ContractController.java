package vn.edu.tlu.dms.contract.api;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.contract.service.ContractService;
import vn.edu.tlu.dms.contract.service.MoveOutService;
import vn.edu.tlu.dms.contract.service.RoomTransferService;
import vn.edu.tlu.dms.contract.service.StorageService;

import java.time.LocalDate;

@RestController
public class ContractController {

    private final ContractService service;

    private final RoomTransferService transfers;

    private final MoveOutService moveOuts;

    private final StorageService storage;

    public ContractController(
            ContractService service,
            RoomTransferService transfers,
            MoveOutService moveOuts,
            StorageService storage) {
        this.service = service;
        this.transfers = transfers;
        this.moveOuts = moveOuts;
        this.storage = storage;
    }

    @RequirePrivilege(Permission.CONTRACT_READ)
    @PostMapping("/api/contracts/search-by-page")
    public PageResponse<ContractDtos.Response> searchByPage(
            @Valid @RequestBody SearchRequest input) {
        return service.searchByPage(input, false);
    }

    @RequirePrivilege(Permission.CONTRACT_READ)
    @GetMapping("/api/contracts/{id}")
    public ContractDtos.Response getContractById(@PathVariable Long id) {
        return service.getContractById(id);
    }

    @RequirePrivilege(Permission.CONTRACT_UPDATE)
    @PostMapping("/api/contracts/{id}/activate")
    public ContractDtos.ActivationResult activate(
            @PathVariable Long id, @Valid @RequestBody ContractDtos.ActivateRequest input) {
        return service.activate(id, input);
    }

    @RequirePrivilege(Permission.CONTRACT_READ)
    @PostMapping("/api/me/contracts/search-by-page")
    public PageResponse<ContractDtos.Response> searchMyContractsByPage(
            @Valid @RequestBody SearchRequest input) {
        return service.searchByPage(input, true);
    }

    @RequirePrivilege(Permission.CONTRACT_READ)
    @GetMapping("/api/me/contract")
    public ContractDtos.Response getMyCurrentContract() {
        return service.getMyCurrentContract();
    }

    @RequirePrivilege(Permission.ROOM_TRANSFER_READ)
    @GetMapping("/api/transfer-requests/{id}")
    public ContractDtos.TransferDetail getTransfer(@PathVariable Long id) {
        return transfers.getTransferRequest(id);
    }

    @RequirePrivilege(Permission.ROOM_TRANSFER_APPROVE)
    @PostMapping("/api/transfer-requests/{id}/approve")
    public ContractDtos.TransferDetail approveTransfer(
            @PathVariable Long id, @RequestBody ContractDtos.TransferApproveRequest body) {
        return transfers.approve(id, body.targetBedId());
    }

    @RequirePrivilege(Permission.ROOM_TRANSFER_APPROVE)
    @PostMapping("/api/transfer-requests/{id}/reject")
    public ContractDtos.TransferDetail rejectTransfer(
            @PathVariable Long id, @RequestBody ContractDtos.TransferRejectRequest body) {
        return transfers.reject(id, body.reason());
    }

    @RequirePrivilege(Permission.ROOM_TRANSFER_DELETE)
    @PostMapping("/api/transfer-requests/{id}/cancel")
    public ContractDtos.TransferDetail cancelTransfer(@PathVariable Long id) {
        return transfers.cancel(id);
    }

    @RequirePrivilege(Permission.MOVE_OUT_READ)
    @GetMapping("/api/contracts/{id}/move-out/preview")
    public MoveOutDtos.Preview previewMoveOut(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate moveOutDate) {
        return moveOuts.preview(id, moveOutDate);
    }

    @RequirePrivilege(Permission.MOVE_OUT_CREATE)
    @PostMapping("/api/contracts/{id}/move-out")
    public MoveOutDtos.Result moveOut(
            @PathVariable Long id, @Valid @RequestBody MoveOutDtos.Request input) {
        return moveOuts.execute(id, input);
    }

    @RequirePrivilege(Permission.STORAGE_CREATE)
    @PostMapping("/api/contracts/{id}/storage-records")
    public StorageDtos.Record createStorageRecord(
            @PathVariable Long id, @Valid @RequestBody StorageDtos.CreateRequest input) {
        return storage.create(id, input);
    }

    @RequirePrivilege(Permission.STORAGE_UPDATE)
    @PostMapping("/api/storage-records/{id}/return")
    public StorageDtos.Record returnStorageRecord(
            @PathVariable Long id, @Valid @RequestBody StorageDtos.ReturnRequest input) {
        return storage.returnStorage(id, input);
    }
}
