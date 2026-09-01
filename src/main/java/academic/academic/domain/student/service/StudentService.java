package academic.academic.domain.student.service;

import academic.academic.domain.parentstudent.entity.ParentStudent;
import academic.academic.domain.parentstudent.repository.ParentStudentRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.dto.ParentInfoRequest;
import academic.academic.domain.student.dto.StudentCreateRequest;
import academic.academic.domain.student.dto.StudentResponse;
import academic.academic.domain.student.dto.StudentUpdateRequest;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.entity.StudentStatus;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.teacherassignment.entity.TeacherAssignment;
import academic.academic.domain.teacherassignment.repository.TeacherAssignmentRepository;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;
import academic.academic.domain.user.repository.UserRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import academic.academic.global.util.EnumParser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StudentResponse createStudent(StudentCreateRequest request) {
        SchoolClass schoolClass = resolveClass(request.classId());

        Student student = Student.builder()
                .name(request.name())
                .birthDate(request.birthDate())
                .school(request.school())
                .grade(request.grade())
                .phone(request.phone())
                .schoolClass(schoolClass)
                .enrolledAt(request.enrolledAt())
                .build();
        studentRepository.save(student);

        if (request.teacherId() != null) {
            User teacher = resolveTeacher(request.teacherId());
            teacherAssignmentRepository.save(TeacherAssignment.forStudent(teacher, student));
        }

        if (request.parent() != null) {
            linkParent(student, request.parent());
        }

        return StudentResponse.from(student);
    }

    public List<StudentResponse> getStudents(Long classId, String status, String keyword) {
        StudentStatus statusEnum = EnumParser.parse(StudentStatus.class, status, "status");
        boolean excludeWithdrawn = statusEnum == null;
        String keywordPattern = StringUtils.hasText(keyword) ? "%" + keyword + "%" : null;
        return studentRepository.search(classId, statusEnum, excludeWithdrawn, keywordPattern)
                .stream().map(StudentResponse::from).toList();
    }

    public StudentResponse getStudent(Long id) {
        return StudentResponse.from(findStudent(id));
    }

    @Transactional
    public StudentResponse updateStudent(Long id, StudentUpdateRequest request) {
        Student student = findStudent(id);
        SchoolClass schoolClass = request.classId() != null ? resolveClass(request.classId()) : null;
        student.update(request.name(), request.birthDate(), request.school(), request.grade(), request.phone(), schoolClass);
        if (request.status() != null) {
            student.changeStatus(request.status());
        }
        return StudentResponse.from(student);
    }

    private void linkParent(Student student, ParentInfoRequest parentInfo) {
        User parentUser;
        if (parentInfo.createNew()) {
            if (!StringUtils.hasText(parentInfo.name())
                    || !StringUtils.hasText(parentInfo.loginId())
                    || !StringUtils.hasText(parentInfo.password())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "학부모 계정 생성에는 이름, 로그인 아이디, 비밀번호가 필요합니다.");
            }
            if (userRepository.existsByLoginId(parentInfo.loginId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID, "이미 사용 중인 로그인 아이디입니다.");
            }
            parentUser = User.builder()
                    .name(parentInfo.name())
                    .role(Role.PARENT)
                    .loginId(parentInfo.loginId())
                    .passwordHash(passwordEncoder.encode(parentInfo.password()))
                    .phone(parentInfo.phone())
                    .build();
            userRepository.save(parentUser);
        } else {
            if (parentInfo.parentUserId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "연결할 학부모 계정 id가 필요합니다.");
            }
            parentUser = userRepository.findById(parentInfo.parentUserId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "학부모 계정을 찾을 수 없습니다."));
            if (parentUser.getRole() != Role.PARENT) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "선택한 사용자는 학부모가 아닙니다.");
            }
        }
        parentStudentRepository.save(ParentStudent.of(parentUser, student));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다. id=" + id));
    }

    private SchoolClass resolveClass(Long classId) {
        if (classId == null) {
            return null;
        }
        return schoolClassRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "반을 찾을 수 없습니다. id=" + classId));
    }

    private User resolveTeacher(Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "선생님을 찾을 수 없습니다. id=" + teacherId));
        if (teacher.getRole() != Role.TEACHER) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "선택한 사용자는 선생님이 아닙니다.");
        }
        return teacher;
    }
}
