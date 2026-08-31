package academic.academic.domain.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public record TeacherDashboardResponse(
        LocalDate date,
        List<ClassChecklistResponse> classes
) {
}
