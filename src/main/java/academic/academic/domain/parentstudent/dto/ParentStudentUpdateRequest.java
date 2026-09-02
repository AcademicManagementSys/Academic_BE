package academic.academic.domain.parentstudent.dto;

import academic.academic.domain.parentstudent.entity.RelationType;
import jakarta.validation.constraints.NotNull;

public record ParentStudentUpdateRequest(
        @NotNull RelationType relationType
) {
}
