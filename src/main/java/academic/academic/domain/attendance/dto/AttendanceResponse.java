package academic.academic.domain.attendance.dto;

import academic.academic.domain.attendance.entity.Attendance;
import academic.academic.domain.attendance.entity.AttendanceStatus;
import academic.academic.domain.student.entity.Student;

import java.time.LocalDate;

public record AttendanceResponse(
        Long id,
        Long studentId,
        String studentName,
        LocalDate date,
        AttendanceStatus status,
        String note
) {
    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getStudent().getId(),
                attendance.getStudent().getName(),
                attendance.getDate(),
                attendance.getStatus(),
                attendance.getNote()
        );
    }

    public static AttendanceResponse unchecked(Student student, LocalDate date) {
        return new AttendanceResponse(null, student.getId(), student.getName(), date, null, null);
    }
}
