package vn.edu.tlu.dms.contract.api;

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
import vn.edu.tlu.dms.contract.service.ContractService;

@RestController
public class ContractController {

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
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
}
