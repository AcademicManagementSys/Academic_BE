package academic.academic.domain.student.dto;

import academic.academic.domain.homework.entity.HomeworkRecord;

import java.time.LocalDate;

public record HomeworkSummaryItem(
        String title,
        boolean isDone,
        LocalDate date
) {
    public static HomeworkSummaryItem from(HomeworkRecord record) {
        return new HomeworkSummaryItem(
                record.getHomeworkItem().getTitle(),
                record.isDone(),
                record.getHomeworkItem().getAssignedDate()
        );
    }
}
