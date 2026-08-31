package academic.academic.domain.monthlyexam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 월말모의고사 회차 생성 (FR-05-01). 입력 항목: 시행 연월, 모의고사명/출처.
 */
public record MonthlyExamCreateRequest(
        @NotBlank String examName,
        @NotBlank @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "examMonth는 YYYY-MM 형식이어야 합니다.")
        String examMonth
) {
}
