package academic.academic.domain.dashboard.service;

import academic.academic.domain.attendance.entity.Attendance;
import academic.academic.domain.attendance.entity.AttendanceStatus;
import academic.academic.domain.attendance.repository.AttendanceRepository;
import academic.academic.domain.dashboard.dto.ClassChecklistResponse;
import academic.academic.domain.dashboard.dto.ClassStatisticsResponse;
import academic.academic.domain.dashboard.dto.TeacherDashboardResponse;
import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.homework.entity.HomeworkRecord;
import academic.academic.domain.homework.repository.HomeworkItemRepository;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.test.entity.TestRecord;
import academic.academic.domain.test.entity.TestSession;
import academic.academic.domain.test.entity.TestSubject;
import academic.academic.domain.test.repository.TestRecordRepository;
import academic.academic.domain.test.repository.TestSessionRepository;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;
import academic.academic.domain.user.repository.UserRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TeacherDashboardServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private HomeworkItemRepository homeworkItemRepository;
    @Mock
    private HomeworkRecordRepository homeworkRecordRepository;
    @Mock
    private TestSessionRepository testSessionRepository;
    @Mock
    private TestRecordRepository testRecordRepository;
    @Mock
    private ClassStatisticsService classStatisticsService;

    private TeacherDashboardService teacherDashboardService;

    private User teacher;
    private SchoolClass schoolClass;
    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        teacherDashboardService = new TeacherDashboardService(userRepository, schoolClassRepository, studentRepository,
                attendanceRepository, homeworkItemRepository, homeworkRecordRepository,
                testSessionRepository, testRecordRepository, classStatisticsService);

        teacher = User.builder().name("김선생").role(Role.TEACHER).loginId("teacher1").passwordHash("hash").build();
        ReflectionTestUtils.setField(teacher, "id", 1L);

        schoolClass = SchoolClass.builder().name("중2 심화반").teacher(teacher).build();
        ReflectionTestUtils.setField(schoolClass, "id", 3L);

        student1 = Student.builder().name("김민준").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student1, "id", 101L);

        student2 = Student.builder().name("이서연").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student2, "id", 102L);
    }

    @Nested
    class GetTeacherDashboard {

        @Test
        void 담당_반의_미체크_항목_현황을_반환한다() {
            LocalDate date = LocalDate.of(2026, 8, 19);

            HomeworkItem homeworkItem = HomeworkItem.builder().schoolClass(schoolClass).title("Ch.5")
                    .assignedDate(date).build();
            ReflectionTestUtils.setField(homeworkItem, "id", 200L);
            HomeworkRecord homeworkRecord = HomeworkRecord.builder().homeworkItem(homeworkItem).student(student1)
                    .done(true).build();

            TestSession testSession = TestSession.builder().schoolClass(schoolClass).title("8월 3주차 테스트")
                    .testDate(date).build();
            ReflectionTestUtils.setField(testSession, "id", 300L);

            given(userRepository.findById(1L)).willReturn(Optional.of(teacher));
            given(schoolClassRepository.findByTeacherId(1L)).willReturn(List.of(schoolClass));
            given(studentRepository.findBySchoolClassId(3L)).willReturn(List.of(student1, student2));
            given(attendanceRepository.findByClassIdAndDate(3L, date))
                    .willReturn(List.of(Attendance.builder().student(student1).date(date)
                            .status(AttendanceStatus.PRESENT).build()));
            given(homeworkItemRepository.search(3L, date, date)).willReturn(List.of(homeworkItem));
            given(homeworkRecordRepository.findByHomeworkItemId(200L)).willReturn(List.of(homeworkRecord));
            given(testSessionRepository.findBySchoolClassIdAndTestDate(3L, date)).willReturn(List.of(testSession));
            given(testRecordRepository.findDistinctStudentIdsByTestSessionIdIn(List.of(300L)))
                    .willReturn(List.of(101L));
            given(classStatisticsService.getClassStatistics(null))
                    .willReturn(List.of(new ClassStatisticsResponse(3L, "중2 심화반", 1L, "김선생", 2, 66.7, 75.0)));

            TeacherDashboardResponse response = teacherDashboardService.getTeacherDashboard(1L, "2026-08-19");

            assertThat(response.date()).isEqualTo(date);
            assertThat(response.classes()).hasSize(1);
            ClassChecklistResponse item = response.classes().get(0);
            assertThat(item.classId()).isEqualTo(3L);
            assertThat(item.studentCount()).isEqualTo(2);
            assertThat(item.attendanceUncheckedCount()).isEqualTo(1);
            assertThat(item.homeworkItemCount()).isEqualTo(1);
            assertThat(item.homeworkUncheckedCount()).isEqualTo(1);
            assertThat(item.testSessionCount()).isEqualTo(1);
            assertThat(item.testUncheckedCount()).isEqualTo(1);
            assertThat(response.allClassesSummary()).hasSize(1);
            assertThat(response.allClassesSummary().get(0).className()).isEqualTo("중2 심화반");
        }

        @Test
        void date를_생략하면_오늘_날짜를_사용한다() {
            given(userRepository.findById(1L)).willReturn(Optional.of(teacher));
            given(schoolClassRepository.findByTeacherId(1L)).willReturn(List.of());
            given(classStatisticsService.getClassStatistics(null)).willReturn(List.of());

            TeacherDashboardResponse response = teacherDashboardService.getTeacherDashboard(1L, null);

            assertThat(response.date()).isEqualTo(LocalDate.now());
        }

        @Test
        void 선생님이_없으면_NOT_FOUND_예외() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> teacherDashboardService.getTeacherDashboard(999L, "2026-08-19"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        void 선생님이_아니면_VALIDATION_ERROR_예외() {
            User parent = User.builder().name("학부모").role(Role.PARENT).loginId("parent1").passwordHash("hash").build();
            ReflectionTestUtils.setField(parent, "id", 2L);
            given(userRepository.findById(2L)).willReturn(Optional.of(parent));

            assertThatThrownBy(() -> teacherDashboardService.getTeacherDashboard(2L, "2026-08-19"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }

        @Test
        void date_형식이_올바르지_않으면_VALIDATION_ERROR_예외() {
            given(userRepository.findById(1L)).willReturn(Optional.of(teacher));

            assertThatThrownBy(() -> teacherDashboardService.getTeacherDashboard(1L, "2026/08/19"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }
}
