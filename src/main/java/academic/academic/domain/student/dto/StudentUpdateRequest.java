package academic.academic.domain.student.dto;

import academic.academic.domain.student.entity.StudentStatus;

import java.time.LocalDate;

public record StudentUpdateRequest(
        String name,
        LocalDate birthDate,
        String school,
        String grade,
        String phone,
        Long classId,
        StudentStatus status
) {
}
