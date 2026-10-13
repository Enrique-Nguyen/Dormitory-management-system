package vn.edu.tlu.dms.contract.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.contract.service.ContractService;

@RestController
public class ContractController {

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @GetMapping("/api/contracts")
    public PageResponse<ContractDtos.Response> searchContracts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "") String q) {
        return service.searchContracts(page, size, status, false, q);
    }

    @GetMapping("/api/contracts/{id}")
    public ContractDtos.Response getContractById(@PathVariable Long id) {
        return service.getContractById(id);
    }

    @GetMapping("/api/me/contracts")
    public PageResponse<ContractDtos.Response> getMyContracts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q) {
        return service.searchContracts(page, size, null, true, q);
    }

    @GetMapping("/api/me/contract")
    public ContractDtos.Response getMyCurrentContract() {
        return service.getMyCurrentContract();
    }
}
