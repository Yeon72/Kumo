package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.kumo.kumo.domain.enums.JobStatus;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter @Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer rowNo;

    @Column(nullable = false)
    private Long datanum;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 700)
    private String href;

    private String writeTime;

    @Lob
    private String imgUrls;

    @Lob
    private String body;

    private String companyName;

    @Column(length = 700)
    private String address;

    private String contactPhone;
    private String position;

    @Lob
    private String jobDescription;
    private String wage;

    @Lob
    private String notes;

    // 일본어 필드
    private String titleJp;
    private String companyNameJp;
    private String positionJp;
    @Lob
    private String jobDescriptionJp;
    private String wageJp;
    @Lob
    private String notesJp;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 공고 상태 관리
     * @Enumerated(EnumType.STRING): DB에 숫자가 아닌 "RECRUITING" 문자열로 저장됨
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private JobStatus status;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;
}