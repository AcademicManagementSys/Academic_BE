package academic.academic.domain.monthlyexam.dto;

import java.util.List;

/**
 * 회차 상세(성적 + 피드백 전체) 응답 (SCR-16).
 */
public record MonthlyExamRecordDetailResponse(
        MonthlyExamRecordResponse record,
        List<TypeFeedbackResponse> typeFeedbacks,
        ScoreFeedbackResponse scoreFeedback
) {
}
