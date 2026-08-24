package academic.academic.domain.schoolclass.dto;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.user.entity.User;

public record ClassResponse(
        Long id,
        String name,
        Long teacherId,
        String teacherName,
        String schedule
) {
    public static ClassResponse from(SchoolClass schoolClass) {
        User teacher = schoolClass.getTeacher();
        return new ClassResponse(
                schoolClass.getId(),
                schoolClass.getName(),
                teacher != null ? teacher.getId() : null,
                teacher != null ? teacher.getName() : null,
                schoolClass.getSchedule()
        );
    }
}
