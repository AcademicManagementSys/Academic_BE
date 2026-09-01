package academic.academic.domain.notice.dto;

import jakarta.validation.constraints.NotNull;

public record NoticePinUpdateRequest(
        @NotNull Boolean isPinned
) {
}
