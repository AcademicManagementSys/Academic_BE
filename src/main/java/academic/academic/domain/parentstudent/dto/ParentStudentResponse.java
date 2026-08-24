package academic.academic.domain.parentstudent.dto;

import academic.academic.domain.parentstudent.entity.ParentStudent;

public record ParentStudentResponse(
        Long id,
        Long parentUserId,
        String parentName,
        Long studentId,
        String studentName
) {
    public static ParentStudentResponse from(ParentStudent link) {
        return new ParentStudentResponse(
                link.getId(),
                link.getParentUser().getId(),
                link.getParentUser().getName(),
                link.getStudent().getId(),
                link.getStudent().getName()
        );
    }
}
