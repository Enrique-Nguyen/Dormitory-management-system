package vn.edu.tlu.dms.contract.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.contract.service.StorageLocationService;

@RestController
@RequestMapping("/api/storage-locations")
@Validated
public class StorageLocationController {
    private final StorageLocationService service;

    public StorageLocationController(StorageLocationService service) {
        this.service = service;
    }

    @PostMapping("/search-by-page")
    @RequirePrivilege(Permission.CATALOG_READ)
    public PageResponse<StorageLocationDtos.Location> searchByPage(
            @Valid @RequestBody StorageLocationDtos.Search input) {
        return service.searchByPage(input);
    }

    @PostMapping("/save-or-update")
    @RequirePrivilege({Permission.CATALOG_CREATE, Permission.CATALOG_UPDATE})
    public StorageLocationDtos.Location saveOrUpdate(
            @Valid @RequestBody StorageLocationDtos.SaveRequest input) {
        return service.saveOrUpdate(input);
    }

    @GetMapping("/{id}")
    @RequirePrivilege(Permission.CATALOG_READ)
    public StorageLocationDtos.Location getById(
            @PathVariable @Positive(message = ValidationMessage.ID_POSITIVE) Long id) {
        return service.getById(id);
    }

    @DeleteMapping("/{id}")
    @RequirePrivilege(Permission.CATALOG_DELETE)
    public void delete(
            @PathVariable @Positive(message = ValidationMessage.ID_POSITIVE) Long id) {
        service.delete(id);
    }
}
