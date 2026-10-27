package vn.edu.tlu.dms.billing.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.billing.service.DepositService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

import java.util.List;

/**
 * Controller API quản lý sổ cọc và các giao dịch tiền cọc (FR-CON-01, FR-BIL-09, FR-BIL-10, FR-CON-09).
 */
@RestController
public class DepositController {

    private final DepositService depositService;

    public DepositController(DepositService depositService) {
        this.depositService = depositService;
    }

    @RequirePrivilege(Permission.DEPOSIT_READ)
    @PostMapping("/api/deposit-balances/search-by-page")
    public PageResponse<DepositDtos.Balance> searchBalancesByPage(
            @Valid @RequestBody SearchRequest request) {
        return depositService.searchBalancesByPage(request, false);
    }

    @RequirePrivilege(Permission.DEPOSIT_READ)
    @PostMapping("/api/me/deposit-balances/search-by-page")
    public PageResponse<DepositDtos.Balance> searchMyBalancesByPage(
            @Valid @RequestBody SearchRequest request) {
        return depositService.searchBalancesByPage(request, true);
    }

    @RequirePrivilege(Permission.DEPOSIT_READ)
    @GetMapping("/api/contracts/{id}/deposit-balance")
    public DepositDtos.Balance getDepositBalance(@PathVariable Long id) {
        return depositService.getBalance(id);
    }

    @RequirePrivilege(Permission.DEPOSIT_READ)
    @GetMapping("/api/contracts/{id}/deposit-transactions")
    public List<DepositDtos.Transaction> listTransactions(@PathVariable Long id) {
        return depositService.listTransactions(id);
    }

    @RequirePrivilege(Permission.DEPOSIT_CREATE)
    @PostMapping("/api/contracts/{id}/initial-deposit")
    public DepositDtos.Transaction recordInitialDeposit(
            @PathVariable Long id, @Valid @RequestBody DepositDtos.InitialDepositRequest request) {
        return depositService.collectInitial(id, request.amount(), request.note());
    }

    @RequirePrivilege(Permission.DEPOSIT_CREATE)
    @PostMapping("/api/contracts/{id}/deposit-topup")
    public DepositDtos.Transaction recordTopup(
            @PathVariable Long id, @Valid @RequestBody DepositDtos.TopupRequest request) {
        return depositService.topup(id, request.amount(), request.note());
    }

    @RequirePrivilege(Permission.DEPOSIT_CREATE)
    @PostMapping("/api/contracts/{id}/deposit-refund")
    public DepositDtos.Transaction refundDeposit(
            @PathVariable Long id, @Valid @RequestBody DepositDtos.RefundRequest request) {
        return depositService.refund(id, request.amount(), request.note());
    }
}
