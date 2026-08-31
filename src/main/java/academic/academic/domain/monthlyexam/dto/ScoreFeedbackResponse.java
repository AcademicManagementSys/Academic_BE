package academic.academic.domain.monthlyexam.dto;

import academic.academic.domain.monthlyexam.entity.MonthlyExamScoreFeedback;

public record ScoreFeedbackResponse(
        String scoreBand,
        String feedbackText
) {
    public static ScoreFeedbackResponse from(MonthlyExamScoreFeedback feedback) {
        if (feedback == null) {
            return null;
        }
        return new ScoreFeedbackResponse(feedback.getScoreBand(), feedback.getFeedbackText());
    }
}
