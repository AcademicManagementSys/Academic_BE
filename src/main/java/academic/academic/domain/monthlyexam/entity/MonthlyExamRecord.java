package academic.academic.domain.monthlyexam.entity;

import academic.academic.domain.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 학생별 월말모의고사 성적 (FR-05-02). 원점수/표준점수/백분위/등급 중 학원에서 사용하는 지표를 선택적으로 입력한다.
 */
@Entity
@Table(name = "monthly_exam_records", uniqueConstraints = @UniqueConstraint(
        name = "uk_monthly_exam_record_exam_student", columnNames = {"monthly_exam_id", "student_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyExamRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monthly_exam_id", nullable = false)
    private MonthlyExam monthlyExam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "raw_score")
    private Integer rawScore;

    @Column(name = "std_score")
    private Integer stdScore;

    private Integer percentile;

    @Column(length = 20)
    private String grade;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public MonthlyExamRecord(MonthlyExam monthlyExam, Student student, Integer rawScore, Integer stdScore,
                              Integer percentile, String grade) {
        this.monthlyExam = monthlyExam;
        this.student = student;
        this.rawScore = rawScore;
        this.stdScore = stdScore;
        this.percentile = percentile;
        this.grade = grade;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(Integer rawScore, Integer stdScore, Integer percentile, String grade) {
        this.rawScore = rawScore;
        this.stdScore = stdScore;
        this.percentile = percentile;
        this.grade = grade;
    }
}
