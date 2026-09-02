package academic.academic.domain.dashboard.service;

import academic.academic.domain.attendance.entity.Attendance;
import academic.academic.domain.attendance.entity.AttendanceStatus;
import academic.academic.domain.attendance.repository.AttendanceRepository;
import academic.academic.domain.dashboard.dto.ClassRateResponse;
import academic.academic.domain.dashboard.dto.TeacherDashboardResponse;
import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.homework.entity.HomeworkRecord;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.entity.Student;
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
    private AttendanceRepository attendanceRepository;
    @Mock
    private HomeworkRecordRepository homeworkRecordRepository;

    private TeacherDashboardService teacherDashboardService;

    private User teacher;
    private SchoolClass schoolClass;
    private SchoolClass otherClass;
    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        teacherDashboardService = new TeacherDashboardService(userRepository, schoolClassRepository,
                attendanceRepository, homeworkRecordRepository);

        teacher = User.builder().name("김선생").role(Role.TEACHER).loginId("teacher1").passwordHash("hash").build();
        ReflectionTestUtils.setField(teacher, "id", 1L);

        schoolClass = SchoolClass.builder().name("중2 심화반").teacher(teacher).build();
        ReflectionTestUtils.setField(schoolClass, "id", 3L);

        otherClass = SchoolClass.builder().name("초등 문법반").build();
        ReflectionTestUtils.setField(otherClass, "id", 1L);

        student1 = Student.builder().name("김민준").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student1, "id", 101L);

        student2 = Student.builder().name("이서연").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student2, "id", 102L);
    }

    @Nested
    class GetTeacherDashboard {

        @Test
        void 담당_반과_전체_반의_오늘_출석률_숙제완료율을_반환한다() {
            LocalDate date = LocalDate.of(2026, 8, 19);

            HomeworkItem homeworkItem = HomeworkItem.builder().schoolClass(schoolClass).title("Ch.5")
                    .assignedDate(date).build();
            ReflectionTestUtils.setField(homeworkItem, "id", 200L);
            HomeworkRecord doneRecord = HomeworkRecord.builder().homeworkItem(homeworkItem).student(student1)
                    .done(true).build();
            HomeworkRecord notDoneRecord = HomeworkRecord.builder().homeworkItem(homeworkItem).student(student2)
                    .done(false).build();

            given(userRepository.findById(1L)).willReturn(Optional.of(teacher));
            given(schoolClassRepository.findByTeacherId(1L)).willReturn(List.of(schoolClass));
            given(schoolClassRepository.findAll()).willReturn(List.of(otherClass, schoolClass));

            given(attendanceRepository.findByClassIdAndDate(3L, date))
                    .willReturn(List.of(
                            Attendance.builder().student(student1).date(date).status(AttendanceStatus.PRESENT).build(),
                            Attendance.builder().student(student2).date(date).status(AttendanceStatus.ABSENT).build()));
            given(homeworkRecordRepository.findByClassIdAndAssignedDateBetween(3L, date, date))
                    .willReturn(List.of(doneRecord, notDoneRecord));

            given(attendanceRepository.findByClassIdAndDate(1L, date)).willReturn(List.of());
            given(homeworkRecordRepository.findByClassIdAndAssignedDateBetween(1L, date, date)).willReturn(List.of());

            TeacherDashboardResponse response = teacherDashboardService.getTeacherDashboard(1L, "2026-08-19");

            assertThat(response.myClasses()).hasSize(1);
            ClassRateResponse mine = response.myClasses().get(0);
            assertThat(mine.classId()).isEqualTo(3L);
            assertThat(mine.className()).isEqualTo("중2 심화반");
            assertThat(mine.todayAttendanceRate()).isEqualTo(0.5);
            assertThat(mine.homeworkDoneRate()).isEqualTo(0.5);

            assertThat(response.allClassesSummary()).hasSize(2);
            ClassRateResponse empty = response.allClassesSummary().stream()
                    .filter(c -> c.classId().equals(1L)).findFirst().orElseThrow();
            assertThat(empty.todayAttendanceRate()).isNull();
            assertThat(empty.homeworkDoneRate()).isNull();
        }

        @Test
        void date를_생략하면_오늘_날짜를_사용한다() {
            given(userRepository.findById(1L)).willReturn(Optional.of(teacher));
            given(schoolClassRepository.findByTeacherId(1L)).willReturn(List.of());
            given(schoolClassRepository.findAll()).willReturn(List.of());

            TeacherDashboardResponse response = teacherDashboardService.getTeacherDashboard(1L, null);

            assertThat(response.myClasses()).isEmpty();
            assertThat(response.allClassesSummary()).isEmpty();
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
