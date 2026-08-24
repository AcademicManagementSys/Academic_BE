package academic.academic.domain.user.service;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.teacherassignment.entity.TeacherAssignment;
import academic.academic.domain.teacherassignment.repository.TeacherAssignmentRepository;
import academic.academic.domain.user.dto.UserCreateRequest;
import academic.academic.domain.user.dto.UserResponse;
import academic.academic.domain.user.dto.UserStatusUpdateRequest;
import academic.academic.domain.user.dto.UserUpdateRequest;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;
import academic.academic.domain.user.repository.UserRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (request.role() != Role.TEACHER && request.role() != Role.PARENT) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "이 API로는 선생님 또는 학부모 계정만 생성할 수 있습니다.");
        }
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID, "이미 사용 중인 로그인 아이디입니다.");
        }

        User user = User.builder()
                .name(request.name())
                .role(request.role())
                .loginId(request.loginId())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .build();
        userRepository.save(user);

        if (request.role() == Role.TEACHER && request.classIds() != null) {
            for (Long classId : request.classIds()) {
                SchoolClass schoolClass = schoolClassRepository.findById(classId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "반을 찾을 수 없습니다. id=" + classId));
                teacherAssignmentRepository.save(TeacherAssignment.forClass(user, schoolClass));
            }
        }

        return UserResponse.from(user);
    }

    public List<UserResponse> getUsers(Role role) {
        List<User> users = role != null ? userRepository.findByRole(role) : userRepository.findAll();
        return users.stream().map(UserResponse::from).toList();
    }

    public UserResponse getUser(Long id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findUser(id);
        user.updateProfile(request.name(), request.phone());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateStatus(Long id, UserStatusUpdateRequest request) {
        User user = findUser(id);
        user.changeActive(request.active());
        return UserResponse.from(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다. id=" + id));
    }
}
