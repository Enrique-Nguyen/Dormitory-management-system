package vn.edu.tlu.dms.account.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.service.CatalogService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

import java.util.Map;

@RestController
@RequestMapping("/api/catalogs/{kind}")
public class CatalogController {
    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @PostMapping("/search-by-page")
    @RequirePrivilege(Permission.CATALOG_READ)
    public PageResponse<Map<String, Object>> searchByPage(
            @PathVariable String kind, @Valid @RequestBody CatalogSearch input) {
        return service.searchByPage(kind, input);
    }

    @GetMapping("/{id}")
    @RequirePrivilege(Permission.CATALOG_READ)
    public Map<String, Object> getById(@PathVariable String kind, @PathVariable Long id) {
        return service.getById(kind, id);
    }

    @PostMapping("/save-or-update")
    @RequirePrivilege({Permission.CATALOG_CREATE, Permission.CATALOG_UPDATE})
    public Map<String, Object> saveOrUpdate(
            @PathVariable String kind, @Valid @RequestBody CatalogRequest input) {
        return service.saveOrUpdate(kind, input);
    }

    @DeleteMapping("/{id}")
    @RequirePrivilege(Permission.CATALOG_DELETE)
    public void delete(@PathVariable String kind, @PathVariable Long id) {
        service.delete(kind, id);
    }
}
