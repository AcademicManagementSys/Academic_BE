package academic.academic.domain.user.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull Boolean active
) {
}
