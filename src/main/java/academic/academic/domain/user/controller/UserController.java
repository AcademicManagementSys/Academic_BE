package academic.academic.domain.user.controller;

import academic.academic.domain.user.dto.UserCreateRequest;
import academic.academic.domain.user.dto.UserResponse;
import academic.academic.domain.user.dto.UserStatusUpdateRequest;
import academic.academic.domain.user.dto.UserUpdateRequest;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.service.UserService;
import academic.academic.global.response.ApiResponse;
import academic.academic.global.util.EnumParser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 계정 관리 API — 관리자 전용 (SCR-06, FR-01-03)
 */
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<List<UserResponse>> getUsers(@RequestParam(required = false) String role) {
        Role roleEnum = EnumParser.parse(Role.class, role, "role");
        return ApiResponse.of(userService.getUsers(roleEnum));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.of(userService.createUser(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        return ApiResponse.of(userService.getUser(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return ApiResponse.of(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> updateStatus(@PathVariable Long id,
                                                   @Valid @RequestBody UserStatusUpdateRequest request) {
        return ApiResponse.of(userService.updateStatus(id, request));
    }
}
