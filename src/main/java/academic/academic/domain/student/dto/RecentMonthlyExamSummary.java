package academic.academic.domain.student.dto;

public record RecentMonthlyExamSummary(
        String examMonth,
        Integer rawScore,
        Integer deltaFromPrev
) {
}
