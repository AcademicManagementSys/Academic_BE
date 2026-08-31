package academic.academic.domain.dashboard.dto;

public record ClassChecklistResponse(
        Long classId,
        String className,
        int studentCount,
        int attendanceUncheckedCount,
        int homeworkItemCount,
        int homeworkUncheckedCount,
        int testSessionCount,
        int testUncheckedCount
) {
}
