package academic.academic.domain.homework.controller;

import academic.academic.domain.homework.dto.HomeworkItemCreateRequest;
import academic.academic.domain.homework.dto.HomeworkItemResponse;
import academic.academic.domain.homework.dto.HomeworkItemUpdateRequest;
import academic.academic.domain.homework.dto.HomeworkRecordBulkRequest;
import academic.academic.domain.homework.dto.HomeworkRecordResponse;
import academic.academic.domain.homework.service.HomeworkItemService;
import academic.academic.domain.homework.service.HomeworkRecordService;
import academic.academic.global.response.ApiResponse;
import academic.academic.global.security.AuthenticatedUser;
import academic.academic.global.security.AuthorizationService;
import academic.academic.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 숙제 API (SCR-08, SCR-14, FR-03-01 ~ FR-03-07)
 */
@RestController
@RequiredArgsConstructor
public class HomeworkController {

    private final HomeworkItemService homeworkItemService;
    private final HomeworkRecordService homeworkRecordService;
    private final AuthorizationService authorizationService;

    @GetMapping("/v1/homework-items")
    public ApiResponse<List<HomeworkItemResponse>> getHomeworkItems(
            @CurrentUser AuthenticatedUser me,
            @RequestParam Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        authorizationService.requireCanManageClass(me, classId);
        return ApiResponse.of(homeworkItemService.search(classId, week));
    }

    @PostMapping("/v1/homework-items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HomeworkItemResponse> createHomeworkItem(@CurrentUser AuthenticatedUser me,
                                                                  @Valid @RequestBody HomeworkItemCreateRequest request) {
        authorizationService.requireCanManageClassOrStudent(me, request.classId(), request.studentId());
        return ApiResponse.of(homeworkItemService.create(request));
    }

    @PatchMapping("/v1/homework-items/{id}")
    public ApiResponse<HomeworkItemResponse> updateHomeworkItem(@CurrentUser AuthenticatedUser me, @PathVariable Long id,
                                                                  @RequestBody HomeworkItemUpdateRequest request) {
        requireCanManageItem(me, id);
        return ApiResponse.of(homeworkItemService.update(id, request));
    }

    @DeleteMapping("/v1/homework-items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHomeworkItem(@CurrentUser AuthenticatedUser me, @PathVariable Long id) {
        requireCanManageItem(me, id);
        homeworkItemService.delete(id);
    }

    @GetMapping("/v1/homework-items/{id}/records")
    public ApiResponse<List<HomeworkRecordResponse>> getHomeworkItemRecords(@CurrentUser AuthenticatedUser me,
                                                                              @PathVariable Long id) {
        requireCanManageItem(me, id);
        return ApiResponse.of(homeworkRecordService.getItemRecords(id));
    }

    @PostMapping("/v1/homework-records/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<HomeworkRecordResponse>> saveBulk(@CurrentUser AuthenticatedUser me,
                                                                @Valid @RequestBody HomeworkRecordBulkRequest request) {
        authorizationService.requireCanManageClass(me, request.classId());
        return ApiResponse.of(homeworkRecordService.saveBulk(request));
    }

    @GetMapping("/v1/students/{studentId}/homework")
    public ApiResponse<List<HomeworkRecordResponse>> getStudentHomework(
            @CurrentUser AuthenticatedUser me,
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        authorizationService.requireCanViewStudent(me, studentId);
        return ApiResponse.of(homeworkRecordService.getStudentHomework(studentId, from, to));
    }

    private void requireCanManageItem(AuthenticatedUser me, Long homeworkItemId) {
        HomeworkItemService.ItemScope scope = homeworkItemService.getScope(homeworkItemId);
        authorizationService.requireCanManageClassOrStudent(me, scope.classId(), scope.studentId());
    }
}
