package academic.academic.domain.monthlyexam.dto;

import jakarta.validation.constraints.NotBlank;

public record TypeCategoryCreateRequest(
        @NotBlank String name
) {
}
