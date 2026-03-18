package net.kumo.kumo.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kumo.kumo.domain.dto.projection.JobSummaryView;
import net.kumo.kumo.domain.entity.BaseEntity;
import net.kumo.kumo.domain.entity.OsakaGeocodedEntity;
import net.kumo.kumo.domain.entity.TokyoGeocodedEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 구인 공고의 요약 정보(목록 조회용)를 프론트엔드로 전달하기 위한 DTO 클래스입니다.
 * DB Projection(View) 데이터 및 BaseEntity 데이터를 다국어 포맷으로 변환하여 매핑합니다.
 */
@Getter
@NoArgsConstructor
public class JobSummaryDTO {

    private Long id;
    private String source;
    private String title;
    private String status;

    private String position;
    private LocalDate deadline;

    private String companyName;
    private String address;
    private String wage;
    private String contactPhone;
    private String thumbnailUrl;
    private String writeTime;
    private LocalDateTime createdAt;
    private Double lat;
    private Double lng;

    /** 공고 작성자 식별자 */
    private Long userId;

    /** 공고 작성자 닉네임 또는 이름 */
    private String managerName;

    /**
     * DB Projection 결과(JobSummaryView)를 기반으로 DTO를 생성합니다.
     * 주로 지도 바텀 시트 요약 정보 제공용으로 사용됩니다.
     *
     * @param view   JPA Projection 조회 결과
     * @param lang   클라이언트 언어 설정
     * @param source 데이터 출처 지역
     */
    public JobSummaryDTO(JobSummaryView view, String lang, String source) {
        this.id = view.getId();
        this.source = source;
        this.thumbnailUrl = view.getThumbnailUrl();
        this.contactPhone = view.getContactPhone();
        this.address = view.getAddress();
        this.writeTime = view.getWriteTime();
        this.lat = view.getLat();
        this.lng = view.getLng();
        this.deadline = null; // 크롤링 데이터는 모집 마감일이 존재하지 않음 처리

        this.userId = view.getUserId();
        this.managerName = view.getManagerName();

        boolean isJp = "ja".equalsIgnoreCase(lang);
        this.title = (isJp && hasText(view.getTitleJp())) ? view.getTitleJp() : view.getTitle();
        this.companyName = (isJp && hasText(view.getCompanyNameJp())) ? view.getCompanyNameJp() : view.getCompanyName();
        this.wage = (isJp && hasText(view.getWageJp())) ? view.getWageJp() : view.getWage();

        this.position = view.getCompanyName();
    }

    /**
     * 엔티티(BaseEntity) 객체를 기반으로 DTO를 생성합니다.
     * 주로 일반 공고 리스트 반환용으로 사용됩니다.
     *
     * @param entity 공고 기본 엔티티
     * @param lang   클라이언트 언어 설정
     * @param source 데이터 출처 지역
     */
    public JobSummaryDTO(BaseEntity entity, String lang, String source) {
        this.id = entity.getId();
        this.source = source;
        this.thumbnailUrl = entity.getImgUrls();
        this.contactPhone = entity.getContactPhone();
        this.address = entity.getAddress();
        this.userId = entity.getUserId();

        if (entity.getStatus() != null) {
            this.status = entity.getStatus().name();
        } else {
            this.status = "RECRUITING"; // 상태 정보 누락 시 기본값 적용
        }

        if (entity.getWriteTime() != null) {
            this.writeTime = entity.getWriteTime();
        } else if (entity.getCreatedAt() != null) {
            this.writeTime = entity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        this.createdAt = entity.getCreatedAt();
        this.deadline = null;

        if (entity instanceof OsakaGeocodedEntity osaka) {
            this.lat = osaka.getLat();
            this.lng = osaka.getLng();
            if (osaka.getUser() != null) {
                this.managerName = osaka.getUser().getNickname();
            }
        } else if (entity instanceof TokyoGeocodedEntity tokyo) {
            this.lat = tokyo.getLat();
            this.lng = tokyo.getLng();
            if (tokyo.getUser() != null) {
                this.managerName = tokyo.getUser().getNickname();
            }
        } else {
            this.lat = null;
            this.lng = null;
        }

        boolean isJp = "ja".equalsIgnoreCase(lang);
        this.title = (isJp && hasText(entity.getTitleJp())) ? entity.getTitleJp() : entity.getTitle();
        this.companyName = (isJp && hasText(entity.getCompanyNameJp())) ? entity.getCompanyNameJp() : entity.getCompanyName();
        this.wage = (isJp && hasText(entity.getWageJp())) ? entity.getWageJp() : entity.getWage();
        this.position = (isJp && hasText(entity.getPositionJp())) ? entity.getPositionJp() : entity.getPosition();
    }

    private boolean hasText(String str) {
        return str != null && !str.isBlank();
    }
}