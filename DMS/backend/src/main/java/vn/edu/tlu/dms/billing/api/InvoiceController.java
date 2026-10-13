package vn.edu.tlu.dms.billing.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.billing.service.InvoiceService;
import vn.edu.tlu.dms.common.dto.PageResponse;

@RestController
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @GetMapping("/api/invoices")
    public PageResponse<InvoiceDtos.Response> searchInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "") String q) {
        return service.searchInvoices(page, size, type, status, false, q);
    }

    @GetMapping("/api/me/invoices")
    public PageResponse<InvoiceDtos.Response> getMyInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "") String q) {
        return service.searchInvoices(page, size, type, status, true, q);
    }

    @GetMapping("/api/invoices/{id}")
    public InvoiceDtos.Response getInvoiceById(@PathVariable Long id) {
        return service.getInvoiceById(id);
    }

    @PostMapping("/api/invoices/{id}/confirm-payment")
    public InvoiceDtos.Response confirmInvoicePayment(
            @PathVariable Long id, @Valid @RequestBody InvoiceDtos.Payment input) {
        return service.confirmInvoicePayment(id, input);
    }
}
