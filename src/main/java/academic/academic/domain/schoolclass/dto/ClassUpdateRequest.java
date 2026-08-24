package academic.academic.domain.schoolclass.dto;

public record ClassUpdateRequest(
        String name,
        Long teacherId,
        String schedule
) {
}
