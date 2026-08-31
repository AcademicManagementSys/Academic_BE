package academic.academic.domain.monthlyexam.dto;

import academic.academic.domain.monthlyexam.entity.MonthlyExamRecord;

/**
 * 학생 추이 조회 응답 (SCR-16 그래프용, FR-05-05).
 */
public record MonthlyExamTrendResponse(
        String examMonth,
        Integer rawScore
) {
    public static MonthlyExamTrendResponse from(MonthlyExamRecord record) {
        return new MonthlyExamTrendResponse(record.getMonthlyExam().getExamMonth(), record.getRawScore());
    }
}
