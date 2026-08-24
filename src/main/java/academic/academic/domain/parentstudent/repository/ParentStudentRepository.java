package academic.academic.domain.parentstudent.repository;

import academic.academic.domain.parentstudent.entity.ParentStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParentStudentRepository extends JpaRepository<ParentStudent, Long> {

    List<ParentStudent> findByParentUserId(Long parentUserId);

    boolean existsByParentUserIdAndStudentId(Long parentUserId, Long studentId);
}
