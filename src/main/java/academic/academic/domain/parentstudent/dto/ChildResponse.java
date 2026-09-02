package academic.academic.domain.parentstudent.dto;

import academic.academic.domain.parentstudent.entity.RelationType;

public record ChildResponse(
        Long studentId,
        String name,
        String grade,
        String className,
        RelationType relationType
) {
}
