package academic.academic.domain.monthlyexam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 점수대별 피드백 (FR-05-04). 성적(MonthlyExamRecord) 1건당 최대 1건이며, 등록/수정은 upsert로 처리한다.
 */
@Entity
@Table(name = "monthly_exam_score_feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyExamScoreFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monthly_exam_record_id", nullable = false, unique = true)
    private MonthlyExamRecord monthlyExamRecord;

    @Column(name = "score_band", nullable = false, length = 50)
    private String scoreBand;

    @Column(name = "feedback_text", length = 1000)
    private String feedbackText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public MonthlyExamScoreFeedback(MonthlyExamRecord monthlyExamRecord, String scoreBand, String feedbackText) {
        this.monthlyExamRecord = monthlyExamRecord;
        this.scoreBand = scoreBand;
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

    public void update(String scoreBand, String feedbackText) {
        this.scoreBand = scoreBand;
        this.feedbackText = feedbackText;
    }
}
