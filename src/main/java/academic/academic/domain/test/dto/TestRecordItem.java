package academic.academic.domain.test.dto;

import academic.academic.domain.test.entity.TestSubject;
import jakarta.validation.constraints.NotNull;

public record TestRecordItem(
        @NotNull Long studentId,
        @NotNull TestSubject subject,
        @NotNull Boolean isTaken,
        Integer score,
        Integer maxScore,
        String comment
) {
}
