package academic.academic.domain.schoolclass.service;

import academic.academic.domain.schoolclass.dto.ClassCreateRequest;
import academic.academic.domain.schoolclass.dto.ClassResponse;
import academic.academic.domain.schoolclass.dto.ClassUpdateRequest;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.dto.StudentResponse;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;
import academic.academic.domain.user.repository.UserRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public ClassResponse createClass(ClassCreateRequest request) {
        User teacher = resolveTeacher(request.teacherId());
        SchoolClass schoolClass = SchoolClass.builder()
                .name(request.name())
                .teacher(teacher)
                .schedule(request.schedule())
                .build();
        schoolClassRepository.save(schoolClass);
        return ClassResponse.from(schoolClass);
    }

    public List<ClassResponse> getClasses(Long teacherId) {
        List<SchoolClass> classes = teacherId != null
                ? schoolClassRepository.findByTeacherId(teacherId)
                : schoolClassRepository.findAll();
        return classes.stream().map(ClassResponse::from).toList();
    }

    public ClassResponse getClass(Long id) {
        return ClassResponse.from(findClass(id));
    }

    @Transactional
    public ClassResponse updateClass(Long id, ClassUpdateRequest request) {
        SchoolClass schoolClass = findClass(id);
        User teacher = request.teacherId() != null ? resolveTeacher(request.teacherId()) : schoolClass.getTeacher();
        schoolClass.update(request.name(), teacher, request.schedule());
        return ClassResponse.from(schoolClass);
    }

    @Transactional
    public void deleteClass(Long id) {
        SchoolClass schoolClass = findClass(id);
        if (studentRepository.existsBySchoolClassId(id)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "소속 학생이 있는 반은 삭제할 수 없습니다.");
        }
        schoolClassRepository.delete(schoolClass);
    }

    public List<StudentResponse> getClassStudents(Long id) {
        findClass(id);
        return studentRepository.findBySchoolClassId(id).stream().map(StudentResponse::from).toList();
    }

    private SchoolClass findClass(Long id) {
        return schoolClassRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "반을 찾을 수 없습니다. id=" + id));
    }

    private User resolveTeacher(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "선생님을 찾을 수 없습니다. id=" + teacherId));
        if (teacher.getRole() != Role.TEACHER) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "선택한 사용자는 선생님이 아닙니다.");
        }
        return teacher;
    }
}
