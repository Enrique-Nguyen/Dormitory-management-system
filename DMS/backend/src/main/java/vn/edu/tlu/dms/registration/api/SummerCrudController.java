package vn.edu.tlu.dms.registration.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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
import vn.edu.tlu.dms.registration.api.RegistrationPeriodDtos.Response;
import vn.edu.tlu.dms.registration.service.RegistrationPeriodService;
import vn.edu.tlu.dms.registration.service.SummerCrudService;

import java.util.Map;

@RestController
public class SummerCrudController {

    private final SummerCrudService service;

    private final RegistrationPeriodService periods;

    public SummerCrudController(SummerCrudService service, RegistrationPeriodService periods) {
        this.service = service;
        this.periods = periods;
    }

    /** Tạo cần hợp đồng/học kỳ/lựa chọn; sửa chỉ đổi lựa chọn của bản ghi có ID. */
    public record Input(
            @Positive Long id,
            @Positive Long contractId,
            @Positive Long semesterId,
            @Size(max = 15) String choice) {}

    @RequirePrivilege(Permission.REGISTRATION_PERIOD_READ)
    @PostMapping("/api/me/registration-periods/open/search-by-page")
    public PageResponse<Response> searchOpenPeriodsByPage(
            @Valid @RequestBody SearchRequest search) {
        // Không cho open=false từ client mở rộng lookup đang mở của sinh viên.
        return periods.searchByPage(search, true);
    }

    @RequirePrivilege(Permission.SUMMER_REGISTRATION_READ)
    @PostMapping("/api/summer-choices/search-by-page")
    public PageResponse<Map<String, Object>> searchByPage(
            @Valid @RequestBody SearchRequest search) {
        return service.searchByPage(search, false);
    }

    @RequirePrivilege(Permission.SUMMER_REGISTRATION_READ)
    @PostMapping("/api/me/summer-choices/search-by-page")
    public PageResponse<Map<String, Object>> searchMyChoicesByPage(
            @Valid @RequestBody SearchRequest search) {
        return service.searchByPage(search, true);
    }

    @RequirePrivilege(Permission.SUMMER_REGISTRATION_READ)
    @GetMapping("/api/summer-choices/{id}")
    public Object getChoice(@PathVariable Long id) {
        return service.getChoice(id);
    }

    @RequirePrivilege({
        Permission.SUMMER_REGISTRATION_CREATE,
        Permission.SUMMER_REGISTRATION_UPDATE
    })
    @PostMapping("/api/summer-choices/save-or-update")
    public Object saveOrUpdate(@Valid @RequestBody Input input) {
        return service.saveOrUpdate(input);
    }

    @RequirePrivilege(Permission.SUMMER_REGISTRATION_DELETE)
    @DeleteMapping("/api/summer-choices/{id}")
    public ResponseEntity<Void> deleteChoice(@PathVariable Long id) {
        service.deleteChoice(id);
        return ResponseEntity.noContent().build();
    }
}
