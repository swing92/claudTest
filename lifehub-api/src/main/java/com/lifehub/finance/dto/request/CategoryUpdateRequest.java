package com.lifehub.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** type is not included: it is immutable after creation because existing transactions denormalize it. */
public record CategoryUpdateRequest(
        @NotBlank @Size(max = 50) String name,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "colorHex must look like #RRGGBB") String colorHex,
        Boolean isDefault
) {
}
