package academic.academic.domain.monthlyexam.dto;

import academic.academic.domain.monthlyexam.entity.MonthlyExamRecord;

/**
 * 학생 추이 조회 응답 (SCR-16 그래프용, FR-05-05). {@code recordId}는 이 점(회차)의 상세
 * ({@code GET /v1/monthly-exam-records/{recordId}})로 연결하기 위한 성적 레코드 id다.
 */
public record MonthlyExamTrendResponse(
        Long recordId,
        String examMonth,
        Integer rawScore
) {
    public static MonthlyExamTrendResponse from(MonthlyExamRecord record) {
        return new MonthlyExamTrendResponse(
                record.getId(),
                record.getMonthlyExam().getExamMonth(),
                record.getRawScore());
    }
}
