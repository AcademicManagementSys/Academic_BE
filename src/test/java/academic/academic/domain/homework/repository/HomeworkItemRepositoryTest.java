package academic.academic.domain.homework.repository;

import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class HomeworkItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HomeworkItemRepository homeworkItemRepository;

    @Test
    void 반의_숙제_항목을_부여일_내림차순으로_조회한다() {
        SchoolClass classA = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        SchoolClass classB = entityManager.persist(SchoolClass.builder().name("초등 문법반").build());

        entityManager.persist(HomeworkItem.builder().schoolClass(classA).title("단어장 Ch.5")
                .assignedDate(LocalDate.of(2026, 8, 17)).dueDate(LocalDate.of(2026, 8, 19)).build());
        entityManager.persist(HomeworkItem.builder().schoolClass(classA).title("문제집 p.20-25")
                .assignedDate(LocalDate.of(2026, 8, 19)).dueDate(LocalDate.of(2026, 8, 21)).build());
        entityManager.persist(HomeworkItem.builder().schoolClass(classB).title("독후감")
                .assignedDate(LocalDate.of(2026, 8, 18)).build());
        entityManager.flush();
        entityManager.clear();

        List<HomeworkItem> result = homeworkItemRepository.search(classA.getId(), null, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("문제집 p.20-25");
        assertThat(result.get(1).getTitle()).isEqualTo("단어장 Ch.5");
    }

    @Test
    void week_범위가_주어지면_부여일_기준으로_필터링한다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());

        entityManager.persist(HomeworkItem.builder().schoolClass(schoolClass).title("이번주 숙제")
                .assignedDate(LocalDate.of(2026, 8, 19)).build());
        entityManager.persist(HomeworkItem.builder().schoolClass(schoolClass).title("다음주 숙제")
                .assignedDate(LocalDate.of(2026, 8, 26)).build());
        entityManager.flush();
        entityManager.clear();

        List<HomeworkItem> result = homeworkItemRepository.search(
                schoolClass.getId(), LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 23));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("이번주 숙제");
    }
}
