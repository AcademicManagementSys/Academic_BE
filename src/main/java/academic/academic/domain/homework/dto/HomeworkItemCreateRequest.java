package academic.academic.domain.homework.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * classId, studentId 중 정확히 하나만 채워야 한다 (FR-03-01: 반 또는 개별 학생 단위).
 */
public record HomeworkItemCreateRequest(
        Long classId,
        Long studentId,
        @NotBlank String title,
        String scope,
        @NotNull LocalDate assignedDate,
        LocalDate dueDate
) {
}
