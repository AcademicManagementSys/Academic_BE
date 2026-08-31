package academic.academic.domain.monthlyexam.dto;

import academic.academic.domain.monthlyexam.entity.FeedbackStatus;

public record TypeFeedbackUpdateRequest(
        FeedbackStatus status,
        String feedbackText
) {
}
