package academic.academic.domain.parentstudent.repository;

import academic.academic.domain.parentstudent.entity.ParentStudent;
import academic.academic.domain.parentstudent.entity.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParentStudentRepository extends JpaRepository<ParentStudent, Long> {

    List<ParentStudent> findByParentUserId(Long parentUserId);

    boolean existsByParentUserIdAndStudentId(Long parentUserId, Long studentId);

    boolean existsByStudentIdAndRelationType(Long studentId, RelationType relationType);

    boolean existsByStudentIdAndRelationTypeAndIdNot(Long studentId, RelationType relationType, Long id);
}
