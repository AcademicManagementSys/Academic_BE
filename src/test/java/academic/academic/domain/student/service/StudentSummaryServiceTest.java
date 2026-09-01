package academic.academic.domain.student.service;

import academic.academic.domain.attendance.entity.Attendance;
import academic.academic.domain.attendance.entity.AttendanceStatus;
import academic.academic.domain.attendance.repository.AttendanceRepository;
import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.homework.entity.HomeworkRecord;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.monthlyexam.entity.MonthlyExam;
import academic.academic.domain.monthlyexam.entity.MonthlyExamRecord;
import academic.academic.domain.monthlyexam.repository.MonthlyExamRecordRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.dto.StudentSummaryResponse;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.test.entity.TestRecord;
import academic.academic.domain.test.entity.TestSession;
import academic.academic.domain.test.entity.TestSubject;
import academic.academic.domain.test.repository.TestRecordRepository;
import academic.academic.domain.test.repository.TestSessionRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StudentSummaryServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private HomeworkRecordRepository homeworkRecordRepository;
    @Mock
    private TestSessionRepository testSessionRepository;
    @Mock
    private TestRecordRepository testRecordRepository;
    @Mock
    private MonthlyExamRecordRepository monthlyExamRecordRepository;

    private StudentSummaryService studentSummaryService;

    private SchoolClass schoolClass;
    private Student student;

    @BeforeEach
    void setUp() {
        studentSummaryService = new StudentSummaryService(studentRepository, attendanceRepository,
                homeworkRecordRepository, testSessionRepository, testRecordRepository, monthlyExamRecordRepository);

        schoolClass = SchoolClass.builder().name("중2 심화반").build();
        ReflectionTestUtils.setField(schoolClass, "id", 3L);

        student = Student.builder().name("김민준").grade("중2").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student, "id", 101L);
    }

    @Nested
    class GetSummary {

        @Test
        void 최근_출석_숙제_테스트_모의고사_요약을_반환한다() {
            LocalDate start = LocalDate.of(2026, 8, 1);
            LocalDate end = LocalDate.of(2026, 8, 31);

            HomeworkItem homeworkItem = HomeworkItem.builder().schoolClass(schoolClass).title("구문 노트 정리")
                    .assignedDate(LocalDate.of(2026, 8, 17)).build();
            HomeworkRecord homeworkRecord = HomeworkRecord.builder().homeworkItem(homeworkItem).student(student)
                    .done(false).build();

            TestSession session = TestSession.builder().schoolClass(schoolClass).title("8월 3주차 테스트")
                    .testDate(LocalDate.of(2026, 8, 19)).build();
            ReflectionTestUtils.setField(session, "id", 900L);
            TestRecord vocabRecord = TestRecord.builder().testSession(session).student(student)
                    .subject(TestSubject.VOCAB).taken(true).score(18).maxScore(20).build();
            TestRecord readingRecord = TestRecord.builder().testSession(session).student(student)
                    .subject(TestSubject.READING).taken(true).score(16).maxScore(20).build();

            MonthlyExam julyExam = MonthlyExam.builder().examName("7월 모의고사").examMonth("2026-07").build();
            MonthlyExam augExam = MonthlyExam.builder().examName("8월 모의고사").examMonth("2026-08").build();
            MonthlyExamRecord augRecord = MonthlyExamRecord.builder().monthlyExam(augExam).student(student)
                    .rawScore(82).build();
            MonthlyExamRecord julyRecord = MonthlyExamRecord.builder().monthlyExam(julyExam).student(student)
                    .rawScore(84).build();

            given(studentRepository.findById(101L)).willReturn(Optional.of(student));
            given(attendanceRepository.findByStudentIdAndDateBetweenOrderByDateAsc(101L, start, end)).willReturn(List.of(
                    Attendance.builder().student(student).date(start).status(AttendanceStatus.PRESENT).build(),
                    Attendance.builder().student(student).date(start.plusDays(1)).status(AttendanceStatus.LATE).build()
            ));
            given(homeworkRecordRepository.findRecentByStudentId(101L, PageRequest.of(0, 5)))
                    .willReturn(List.of(homeworkRecord));
            given(testSessionRepository.findRecentSessionsByStudentId(101L, PageRequest.of(0, 1)))
                    .willReturn(List.of(session));
            given(testRecordRepository.findByStudentIdAndTestSessionIdIn(101L, List.of(900L)))
                    .willReturn(List.of(vocabRecord, readingRecord));
            given(monthlyExamRecordRepository.findRecentByStudentId(101L, PageRequest.of(0, 2)))
                    .willReturn(List.of(augRecord, julyRecord));

            StudentSummaryResponse response = studentSummaryService.getSummary(101L, "2026-08");

            assertThat(response.student().name()).isEqualTo("김민준");
            assertThat(response.student().className()).isEqualTo("중2 심화반");
            assertThat(response.attendance().totalDays()).isEqualTo(2);
            assertThat(response.attendance().presentDays()).isEqualTo(1);
            assertThat(response.attendance().lateCount()).isEqualTo(1);
            assertThat(response.attendance().absentCount()).isZero();
            assertThat(response.homework()).hasSize(1);
            assertThat(response.homework().get(0).title()).isEqualTo("구문 노트 정리");
            assertThat(response.homework().get(0).isDone()).isFalse();
            assertThat(response.recentTest().sessionDate()).isEqualTo(LocalDate.of(2026, 8, 19));
            assertThat(response.recentTest().scores().vocab()).isEqualTo(18);
            assertThat(response.recentTest().scores().reading()).isEqualTo(16);
            assertThat(response.recentTest().scores().grammar()).isNull();
            assertThat(response.recentMonthlyExam().examMonth()).isEqualTo("2026-08");
            assertThat(response.recentMonthlyExam().rawScore()).isEqualTo(82);
            assertThat(response.recentMonthlyExam().deltaFromPrev()).isEqualTo(-2);
        }

        @Test
        void 이전_회차가_없으면_deltaFromPrev는_null이다() {
            YearMonth now = YearMonth.now();
            MonthlyExam onlyExam = MonthlyExam.builder().examName("첫 모의고사").examMonth("2026-08").build();
            MonthlyExamRecord onlyRecord = MonthlyExamRecord.builder().monthlyExam(onlyExam).student(student)
                    .rawScore(82).build();

            given(studentRepository.findById(101L)).willReturn(Optional.of(student));
            given(attendanceRepository.findByStudentIdAndDateBetweenOrderByDateAsc(
                    101L, now.atDay(1), now.atEndOfMonth())).willReturn(List.of());
            given(homeworkRecordRepository.findRecentByStudentId(101L, PageRequest.of(0, 5))).willReturn(List.of());
            given(testSessionRepository.findRecentSessionsByStudentId(101L, PageRequest.of(0, 1))).willReturn(List.of());
            given(monthlyExamRecordRepository.findRecentByStudentId(101L, PageRequest.of(0, 2)))
                    .willReturn(List.of(onlyRecord));

            StudentSummaryResponse response = studentSummaryService.getSummary(101L, null);

            assertThat(response.recentMonthlyExam().rawScore()).isEqualTo(82);
            assertThat(response.recentMonthlyExam().deltaFromPrev()).isNull();
        }

        @Test
        void 응시하지_않은_영역의_점수는_null로_표시된다() {
            YearMonth now = YearMonth.now();
            TestSession session = TestSession.builder().schoolClass(schoolClass).title("8월 3주차 테스트")
                    .testDate(LocalDate.of(2026, 8, 19)).build();
            ReflectionTestUtils.setField(session, "id", 900L);
            TestRecord vocabOnly = TestRecord.builder().testSession(session).student(student)
                    .subject(TestSubject.VOCAB).taken(true).score(18).maxScore(20).build();
            TestRecord readingNotTaken = TestRecord.builder().testSession(session).student(student)
                    .subject(TestSubject.READING).taken(false).build();

            given(studentRepository.findById(101L)).willReturn(Optional.of(student));
            given(attendanceRepository.findByStudentIdAndDateBetweenOrderByDateAsc(
                    101L, now.atDay(1), now.atEndOfMonth())).willReturn(List.of());
            given(homeworkRecordRepository.findRecentByStudentId(101L, PageRequest.of(0, 5))).willReturn(List.of());
            given(testSessionRepository.findRecentSessionsByStudentId(101L, PageRequest.of(0, 1)))
                    .willReturn(List.of(session));
            given(testRecordRepository.findByStudentIdAndTestSessionIdIn(101L, List.of(900L)))
                    .willReturn(List.of(vocabOnly, readingNotTaken));
            given(monthlyExamRecordRepository.findRecentByStudentId(101L, PageRequest.of(0, 2))).willReturn(List.of());

            StudentSummaryResponse response = studentSummaryService.getSummary(101L, null);

            assertThat(response.recentTest().scores().vocab()).isEqualTo(18);
            assertThat(response.recentTest().scores().reading()).isNull();
            assertThat(response.recentTest().scores().grammar()).isNull();
            assertThat(response.recentTest().scores().syntax()).isNull();
        }

        @Test
        void 학생이_없으면_NOT_FOUND_예외() {
            given(studentRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> studentSummaryService.getSummary(999L, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        void 데이터가_없으면_테스트와_모의고사_요약은_null이고_숙제는_빈_목록이다() {
            YearMonth now = YearMonth.now();
            given(studentRepository.findById(101L)).willReturn(Optional.of(student));
            given(attendanceRepository.findByStudentIdAndDateBetweenOrderByDateAsc(
                    101L, now.atDay(1), now.atEndOfMonth())).willReturn(List.of());
            given(homeworkRecordRepository.findRecentByStudentId(101L, PageRequest.of(0, 5))).willReturn(List.of());
            given(testSessionRepository.findRecentSessionsByStudentId(101L, PageRequest.of(0, 1))).willReturn(List.of());
            given(monthlyExamRecordRepository.findRecentByStudentId(101L, PageRequest.of(0, 2))).willReturn(List.of());

            StudentSummaryResponse response = studentSummaryService.getSummary(101L, null);

            assertThat(response.attendance().totalDays()).isZero();
            assertThat(response.homework()).isEmpty();
            assertThat(response.recentTest()).isNull();
            assertThat(response.recentMonthlyExam()).isNull();
        }

        @Test
        void month_형식이_올바르지_않으면_VALIDATION_ERROR_예외() {
            given(studentRepository.findById(101L)).willReturn(Optional.of(student));

            assertThatThrownBy(() -> studentSummaryService.getSummary(101L, "2026/08"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }
}
