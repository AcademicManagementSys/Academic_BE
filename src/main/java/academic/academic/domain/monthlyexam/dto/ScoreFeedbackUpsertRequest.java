package academic.academic.domain.monthlyexam.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 점수대별 피드백 등록/수정 (upsert).
 */
public record ScoreFeedbackUpsertRequest(
        @NotBlank String scoreBand,
        String feedbackText
) {
}
