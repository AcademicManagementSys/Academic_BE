package academic.academic.domain.attendance.controller;

import academic.academic.domain.attendance.dto.AttendanceBulkRequest;
import academic.academic.domain.attendance.dto.AttendanceRecordItem;
import academic.academic.domain.attendance.dto.AttendanceResponse;
import academic.academic.domain.attendance.dto.AttendanceUpdateRequest;
import academic.academic.domain.attendance.entity.AttendanceStatus;
import academic.academic.domain.attendance.service.AttendanceService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceController.class)
@Import(JwtProvider.class)
class AttendanceControllerTest {

    private static final String TEACHER_TOKEN = AuthTestSupport.bearer(2L, Role.TEACHER);
    private static final String PARENT_TOKEN = AuthTestSupport.bearer(45L, Role.PARENT);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AttendanceService attendanceService;
    @MockitoBean
    private AuthorizationService authorizationService;

    @Test
    void 반과_날짜로_출석_목록을_조회한다() throws Exception {
        given(attendanceService.getAttendanceByClassAndDate(3L, "2026-08-19"))
                .willReturn(List.of(new AttendanceResponse(1L, 101L, "김민준",
                        LocalDate.of(2026, 8, 19), AttendanceStatus.PRESENT, null)));

        mockMvc.perform(get("/v1/attendance").header("Authorization", TEACHER_TOKEN)
                        .param("classId", "3").param("date", "2026-08-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].studentName").value("김민준"))
                .andExpect(jsonPath("$.data[0].status").value("present"));
    }

    @Test
    void 토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/v1/attendance").param("classId", "3").param("date", "2026-08-19"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void 담당하지_않은_반이면_403을_반환한다() throws Exception {
        willThrow(new BusinessException(ErrorCode.FORBIDDEN_SCOPE, "담당하지 않은 반의 데이터에는 접근할 수 없습니다."))
                .given(authorizationService).requireCanManageClass(any(), eq(3L));

        mockMvc.perform(get("/v1/attendance").header("Authorization", TEACHER_TOKEN)
                        .param("classId", "3").param("date", "2026-08-19"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_SCOPE"));
    }

    @Test
    void 필수_파라미터가_없으면_422를_반환한다() throws Exception {
        mockMvc.perform(get("/v1/attendance").header("Authorization", TEACHER_TOKEN).param("classId", "3"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 반_단위_출석을_일괄_저장하면_201을_반환한다() throws Exception {
        AttendanceBulkRequest request = new AttendanceBulkRequest(3L, LocalDate.of(2026, 8, 19), List.of(
                new AttendanceRecordItem(101L, AttendanceStatus.PRESENT, null)
        ));
        given(attendanceService.saveBulk(any(AttendanceBulkRequest.class)))
                .willReturn(List.of(new AttendanceResponse(1L, 101L, "김민준",
                        LocalDate.of(2026, 8, 19), AttendanceStatus.PRESENT, null)));

        mockMvc.perform(post("/v1/attendance/bulk").header("Authorization", TEACHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].status").value("present"));
    }

    @Test
    void classId가_없는_요청은_422를_반환한다() throws Exception {
        String invalidJson = """
                {"date":"2026-08-19","records":[{"studentId":101,"status":"present"}]}
                """;

        mockMvc.perform(post("/v1/attendance/bulk").header("Authorization", TEACHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 존재하지_않는_출석_기록을_수정하면_404를_반환한다() throws Exception {
        given(attendanceService.updateAttendance(anyLong(), any(AttendanceUpdateRequest.class)))
                .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "출석 기록을 찾을 수 없습니다. id=999"));

        mockMvc.perform(patch("/v1/attendance/999").header("Authorization", TEACHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"late\",\"note\":\"지각\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void 학생_월별_출석을_조회한다() throws Exception {
        given(attendanceService.getStudentAttendance(101L, "2026-08"))
                .willReturn(List.of(
                        new AttendanceResponse(1L, 101L, "김민준", LocalDate.of(2026, 8, 3), AttendanceStatus.PRESENT, null),
                        new AttendanceResponse(2L, 101L, "김민준", LocalDate.of(2026, 8, 10), AttendanceStatus.ABSENT, "병원 진료")
                ));

        mockMvc.perform(get("/v1/students/101/attendance").header("Authorization", PARENT_TOKEN)
                        .param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].status").value("absent"));
    }
}
