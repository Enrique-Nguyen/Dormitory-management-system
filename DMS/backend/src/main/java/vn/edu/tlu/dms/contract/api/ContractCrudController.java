package vn.edu.tlu.dms.contract.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.contract.service.ContractCrudService;

import java.util.Map;

/** Chỉ CRUD cơ bản, không chứa các lệnh kích hoạt, thu cọc, phân giường hoặc xét duyệt. */
@RestController
public class ContractCrudController {

    private final ContractCrudService service;

    public ContractCrudController(ContractCrudService service) {
        this.service = service;
    }

    @PostMapping("/api/contracts")
    public Object createContract(@RequestBody Map<String, Object> input) {
        return service.createPendingContract(input);
    }

    @PatchMapping("/api/contracts/{id}")
    public Object updateContract(@PathVariable Long id, @RequestBody Map<String, Object> input) {
        return service.updatePendingContract(id, input);
    }

    @DeleteMapping("/api/contracts/{id}")
    public ResponseEntity<Void> deleteContract(@PathVariable Long id) {
        service.deletePendingContract(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/api/extension-requests", "/api/me/extension-requests"})
    public Object searchExtensions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q) {
        return service.searchStayRequests(true, page, size, q);
    }

    @GetMapping({"/api/transfer-requests", "/api/me/transfer-requests"})
    public Object searchTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q) {
        return service.searchStayRequests(false, page, size, q);
    }

    @GetMapping("/api/extension-requests/{id}")
    public Object getExtension(@PathVariable Long id) {
        return service.getStayRequest(true, id);
    }

    @GetMapping("/api/transfer-requests/{id}")
    public Object getTransfer(@PathVariable Long id) {
        return service.getStayRequest(false, id);
    }

    @PostMapping("/api/extension-requests")
    public Object createExtension(@RequestBody Map<String, Object> input) {
        return service.createStayRequest(true, input);
    }

    @PostMapping("/api/transfer-requests")
    public Object createTransfer(@RequestBody Map<String, Object> input) {
        return service.createStayRequest(false, input);
    }

    @PatchMapping("/api/extension-requests/{id}")
    public Object updateExtension(@PathVariable Long id, @RequestBody Map<String, Object> input) {
        return service.updateStayRequest(true, id, input);
    }

    @PatchMapping("/api/transfer-requests/{id}")
    public Object updateTransfer(@PathVariable Long id, @RequestBody Map<String, Object> input) {
        return service.updateStayRequest(false, id, input);
    }

    @DeleteMapping("/api/extension-requests/{id}")
    public ResponseEntity<Void> deleteExtension(@PathVariable Long id) {
        service.deletePendingStayRequest(true, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/transfer-requests/{id}")
    public ResponseEntity<Void> deleteTransfer(@PathVariable Long id) {
        service.deletePendingStayRequest(false, id);
        return ResponseEntity.noContent().build();
    }
}
