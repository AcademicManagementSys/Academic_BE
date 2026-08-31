package academic.academic.domain.monthlyexam.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 학생 성적 등록 (FR-05-02). 원점수/표준점수/백분위/등급 중 학원에서 사용하는 지표를 선택적으로 입력한다.
 */
public record MonthlyExamRecordCreateRequest(
        @NotNull Long monthlyExamId,
        @NotNull Long studentId,
        Integer rawScore,
        Integer stdScore,
        Integer percentile,
        String grade
) {
}
