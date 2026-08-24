package academic.academic.domain.teacherassignment.repository;

import academic.academic.domain.teacherassignment.entity.TeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {

    List<TeacherAssignment> findByTeacherId(Long teacherId);
}
