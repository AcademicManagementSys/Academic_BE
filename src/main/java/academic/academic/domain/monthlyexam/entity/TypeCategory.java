package academic.academic.domain.monthlyexam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 월말모의고사 문항 유형 카테고리 마스터 (FR-05-03, FR-05-08). 관리자가 사전에 정의한다(어휘, 어법, 빈칸추론 등).
 */
@Entity
@Table(name = "type_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TypeCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @Builder
    public TypeCategory(String name) {
        this.name = name;
    }
}
