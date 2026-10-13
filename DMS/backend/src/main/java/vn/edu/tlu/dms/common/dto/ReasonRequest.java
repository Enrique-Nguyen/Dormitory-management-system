package vn.edu.tlu.dms.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReasonRequest(@NotBlank @Size(min = 5, max = 500) String reason) {

    public ReasonRequest {
        if (reason != null) {
            reason = reason.trim();
        }
    }
}
