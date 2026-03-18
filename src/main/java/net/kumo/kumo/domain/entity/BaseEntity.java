package net.kumo.kumo.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import net.kumo.kumo.domain.enums.JobStatus;

/**
 * 구인 공고(OSAKA, TOKYO 등 지역별 테이블) 엔티티들의
 * 공통 필드 및 다국어 속성을 정의하는 MappedSuperclass 입니다.
 */
@MappedSuperclass
@Getter
@Setter
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
    @Column(name = "img_urls", columnDefinition = "LONGTEXT")
    private String imgUrls;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String body;

    private String companyName;

    @Column(length = 300)
    private String address;

    private String contactPhone;

    private String position;

    @Lob
    private String jobDescription;

    private String wage;

    @Lob
    private String notes;

    /* --- 다국어(일본어) 지원 필드 --- */

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
     * 공고 모집 상태 관리
     * DB에는 열거형의 이름("RECRUITING", "CLOSED" 등) 문자열로 저장됩니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private JobStatus status;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(name = "view_count")
    private Integer viewCount;

    /**
     * 공고 조회수를 1씩 증가시키는 비즈니스 로직 메서드입니다.
     */
    public void addViewCount() {
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
        this.viewCount++;
    }
}