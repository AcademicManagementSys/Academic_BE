package academic.academic.domain.dashboard.service;

import academic.academic.domain.attendance.entity.Attendance;
import academic.academic.domain.attendance.entity.AttendanceStatus;
import academic.academic.domain.attendance.repository.AttendanceRepository;
import academic.academic.domain.dashboard.dto.AdminDashboardResponse;
import academic.academic.domain.dashboard.dto.ClassStatisticsResponse;
import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.homework.entity.HomeworkRecord;
import academic.academic.domain.homework.repository.HomeworkItemRepository;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.entity.StudentStatus;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private HomeworkItemRepository homeworkItemRepository;
    @Mock
    private HomeworkRecordRepository homeworkRecordRepository;
    @Mock
    private ClassStatisticsService classStatisticsService;

    private AdminDashboardService adminDashboardService;

    private SchoolClass schoolClass;
    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        adminDashboardService = new AdminDashboardService(studentRepository, schoolClassRepository,
                attendanceRepository, homeworkItemRepository, homeworkRecordRepository, classStatisticsService);

        User teacher = User.builder().name("김선생").role(Role.TEACHER).loginId("teacher1").passwordHash("hash").build();
        ReflectionTestUtils.setField(teacher, "id", 1L);

        schoolClass = SchoolClass.builder().name("중2 심화반").teacher(teacher).build();
        ReflectionTestUtils.setField(schoolClass, "id", 3L);

        student1 = Student.builder().name("김민준").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student1, "id", 101L);

        student2 = Student.builder().name("이서연").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student2, "id", 102L);
    }

    @Nested
    class GetAdminDashboard {

        @Test
        void 전체_재원생_수와_오늘_출석률_숙제_미완료_건수_반별_현황을_반환한다() {
            LocalDate date = LocalDate.of(2026, 8, 19);

            HomeworkItem homeworkItem = HomeworkItem.builder().schoolClass(schoolClass).title("Ch.5")
                    .assignedDate(date).build();
            ReflectionTestUtils.setField(homeworkItem, "id", 200L);
            HomeworkRecord homeworkRecord = HomeworkRecord.builder().homeworkItem(homeworkItem).student(student1)
                    .done(true).build();

            given(studentRepository.search(null, StudentStatus.ENROLLED, true, null))
                    .willReturn(List.of(student1, student2));
            given(schoolClassRepository.findAll()).willReturn(List.of(schoolClass));
            given(studentRepository.findBySchoolClassId(3L)).willReturn(List.of(student1, student2));
            given(attendanceRepository.findByClassIdAndDate(3L, date)).willReturn(List.of(
                    Attendance.builder().student(student1).date(date).status(AttendanceStatus.PRESENT).build()
            ));
            given(homeworkItemRepository.search(3L, date, date)).willReturn(List.of(homeworkItem));
            given(homeworkRecordRepository.findByHomeworkItemId(200L)).willReturn(List.of(homeworkRecord));
            given(classStatisticsService.getClassStatistics(null))
                    .willReturn(List.of(new ClassStatisticsResponse(3L, "중2 심화반", 1L, "김선생", 2, 66.7, 75.0)));

            AdminDashboardResponse response = adminDashboardService.getAdminDashboard("2026-08-19");

            assertThat(response.date()).isEqualTo(date);
            assertThat(response.totalStudentCount()).isEqualTo(2);
            assertThat(response.todayAttendanceRate()).isEqualTo(50.0);
            assertThat(response.todayHomeworkUncheckedCount()).isEqualTo(1);
            assertThat(response.classes()).hasSize(1);
            assertThat(response.classes().get(0).className()).isEqualTo("중2 심화반");
        }

        @Test
        void 여러_반의_통계를_합산한다() {
            LocalDate date = LocalDate.of(2026, 8, 19);

            User teacher2 = User.builder().name("이선생").role(Role.TEACHER).loginId("teacher2").passwordHash("hash").build();
            ReflectionTestUtils.setField(teacher2, "id", 2L);
            SchoolClass schoolClass2 = SchoolClass.builder().name("초등 문법반").teacher(teacher2).build();
            ReflectionTestUtils.setField(schoolClass2, "id", 4L);
            Student student3 = Student.builder().name("박도윤").schoolClass(schoolClass2).build();
            ReflectionTestUtils.setField(student3, "id", 103L);

            HomeworkItem homeworkItemA = HomeworkItem.builder().schoolClass(schoolClass).title("Ch.5")
                    .assignedDate(date).build();
            ReflectionTestUtils.setField(homeworkItemA, "id", 200L);
            HomeworkRecord homeworkRecordA = HomeworkRecord.builder().homeworkItem(homeworkItemA).student(student1)
                    .done(true).build();

            HomeworkItem homeworkItemB = HomeworkItem.builder().schoolClass(schoolClass2).title("Ch.1")
                    .assignedDate(date).build();
            ReflectionTestUtils.setField(homeworkItemB, "id", 201L);

            given(studentRepository.search(null, StudentStatus.ENROLLED, true, null))
                    .willReturn(List.of(student1, student2, student3));
            given(schoolClassRepository.findAll()).willReturn(List.of(schoolClass, schoolClass2));

            given(studentRepository.findBySchoolClassId(3L)).willReturn(List.of(student1, student2));
            given(attendanceRepository.findByClassIdAndDate(3L, date)).willReturn(List.of(
                    Attendance.builder().student(student1).date(date).status(AttendanceStatus.PRESENT).build(),
                    Attendance.builder().student(student2).date(date).status(AttendanceStatus.ABSENT).build()
            ));
            given(homeworkItemRepository.search(3L, date, date)).willReturn(List.of(homeworkItemA));
            given(homeworkRecordRepository.findByHomeworkItemId(200L)).willReturn(List.of(homeworkRecordA));

            given(studentRepository.findBySchoolClassId(4L)).willReturn(List.of(student3));
            given(attendanceRepository.findByClassIdAndDate(4L, date)).willReturn(List.of());
            given(homeworkItemRepository.search(4L, date, date)).willReturn(List.of(homeworkItemB));
            given(homeworkRecordRepository.findByHomeworkItemId(201L)).willReturn(List.of());

            given(classStatisticsService.getClassStatistics(null)).willReturn(List.of(
                    new ClassStatisticsResponse(3L, "중2 심화반", 1L, "김선생", 2, 50.0, 50.0),
                    new ClassStatisticsResponse(4L, "초등 문법반", 2L, "이선생", 1, null, null)
            ));

            AdminDashboardResponse response = adminDashboardService.getAdminDashboard("2026-08-19");

            assertThat(response.totalStudentCount()).isEqualTo(3);
            assertThat(response.todayAttendanceRate()).isEqualTo(33.3);
            assertThat(response.todayHomeworkUncheckedCount()).isEqualTo(2);
            assertThat(response.classes()).hasSize(2);
        }

        @Test
        void 오늘_출석_대상_학생이_없으면_출석률은_null이다() {
            given(studentRepository.search(null, StudentStatus.ENROLLED, true, null)).willReturn(List.of());
            given(schoolClassRepository.findAll()).willReturn(List.of());
            given(classStatisticsService.getClassStatistics(null)).willReturn(List.of());

            AdminDashboardResponse response = adminDashboardService.getAdminDashboard("2026-08-19");

            assertThat(response.todayAttendanceRate()).isNull();
            assertThat(response.todayHomeworkUncheckedCount()).isZero();
        }

        @Test
        void date를_생략하면_오늘_날짜를_사용한다() {
            given(studentRepository.search(null, StudentStatus.ENROLLED, true, null)).willReturn(List.of());
            given(schoolClassRepository.findAll()).willReturn(List.of());
            given(classStatisticsService.getClassStatistics(null)).willReturn(List.of());

            AdminDashboardResponse response = adminDashboardService.getAdminDashboard(null);

            assertThat(response.date()).isEqualTo(LocalDate.now());
        }

        @Test
        void date_형식이_올바르지_않으면_VALIDATION_ERROR_예외() {
            assertThatThrownBy(() -> adminDashboardService.getAdminDashboard("2026/08/19"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }
}
