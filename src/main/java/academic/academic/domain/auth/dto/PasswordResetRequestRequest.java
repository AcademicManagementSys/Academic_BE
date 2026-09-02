package academic.academic.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestRequest(@NotBlank String loginId) {
}
