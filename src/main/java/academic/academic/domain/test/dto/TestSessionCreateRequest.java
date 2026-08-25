package academic.academic.domain.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 테스트 회차 생성 (FR-04-01). 입력 항목: 시행일, 테스트 범위/제목.
 */
public record TestSessionCreateRequest(
        @NotNull Long classId,
        @NotBlank String title,
        @NotNull LocalDate testDate
) {
}
