package academic.academic.domain.dashboard.dto;

public record ClassStatisticsResponse(
        Long classId,
        String className,
        Long teacherId,
        String teacherName,
        int studentCount,
        Double attendanceRate,
        Double homeworkCompletionRate
) {
}
