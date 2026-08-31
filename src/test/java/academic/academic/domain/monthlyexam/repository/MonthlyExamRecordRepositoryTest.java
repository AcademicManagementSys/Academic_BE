package academic.academic.domain.monthlyexam.repository;

import academic.academic.domain.monthlyexam.entity.MonthlyExam;
import academic.academic.domain.monthlyexam.entity.MonthlyExamRecord;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class MonthlyExamRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MonthlyExamRecordRepository monthlyExamRecordRepository;

    @Test
    void 회차와_학생으로_성적을_조회한다() {
        MonthlyExam exam = entityManager.persist(MonthlyExam.builder().examName("8월 학평").examMonth("2026-08").build());
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        entityManager.persist(MonthlyExamRecord.builder().monthlyExam(exam).student(student)
                .rawScore(82).stdScore(128).percentile(91).grade("2등급").build());
        entityManager.flush();
        entityManager.clear();

        Optional<MonthlyExamRecord> found = monthlyExamRecordRepository
                .findByMonthlyExamIdAndStudentId(exam.getId(), student.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getRawScore()).isEqualTo(82);
    }

    @Test
    void 같은_회차_같은_학생의_성적은_중복_저장할_수_없다() {
        MonthlyExam exam = entityManager.persist(MonthlyExam.builder().examName("8월 학평").examMonth("2026-08").build());
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        entityManager.persist(MonthlyExamRecord.builder().monthlyExam(exam).student(student).rawScore(82).build());
        entityManager.flush();

        MonthlyExamRecord duplicate = MonthlyExamRecord.builder().monthlyExam(exam).student(student).rawScore(90).build();

        assertThatThrownBy(() -> monthlyExamRecordRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 회차와_반으로_소속_학생의_성적을_조회한다() {
        MonthlyExam exam = entityManager.persist(MonthlyExam.builder().examName("8월 학평").examMonth("2026-08").build());
        SchoolClass classA = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        SchoolClass classB = entityManager.persist(SchoolClass.builder().name("초등 문법반").build());
        Student studentA = entityManager.persist(Student.builder().name("김민준").schoolClass(classA).build());
        Student studentB = entityManager.persist(Student.builder().name("이서연").schoolClass(classB).build());
        entityManager.persist(MonthlyExamRecord.builder().monthlyExam(exam).student(studentA).rawScore(82).build());
        entityManager.persist(MonthlyExamRecord.builder().monthlyExam(exam).student(studentB).rawScore(70).build());
        entityManager.flush();
        entityManager.clear();

        List<MonthlyExamRecord> result = monthlyExamRecordRepository
                .findByMonthlyExamIdAndStudent_SchoolClassId(exam.getId(), classA.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudent().getId()).isEqualTo(studentA.getId());
    }

    @Test
    void 학생의_최근_성적을_시행연월_내림차순으로_제한된_개수만큼_조회한다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        MonthlyExam exam1 = entityManager.persist(MonthlyExam.builder().examName("6월 학평").examMonth("2026-06").build());
        MonthlyExam exam2 = entityManager.persist(MonthlyExam.builder().examName("7월 학평").examMonth("2026-07").build());
        MonthlyExam exam3 = entityManager.persist(MonthlyExam.builder().examName("8월 학평").examMonth("2026-08").build());
        entityManager.persist(MonthlyExamRecord.builder().monthlyExam(exam1).student(student).rawScore(72).build());
        entityManager.persist(MonthlyExamRecord.builder().monthlyExam(exam2).student(student).rawScore(78).build());
        entityManager.persist(MonthlyExamRecord.builder().monthlyExam(exam3).student(student).rawScore(82).build());
        entityManager.flush();
        entityManager.clear();

        List<MonthlyExamRecord> result = monthlyExamRecordRepository
                .findRecentByStudentId(student.getId(), PageRequest.of(0, 2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMonthlyExam().getExamMonth()).isEqualTo("2026-08");
        assertThat(result.get(1).getMonthlyExam().getExamMonth()).isEqualTo("2026-07");
    }
}
