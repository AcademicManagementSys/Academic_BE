package academic.academic.domain.student.dto;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.entity.StudentStatus;

import java.time.LocalDate;

public record StudentResponse(
        Long id,
        String name,
        LocalDate birthDate,
        String school,
        String grade,
        String phone,
        Long classId,
        String className,
        StudentStatus status,
        LocalDate enrolledAt
) {
    public static StudentResponse from(Student student) {
        SchoolClass schoolClass = student.getSchoolClass();
        return new StudentResponse(
                student.getId(),
                student.getName(),
                student.getBirthDate(),
                student.getSchool(),
                student.getGrade(),
                student.getPhone(),
                schoolClass != null ? schoolClass.getId() : null,
                schoolClass != null ? schoolClass.getName() : null,
                student.getStatus(),
                student.getEnrolledAt()
        );
    }
}
