package academic.academic.domain.dashboard.service;

import academic.academic.domain.attendance.repository.AttendanceRepository;
import academic.academic.domain.dashboard.dto.ClassChecklistResponse;
import academic.academic.domain.dashboard.dto.ClassStatisticsResponse;
import academic.academic.domain.dashboard.dto.TeacherDashboardResponse;
import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.homework.repository.HomeworkItemRepository;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.test.entity.TestSession;
import academic.academic.domain.test.repository.TestRecordRepository;
import academic.academic.domain.test.repository.TestSessionRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 선생님 대시보드 (FR-06-01, FR-06-03). 담당 반의 오늘 수업/미입력 항목(출석·숙제·테스트 미체크)과
 * 학원 전체 반의 통계 요약(조회 전용)을 함께 제공한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherDashboardService {

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final HomeworkItemRepository homeworkItemRepository;
    private final HomeworkRecordRepository homeworkRecordRepository;
    private final TestSessionRepository testSessionRepository;
    private final TestRecordRepository testRecordRepository;
    private final ClassStatisticsService classStatisticsService;

    public TeacherDashboardResponse getTeacherDashboard(Long teacherId, String date) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "선생님을 찾을 수 없습니다. id=" + teacherId));
        if (teacher.getRole() != Role.TEACHER) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "선택한 사용자는 선생님이 아닙니다.");
        }
        LocalDate targetDate = parseDate(date);

        List<ClassChecklistResponse> items = schoolClassRepository.findByTeacherId(teacherId).stream()
                .map(schoolClass -> buildChecklist(schoolClass, targetDate))
                .toList();
        List<ClassStatisticsResponse> allClassesSummary = classStatisticsService.getClassStatistics(null);
        return new TeacherDashboardResponse(targetDate, items, allClassesSummary);
    }

    private ClassChecklistResponse buildChecklist(SchoolClass schoolClass, LocalDate date) {
        List<Student> students = studentRepository.findBySchoolClassId(schoolClass.getId());
        int studentCount = students.size();

        int attendanceChecked = attendanceRepository.findByClassIdAndDate(schoolClass.getId(), date).size();

        List<HomeworkItem> homeworkItems = homeworkItemRepository.search(schoolClass.getId(), date, date);
        int homeworkUnchecked = 0;
        for (HomeworkItem item : homeworkItems) {
            int recorded = homeworkRecordRepository.findByHomeworkItemId(item.getId()).size();
            homeworkUnchecked += Math.max(0, studentCount - recorded);
        }

        List<TestSession> testSessions = testSessionRepository.findBySchoolClassIdAndTestDate(schoolClass.getId(), date);
        int testUnchecked = 0;
        if (!testSessions.isEmpty()) {
            List<Long> sessionIds = testSessions.stream().map(TestSession::getId).toList();
            Set<Long> studentsWithRecord = new HashSet<>(testRecordRepository.findDistinctStudentIdsByTestSessionIdIn(sessionIds));
            testUnchecked = (int) students.stream().filter(s -> !studentsWithRecord.contains(s.getId())).count();
        }

        return new ClassChecklistResponse(
                schoolClass.getId(),
                schoolClass.getName(),
                studentCount,
                Math.max(0, studentCount - attendanceChecked),
                homeworkItems.size(),
                homeworkUnchecked,
                testSessions.size(),
                testUnchecked
        );
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
