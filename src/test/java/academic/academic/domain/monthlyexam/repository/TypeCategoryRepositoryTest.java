package academic.academic.domain.monthlyexam.repository;

import academic.academic.domain.monthlyexam.entity.TypeCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TypeCategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TypeCategoryRepository typeCategoryRepository;

    @Test
    void 이름_존재_여부를_확인한다() {
        entityManager.persist(TypeCategory.builder().name("어휘").build());
        entityManager.flush();
        entityManager.clear();

        assertThat(typeCategoryRepository.existsByName("어휘")).isTrue();
        assertThat(typeCategoryRepository.existsByName("어법")).isFalse();
    }

    @Test
    void 유형_카테고리_목록을_이름순으로_조회한다() {
        entityManager.persist(TypeCategory.builder().name("빈칸추론").build());
        entityManager.persist(TypeCategory.builder().name("어법").build());
        entityManager.persist(TypeCategory.builder().name("어휘").build());
        entityManager.flush();
        entityManager.clear();

        List<TypeCategory> result = typeCategoryRepository.findAllByOrderByNameAsc();

        assertThat(result).extracting(TypeCategory::getName)
                .containsExactly("빈칸추론", "어법", "어휘");
    }
}
