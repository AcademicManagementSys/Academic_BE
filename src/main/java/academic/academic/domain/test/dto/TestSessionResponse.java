package academic.academic.domain.test.dto;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.test.entity.TestSession;

import java.time.LocalDate;

public record TestSessionResponse(
        Long id,
        Long classId,
        String className,
        String title,
        LocalDate testDate
) {
    public static TestSessionResponse from(TestSession session) {
        SchoolClass schoolClass = session.getSchoolClass();
        return new TestSessionResponse(
                session.getId(),
                schoolClass.getId(),
                schoolClass.getName(),
                session.getTitle(),
                session.getTestDate()
        );
    }
}
