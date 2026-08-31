package academic.academic.domain.test.repository;

import academic.academic.domain.test.entity.TestRecord;
import academic.academic.domain.test.entity.TestSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TestRecordRepository extends JpaRepository<TestRecord, Long> {

    Optional<TestRecord> findByTestSessionIdAndStudentIdAndSubject(Long testSessionId, Long studentId, TestSubject subject);

    List<TestRecord> findByTestSessionId(Long testSessionId);

    List<TestRecord> findByStudentIdAndTestSessionIdIn(Long studentId, List<Long> testSessionIds);

    @Query("select distinct r.student.id from TestRecord r where r.testSession.id in :sessionIds")
    List<Long> findDistinctStudentIdsByTestSessionIdIn(@Param("sessionIds") List<Long> sessionIds);
}
