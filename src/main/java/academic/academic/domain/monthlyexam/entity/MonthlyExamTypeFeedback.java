package academic.academic.domain.monthlyexam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 유형별 피드백 (FR-05-03, FR-05-04). 하나의 성적(MonthlyExamRecord)에 유형 카테고리별로 여러 건 존재할 수 있다.
 */
@Entity
@Table(name = "monthly_exam_type_feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyExamTypeFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monthly_exam_record_id", nullable = false)
    private MonthlyExamRecord monthlyExamRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_category_id", nullable = false)
    private TypeCategory typeCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status;

    @Column(name = "feedback_text", length = 1000)
    private String feedbackText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public MonthlyExamTypeFeedback(MonthlyExamRecord monthlyExamRecord, TypeCategory typeCategory,
                                    FeedbackStatus status, String feedbackText) {
        this.monthlyExamRecord = monthlyExamRecord;
        this.typeCategory = typeCategory;
        this.status = status;
        this.feedbackText = feedbackText;
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

    public void update(FeedbackStatus status, String feedbackText) {
        if (status != null) {
            this.status = status;
        }
        this.feedbackText = feedbackText;
    }
}
