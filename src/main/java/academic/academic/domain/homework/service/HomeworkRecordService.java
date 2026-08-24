package academic.academic.domain.homework.service;

import academic.academic.domain.homework.dto.HomeworkItemRecordsGroup;
import academic.academic.domain.homework.dto.HomeworkRecordBulkRequest;
import academic.academic.domain.homework.dto.HomeworkRecordItem;
import academic.academic.domain.homework.dto.HomeworkRecordResponse;
import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.homework.entity.HomeworkRecord;
import academic.academic.domain.homework.repository.HomeworkItemRepository;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeworkRecordService {

    private final HomeworkItemRepository homeworkItemRepository;
    private final HomeworkRecordRepository homeworkRecordRepository;
    private final StudentRepository studentRepository;

    public List<HomeworkRecordResponse> getItemRecords(Long homeworkItemId) {
        HomeworkItem item = homeworkItemRepository.findById(homeworkItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "숙제 항목을 찾을 수 없습니다. id=" + homeworkItemId));

        List<Student> targetStudents = item.getSchoolClass() != null
                ? studentRepository.findBySchoolClassId(item.getSchoolClass().getId())
                : List.of(item.getStudent());

        Map<Long, HomeworkRecord> byStudentId = homeworkRecordRepository.findByHomeworkItemId(homeworkItemId).stream()
                .collect(Collectors.toMap(r -> r.getStudent().getId(), r -> r));

        return targetStudents.stream()
                .map(s -> {
                    HomeworkRecord record = byStudentId.get(s.getId());
                    return record != null ? HomeworkRecordResponse.from(record) : HomeworkRecordResponse.unchecked(item, s);
                })
                .toList();
    }

    @Transactional
    public List<HomeworkRecordResponse> saveBulk(HomeworkRecordBulkRequest request) {
        List<HomeworkRecordResponse> responses = new ArrayList<>();

        for (HomeworkItemRecordsGroup group : request.items()) {
            HomeworkItem item = homeworkItemRepository.findById(group.homeworkItemId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                            "숙제 항목을 찾을 수 없습니다. id=" + group.homeworkItemId()));

            if (item.getSchoolClass() != null && !item.getSchoolClass().getId().equals(request.classId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "요청한 반의 숙제 항목이 아닙니다. homeworkItemId=" + item.getId());
            }

            for (HomeworkRecordItem recordItem : group.records()) {
                Student student = studentRepository.findById(recordItem.studentId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                                "학생을 찾을 수 없습니다. id=" + recordItem.studentId()));
                validateStudentInScope(item, student);

                HomeworkRecord record = homeworkRecordRepository
                        .findByHomeworkItemIdAndStudentId(item.getId(), student.getId())
                        .orElse(null);
                if (record == null) {
                    record = homeworkRecordRepository.save(HomeworkRecord.builder()
                            .homeworkItem(item)
                            .student(student)
                            .done(recordItem.isDone())
                            .score(recordItem.score())
                            .comment(recordItem.comment())
                            .build());
                } else {
                    record.update(recordItem.isDone(), recordItem.score(), recordItem.comment());
                }
                responses.add(HomeworkRecordResponse.from(record));
            }
        }
        return responses;
    }

    public List<HomeworkRecordResponse> getStudentHomework(Long studentId, LocalDate from, LocalDate to) {
        if (!studentRepository.existsById(studentId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다. id=" + studentId);
        }
        return homeworkRecordRepository.findByStudentIdAndAssignedDateBetween(studentId, from, to).stream()
                .map(HomeworkRecordResponse::from)
                .toList();
    }

    private void validateStudentInScope(HomeworkItem item, Student student) {
        if (item.getSchoolClass() != null) {
            if (student.getSchoolClass() == null || !student.getSchoolClass().getId().equals(item.getSchoolClass().getId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "해당 숙제 항목의 대상 반 소속 학생이 아닙니다. studentId=" + student.getId());
            }
        } else if (!item.getStudent().getId().equals(student.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "해당 숙제 항목의 대상 학생이 아닙니다. studentId=" + student.getId());
        }
    }
}
