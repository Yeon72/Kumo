package net.kumo.kumo.domain.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 구직자(Seeker)의 과거 구인 공고 지원 내역을 클라이언트(프론트엔드)로
 * 전달하기 위한 데이터 전송 객체(DTO)입니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeekerApplicationHistoryDTO {

    /** 지원 내역 고유 식별자 */
    private Long appId;

    /** 지원 대상 공고의 데이터 출처 (OSAKA, TOKYO 등) */
    private String targetSource;

    /** 지원 대상 공고의 고유 식별자 */
    private Long targetPostId;

    private String title;
    private String businessName;
    private String location;
    private String wage;
    private String wageJp;
    private String contact;
    private String manager;

    /** 공고 지원 일시 */
    private LocalDateTime appliedAt;

    /** 지원 처리 상태 (합격, 불합격, 대기 등) */
    private String status;

}