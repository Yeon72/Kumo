package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 플랫폼 내에 직접 등록되는 구인 공고(JobPosting)에
 * 다중 첨부된 상세 이미지 파일 경로를 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "job_images")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class JobImageEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "img_id")
    private Long id;

    /** 이미지가 첨부된 대상 구인 공고 정보 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_post_id", nullable = false)
    private JobPostingEntity jobPosting;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

}