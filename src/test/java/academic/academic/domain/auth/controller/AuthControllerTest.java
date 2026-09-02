package academic.academic.domain.auth.controller;

import academic.academic.domain.auth.dto.LoginResponse;
import academic.academic.domain.auth.dto.PasswordResetRequestResponse;
import academic.academic.domain.auth.dto.TokenPairResponse;
import academic.academic.domain.auth.dto.UserSummary;
import academic.academic.domain.auth.service.AuthService;
import academic.academic.domain.user.entity.Role;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(JwtProvider.class)
class AuthControllerTest {

    private static final String TEACHER_TOKEN = AuthTestSupport.bearer(2L, Role.TEACHER);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void 로그인에_성공하면_토큰과_사용자_정보를_반환한다() throws Exception {
        given(authService.login(any())).willReturn(new LoginResponse(
                "access-token", "refresh-token", new UserSummary(2L, "김선생", Role.TEACHER, false)));

        mockMvc.perform(post("/v1/auth/login").contentType("application/json")
                        .content("{\"loginId\":\"teacher1\",\"password\":\"pw1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.user.role").value("teacher"));
    }

    @Test
    void 로그인_실패시_401을_반환한다() throws Exception {
        given(authService.login(any()))
                .willThrow(new BusinessException(ErrorCode.UNAUTHENTICATED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        mockMvc.perform(post("/v1/auth/login").contentType("application/json")
                        .content("{\"loginId\":\"teacher1\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void 로그인_요청에_아이디가_없으면_422를_반환한다() throws Exception {
        mockMvc.perform(post("/v1/auth/login").contentType("application/json")
                        .content("{\"password\":\"pw1234\"}"))
                .andExpect(status().is(422));
    }

    @Test
    void 토큰을_재발급한다() throws Exception {
        given(authService.refresh("old-refresh-token")).willReturn(new TokenPairResponse("new-access", "new-refresh"));

        mockMvc.perform(post("/v1/auth/refresh").contentType("application/json")
                        .content("{\"refreshToken\":\"old-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));
    }

    @Test
    void 로그아웃은_토큰이_있어야_한다() throws Exception {
        mockMvc.perform(post("/v1/auth/logout").contentType("application/json")
                        .content("{\"refreshToken\":\"some-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 로그인_상태면_로그아웃할_수_있다() throws Exception {
        mockMvc.perform(post("/v1/auth/logout").header("Authorization", TEACHER_TOKEN).contentType("application/json")
                        .content("{\"refreshToken\":\"some-token\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 비밀번호_재설정_토큰을_발급한다() throws Exception {
        given(authService.requestPasswordReset(eq("teacher1")))
                .willReturn(new PasswordResetRequestResponse("reset-token", LocalDateTime.of(2026, 9, 2, 10, 30)));

        mockMvc.perform(post("/v1/auth/password/reset-request").contentType("application/json")
                        .content("{\"loginId\":\"teacher1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resetToken").value("reset-token"));
    }

    @Test
    void 비밀번호를_재설정한다() throws Exception {
        mockMvc.perform(post("/v1/auth/password/reset").contentType("application/json")
                        .content("{\"resetToken\":\"reset-token\",\"newPassword\":\"newPassword1!\"}"))
                .andExpect(status().isOk());
    }
}
