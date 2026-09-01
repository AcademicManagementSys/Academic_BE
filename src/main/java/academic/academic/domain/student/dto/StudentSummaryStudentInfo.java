package academic.academic.domain.student.dto;

import academic.academic.domain.student.entity.Student;
import academic.academic.domain.schoolclass.entity.SchoolClass;

public record StudentSummaryStudentInfo(
        Long id,
        String name,
        String grade,
        String className
) {
    public static StudentSummaryStudentInfo from(Student student) {
        SchoolClass schoolClass = student.getSchoolClass();
        return new StudentSummaryStudentInfo(
                student.getId(),
                student.getName(),
                student.getGrade(),
                schoolClass != null ? schoolClass.getName() : null
        );
    }
}
