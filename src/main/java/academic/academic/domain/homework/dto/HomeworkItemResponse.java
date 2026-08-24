package academic.academic.domain.homework.dto;

import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;

import java.time.LocalDate;

public record HomeworkItemResponse(
        Long id,
        Long classId,
        String className,
        Long studentId,
        String studentName,
        String title,
        String scope,
        LocalDate assignedDate,
        LocalDate dueDate
) {
    public static HomeworkItemResponse from(HomeworkItem item) {
        SchoolClass schoolClass = item.getSchoolClass();
        Student student = item.getStudent();
        return new HomeworkItemResponse(
                item.getId(),
                schoolClass != null ? schoolClass.getId() : null,
                schoolClass != null ? schoolClass.getName() : null,
                student != null ? student.getId() : null,
                student != null ? student.getName() : null,
                item.getTitle(),
                item.getScope(),
                item.getAssignedDate(),
                item.getDueDate()
        );
    }
}
