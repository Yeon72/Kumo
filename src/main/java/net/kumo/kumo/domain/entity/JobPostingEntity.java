package net.kumo.kumo.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 외부 크롤링 데이터가 아닌, 시스템 내부에서 구인자가 직접 작성하여
 * 등록한 로컬 구인 공고(Job Posting) 데이터를 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "job_postings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPostingEntity {

    /** 자체 공고 전용 급여 지급 기준 열거형 */
    public enum SalaryType {
        HOURLY, MONTHLY, DAILY, SALARY
    }

    /** 자체 공고 모집 상태 열거형 */
    public enum JobStatus {
        RECRUITING, CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_post_id")
    private Long jobPostId;

    /** 공고를 작성한 사용자(구인자) 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** 공고가 연결된 대상 사업장(회사) 정보 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyEntity company;

    @Column(name = "region_id")
    private Long regionId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String position;

    @Lob
    private String description;

    /** 설정된 급여 지급 유형 (DB에는 문자열로 저장됨) */
    @Column(name = "salary_type")
    @Enumerated(EnumType.STRING)
    private SalaryType salaryType;

    @Column(name = "salary_amount")
    private Integer salaryAmount;

    @Column(name = "work_address")
    private String workAddress;

    private Double latitude;
    private Double longitude;

    /** 공고 진행 상태 (기본값: RECRUITING) */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('RECRUITING', 'CLOSED') DEFAULT 'RECRUITING'")
    private JobStatus status;

    /** 공고 모집 마감 기한 */
    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "view_count")
    private Integer viewCount;

    /** 공고 데이터 최초 생성(등록) 일시 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 엔티티가 영속화(Persist)되기 전, 누락된 필수 필드들의
     * 기본값을 자동으로 초기화하는 콜백 메서드입니다.
     */
    @PrePersist
    public void prePersist() {
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
        if (this.status == null) {
            this.status = JobStatus.RECRUITING;
        }
        if (this.salaryType == null) {
            this.salaryType = SalaryType.HOURLY;
        }
    }
}