package vn.edu.tlu.dms.common.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.common.service.ConfigService;

@RestController
@RequestMapping("/api/admin/configs")
public class ConfigController {

    private final ConfigService service;

    public ConfigController(ConfigService service) {
        this.service = service;
    }

    public record Input(@NotBlank String key, @NotBlank String value) {}

    @PostMapping("/search-by-page")
    @RequirePrivilege(Permission.CONFIG_READ)
    public PageResponse<ConfigService.ConfigValue> searchByPage(
            @Valid @RequestBody SearchRequest input) {
        return service.searchByPage(input);
    }

    @PostMapping("/save-or-update")
    @RequirePrivilege(Permission.CONFIG_UPDATE)
    public ConfigService.ConfigValue saveOrUpdate(@Valid @RequestBody Input input) {
        return service.saveOrUpdate(input.key(), input.value());
    }
}
