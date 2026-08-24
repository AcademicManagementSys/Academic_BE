package academic.academic.domain.homework.dto;

import jakarta.validation.constraints.NotNull;

public record HomeworkRecordItem(
        @NotNull Long studentId,
        @NotNull Boolean isDone,
        Integer score,
        String comment
) {
}
