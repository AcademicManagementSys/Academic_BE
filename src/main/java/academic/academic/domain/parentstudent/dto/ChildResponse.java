package academic.academic.domain.parentstudent.dto;

public record ChildResponse(
        Long studentId,
        String name,
        String grade,
        String className
) {
}
