package academic.academic.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank String resetToken,
        @NotBlank @Size(min = 8) String newPassword
) {
}
