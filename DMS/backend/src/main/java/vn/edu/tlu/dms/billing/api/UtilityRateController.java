package vn.edu.tlu.dms.billing.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.billing.service.UtilityRateService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

@RestController
@RequestMapping("/api/utility-rates")
public class UtilityRateController {

    private final UtilityRateService service;

    public UtilityRateController(UtilityRateService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.UTILITY_RATE_READ)
    @PostMapping("/search-by-page")
    public PageResponse<UtilityRateDtos.Response> searchByPage(
            @Valid @RequestBody SearchRequest request) {
        return service.searchByPage(request);
    }

    @RequirePrivilege({Permission.UTILITY_RATE_CREATE, Permission.UTILITY_RATE_UPDATE})
    @PostMapping("/save-or-update")
    public UtilityRateDtos.Response saveOrUpdate(@Valid @RequestBody UtilityRateDtos.Save input) {
        return service.saveOrUpdate(input);
    }
}
