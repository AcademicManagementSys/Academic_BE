package academic.academic.domain.dashboard.service;

import academic.academic.domain.attendance.entity.Attendance;
import academic.academic.domain.attendance.entity.AttendanceStatus;
import academic.academic.domain.attendance.repository.AttendanceRepository;
import academic.academic.domain.dashboard.dto.ClassStatisticsResponse;
import academic.academic.domain.homework.entity.HomeworkRecord;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.user.entity.User;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 반별 요약 통계 대시보드 (FR-06-02, FR-06-03). 관리자는 전체 반, 선생님은 조회 권한으로 전체 반 통계를 확인한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassStatisticsService {

    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final HomeworkRecordRepository homeworkRecordRepository;

    public List<ClassStatisticsResponse> getClassStatistics(String month) {
        YearMonth yearMonth = parseMonth(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        return schoolClassRepository.findAll().stream()
                .map(schoolClass -> buildStatistics(schoolClass, start, end))
                .toList();
    }

    private ClassStatisticsResponse buildStatistics(SchoolClass schoolClass, LocalDate start, LocalDate end) {
        int studentCount = studentRepository.findBySchoolClassId(schoolClass.getId()).size();

        List<Attendance> attendances = attendanceRepository.findByClassIdAndDateBetween(schoolClass.getId(), start, end);
        Double attendanceRate = attendances.isEmpty() ? null
                : round1(attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count() * 100.0
                        / attendances.size());

        List<HomeworkRecord> homeworkRecords = homeworkRecordRepository.findByClassIdAndAssignedDateBetween(
                schoolClass.getId(), start, end);
        Double homeworkCompletionRate = homeworkRecords.isEmpty() ? null
                : round1(homeworkRecords.stream().filter(HomeworkRecord::isDone).count() * 100.0 / homeworkRecords.size());

        User teacher = schoolClass.getTeacher();
        return new ClassStatisticsResponse(
                schoolClass.getId(),
                schoolClass.getName(),
                teacher != null ? teacher.getId() : null,
                teacher != null ? teacher.getName() : null,
                studentCount,
                attendanceRate,
                homeworkCompletionRate
        );
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private YearMonth parseMonth(String month) {
        if (month == null) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "month 형식이 올바르지 않습니다. (yyyy-MM)");
        }
    }
}
