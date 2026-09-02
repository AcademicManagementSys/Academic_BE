package academic.academic.domain.student.dto;

import academic.academic.domain.parentstudent.entity.RelationType;
import jakarta.validation.constraints.NotNull;

public record ParentInfoRequest(
        boolean createNew,
        Long parentUserId,
        String name,
        String phone,
        String loginId,
        String password,
        @NotNull RelationType relationType
) {
}
