package academic.academic.domain.notice.controller;

import academic.academic.domain.notice.dto.NoticeCreateRequest;
import academic.academic.domain.notice.dto.NoticePinUpdateRequest;
import academic.academic.domain.notice.dto.NoticeResponse;
import academic.academic.domain.notice.dto.NoticeUpdateRequest;
import academic.academic.domain.notice.service.NoticeService;
import academic.academic.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

/**
 * 공지사항 API (SCR-18, SCR-19, FR-09-01 ~ FR-09-06)
 */
@RestController
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/v1/notices")
    public ApiResponse<List<NoticeResponse>> getNotices(@RequestParam(required = false) String scope,
                                                          @RequestParam(required = false) Long classId,
                                                          @RequestParam(required = false) Integer limit) {
        return ApiResponse.of(noticeService.search(scope, classId, limit));
    }

    @PostMapping("/v1/notices")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NoticeResponse> createNotice(@Valid @RequestBody NoticeCreateRequest request) {
        return ApiResponse.of(noticeService.create(request));
    }

    @GetMapping("/v1/notices/{id}")
    public ApiResponse<NoticeResponse> getNotice(@PathVariable Long id) {
        return ApiResponse.of(noticeService.getNotice(id));
    }

    @PatchMapping("/v1/notices/{id}")
    public ApiResponse<NoticeResponse> updateNotice(@PathVariable Long id, @RequestBody NoticeUpdateRequest request) {
        return ApiResponse.of(noticeService.update(id, request));
    }

    @DeleteMapping("/v1/notices/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotice(@PathVariable Long id) {
        noticeService.delete(id);
    }

    @PatchMapping("/v1/notices/{id}/pin")
    public ApiResponse<NoticeResponse> updatePinned(@PathVariable Long id,
                                                     @Valid @RequestBody NoticePinUpdateRequest request) {
        return ApiResponse.of(noticeService.updatePinned(id, request));
    }

    @GetMapping("/v1/students/{studentId}/notices")
    public ApiResponse<List<NoticeResponse>> getStudentNotices(@PathVariable Long studentId,
                                                                 @RequestParam(required = false) Integer limit) {
        return ApiResponse.of(noticeService.getRelevantToStudent(studentId, limit));
    }
}
