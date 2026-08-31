package academic.academic.domain.monthlyexam.dto;

import academic.academic.domain.monthlyexam.entity.FeedbackStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 유형별 피드백 추가 (FR-05-08).
 */
public record TypeFeedbackCreateRequest(
        @NotNull Long typeCategoryId,
        @NotNull FeedbackStatus status,
        String feedbackText
) {
}
