package academic.academic.domain.student.dto;

import java.time.LocalDate;

public record RecentTestSummary(
        LocalDate sessionDate,
        TestScoresSummary scores
) {
}
