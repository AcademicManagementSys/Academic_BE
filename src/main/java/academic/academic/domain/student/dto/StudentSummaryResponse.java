package academic.academic.domain.student.dto;

import java.util.List;

/**
 * 학생 홈 요약 (SCR-11 학생 상세, SCR-12 학부모/학생 홈, FR-07-01).
 * 최근 출석/숙제/테스트/월말모의고사를 한 번에 제공한다.
 */
public record StudentSummaryResponse(
        StudentSummaryStudentInfo student,
        AttendanceSummaryResponse attendance,
        List<HomeworkSummaryItem> homework,
        RecentTestSummary recentTest,
        RecentMonthlyExamSummary recentMonthlyExam
) {
}
