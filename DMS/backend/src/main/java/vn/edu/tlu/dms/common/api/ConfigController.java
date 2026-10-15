package vn.edu.tlu.dms.common.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.common.service.ConfigService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/configs")
public class ConfigController {

    private final ConfigService service;

    public ConfigController(ConfigService service) {
        this.service = service;
    }

    public record Input(@NotBlank String value) {}

    @GetMapping
    @RequirePrivilege(Permission.CONFIG_READ)
    public List<ConfigService.ConfigValue> list() {
        return service.list();
    }

    @PutMapping("/{key}")
    @RequirePrivilege(Permission.CONFIG_UPDATE)
    public ConfigService.ConfigValue update(
            @PathVariable String key, @Valid @RequestBody Input input) {
        return service.update(key, input.value());
    }
}
