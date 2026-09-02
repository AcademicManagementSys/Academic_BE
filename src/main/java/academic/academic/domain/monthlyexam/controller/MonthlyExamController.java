package academic.academic.domain.monthlyexam.controller;

import academic.academic.domain.monthlyexam.dto.MonthlyExamCreateRequest;
import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordCreateRequest;
import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordDetailResponse;
import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordResponse;
import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordUpdateRequest;
import academic.academic.domain.monthlyexam.dto.MonthlyExamResponse;
import academic.academic.domain.monthlyexam.dto.MonthlyExamTrendResponse;
import academic.academic.domain.monthlyexam.dto.ScoreFeedbackResponse;
import academic.academic.domain.monthlyexam.dto.ScoreFeedbackUpsertRequest;
import academic.academic.domain.monthlyexam.dto.TypeFeedbackCreateRequest;
import academic.academic.domain.monthlyexam.dto.TypeFeedbackResponse;
import academic.academic.domain.monthlyexam.dto.TypeFeedbackUpdateRequest;
import academic.academic.domain.monthlyexam.service.MonthlyExamFeedbackService;
import academic.academic.domain.monthlyexam.service.MonthlyExamRecordService;
import academic.academic.domain.monthlyexam.service.MonthlyExamService;
import academic.academic.domain.user.entity.Role;
import academic.academic.global.response.ApiResponse;
import academic.academic.global.security.AuthenticatedUser;
import academic.academic.global.security.AuthorizationService;
import academic.academic.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 월말모의고사 API (SCR-10, SCR-16, FR-05-01 ~ FR-05-08). MonthlyExam(회차) 자체는 반과 무관한
 * 학원 전체 자원이라 회차 목록/생성은 admin·teacher 역할 체크만 하고, 성적·피드백은 학생 단위로
 * 소유권을 체크한다.
 */
@RestController
@RequiredArgsConstructor
public class MonthlyExamController {

    private final MonthlyExamService monthlyExamService;
    private final MonthlyExamRecordService monthlyExamRecordService;
    private final MonthlyExamFeedbackService monthlyExamFeedbackService;
    private final AuthorizationService authorizationService;

    @GetMapping("/v1/monthly-exams")
    public ApiResponse<List<MonthlyExamResponse>> getMonthlyExams(@CurrentUser AuthenticatedUser me) {
        authorizationService.requireRole(me, Role.ADMIN, Role.TEACHER);
        return ApiResponse.of(monthlyExamService.list());
    }

    @PostMapping("/v1/monthly-exams")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MonthlyExamResponse> createMonthlyExam(@CurrentUser AuthenticatedUser me,
                                                                @Valid @RequestBody MonthlyExamCreateRequest request) {
        authorizationService.requireRole(me, Role.ADMIN, Role.TEACHER);
        return ApiResponse.of(monthlyExamService.create(request));
    }

    @GetMapping("/v1/monthly-exams/{id}/records")
    public ApiResponse<List<MonthlyExamRecordResponse>> getMonthlyExamRecords(
            @CurrentUser AuthenticatedUser me,
            @PathVariable Long id,
            @RequestParam Long classId) {
        authorizationService.requireCanManageClass(me, classId);
        return ApiResponse.of(monthlyExamRecordService.getByClass(id, classId));
    }

    @PostMapping("/v1/monthly-exam-records")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MonthlyExamRecordResponse> createMonthlyExamRecord(
            @CurrentUser AuthenticatedUser me,
            @Valid @RequestBody MonthlyExamRecordCreateRequest request) {
        authorizationService.requireCanManageClassOrStudent(me, null, request.studentId());
        return ApiResponse.of(monthlyExamRecordService.create(request));
    }

    @PatchMapping("/v1/monthly-exam-records/{id}")
    public ApiResponse<MonthlyExamRecordResponse> updateMonthlyExamRecord(
            @CurrentUser AuthenticatedUser me,
            @PathVariable Long id,
            @RequestBody MonthlyExamRecordUpdateRequest request) {
        authorizationService.requireCanManageClassOrStudent(me, null, monthlyExamRecordService.getStudentIdForRecord(id));
        return ApiResponse.of(monthlyExamRecordService.update(id, request));
    }

    @GetMapping("/v1/monthly-exam-records/{id}")
    public ApiResponse<MonthlyExamRecordDetailResponse> getMonthlyExamRecordDetail(@CurrentUser AuthenticatedUser me,
                                                                                     @PathVariable Long id) {
        authorizationService.requireCanViewStudent(me, monthlyExamFeedbackService.getStudentIdForRecord(id));
        return ApiResponse.of(monthlyExamFeedbackService.getDetail(id));
    }

    @GetMapping("/v1/students/{studentId}/monthly-exams")
    public ApiResponse<List<MonthlyExamTrendResponse>> getStudentMonthlyExams(
            @CurrentUser AuthenticatedUser me,
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "5") int limit) {
        authorizationService.requireCanViewStudent(me, studentId);
        return ApiResponse.of(monthlyExamRecordService.getStudentTrend(studentId, limit));
    }

    @PostMapping("/v1/monthly-exam-records/{id}/type-feedbacks")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TypeFeedbackResponse> addTypeFeedback(
            @CurrentUser AuthenticatedUser me,
            @PathVariable Long id,
            @Valid @RequestBody TypeFeedbackCreateRequest request) {
        authorizationService.requireCanManageClassOrStudent(me, null, monthlyExamFeedbackService.getStudentIdForRecord(id));
        return ApiResponse.of(monthlyExamFeedbackService.addTypeFeedback(id, request));
    }

    @PatchMapping("/v1/type-feedbacks/{id}")
    public ApiResponse<TypeFeedbackResponse> updateTypeFeedback(
            @CurrentUser AuthenticatedUser me,
            @PathVariable Long id,
            @RequestBody TypeFeedbackUpdateRequest request) {
        authorizationService.requireCanManageClassOrStudent(me, null, monthlyExamFeedbackService.getStudentIdForTypeFeedback(id));
        return ApiResponse.of(monthlyExamFeedbackService.updateTypeFeedback(id, request));
    }

    @DeleteMapping("/v1/type-feedbacks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTypeFeedback(@CurrentUser AuthenticatedUser me, @PathVariable Long id) {
        authorizationService.requireCanManageClassOrStudent(me, null, monthlyExamFeedbackService.getStudentIdForTypeFeedback(id));
        monthlyExamFeedbackService.deleteTypeFeedback(id);
    }

    @PutMapping("/v1/monthly-exam-records/{id}/score-feedback")
    public ApiResponse<ScoreFeedbackResponse> upsertScoreFeedback(
            @CurrentUser AuthenticatedUser me,
            @PathVariable Long id,
            @Valid @RequestBody ScoreFeedbackUpsertRequest request) {
        authorizationService.requireCanManageClassOrStudent(me, null, monthlyExamFeedbackService.getStudentIdForRecord(id));
        return ApiResponse.of(monthlyExamFeedbackService.upsertScoreFeedback(id, request));
    }
}
