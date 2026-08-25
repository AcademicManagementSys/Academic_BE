package academic.academic.domain.test.repository;

import academic.academic.domain.test.entity.TestSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestSessionRepository extends JpaRepository<TestSession, Long> {

    List<TestSession> findBySchoolClassIdOrderByTestDateDescIdDesc(Long classId);

    @Query("select distinct ts from TestSession ts join TestRecord tr on tr.testSession = ts "
            + "where tr.student.id = :studentId "
            + "order by ts.testDate desc, ts.id desc")
    List<TestSession> findRecentSessionsByStudentId(@Param("studentId") Long studentId, Pageable pageable);
}
