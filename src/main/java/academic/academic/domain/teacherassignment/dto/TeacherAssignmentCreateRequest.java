package academic.academic.domain.teacherassignment.dto;

import jakarta.validation.constraints.NotNull;

public record TeacherAssignmentCreateRequest(
        @NotNull Long teacherId,
        Long classId,
        Long studentId
) {
}
