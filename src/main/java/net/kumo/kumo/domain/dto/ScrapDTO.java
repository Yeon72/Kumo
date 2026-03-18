package net.kumo.kumo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 사용자가 즐겨찾기(찜)한 구인 공고 정보를 담는 데이터 전송 객체입니다.
 * 클라이언트와의 비동기 통신(AJAX/Fetch) 및 ORM(MyBatis/JPA) 매핑용으로 함께 사용됩니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScrapDTO {

    private Long scrapId;
    private Long userId;
    private Long jobPostId;
    private Timestamp createTime;

    /** 클라이언트 통신을 위한 대상 구인 공고 식별자 */
    private Long targetPostId;

    /** 클라이언트 응답용 스크랩 처리 결과 상태 반환값 */
    private boolean isScraped;

    /** 대상 공고 데이터 출처 (OSAKA, TOKYO 등) */
    private String targetSource;

}