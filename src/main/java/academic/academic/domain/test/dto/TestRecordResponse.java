package academic.academic.domain.test.dto;

import academic.academic.domain.student.entity.Student;
import academic.academic.domain.test.entity.TestRecord;
import academic.academic.domain.test.entity.TestSession;
import academic.academic.domain.test.entity.TestSubject;

import java.time.LocalDate;

public record TestRecordResponse(
        Long id,
        Long testSessionId,
        String testTitle,
        LocalDate testDate,
        Long studentId,
        String studentName,
        TestSubject subject,
        boolean isTaken,
        Integer score,
        Integer maxScore,
        String comment
) {
    public static TestRecordResponse from(TestRecord record) {
        TestSession session = record.getTestSession();
        return new TestRecordResponse(
                record.getId(),
                session.getId(),
                session.getTitle(),
                session.getTestDate(),
                record.getStudent().getId(),
                record.getStudent().getName(),
                record.getSubject(),
                record.isTaken(),
                record.getScore(),
                record.getMaxScore(),
                record.getComment()
        );
    }

    public static TestRecordResponse unchecked(TestSession session, Student student, TestSubject subject) {
        return new TestRecordResponse(
                null,
                session.getId(),
                session.getTitle(),
                session.getTestDate(),
                student.getId(),
                student.getName(),
                subject,
                false,
                null,
                null,
                null
        );
    }
}
