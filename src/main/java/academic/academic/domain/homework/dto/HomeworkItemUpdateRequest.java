package academic.academic.domain.homework.dto;

import java.time.LocalDate;

public record HomeworkItemUpdateRequest(
        String title,
        String scope,
        LocalDate assignedDate,
        LocalDate dueDate
) {
}
