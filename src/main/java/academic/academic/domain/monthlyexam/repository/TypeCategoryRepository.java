package academic.academic.domain.monthlyexam.repository;

import academic.academic.domain.monthlyexam.entity.TypeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TypeCategoryRepository extends JpaRepository<TypeCategory, Long> {

    boolean existsByName(String name);

    List<TypeCategory> findAllByOrderByNameAsc();
}
