package academic.academic.domain.homework.controller;

import academic.academic.domain.homework.dto.HomeworkItemCreateRequest;
import academic.academic.domain.homework.dto.HomeworkItemResponse;
import academic.academic.domain.homework.dto.HomeworkItemUpdateRequest;
import academic.academic.domain.homework.dto.HomeworkRecordBulkRequest;
import academic.academic.domain.homework.dto.HomeworkRecordResponse;
import academic.academic.domain.homework.service.HomeworkItemService;
import academic.academic.domain.homework.service.HomeworkRecordService;
import academic.academic.global.response.ApiResponse;
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

    @GetMapping("/v1/homework-items")
    public ApiResponse<List<HomeworkItemResponse>> getHomeworkItems(
            @RequestParam Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return ApiResponse.of(homeworkItemService.search(classId, week));
    }

    @PostMapping("/v1/homework-items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HomeworkItemResponse> createHomeworkItem(@Valid @RequestBody HomeworkItemCreateRequest request) {
        return ApiResponse.of(homeworkItemService.create(request));
    }

    @PatchMapping("/v1/homework-items/{id}")
    public ApiResponse<HomeworkItemResponse> updateHomeworkItem(@PathVariable Long id,
                                                                  @RequestBody HomeworkItemUpdateRequest request) {
        return ApiResponse.of(homeworkItemService.update(id, request));
    }

    @DeleteMapping("/v1/homework-items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHomeworkItem(@PathVariable Long id) {
        homeworkItemService.delete(id);
    }

    @GetMapping("/v1/homework-items/{id}/records")
    public ApiResponse<List<HomeworkRecordResponse>> getHomeworkItemRecords(@PathVariable Long id) {
        return ApiResponse.of(homeworkRecordService.getItemRecords(id));
    }

    @PostMapping("/v1/homework-records/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<HomeworkRecordResponse>> saveBulk(@Valid @RequestBody HomeworkRecordBulkRequest request) {
        return ApiResponse.of(homeworkRecordService.saveBulk(request));
    }

    @GetMapping("/v1/students/{studentId}/homework")
    public ApiResponse<List<HomeworkRecordResponse>> getStudentHomework(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.of(homeworkRecordService.getStudentHomework(studentId, from, to));
    }
}
