package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

/**
 * 사용자가 관심 있는 특정 구인 공고를 즐겨찾기(스크랩)한
 * 내역을 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "scraps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ScrapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scrapId;

    /** 스크랩을 수행한 사용자 식별자 */
    @Column(nullable = false)
    private Long userId;

    /** 스크랩 대상 구인 공고 식별자 */
    @Column(nullable = false)
    private Long jobPostId;

    /** 스크랩 대상 공고의 데이터 출처 (TOKYO, OSAKA 등) */
    @Column(name = "source", length = 20)
    private String source;

    /** 스크랩(즐겨찾기 추가) 생성 일시 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

}