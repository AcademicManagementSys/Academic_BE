package academic.academic.domain.homework.repository;

import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.homework.entity.HomeworkRecord;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class HomeworkRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HomeworkRecordRepository homeworkRecordRepository;

    @Test
    void 숙제_항목과_학생으로_기록을_조회한다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        HomeworkItem item = entityManager.persist(HomeworkItem.builder().schoolClass(schoolClass).title("단어장 Ch.5")
                .assignedDate(LocalDate.of(2026, 8, 17)).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(item).student(student)
                .done(true).score(98).comment("오타 1개").build());
        entityManager.flush();
        entityManager.clear();

        Optional<HomeworkRecord> found = homeworkRecordRepository.findByHomeworkItemIdAndStudentId(item.getId(), student.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getScore()).isEqualTo(98);
    }

    @Test
    void 항목별_학생_기록_목록을_조회한다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student1 = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        Student student2 = entityManager.persist(Student.builder().name("이서연").schoolClass(schoolClass).build());
        HomeworkItem item = entityManager.persist(HomeworkItem.builder().schoolClass(schoolClass).title("단어장 Ch.5")
                .assignedDate(LocalDate.of(2026, 8, 17)).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(item).student(student1).done(true).score(98).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(item).student(student2).done(false).build());
        entityManager.flush();
        entityManager.clear();

        List<HomeworkRecord> result = homeworkRecordRepository.findByHomeworkItemId(item.getId());

        assertThat(result).hasSize(2)
                .extracting(r -> r.getStudent().getId())
                .containsExactlyInAnyOrder(student1.getId(), student2.getId());
    }

    @Test
    void 같은_항목_같은_학생의_기록은_중복_저장할_수_없다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        HomeworkItem item = entityManager.persist(HomeworkItem.builder().schoolClass(schoolClass).title("단어장 Ch.5")
                .assignedDate(LocalDate.of(2026, 8, 17)).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(item).student(student).done(true).build());
        entityManager.flush();

        HomeworkRecord duplicate = HomeworkRecord.builder().homeworkItem(item).student(student).done(false).build();

        assertThatThrownBy(() -> homeworkRecordRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 학생의_기간_내_숙제_기록을_부여일_내림차순으로_조회한다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        HomeworkItem item1 = entityManager.persist(HomeworkItem.builder().schoolClass(schoolClass).title("Ch.5")
                .assignedDate(LocalDate.of(2026, 8, 10)).build());
        HomeworkItem item2 = entityManager.persist(HomeworkItem.builder().schoolClass(schoolClass).title("Ch.6")
                .assignedDate(LocalDate.of(2026, 8, 17)).build());
        HomeworkItem itemOutOfRange = entityManager.persist(HomeworkItem.builder().schoolClass(schoolClass).title("Ch.4")
                .assignedDate(LocalDate.of(2026, 7, 20)).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(item1).student(student).done(true).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(item2).student(student).done(false).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(itemOutOfRange).student(student).done(true).build());
        entityManager.flush();
        entityManager.clear();

        List<HomeworkRecord> result = homeworkRecordRepository.findByStudentIdAndAssignedDateBetween(
                student.getId(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getHomeworkItem().getTitle()).isEqualTo("Ch.6");
        assertThat(result.get(1).getHomeworkItem().getTitle()).isEqualTo("Ch.5");
    }

    @Test
    void 학생의_최근_숙제_기록을_부여일_내림차순으로_N건_조회한다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        Student other = entityManager.persist(Student.builder().name("이서연").schoolClass(schoolClass).build());
        HomeworkItem item1 = entityManager.persist(HomeworkItem.builder().schoolClass(schoolClass).title("Ch.5")
                .assignedDate(LocalDate.of(2026, 8, 10)).build());
        HomeworkItem item2 = entityManager.persist(HomeworkItem.builder().schoolClass(schoolClass).title("Ch.6")
                .assignedDate(LocalDate.of(2026, 8, 17)).build());
        HomeworkItem item3 = entityManager.persist(HomeworkItem.builder().schoolClass(schoolClass).title("Ch.7")
                .assignedDate(LocalDate.of(2026, 8, 24)).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(item1).student(student).done(true).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(item2).student(student).done(false).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(item3).student(student).done(true).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(item3).student(other).done(true).build());
        entityManager.flush();
        entityManager.clear();

        List<HomeworkRecord> result = homeworkRecordRepository.findRecentByStudentId(student.getId(), PageRequest.of(0, 2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getHomeworkItem().getTitle()).isEqualTo("Ch.7");
        assertThat(result.get(1).getHomeworkItem().getTitle()).isEqualTo("Ch.6");
    }

    @Test
    void 항목_삭제시_소속_기록을_모두_제거한다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student1 = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        Student student2 = entityManager.persist(Student.builder().name("이서연").schoolClass(schoolClass).build());
        HomeworkItem item = entityManager.persist(HomeworkItem.builder().schoolClass(schoolClass).title("단어장 Ch.5")
                .assignedDate(LocalDate.of(2026, 8, 17)).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(item).student(student1).done(true).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(item).student(student2).done(false).build());
        entityManager.flush();

        homeworkRecordRepository.deleteByHomeworkItemId(item.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(homeworkRecordRepository.findByHomeworkItemId(item.getId())).isEmpty();
    }

    @Test
    void 반의_기간_내_숙제_기록을_조회한다() {
        SchoolClass classA = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        SchoolClass classB = entityManager.persist(SchoolClass.builder().name("초등 문법반").build());
        Student studentInA = entityManager.persist(Student.builder().name("김민준").schoolClass(classA).build());
        Student studentInB = entityManager.persist(Student.builder().name("박서준").schoolClass(classB).build());
        HomeworkItem itemA1 = entityManager.persist(HomeworkItem.builder().schoolClass(classA).title("Ch.5")
                .assignedDate(LocalDate.of(2026, 8, 10)).build());
        HomeworkItem itemAOutOfRange = entityManager.persist(HomeworkItem.builder().schoolClass(classA).title("Ch.4")
                .assignedDate(LocalDate.of(2026, 7, 20)).build());
        HomeworkItem itemB = entityManager.persist(HomeworkItem.builder().schoolClass(classB).title("Ch.1")
                .assignedDate(LocalDate.of(2026, 8, 10)).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(itemA1).student(studentInA).done(true).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(itemAOutOfRange).student(studentInA).done(true).build());
        entityManager.persist(HomeworkRecord.builder().homeworkItem(itemB).student(studentInB).done(false).build());
        entityManager.flush();
        entityManager.clear();

        List<HomeworkRecord> result = homeworkRecordRepository.findByClassIdAndAssignedDateBetween(
                classA.getId(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHomeworkItem().getTitle()).isEqualTo("Ch.5");
    }
}
