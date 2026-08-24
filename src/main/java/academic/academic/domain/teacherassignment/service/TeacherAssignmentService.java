package academic.academic.domain.teacherassignment.service;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.teacherassignment.dto.TeacherAssignmentCreateRequest;
import academic.academic.domain.teacherassignment.dto.TeacherAssignmentResponse;
import academic.academic.domain.teacherassignment.entity.TeacherAssignment;
import academic.academic.domain.teacherassignment.repository.TeacherAssignmentRepository;
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
public class TeacherAssignmentService {

    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public TeacherAssignmentResponse createAssignment(TeacherAssignmentCreateRequest request) {
        boolean hasClass = request.classId() != null;
        boolean hasStudent = request.studentId() != null;
        if (hasClass == hasStudent) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "classId 또는 studentId 중 하나만 지정해야 합니다.");
        }

        User teacher = userRepository.findById(request.teacherId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "선생님을 찾을 수 없습니다."));
        if (teacher.getRole() != Role.TEACHER) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "선택한 사용자는 선생님이 아닙니다.");
        }

        TeacherAssignment assignment;
        if (hasClass) {
            SchoolClass schoolClass = schoolClassRepository.findById(request.classId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "반을 찾을 수 없습니다."));
            assignment = TeacherAssignment.forClass(teacher, schoolClass);
        } else {
            Student student = studentRepository.findById(request.studentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다."));
            assignment = TeacherAssignment.forStudent(teacher, student);
        }

        teacherAssignmentRepository.save(assignment);
        return TeacherAssignmentResponse.from(assignment);
    }

    public List<TeacherAssignmentResponse> getAssignments(Long teacherId) {
        List<TeacherAssignment> assignments = teacherId != null
                ? teacherAssignmentRepository.findByTeacherId(teacherId)
                : teacherAssignmentRepository.findAll();
        return assignments.stream().map(TeacherAssignmentResponse::from).toList();
    }

    @Transactional
    public void deleteAssignment(Long id) {
        if (!teacherAssignmentRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "배정 정보를 찾을 수 없습니다. id=" + id);
        }
        teacherAssignmentRepository.deleteById(id);
    }
}
