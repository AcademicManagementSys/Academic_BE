package academic.academic.domain.monthlyexam.dto;

import academic.academic.domain.monthlyexam.entity.FeedbackStatus;
import academic.academic.domain.monthlyexam.entity.MonthlyExamTypeFeedback;

public record TypeFeedbackResponse(
        Long id,
        Long typeCategoryId,
        String typeCategory,
        FeedbackStatus status,
        String feedbackText
) {
    public static TypeFeedbackResponse from(MonthlyExamTypeFeedback feedback) {
        return new TypeFeedbackResponse(
                feedback.getId(),
                feedback.getTypeCategory().getId(),
                feedback.getTypeCategory().getName(),
                feedback.getStatus(),
                feedback.getFeedbackText()
        );
    }
}
