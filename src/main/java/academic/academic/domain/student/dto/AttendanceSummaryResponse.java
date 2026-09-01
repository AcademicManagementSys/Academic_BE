package academic.academic.domain.student.dto;

public record AttendanceSummaryResponse(
        int presentDays,
        int totalDays,
        int lateCount,
        int absentCount
) {
}
