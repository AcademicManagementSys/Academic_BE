package academic.academic.domain.parentstudent.dto;

import academic.academic.domain.parentstudent.entity.ParentStudent;
import academic.academic.domain.parentstudent.entity.RelationType;

public record ParentStudentResponse(
        Long id,
        Long parentUserId,
        String parentName,
        Long studentId,
        String studentName,
        RelationType relationType
) {
    public static ParentStudentResponse from(ParentStudent link) {
        return new ParentStudentResponse(
                link.getId(),
                link.getParentUser().getId(),
                link.getParentUser().getName(),
                link.getStudent().getId(),
                link.getStudent().getName(),
                link.getRelationType()
        );
    }
}
