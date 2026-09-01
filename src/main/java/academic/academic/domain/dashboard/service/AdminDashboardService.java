package academic.academic.domain.dashboard.service;

import academic.academic.domain.attendance.entity.Attendance;
import academic.academic.domain.attendance.entity.AttendanceStatus;
import academic.academic.domain.attendance.repository.AttendanceRepository;
import academic.academic.domain.dashboard.dto.AdminDashboardResponse;
import academic.academic.domain.dashboard.dto.ClassStatisticsResponse;
import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.homework.repository.HomeworkItemRepository;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.entity.StudentStatus;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 관리자 대시보드 (FR-06-02). 전체 재원생 수, 오늘 출석률, 오늘 숙제 미완료 건수, 반별 현황을 제공한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AttendanceRepository attendanceRepository;
    private final HomeworkItemRepository homeworkItemRepository;
    private final HomeworkRecordRepository homeworkRecordRepository;
    private final ClassStatisticsService classStatisticsService;

    public AdminDashboardResponse getAdminDashboard(String date) {
        LocalDate targetDate = parseDate(date);
        int totalStudentCount = studentRepository.search(null, StudentStatus.ENROLLED, true, null).size();

        int attendanceTargetCount = 0;
        int presentCount = 0;
        int homeworkUncheckedCount = 0;
        for (SchoolClass schoolClass : schoolClassRepository.findAll()) {
            List<Student> students = studentRepository.findBySchoolClassId(schoolClass.getId());
            attendanceTargetCount += students.size();

            List<Attendance> todayAttendance = attendanceRepository.findByClassIdAndDate(schoolClass.getId(), targetDate);
            presentCount += (int) todayAttendance.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();

            for (HomeworkItem item : homeworkItemRepository.search(schoolClass.getId(), targetDate, targetDate)) {
                int recorded = homeworkRecordRepository.findByHomeworkItemId(item.getId()).size();
                homeworkUncheckedCount += Math.max(0, students.size() - recorded);
            }
        }
        Double todayAttendanceRate = attendanceTargetCount == 0 ? null
                : round1(presentCount * 100.0 / attendanceTargetCount);

        List<ClassStatisticsResponse> classes = classStatisticsService.getClassStatistics(null);

        return new AdminDashboardResponse(targetDate, totalStudentCount, todayAttendanceRate, homeworkUncheckedCount, classes);
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
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
