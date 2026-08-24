package academic.academic.domain.schoolclass.entity;

import academic.academic.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "classes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(length = 200)
    private String schedule;

    @Builder
    public SchoolClass(String name, User teacher, String schedule) {
        this.name = name;
        this.teacher = teacher;
        this.schedule = schedule;
    }

    public void update(String name, User teacher, String schedule) {
        if (name != null) {
            this.name = name;
        }
        this.teacher = teacher;
        this.schedule = schedule;
    }
}
