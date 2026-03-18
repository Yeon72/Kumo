package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import net.kumo.kumo.domain.enums.JobStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 오사카 지역 기반의 구인 공고 중, 지도 마커 배치를 위한
 * 유효한 위치 좌표(Geocoding) 데이터를 포함하는 공고 정보를 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "osaka_geocoded", indexes = {
        @Index(name = "idx_lat_lng", columnList = "lat, lng"),
        @Index(name = "idx_company_address", columnList = "company_name, address"),
        @Index(name = "idx_region_jp", columnList = "prefecture_jp, city_jp, ward_jp"),
        @Index(name = "idx_region_kr", columnList = "prefecture_kr, city_kr, ward_kr")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OsakaGeocodedEntity extends BaseEntity {

    @Column(name = "row_no")
    private Integer rowNo;

    @Column(name = "datanum", unique = true)
    private Long datanum;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "href", length = 200)
    private String href;

    @Column(name = "write_time")
    private String writeTime;

    @Column(name = "img_urls", length = 500)
    private String imgUrls;

    @Lob
    @Column(name = "body")
    private String body;

    @Column(name = "company_name", length = 50)
    private String companyName;

    @Column(name = "address", length = 50)
    private String address;

    @Column(name = "contact_phone", length = 100)
    private String contactPhone;

    @Column(name = "position", length = 100)
    private String position;

    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "wage")
    private String wage;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "title_jp", length = 50)
    private String titleJp;

    @Column(name = "company_name_jp", length = 50)
    private String companyNameJp;

    @Column(name = "position_jp", length = 100)
    private String positionJp;

    @Column(name = "job_description_jp", columnDefinition = "TEXT")
    private String jobDescriptionJp;

    @Column(name = "contact_phone_jp", length = 100)
    private String contactPhoneJp;

    @Column(name = "wage_jp")
    private String wageJp;

    @Column(name = "notes_jp", columnDefinition = "TEXT")
    private String notesJp;

    @Column(name = "lat", nullable = false)
    private Double lat;

    @Column(name = "lng", nullable = false)
    private Double lng;

    @Column(name = "prefecture_jp", length = 20)
    private String prefectureJp;

    @Column(name = "city_jp", length = 20)
    private String cityJp;

    @Column(name = "ward_jp", length = 20)
    private String wardJp;

    @Column(name = "prefecture_kr", length = 20)
    private String prefectureKr;

    @Column(name = "city_kr", length = 20)
    private String cityKr;

    @Column(name = "ward_kr", length = 20)
    private String wardKr;

    /** 구인자(Recruiter)가 플랫폼 내에서 신규 등록한 공고일 경우 작성자 계정 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    /** 플랫폼 내 신규 등록 공고일 경우 연관 회사 정보 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyEntity company;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('RECRUITING', 'CLOSED') DEFAULT 'RECRUITING'")
    private JobStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = JobStatus.RECRUITING;
        }
    }

    @Column(name = "salary_type")
    private String salaryType;

    @Column(name = "salary_amount")
    private Integer salaryAmount;

}