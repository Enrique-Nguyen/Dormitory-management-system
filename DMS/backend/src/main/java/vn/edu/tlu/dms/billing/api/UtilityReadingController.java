package vn.edu.tlu.dms.billing.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.billing.service.UtilityReadingService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/utility-readings")
public class UtilityReadingController {

    private final UtilityReadingService service;

    public UtilityReadingController(UtilityReadingService service) {
        this.service = service;
    }

    public record Input(
            @NotNull @Positive Long roomId,
            @NotBlank @Pattern(regexp = "ELECTRIC|WATER") String meterType,
            @NotNull LocalDate billingMonth,
            @Min(0) Integer startReading,
            @Min(0) int endReading,
            @Min(0) Integer oldMeterFinal,
            @Min(0) Integer newMeterStart,
            @Size(max = 300) String replacementReason) {}

    @GetMapping
    public Object searchReadings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) LocalDate billingMonth) {
        return service.searchReadings(page, size, q, billingMonth);
    }

    @GetMapping("/{id}")
    public Object getReading(@PathVariable Long id) {
        return service.getReading(id);
    }

    @PostMapping
    public Object createReading(@Valid @RequestBody Input input) {
        return service.saveReading(null, input);
    }

    @PatchMapping("/{id}")
    public Object updateReading(@PathVariable Long id, @Valid @RequestBody Input input) {
        return service.saveReading(id, input);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReading(@PathVariable Long id) {
        service.deleteUnusedReading(id);
        return ResponseEntity.noContent().build();
    }
}
