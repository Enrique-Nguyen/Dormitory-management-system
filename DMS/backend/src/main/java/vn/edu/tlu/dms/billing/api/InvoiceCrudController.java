package vn.edu.tlu.dms.billing.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.billing.service.InvoiceCrudService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

@RestController
public class InvoiceCrudController {

    private final InvoiceCrudService service;

    public InvoiceCrudController(InvoiceCrudService service) {
        this.service = service;
    }

    @RequirePrivilege({Permission.INVOICE_CREATE, Permission.INVOICE_UPDATE})
    @PostMapping("/api/invoices/save-or-update")
    public Object saveOrUpdate(@Valid @RequestBody InvoiceDtos.Save input) {
        return service.saveOrUpdate(input);
    }

    @RequirePrivilege(Permission.INVOICE_DELETE)
    @DeleteMapping("/api/invoices/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        service.deleteUnusedInvoiceRecord(id);
        return ResponseEntity.noContent().build();
    }
}
