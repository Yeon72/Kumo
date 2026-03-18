package net.kumo.kumo.domain.dto.projection;

import net.kumo.kumo.domain.enums.JobStatus;
import org.springframework.beans.factory.annotation.Value;

/**
 * 구인 공고 요약 정보를 조회하기 위한 JPA Projection 인터페이스입니다.
 */
public interface JobSummaryView {

    Long getId();
    String getImgUrls();

    /**
     * 공고 작성자(구인자)의 식별자를 반환합니다.
     */
    Long getUserId();

    /**
     * 공고 엔티티와 연관된 작성자의 닉네임을 반환합니다.
     *
     * @return 작성자 닉네임 (연관된 유저가 없을 경우 null 반환)
     */
    @Value("#{target.user != null ? target.user.nickname : null}")
    String getManagerName();

    /**
     * 구인 공고의 현재 모집 상태를 반환합니다.
     */
    JobStatus getStatus();

    String getTitle();
    String getCompanyName();
    String getAddress();
    String getContactPhone();
    String getWage();
    String getWriteTime();

    String getTitleJp();
    String getCompanyNameJp();
    String getWageJp();

    Double getLat();
    Double getLng();

    /**
     * 다중 이미지 URL 문자열 중 첫 번째 이미지를 썸네일 URL로 추출하여 반환합니다.
     *
     * @return 썸네일 이미지 URL 문자열
     */
    default String getThumbnailUrl() {
        String urls = getImgUrls();
        if (urls == null || urls.isBlank()) {
            return null;
        }
        return urls.split(",")[0].trim();
    }
}