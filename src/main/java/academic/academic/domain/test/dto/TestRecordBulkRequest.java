package academic.academic.domain.test.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 학생×영역(4열) 매트릭스 일괄 저장 (FR-04-04).
 */
public record TestRecordBulkRequest(
        @NotNull Long testSessionId,
        @NotEmpty @Valid List<TestRecordItem> records
) {
}
