package academic.academic.domain.notice.controller;

import academic.academic.domain.notice.dto.NoticeResponse;
import academic.academic.domain.notice.entity.NoticeScope;
import academic.academic.domain.notice.service.NoticeService;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoticeController.class)
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoticeService noticeService;

    @Test
    void 공지_목록을_조회한다() throws Exception {
        given(noticeService.search(eq("class"), eq(3L), eq(5))).willReturn(List.of(
                new NoticeResponse(1L, 1L, "원장", NoticeScope.CLASS, 3L, "중2 심화반",
                        "특강 안내", "내용", false, LocalDateTime.of(2026, 8, 27, 10, 30))
        ));

        mockMvc.perform(get("/v1/notices").param("scope", "class").param("classId", "3").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("특강 안내"));
    }

    @Test
    void 공지를_작성한다() throws Exception {
        given(noticeService.create(any())).willReturn(
                new NoticeResponse(1L, 1L, "원장", NoticeScope.ALL, null, null,
                        "추석 연휴 휴원 안내", "내용", true, LocalDateTime.of(2026, 8, 25, 9, 0)));

        String body = """
                {"authorId":1,"scope":"all","title":"추석 연휴 휴원 안내","content":"내용","isPinned":true}
                """;

        mockMvc.perform(post("/v1/notices").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.scope").value("all"))
                .andExpect(jsonPath("$.data.isPinned").value(true));
    }

    @Test
    void 작성_권한이_없으면_422를_반환한다() throws Exception {
        given(noticeService.create(any()))
                .willThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "학원 전체 대상 공지는 원장/관리자만 작성할 수 있습니다."));

        String body = """
                {"authorId":2,"scope":"all","title":"제목","content":"내용"}
                """;

        mockMvc.perform(post("/v1/notices").contentType("application/json").content(body))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 공지_상세를_조회한다() throws Exception {
        given(noticeService.getNotice(1L)).willReturn(
                new NoticeResponse(1L, 1L, "원장", NoticeScope.ALL, null, null,
                        "제목", "내용", false, LocalDateTime.of(2026, 8, 25, 9, 0)));

        mockMvc.perform(get("/v1/notices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("제목"));
    }

    @Test
    void 공지가_없으면_404를_반환한다() throws Exception {
        given(noticeService.getNotice(999L))
                .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다. id=999"));

        mockMvc.perform(get("/v1/notices/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 공지를_수정한다() throws Exception {
        given(noticeService.update(eq(1L), any())).willReturn(
                new NoticeResponse(1L, 1L, "원장", NoticeScope.ALL, null, null,
                        "수정된 제목", "내용", false, LocalDateTime.of(2026, 8, 25, 9, 0)));

        mockMvc.perform(patch("/v1/notices/1").contentType("application/json")
                        .content("{\"title\":\"수정된 제목\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 제목"));
    }

    @Test
    void 공지를_삭제한다() throws Exception {
        mockMvc.perform(delete("/v1/notices/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 상단_고정_상태를_변경한다() throws Exception {
        given(noticeService.updatePinned(eq(1L), any())).willReturn(
                new NoticeResponse(1L, 1L, "원장", NoticeScope.ALL, null, null,
                        "제목", "내용", true, LocalDateTime.of(2026, 8, 25, 9, 0)));

        mockMvc.perform(patch("/v1/notices/1/pin").contentType("application/json")
                        .content("{\"isPinned\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPinned").value(true));
    }

    @Test
    void 학생에게_노출되는_공지_목록을_조회한다() throws Exception {
        given(noticeService.getRelevantToStudent(eq(101L), isNull())).willReturn(List.of(
                new NoticeResponse(1L, 1L, "원장", NoticeScope.ALL, null, null,
                        "추석 연휴 휴원 안내", "내용", true, LocalDateTime.of(2026, 8, 25, 9, 0)),
                new NoticeResponse(2L, 2L, "김선생", NoticeScope.CLASS, 3L, "중2 심화반",
                        "특강 안내", "내용", false, LocalDateTime.of(2026, 8, 27, 10, 30))
        ));

        mockMvc.perform(get("/v1/students/101/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].scope").value("all"));
    }
}
