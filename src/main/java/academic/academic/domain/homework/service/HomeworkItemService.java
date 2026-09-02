package academic.academic.domain.homework.service;

import academic.academic.domain.homework.dto.HomeworkItemCreateRequest;
import academic.academic.domain.homework.dto.HomeworkItemResponse;
import academic.academic.domain.homework.dto.HomeworkItemUpdateRequest;
import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.homework.repository.HomeworkItemRepository;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeworkItemService {

    private final HomeworkItemRepository homeworkItemRepository;
    private final HomeworkRecordRepository homeworkRecordRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public HomeworkItemResponse create(HomeworkItemCreateRequest request) {
        boolean hasClass = request.classId() != null;
        boolean hasStudent = request.studentId() != null;
        if (hasClass == hasStudent) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "classId, studentId 중 하나만 지정해야 합니다.");
        }

        SchoolClass schoolClass = null;
        Student student = null;
        if (hasClass) {
            schoolClass = schoolClassRepository.findById(request.classId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "반을 찾을 수 없습니다. id=" + request.classId()));
        } else {
            student = studentRepository.findById(request.studentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다. id=" + request.studentId()));
        }

        HomeworkItem item = homeworkItemRepository.save(HomeworkItem.builder()
                .schoolClass(schoolClass)
                .student(student)
                .title(request.title())
                .scope(request.scope())
                .assignedDate(request.assignedDate())
                .dueDate(request.dueDate())
                .build());
        return HomeworkItemResponse.from(item);
    }

    public List<HomeworkItemResponse> search(Long classId, LocalDate week) {
        schoolClassRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "반을 찾을 수 없습니다. id=" + classId));
        LocalDate weekStart = week;
        LocalDate weekEnd = week != null ? week.plusDays(6) : null;
        return homeworkItemRepository.search(classId, weekStart, weekEnd).stream()
                .map(HomeworkItemResponse::from)
                .toList();
    }

    @Transactional
    public HomeworkItemResponse update(Long id, HomeworkItemUpdateRequest request) {
        HomeworkItem item = getItem(id);
        item.update(request.title(), request.scope(), request.assignedDate(), request.dueDate());
        return HomeworkItemResponse.from(item);
    }

    @Transactional
    public void delete(Long id) {
        HomeworkItem item = getItem(id);
        homeworkRecordRepository.deleteByHomeworkItemId(item.getId());
        homeworkItemRepository.delete(item);
    }

    HomeworkItem getItem(Long id) {
        return homeworkItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "숙제 항목을 찾을 수 없습니다. id=" + id));
    }

    /** 소유권 체크용 — 숙제 항목이 반 단위인지 개별 학생 단위인지 조회한다. */
    public ItemScope getScope(Long id) {
        HomeworkItem item = getItem(id);
        Long classId = item.getSchoolClass() != null ? item.getSchoolClass().getId() : null;
        Long studentId = item.getStudent() != null ? item.getStudent().getId() : null;
        return new ItemScope(classId, studentId);
    }

    public record ItemScope(Long classId, Long studentId) {
    }
}
