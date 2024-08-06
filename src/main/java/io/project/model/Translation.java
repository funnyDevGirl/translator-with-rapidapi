package io.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDate;
import static jakarta.persistence.GenerationType.IDENTITY;


@Entity
@Table(name = "translation")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@EqualsAndHashCode
public class Translation implements BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    private String ipAddress;

    @NotBlank
    @Size(max = 10000)
    private String inputText;

    private String translatedText;

    @CreatedDate
    private LocalDate createdAt;
}
