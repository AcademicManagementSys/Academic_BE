package academic.academic.domain.schoolclass.repository;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    List<SchoolClass> findByTeacherId(Long teacherId);

    boolean existsByIdAndTeacherId(Long id, Long teacherId);
}
