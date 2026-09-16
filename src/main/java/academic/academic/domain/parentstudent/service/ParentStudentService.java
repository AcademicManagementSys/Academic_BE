package academic.academic.domain.parentstudent.service;

import academic.academic.domain.parentstudent.dto.ChildResponse;
import academic.academic.domain.parentstudent.dto.ParentStudentCreateRequest;
import academic.academic.domain.parentstudent.dto.ParentStudentResponse;
import academic.academic.domain.parentstudent.dto.ParentStudentUpdateRequest;
import academic.academic.domain.parentstudent.entity.ParentStudent;
import academic.academic.domain.parentstudent.entity.RelationType;
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
        requireRelationTypeAvailable(student.getId(), request.relationType(), null);

        ParentStudent link = ParentStudent.of(parent, student, request.relationType());
        parentStudentRepository.save(link);
        return ParentStudentResponse.from(link);
    }

    @Transactional
    public ParentStudentResponse updateLink(Long id, ParentStudentUpdateRequest request) {
        ParentStudent link = parentStudentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "연결 정보를 찾을 수 없습니다. id=" + id));
        requireRelationTypeAvailable(link.getStudent().getId(), request.relationType(), id);
        link.changeRelationType(request.relationType());
        return ParentStudentResponse.from(link);
    }

    /**
     * 한 학생에게 아빠/엄마는 각각 최대 1명만 연결할 수 있다(1차 피드백 — 같은 학생에 Mother가 둘
     * 연결되는 데이터 오류 방지). 기타(OTHER)는 조부모 등 여러 명일 수 있어 제한하지 않는다.
     * excludeLinkId는 updateLink에서 자기 자신을 중복 체크에서 빼기 위한 것 — 신규 생성이면 null.
     */
    private void requireRelationTypeAvailable(Long studentId, RelationType relationType, Long excludeLinkId) {
        if (relationType == RelationType.OTHER) {
            return;
        }
        boolean alreadyTaken = excludeLinkId == null
                ? parentStudentRepository.existsByStudentIdAndRelationType(studentId, relationType)
                : parentStudentRepository.existsByStudentIdAndRelationTypeAndIdNot(studentId, relationType, excludeLinkId);
        if (alreadyTaken) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "이미 같은 관계(" + (relationType == RelationType.FATHER ? "아빠" : "엄마") + ")로 연결된 보호자가 있습니다.");
        }
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
                            schoolClass != null ? schoolClass.getName() : null,
                            link.getRelationType()
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
