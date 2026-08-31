package academic.academic.domain.dashboard.service;

import academic.academic.domain.attendance.entity.Attendance;
import academic.academic.domain.attendance.entity.AttendanceStatus;
import academic.academic.domain.attendance.repository.AttendanceRepository;
import academic.academic.domain.dashboard.dto.ClassStatisticsResponse;
import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.homework.entity.HomeworkRecord;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.entity.Student;
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
class ClassStatisticsServiceTest {

    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private HomeworkRecordRepository homeworkRecordRepository;

    private ClassStatisticsService classStatisticsService;

    private User teacher;
    private SchoolClass schoolClass;
    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        classStatisticsService = new ClassStatisticsService(
                schoolClassRepository, studentRepository, attendanceRepository, homeworkRecordRepository);

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
    class GetClassStatistics {

        @Test
        void 반별_출석률과_숙제_완료율을_반환한다() {
            LocalDate start = LocalDate.of(2026, 8, 1);
            LocalDate end = LocalDate.of(2026, 8, 31);

            HomeworkItem homeworkItem = HomeworkItem.builder().schoolClass(schoolClass).title("Ch.5")
                    .assignedDate(LocalDate.of(2026, 8, 10)).build();

            given(schoolClassRepository.findAll()).willReturn(List.of(schoolClass));
            given(studentRepository.findBySchoolClassId(3L)).willReturn(List.of(student1, student2));
            given(attendanceRepository.findByClassIdAndDateBetween(3L, start, end)).willReturn(List.of(
                    Attendance.builder().student(student1).date(start).status(AttendanceStatus.PRESENT).build(),
                    Attendance.builder().student(student2).date(start).status(AttendanceStatus.PRESENT).build(),
                    Attendance.builder().student(student1).date(start.plusDays(1)).status(AttendanceStatus.ABSENT).build()
            ));
            given(homeworkRecordRepository.findByClassIdAndAssignedDateBetween(3L, start, end)).willReturn(List.of(
                    HomeworkRecord.builder().homeworkItem(homeworkItem).student(student1).done(true).build(),
                    HomeworkRecord.builder().homeworkItem(homeworkItem).student(student2).done(true).build(),
                    HomeworkRecord.builder().homeworkItem(homeworkItem).student(student1).done(false).build(),
                    HomeworkRecord.builder().homeworkItem(homeworkItem).student(student2).done(true).build()
            ));

            List<ClassStatisticsResponse> result = classStatisticsService.getClassStatistics("2026-08");

            assertThat(result).hasSize(1);
            ClassStatisticsResponse stat = result.get(0);
            assertThat(stat.classId()).isEqualTo(3L);
            assertThat(stat.teacherName()).isEqualTo("김선생");
            assertThat(stat.studentCount()).isEqualTo(2);
            assertThat(stat.attendanceRate()).isEqualTo(66.7);
            assertThat(stat.homeworkCompletionRate()).isEqualTo(75.0);
        }

        @Test
        void 기록이_없으면_비율은_null이다() {
            given(schoolClassRepository.findAll()).willReturn(List.of(schoolClass));
            given(studentRepository.findBySchoolClassId(3L)).willReturn(List.of(student1));
            given(attendanceRepository.findByClassIdAndDateBetween(3L,
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))).willReturn(List.of());
            given(homeworkRecordRepository.findByClassIdAndAssignedDateBetween(3L,
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))).willReturn(List.of());

            List<ClassStatisticsResponse> result = classStatisticsService.getClassStatistics("2026-08");

            assertThat(result.get(0).attendanceRate()).isNull();
            assertThat(result.get(0).homeworkCompletionRate()).isNull();
        }

        @Test
        void month를_생략하면_현재월을_사용한다() {
            given(schoolClassRepository.findAll()).willReturn(List.of());

            List<ClassStatisticsResponse> result = classStatisticsService.getClassStatistics(null);

            assertThat(result).isEmpty();
        }

        @Test
        void month_형식이_올바르지_않으면_VALIDATION_ERROR_예외() {
            assertThatThrownBy(() -> classStatisticsService.getClassStatistics("2026-13"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }
}
