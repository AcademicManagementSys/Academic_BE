package academic.academic.domain.attendance.service;

import academic.academic.domain.attendance.dto.AttendanceBulkRequest;
import academic.academic.domain.attendance.dto.AttendanceRecordItem;
import academic.academic.domain.attendance.dto.AttendanceResponse;
import academic.academic.domain.attendance.dto.AttendanceUpdateRequest;
import academic.academic.domain.attendance.entity.Attendance;
import academic.academic.domain.attendance.repository.AttendanceRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;

    @Transactional
    public List<AttendanceResponse> saveBulk(AttendanceBulkRequest request) {
        SchoolClass schoolClass = schoolClassRepository.findById(request.classId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "반을 찾을 수 없습니다. id=" + request.classId()));

        List<AttendanceResponse> responses = new ArrayList<>();
        for (AttendanceRecordItem item : request.records()) {
            Student student = studentRepository.findById(item.studentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다. id=" + item.studentId()));
            if (student.getSchoolClass() == null || !student.getSchoolClass().getId().equals(schoolClass.getId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "해당 반 소속 학생이 아닙니다. studentId=" + item.studentId());
            }

            Attendance attendance = attendanceRepository.findByStudentIdAndDate(student.getId(), request.date())
                    .orElse(null);
            if (attendance == null) {
                attendance = attendanceRepository.save(Attendance.builder()
                        .student(student)
                        .date(request.date())
                        .status(item.status())
                        .note(item.note())
                        .build());
            } else {
                attendance.update(item.status(), item.note());
            }
            responses.add(AttendanceResponse.from(attendance));
        }
        return responses;
    }

    public List<AttendanceResponse> getAttendanceByClassAndDate(Long classId, String date) {
        schoolClassRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "반을 찾을 수 없습니다. id=" + classId));
        LocalDate parsedDate = parseDate(date);

        List<Student> students = studentRepository.findBySchoolClassId(classId);
        Map<Long, Attendance> byStudentId = attendanceRepository.findByClassIdAndDate(classId, parsedDate).stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

        return students.stream()
                .map(s -> {
                    Attendance a = byStudentId.get(s.getId());
                    return a != null ? AttendanceResponse.from(a) : AttendanceResponse.unchecked(s, parsedDate);
                })
                .toList();
    }

    /** 소유권(담당 반) 체크용 — 수정하려는 출석 기록이 어느 반 소속인지 조회한다. */
    public Long getClassIdForAttendance(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "출석 기록을 찾을 수 없습니다. id=" + id));
        SchoolClass schoolClass = attendance.getStudent().getSchoolClass();
        return schoolClass != null ? schoolClass.getId() : null;
    }

    @Transactional
    public AttendanceResponse updateAttendance(Long id, AttendanceUpdateRequest request) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "출석 기록을 찾을 수 없습니다. id=" + id));
        attendance.update(request.status(), request.note());
        return AttendanceResponse.from(attendance);
    }

    public List<AttendanceResponse> getStudentAttendance(Long studentId, String month) {
        if (!studentRepository.existsById(studentId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다. id=" + studentId);
        }
        YearMonth yearMonth = parseMonth(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return attendanceRepository.findByStudentIdAndDateBetweenOrderByDateAsc(studentId, start, end)
                .stream().map(AttendanceResponse::from).toList();
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "date 형식이 올바르지 않습니다. (yyyy-MM-dd)");
        }
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "month 형식이 올바르지 않습니다. (yyyy-MM)");
        }
    }
}
