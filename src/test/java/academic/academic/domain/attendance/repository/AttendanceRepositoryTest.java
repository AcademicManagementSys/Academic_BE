package academic.academic.domain.attendance.repository;

import academic.academic.domain.attendance.entity.Attendance;
import academic.academic.domain.attendance.entity.AttendanceStatus;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class AttendanceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Test
    void 학생과_날짜로_출석_기록을_조회한다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        entityManager.persist(Attendance.builder().student(student).date(LocalDate.of(2026, 8, 19))
                .status(AttendanceStatus.PRESENT).build());
        entityManager.flush();
        entityManager.clear();

        Optional<Attendance> found = attendanceRepository.findByStudentIdAndDate(student.getId(), LocalDate.of(2026, 8, 19));

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    void 반과_날짜로_출석_기록_목록을_조회한다() {
        SchoolClass classA = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        SchoolClass classB = entityManager.persist(SchoolClass.builder().name("초등 문법반").build());
        Student studentInA1 = entityManager.persist(Student.builder().name("김민준").schoolClass(classA).build());
        Student studentInA2 = entityManager.persist(Student.builder().name("이서연").schoolClass(classA).build());
        Student studentInB = entityManager.persist(Student.builder().name("박서준").schoolClass(classB).build());

        LocalDate date = LocalDate.of(2026, 8, 19);
        entityManager.persist(Attendance.builder().student(studentInA1).date(date).status(AttendanceStatus.PRESENT).build());
        entityManager.persist(Attendance.builder().student(studentInA2).date(date).status(AttendanceStatus.LATE).build());
        entityManager.persist(Attendance.builder().student(studentInB).date(date).status(AttendanceStatus.ABSENT).build());
        entityManager.flush();
        entityManager.clear();

        List<Attendance> result = attendanceRepository.findByClassIdAndDate(classA.getId(), date);

        assertThat(result).hasSize(2)
                .extracting(a -> a.getStudent().getId())
                .containsExactlyInAnyOrder(studentInA1.getId(), studentInA2.getId());
    }

    @Test
    void 같은_학생_같은_날짜의_출석은_중복_저장할_수_없다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        LocalDate date = LocalDate.of(2026, 8, 19);
        entityManager.persist(Attendance.builder().student(student).date(date).status(AttendanceStatus.PRESENT).build());
        entityManager.flush();

        Attendance duplicate = Attendance.builder().student(student).date(date).status(AttendanceStatus.LATE).build();

        assertThatThrownBy(() -> attendanceRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 학생의_기간_내_출석_기록을_날짜순으로_조회한다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        entityManager.persist(Attendance.builder().student(student).date(LocalDate.of(2026, 8, 10))
                .status(AttendanceStatus.PRESENT).build());
        entityManager.persist(Attendance.builder().student(student).date(LocalDate.of(2026, 8, 3))
                .status(AttendanceStatus.ABSENT).build());
        entityManager.persist(Attendance.builder().student(student).date(LocalDate.of(2026, 7, 30))
                .status(AttendanceStatus.PRESENT).build());
        entityManager.flush();
        entityManager.clear();

        List<Attendance> result = attendanceRepository.findByStudentIdAndDateBetweenOrderByDateAsc(
                student.getId(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(result.get(1).getDate()).isEqualTo(LocalDate.of(2026, 8, 10));
    }
}
