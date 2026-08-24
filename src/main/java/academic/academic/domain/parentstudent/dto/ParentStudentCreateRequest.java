package academic.academic.domain.parentstudent.dto;

import jakarta.validation.constraints.NotNull;

public record ParentStudentCreateRequest(
        @NotNull Long parentUserId,
        @NotNull Long studentId
) {
}
