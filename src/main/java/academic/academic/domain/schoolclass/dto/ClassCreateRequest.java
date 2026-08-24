package academic.academic.domain.schoolclass.dto;

import jakarta.validation.constraints.NotBlank;

public record ClassCreateRequest(
        @NotBlank String name,
        Long teacherId,
        String schedule
) {
}
