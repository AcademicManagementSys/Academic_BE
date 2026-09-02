package academic.academic.domain.user.controller;

import academic.academic.domain.user.dto.PasswordResetResponse;
import academic.academic.domain.user.dto.UserResponse;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.service.UserService;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import academic.academic.global.security.AuthorizationService;
import academic.academic.global.security.JwtProvider;
import academic.academic.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(JwtProvider.class)
class UserControllerTest {

    private static final String ADMIN_TOKEN = AuthTestSupport.bearer(1L, Role.ADMIN);
    private static final String TEACHER_TOKEN = AuthTestSupport.bearer(2L, Role.TEACHER);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @Test
    void 비밀번호를_초기화한다() throws Exception {
        given(userService.resetPassword(1L)).willReturn(
                new PasswordResetResponse(1L, "teacher1", "TempPass123!", true));

        mockMvc.perform(post("/v1/users/1/reset-password").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginId").value("teacher1"))
                .andExpect(jsonPath("$.data.tempPassword").value("TempPass123!"))
                .andExpect(jsonPath("$.data.mustChangePassword").value(true));
    }

    @Test
    void 토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/v1/users/1/reset-password"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 관리자가_아니면_403을_반환한다() throws Exception {
        willThrow(new BusinessException(ErrorCode.FORBIDDEN_ROLE, "이 API에 접근할 수 있는 역할이 아닙니다."))
                .given(authorizationService).requireRole(any(), eq(Role.ADMIN));

        mockMvc.perform(post("/v1/users/1/reset-password").header("Authorization", TEACHER_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
    }

    @Test
    void 계정이_없으면_404를_반환한다() throws Exception {
        given(userService.resetPassword(999L))
                .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다. id=999"));

        mockMvc.perform(post("/v1/users/999/reset-password").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void 계정_목록을_조회한다() throws Exception {
        given(userService.getUsers(eq(Role.TEACHER))).willReturn(java.util.List.of(
                new UserResponse(1L, "김선생", Role.TEACHER, "teacher1", null, true, LocalDateTime.now())
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/v1/users").header("Authorization", ADMIN_TOKEN).param("role", "teacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].loginId").value("teacher1"));
    }
}
