package academic.academic.domain.notice.controller;

import academic.academic.domain.notice.dto.NoticeResponse;
import academic.academic.domain.notice.entity.NoticeScope;
import academic.academic.domain.notice.service.NoticeService;
import academic.academic.domain.parentstudent.service.ParentStudentService;
import academic.academic.domain.student.service.StudentService;
import academic.academic.domain.user.entity.Role;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoticeController.class)
@Import(JwtProvider.class)
class NoticeControllerTest {

    private static final String ADMIN_TOKEN = AuthTestSupport.bearer(1L, Role.ADMIN);
    private static final String TEACHER_TOKEN = AuthTestSupport.bearer(2L, Role.TEACHER);
    private static final String STUDENT_TOKEN = AuthTestSupport.bearer(101L, Role.STUDENT);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoticeService noticeService;
    @MockitoBean
    private StudentService studentService;
    @MockitoBean
    private ParentStudentService parentStudentService;
    @MockitoBean
    private AuthorizationService authorizationService;

    @Test
    void 공지_목록을_조회한다() throws Exception {
        given(noticeService.search(eq("class"), eq(3L), eq(5))).willReturn(List.of(
                new NoticeResponse(1L, 1L, "원장", NoticeScope.CLASS, 3L, "중2 심화반",
                        "특강 안내", "내용", false, LocalDateTime.of(2026, 8, 27, 10, 30))
        ));

        mockMvc.perform(get("/v1/notices").header("Authorization", ADMIN_TOKEN)
                        .param("scope", "class").param("classId", "3").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("특강 안내"));
    }

    @Test
    void 토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/v1/notices"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void 학부모_학생은_목록_조회가_403이다() throws Exception {
        willThrow(new BusinessException(ErrorCode.FORBIDDEN_ROLE, "이 API에 접근할 수 있는 역할이 아닙니다."))
                .given(authorizationService).requireRole(any(), eq(Role.ADMIN), eq(Role.TEACHER));

        mockMvc.perform(get("/v1/notices").header("Authorization", STUDENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
    }

    @Test
    void 공지를_작성한다() throws Exception {
        given(noticeService.create(any(), eq(1L))).willReturn(
                new NoticeResponse(1L, 1L, "원장", NoticeScope.ALL, null, null,
                        "추석 연휴 휴원 안내", "내용", true, LocalDateTime.of(2026, 8, 25, 9, 0)));

        String body = """
                {"scope":"all","title":"추석 연휴 휴원 안내","content":"내용","isPinned":true}
                """;

        mockMvc.perform(post("/v1/notices").header("Authorization", ADMIN_TOKEN)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.scope").value("all"))
                .andExpect(jsonPath("$.data.isPinned").value(true));
    }

    @Test
    void 작성_권한이_없으면_422를_반환한다() throws Exception {
        given(noticeService.create(any(), eq(2L)))
                .willThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "학원 전체 대상 공지는 원장/관리자만 작성할 수 있습니다."));

        String body = """
                {"scope":"all","title":"제목","content":"내용"}
                """;

        mockMvc.perform(post("/v1/notices").header("Authorization", TEACHER_TOKEN)
                        .contentType("application/json").content(body))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 공지_상세를_조회한다() throws Exception {
        given(noticeService.getScopeInfo(1L)).willReturn(new NoticeService.NoticeScopeInfo(NoticeScope.ALL, null));
        given(noticeService.getNotice(1L)).willReturn(
                new NoticeResponse(1L, 1L, "원장", NoticeScope.ALL, null, null,
                        "제목", "내용", false, LocalDateTime.of(2026, 8, 25, 9, 0)));

        mockMvc.perform(get("/v1/notices/1").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("제목"));
    }

    @Test
    void 공지가_없으면_404를_반환한다() throws Exception {
        given(noticeService.getScopeInfo(999L))
                .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다. id=999"));

        mockMvc.perform(get("/v1/notices/999").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void 공지를_수정한다() throws Exception {
        given(noticeService.getScopeInfo(1L)).willReturn(new NoticeService.NoticeScopeInfo(NoticeScope.ALL, null));
        given(noticeService.update(eq(1L), any())).willReturn(
                new NoticeResponse(1L, 1L, "원장", NoticeScope.ALL, null, null,
                        "수정된 제목", "내용", false, LocalDateTime.of(2026, 8, 25, 9, 0)));

        mockMvc.perform(patch("/v1/notices/1").header("Authorization", ADMIN_TOKEN)
                        .contentType("application/json")
                        .content("{\"title\":\"수정된 제목\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 제목"));
    }

    @Test
    void 공지를_삭제한다() throws Exception {
        given(noticeService.getScopeInfo(1L)).willReturn(new NoticeService.NoticeScopeInfo(NoticeScope.ALL, null));

        mockMvc.perform(delete("/v1/notices/1").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    void 상단_고정_상태를_변경한다() throws Exception {
        given(noticeService.getScopeInfo(1L)).willReturn(new NoticeService.NoticeScopeInfo(NoticeScope.ALL, null));
        given(noticeService.updatePinned(eq(1L), any())).willReturn(
                new NoticeResponse(1L, 1L, "원장", NoticeScope.ALL, null, null,
                        "제목", "내용", true, LocalDateTime.of(2026, 8, 25, 9, 0)));

        mockMvc.perform(patch("/v1/notices/1/pin").header("Authorization", ADMIN_TOKEN)
                        .contentType("application/json")
                        .content("{\"isPinned\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPinned").value(true));
    }

    @Test
    void 학생에게_노출되는_공지_목록을_me_notices로_조회한다() throws Exception {
        given(studentService.findStudentIdByUserId(101L)).willReturn(101L);
        given(noticeService.getRelevantToStudent(eq(101L), isNull())).willReturn(List.of(
                new NoticeResponse(1L, 1L, "원장", NoticeScope.ALL, null, null,
                        "추석 연휴 휴원 안내", "내용", true, LocalDateTime.of(2026, 8, 25, 9, 0)),
                new NoticeResponse(2L, 2L, "김선생", NoticeScope.CLASS, 3L, "중2 심화반",
                        "특강 안내", "내용", false, LocalDateTime.of(2026, 8, 27, 10, 30))
        ));

        mockMvc.perform(get("/v1/me/notices").header("Authorization", STUDENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].scope").value("all"));
    }
}
