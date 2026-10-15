package vn.edu.tlu.dms.billing.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.billing.service.InvoiceCrudService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

import java.util.Map;

@RestController
public class InvoiceCrudController {

    private final InvoiceCrudService service;

    public InvoiceCrudController(InvoiceCrudService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.INVOICE_CREATE)
    @PostMapping("/api/invoices")
    public Object createInvoice(@RequestBody Map<String, Object> input) {
        return service.createInvoiceRecord(input);
    }

    @RequirePrivilege(Permission.INVOICE_UPDATE)
    @PatchMapping("/api/invoices/{id}")
    public Object updateInvoice(@PathVariable Long id, @RequestBody Map<String, Object> input) {
        return service.updateUnpaidInvoiceRecord(id, input);
    }

    @RequirePrivilege(Permission.INVOICE_DELETE)
    @DeleteMapping("/api/invoices/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        service.deleteUnusedInvoiceRecord(id);
        return ResponseEntity.noContent().build();
    }
}
