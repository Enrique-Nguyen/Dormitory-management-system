package vn.edu.tlu.dms.notification.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.notification.service.NotificationCrudService;

import java.util.Map;

@RestController
public class NotificationCrudController {

    private final NotificationCrudService service;

    public NotificationCrudController(NotificationCrudService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.NOTIFICATION_READ)
    @PostMapping("/api/notifications/published/search-by-page")
    public PageResponse<Map<String, Object>> searchByPage(
            @Valid @RequestBody SearchRequest request) {
        return service.searchByPage(request);
    }

    @RequirePrivilege(Permission.NOTIFICATION_READ)
    @GetMapping("/api/notifications/published/{id}")
    public Object getPublished(@PathVariable Long id) {
        return service.getPublished(id);
    }

    @RequirePrivilege({Permission.NOTIFICATION_CREATE, Permission.NOTIFICATION_UPDATE})
    @PostMapping("/api/notifications/save-or-update")
    public Object saveOrUpdate(@Valid @RequestBody NotificationDtos.Create input) {
        return service.saveOrUpdate(input);
    }

    @RequirePrivilege(Permission.NOTIFICATION_DELETE)
    @DeleteMapping("/api/notifications/{id}")
    public ResponseEntity<Void> deletePublished(@PathVariable Long id) {
        service.deletePublished(id);
        return ResponseEntity.noContent().build();
    }
}
