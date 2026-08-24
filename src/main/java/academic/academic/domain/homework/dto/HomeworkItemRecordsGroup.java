package academic.academic.domain.homework.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record HomeworkItemRecordsGroup(
        @NotNull Long homeworkItemId,
        @NotEmpty @Valid List<HomeworkRecordItem> records
) {
}
