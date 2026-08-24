package academic.academic.domain.homework.dto;

import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.homework.entity.HomeworkRecord;
import academic.academic.domain.student.entity.Student;

import java.time.LocalDate;

public record HomeworkRecordResponse(
        Long id,
        Long homeworkItemId,
        String homeworkItemTitle,
        LocalDate assignedDate,
        LocalDate dueDate,
        Long studentId,
        String studentName,
        boolean isDone,
        Integer score,
        String comment
) {
    public static HomeworkRecordResponse from(HomeworkRecord record) {
        return new HomeworkRecordResponse(
                record.getId(),
                record.getHomeworkItem().getId(),
                record.getHomeworkItem().getTitle(),
                record.getHomeworkItem().getAssignedDate(),
                record.getHomeworkItem().getDueDate(),
                record.getStudent().getId(),
                record.getStudent().getName(),
                record.isDone(),
                record.getScore(),
                record.getComment()
        );
    }

    public static HomeworkRecordResponse unchecked(HomeworkItem item, Student student) {
        return new HomeworkRecordResponse(
                null,
                item.getId(),
                item.getTitle(),
                item.getAssignedDate(),
                item.getDueDate(),
                student.getId(),
                student.getName(),
                false,
                null,
                null
        );
    }
}
