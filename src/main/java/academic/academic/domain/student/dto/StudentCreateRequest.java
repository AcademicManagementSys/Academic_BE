package academic.academic.domain.student.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record StudentCreateRequest(
        @NotBlank String name,
        LocalDate birthDate,
        String school,
        String grade,
        String phone,
        Long classId,
        Long teacherId,
        LocalDate enrolledAt,
        @Valid ParentInfoRequest parent,
        @Valid StudentAccountRequest account
) {
}
