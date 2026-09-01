package academic.academic.domain.student.service;

import academic.academic.domain.attendance.repository.AttendanceRepository;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.monthlyexam.repository.MonthlyExamRecordRepository;
import academic.academic.domain.student.dto.NotificationBadgeResponse;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.test.repository.TestRecordRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NotificationBadgeServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private HomeworkRecordRepository homeworkRecordRepository;
    @Mock
    private TestRecordRepository testRecordRepository;
    @Mock
    private MonthlyExamRecordRepository monthlyExamRecordRepository;

    private NotificationBadgeService notificationBadgeService;

    @BeforeEach
    void setUp() {
        notificationBadgeService = new NotificationBadgeService(studentRepository, attendanceRepository,
                homeworkRecordRepository, testRecordRepository, monthlyExamRecordRepository);
    }

    @Nested
    class GetBadge {

        @Test
        void since_이후_새로_생긴_항목_건수를_영역별로_합산한다() {
            LocalDateTime since = LocalDateTime.of(2026, 8, 19, 0, 0);
            given(studentRepository.existsById(101L)).willReturn(true);
            given(attendanceRepository.countByStudentIdAndCreatedAtAfter(101L, since)).willReturn(1L);
            given(homeworkRecordRepository.countByStudentIdAndCreatedAtAfter(101L, since)).willReturn(2L);
            given(testRecordRepository.countByStudentIdAndCreatedAtAfter(101L, since)).willReturn(0L);
            given(monthlyExamRecordRepository.countByStudentIdAndCreatedAtAfter(101L, since)).willReturn(1L);

            NotificationBadgeResponse response = notificationBadgeService.getBadge(101L, "2026-08-19T00:00:00");

            assertThat(response.since()).isEqualTo(since);
            assertThat(response.attendanceCount()).isEqualTo(1);
            assertThat(response.homeworkCount()).isEqualTo(2);
            assertThat(response.testCount()).isEqualTo(0);
            assertThat(response.monthlyExamCount()).isEqualTo(1);
            assertThat(response.totalCount()).isEqualTo(4);
        }

        @Test
        void since를_생략하면_오늘_자정을_사용한다() {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            given(studentRepository.existsById(101L)).willReturn(true);
            given(attendanceRepository.countByStudentIdAndCreatedAtAfter(101L, todayStart)).willReturn(0L);
            given(homeworkRecordRepository.countByStudentIdAndCreatedAtAfter(101L, todayStart)).willReturn(0L);
            given(testRecordRepository.countByStudentIdAndCreatedAtAfter(101L, todayStart)).willReturn(0L);
            given(monthlyExamRecordRepository.countByStudentIdAndCreatedAtAfter(101L, todayStart)).willReturn(0L);

            NotificationBadgeResponse response = notificationBadgeService.getBadge(101L, null);

            assertThat(response.since()).isEqualTo(todayStart);
            assertThat(response.totalCount()).isZero();
        }

        @Test
        void 학생이_없으면_NOT_FOUND_예외() {
            given(studentRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> notificationBadgeService.getBadge(999L, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        void since_형식이_올바르지_않으면_VALIDATION_ERROR_예외() {
            given(studentRepository.existsById(101L)).willReturn(true);

            assertThatThrownBy(() -> notificationBadgeService.getBadge(101L, "2026-08-19"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }
}
