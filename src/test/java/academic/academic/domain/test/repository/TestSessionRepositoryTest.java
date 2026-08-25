package academic.academic.domain.test.repository;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.test.entity.TestRecord;
import academic.academic.domain.test.entity.TestSession;
import academic.academic.domain.test.entity.TestSubject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TestSessionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TestSessionRepository testSessionRepository;

    @Autowired
    private TestRecordRepository testRecordRepository;

    @Test
    void 반의_테스트_회차를_시행일_내림차순으로_조회한다() {
        SchoolClass classA = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        SchoolClass classB = entityManager.persist(SchoolClass.builder().name("초등 문법반").build());

        entityManager.persist(TestSession.builder().schoolClass(classA).title("8월 3주차 테스트")
                .testDate(LocalDate.of(2026, 8, 19)).build());
        entityManager.persist(TestSession.builder().schoolClass(classA).title("8월 4주차 테스트")
                .testDate(LocalDate.of(2026, 8, 26)).build());
        entityManager.persist(TestSession.builder().schoolClass(classB).title("초등반 테스트")
                .testDate(LocalDate.of(2026, 8, 20)).build());
        entityManager.flush();
        entityManager.clear();

        List<TestSession> result = testSessionRepository.findBySchoolClassIdOrderByTestDateDescIdDesc(classA.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("8월 4주차 테스트");
        assertThat(result.get(1).getTitle()).isEqualTo("8월 3주차 테스트");
    }

    @Test
    void 학생이_응시한_최근_회차를_시행일_내림차순으로_제한된_개수만큼_조회한다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        Student other = entityManager.persist(Student.builder().name("이서연").schoolClass(schoolClass).build());

        TestSession session1 = entityManager.persist(TestSession.builder().schoolClass(schoolClass).title("1회차")
                .testDate(LocalDate.of(2026, 6, 1)).build());
        TestSession session2 = entityManager.persist(TestSession.builder().schoolClass(schoolClass).title("2회차")
                .testDate(LocalDate.of(2026, 7, 1)).build());
        TestSession session3 = entityManager.persist(TestSession.builder().schoolClass(schoolClass).title("3회차")
                .testDate(LocalDate.of(2026, 8, 1)).build());
        entityManager.persist(TestRecord.builder().testSession(session1).student(student)
                .subject(TestSubject.VOCAB).taken(true).score(18).maxScore(20).build());
        entityManager.persist(TestRecord.builder().testSession(session2).student(student)
                .subject(TestSubject.VOCAB).taken(true).score(19).maxScore(20).build());
        entityManager.persist(TestRecord.builder().testSession(session3).student(student)
                .subject(TestSubject.VOCAB).taken(true).score(20).maxScore(20).build());
        entityManager.persist(TestRecord.builder().testSession(session3).student(other)
                .subject(TestSubject.VOCAB).taken(true).score(15).maxScore(20).build());
        entityManager.flush();
        entityManager.clear();

        List<TestSession> result = testSessionRepository.findRecentSessionsByStudentId(student.getId(), PageRequest.of(0, 2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("3회차");
        assertThat(result.get(1).getTitle()).isEqualTo("2회차");
    }
}
