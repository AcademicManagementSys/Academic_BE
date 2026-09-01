package academic.academic.domain.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminDashboardResponse(
        LocalDate date,
        int totalStudentCount,
        Double todayAttendanceRate,
        int todayHomeworkUncheckedCount,
        List<ClassStatisticsResponse> classes
) {
}
