package vn.edu.tlu.dms.billing.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.billing.service.UtilityRateService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/utility-rates")
public class UtilityRateController {

    private final UtilityRateService service;

    public UtilityRateController(UtilityRateService service) {
        this.service = service;
    }

    @GetMapping
    public List<UtilityRateDtos.Response> getUtilityRates(
            @RequestParam(required = false) String meterType) {
        return service.getUtilityRates(meterType);
    }

    @PostMapping
    public ResponseEntity<UtilityRateDtos.Response> createUtilityRate(
            @Valid @RequestBody UtilityRateDtos.Create input) {
        UtilityRateDtos.Response response = service.createUtilityRate(input);
        return ResponseEntity.created(URI.create("/api/utility-rates/" + response.id()))
                .body(response);
    }
}
