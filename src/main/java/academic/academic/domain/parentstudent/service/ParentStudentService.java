package academic.academic.domain.parentstudent.service;

import academic.academic.domain.parentstudent.dto.ChildResponse;
import academic.academic.domain.parentstudent.dto.ParentStudentCreateRequest;
import academic.academic.domain.parentstudent.dto.ParentStudentResponse;
import academic.academic.domain.parentstudent.entity.ParentStudent;
import academic.academic.domain.parentstudent.repository.ParentStudentRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;
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
public class ParentStudentService {

    private final ParentStudentRepository parentStudentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public ParentStudentResponse createLink(ParentStudentCreateRequest request) {
        User parent = userRepository.findById(request.parentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "학부모 계정을 찾을 수 없습니다."));
        if (parent.getRole() != Role.PARENT) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "선택한 사용자는 학부모가 아닙니다.");
        }
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다."));
        if (parentStudentRepository.existsByParentUserIdAndStudentId(parent.getId(), student.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "이미 연결된 학부모-자녀 관계입니다.");
        }

        ParentStudent link = ParentStudent.of(parent, student);
        parentStudentRepository.save(link);
        return ParentStudentResponse.from(link);
    }

    public List<ChildResponse> getChildren(Long parentUserId) {
        return parentStudentRepository.findByParentUserId(parentUserId).stream()
                .map(link -> {
                    Student student = link.getStudent();
                    SchoolClass schoolClass = student.getSchoolClass();
                    return new ChildResponse(
                            student.getId(),
                            student.getName(),
                            student.getGrade(),
                            schoolClass != null ? schoolClass.getName() : null
                    );
                })
                .toList();
    }

    @Transactional
    public void deleteLink(Long id) {
        if (!parentStudentRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "연결 정보를 찾을 수 없습니다. id=" + id);
        }
        parentStudentRepository.deleteById(id);
    }
}
