package academic.academic.domain.dashboard.service;

import academic.academic.domain.attendance.entity.Attendance;
import academic.academic.domain.attendance.entity.AttendanceStatus;
import academic.academic.domain.attendance.repository.AttendanceRepository;
import academic.academic.domain.dashboard.dto.ClassRateResponse;
import academic.academic.domain.dashboard.dto.TeacherDashboardResponse;
import academic.academic.domain.homework.entity.HomeworkRecord;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;
import academic.academic.domain.user.repository.UserRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 선생님 대시보드 (FR-06-01, FR-06-03, v1.1). 담당 반(myClasses)과 학원 전체 반(allClassesSummary)의
 * 오늘 출석률·숙제완료율을 함께 제공한다. API_명세서_V2 §13 참고.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherDashboardService {

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AttendanceRepository attendanceRepository;
    private final HomeworkRecordRepository homeworkRecordRepository;

    public TeacherDashboardResponse getTeacherDashboard(Long teacherId, String date) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "선생님을 찾을 수 없습니다. id=" + teacherId));
        if (teacher.getRole() != Role.TEACHER) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "선택한 사용자는 선생님이 아닙니다.");
        }
        LocalDate targetDate = parseDate(date);

        List<ClassRateResponse> myClasses = schoolClassRepository.findByTeacherId(teacherId).stream()
                .map(schoolClass -> buildRate(schoolClass, targetDate))
                .toList();
        List<ClassRateResponse> allClassesSummary = schoolClassRepository.findAll().stream()
                .map(schoolClass -> buildRate(schoolClass, targetDate))
                .toList();

        return new TeacherDashboardResponse(myClasses, allClassesSummary);
    }

    private ClassRateResponse buildRate(SchoolClass schoolClass, LocalDate date) {
        List<Attendance> attendances = attendanceRepository.findByClassIdAndDate(schoolClass.getId(), date);
        Double todayAttendanceRate = attendances.isEmpty() ? null
                : round2(attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count()
                        / (double) attendances.size());

        List<HomeworkRecord> homeworkRecords = homeworkRecordRepository.findByClassIdAndAssignedDateBetween(
                schoolClass.getId(), date, date);
        Double homeworkDoneRate = homeworkRecords.isEmpty() ? null
                : round2(homeworkRecords.stream().filter(HomeworkRecord::isDone).count()
                        / (double) homeworkRecords.size());

        return new ClassRateResponse(schoolClass.getId(), schoolClass.getName(), todayAttendanceRate, homeworkDoneRate);
    }

    private double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private LocalDate parseDate(String date) {
        if (date == null) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "date 형식이 올바르지 않습니다. (yyyy-MM-dd)");
        }
    }
}
