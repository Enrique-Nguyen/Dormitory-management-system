package vn.edu.tlu.dms.billing.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.billing.service.InvoiceService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

@RestController
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.INVOICE_READ)
    @PostMapping("/api/invoices/search-by-page")
    public PageResponse<InvoiceDtos.Response> searchByPage(
            @Valid @RequestBody SearchRequest request) {
        return service.searchByPage(request, false);
    }

    @RequirePrivilege(Permission.INVOICE_READ)
    @PostMapping("/api/me/invoices/search-by-page")
    public PageResponse<InvoiceDtos.Response> searchMyInvoicesByPage(
            @Valid @RequestBody SearchRequest request) {
        return service.searchByPage(request, true);
    }

    @RequirePrivilege(Permission.INVOICE_READ)
    @GetMapping("/api/invoices/{id}")
    public InvoiceDtos.Response getInvoiceById(@PathVariable Long id) {
        return service.getInvoiceById(id);
    }

    @RequirePrivilege(Permission.INVOICE_UPDATE)
    @PostMapping("/api/invoices/{id}/confirm-payment")
    public InvoiceDtos.Response confirmInvoicePayment(
            @PathVariable Long id, @Valid @RequestBody InvoiceDtos.Payment input) {
        return service.confirmPayment(id, input);
    }

    @RequirePrivilege(Permission.INVOICE_CREATE)
    @PostMapping("/api/invoices/issue-room-fee")
    public InvoiceDtos.BatchIssueResult issueRoomFeeInvoices(
            @Valid @RequestBody InvoiceDtos.RoomFeeBatchIssueRequest request) {
        return service.issueRoomFee(request.semesterId());
    }

    @RequirePrivilege(Permission.INVOICE_CREATE)
    @PostMapping("/api/invoices/issue-utility")
    public InvoiceDtos.BatchIssueResult issueUtilityInvoices(
            @Valid @RequestBody InvoiceDtos.UtilityBatchIssueRequest request) {
        return service.issueUtility(request.managementUnitId(), request.billingMonth());
    }
}
