package academic.academic.domain.student.dto;

public record ParentInfoRequest(
        boolean createNew,
        Long parentUserId,
        String name,
        String phone,
        String loginId,
        String password
) {
}
