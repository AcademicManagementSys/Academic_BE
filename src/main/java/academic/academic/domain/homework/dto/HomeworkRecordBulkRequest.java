package academic.academic.domain.homework.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 학생×숙제항목 매트릭스 일괄 저장 (FR-03-05).
 */
public record HomeworkRecordBulkRequest(
        @NotNull Long classId,
        @NotEmpty @Valid List<HomeworkItemRecordsGroup> items
) {
}
