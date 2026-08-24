package academic.academic.domain.teacherassignment.dto;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.teacherassignment.entity.TeacherAssignment;

public record TeacherAssignmentResponse(
        Long id,
        Long teacherId,
        String teacherName,
        Long classId,
        String className,
        Long studentId,
        String studentName
) {
    public static TeacherAssignmentResponse from(TeacherAssignment assignment) {
        SchoolClass schoolClass = assignment.getSchoolClass();
        Student student = assignment.getStudent();
        return new TeacherAssignmentResponse(
                assignment.getId(),
                assignment.getTeacher().getId(),
                assignment.getTeacher().getName(),
                schoolClass != null ? schoolClass.getId() : null,
                schoolClass != null ? schoolClass.getName() : null,
                student != null ? student.getId() : null,
                student != null ? student.getName() : null
        );
    }
}
