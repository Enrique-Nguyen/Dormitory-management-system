package vn.edu.tlu.dms.billing.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.billing.service.UtilityReadingService;
import vn.edu.tlu.dms.common.constants.DmsConstants.MeterType;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/utility-readings")
public class UtilityReadingController {

    private final UtilityReadingService service;

    public UtilityReadingController(UtilityReadingService service) {
        this.service = service;
    }

    public record Input(
            @Positive Long id,
            @Positive Long roomId,
            @Pattern(regexp = MeterType.PATTERN) String meterType,
            LocalDate billingMonth,
            @Min(0) Integer startReading,
            @Min(0) Integer endReading,
            @Min(0) Integer oldMeterFinal,
            @Min(0) Integer newMeterStart,
            @Size(max = 300) String replacementReason) {

        public Input(
                Long roomId,
                String meterType,
                LocalDate billingMonth,
                Integer startReading,
                Integer endReading,
                Integer oldMeterFinal,
                Integer newMeterStart,
                String replacementReason) {
            this(
                    null,
                    roomId,
                    meterType,
                    billingMonth,
                    startReading,
                    endReading,
                    oldMeterFinal,
                    newMeterStart,
                    replacementReason);
        }
    }

    @RequirePrivilege(Permission.UTILITY_READING_READ)
    @PostMapping("/search-by-page")
    public PageResponse<Map<String, Object>> searchByPage(
            @Valid @RequestBody SearchRequest request) {
        return service.searchByPage(request);
    }

    @RequirePrivilege(Permission.UTILITY_READING_READ)
    @GetMapping("/{id}")
    public Map<String, Object> getReading(@PathVariable Long id) {
        return service.getReading(id);
    }

    @RequirePrivilege({Permission.UTILITY_READING_CREATE, Permission.UTILITY_READING_UPDATE})
    @PostMapping("/save-or-update")
    public Map<String, Object> saveOrUpdate(@Valid @RequestBody Input input) {
        return service.saveOrUpdate(input);
    }

    @RequirePrivilege(Permission.UTILITY_READING_DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReading(@PathVariable Long id) {
        service.deleteUnusedReading(id);
        return ResponseEntity.noContent().build();
    }
}
