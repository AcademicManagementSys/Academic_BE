package academic.academic.domain.student.service;

import academic.academic.domain.attendance.entity.Attendance;
import academic.academic.domain.attendance.entity.AttendanceStatus;
import academic.academic.domain.attendance.repository.AttendanceRepository;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.monthlyexam.entity.MonthlyExamRecord;
import academic.academic.domain.monthlyexam.repository.MonthlyExamRecordRepository;
import academic.academic.domain.student.dto.AttendanceSummaryResponse;
import academic.academic.domain.student.dto.HomeworkSummaryItem;
import academic.academic.domain.student.dto.RecentMonthlyExamSummary;
import academic.academic.domain.student.dto.RecentTestSummary;
import academic.academic.domain.student.dto.StudentSummaryResponse;
import academic.academic.domain.student.dto.StudentSummaryStudentInfo;
import academic.academic.domain.student.dto.TestScoresSummary;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.test.entity.TestRecord;
import academic.academic.domain.test.entity.TestSession;
import academic.academic.domain.test.entity.TestSubject;
import academic.academic.domain.test.repository.TestRecordRepository;
import academic.academic.domain.test.repository.TestSessionRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 학생 홈 요약 (SCR-11, SCR-12, FR-07-01). 최근 출석·숙제·테스트·월말모의고사를 한 번에 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentSummaryService {

    private static final int RECENT_HOMEWORK_LIMIT = 5;

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final HomeworkRecordRepository homeworkRecordRepository;
    private final TestSessionRepository testSessionRepository;
    private final TestRecordRepository testRecordRepository;
    private final MonthlyExamRecordRepository monthlyExamRecordRepository;

    public StudentSummaryResponse getSummary(Long studentId, String month) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다. id=" + studentId));

        return new StudentSummaryResponse(
                StudentSummaryStudentInfo.from(student),
                buildAttendanceSummary(studentId, month),
                buildRecentHomework(studentId),
                buildRecentTest(studentId),
                buildRecentMonthlyExam(studentId)
        );
    }

    private AttendanceSummaryResponse buildAttendanceSummary(Long studentId, String month) {
        YearMonth yearMonth = parseMonth(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<Attendance> attendances = attendanceRepository.findByStudentIdAndDateBetweenOrderByDateAsc(studentId, start, end);
        int presentDays = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        int lateCount = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        int absentCount = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        return new AttendanceSummaryResponse(presentDays, attendances.size(), lateCount, absentCount);
    }

    private List<HomeworkSummaryItem> buildRecentHomework(Long studentId) {
        return homeworkRecordRepository.findRecentByStudentId(studentId, PageRequest.of(0, RECENT_HOMEWORK_LIMIT)).stream()
                .map(HomeworkSummaryItem::from)
                .toList();
    }

    private RecentTestSummary buildRecentTest(Long studentId) {
        List<TestSession> sessions = testSessionRepository.findRecentSessionsByStudentId(studentId, PageRequest.of(0, 1));
        if (sessions.isEmpty()) {
            return null;
        }
        TestSession session = sessions.get(0);
        Map<TestSubject, Integer> scoreBySubject = testRecordRepository
                .findByStudentIdAndTestSessionIdIn(studentId, List.of(session.getId())).stream()
                .filter(r -> r.getScore() != null)
                .collect(Collectors.toMap(TestRecord::getSubject, TestRecord::getScore));

        return new RecentTestSummary(session.getTestDate(), new TestScoresSummary(
                scoreBySubject.get(TestSubject.VOCAB),
                scoreBySubject.get(TestSubject.READING),
                scoreBySubject.get(TestSubject.GRAMMAR),
                scoreBySubject.get(TestSubject.SYNTAX)
        ));
    }

    private RecentMonthlyExamSummary buildRecentMonthlyExam(Long studentId) {
        List<MonthlyExamRecord> recent = monthlyExamRecordRepository.findRecentByStudentId(studentId, PageRequest.of(0, 2));
        if (recent.isEmpty()) {
            return null;
        }
        MonthlyExamRecord latest = recent.get(0);
        Integer deltaFromPrev = null;
        if (recent.size() > 1 && latest.getRawScore() != null && recent.get(1).getRawScore() != null) {
            deltaFromPrev = latest.getRawScore() - recent.get(1).getRawScore();
        }
        return new RecentMonthlyExamSummary(latest.getMonthlyExam().getExamMonth(), latest.getRawScore(), deltaFromPrev);
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
