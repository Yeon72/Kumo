package net.kumo.kumo.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kumo.kumo.domain.entity.BaseEntity;
import net.kumo.kumo.domain.entity.OsakaGeocodedEntity;
import net.kumo.kumo.domain.entity.TokyoGeocodedEntity;

/**
 * 구인 공고의 상세 정보를 클라이언트(프론트엔드)에 전달하기 위한 DTO 클래스입니다.
 * 다국어 처리 및 지리적 좌표(Geocoding), 작성자 정보 매핑 로직을 포함합니다.
 */
@Getter
@NoArgsConstructor
public class JobDetailDTO {
    private Long id;

    /** 데이터 출처 (OSAKA, TOKYO 등) */
    private String source;
    private String title;
    private String companyName;
    private String address;
    private String wage;
    private String wageJp;
    private String contactPhone;

    /** 담당 업무명 */
    private String position;

    /** 업무 상세 요약 */
    private String jobDescription;

    /** 상세 정보 (본문 전체) */
    private String body;

    /** 기타 추가 참고 사항 */
    private String notes;

    /** 등록된 다중 이미지 URL 목록 (콤마 분리) */
    private String imgUrls;

    /** 지도 마커용 위도 */
    private Double lat;

    /** 지도 마커용 경도 */
    private Double lng;

    /** 공고 작성자(구인자)의 고유 식별자 */
    private Long userId;

    /** 공고 작성자(구인자)의 닉네임 또는 이름 */
    private String managerName;

    /** 공고 작성자(구인자)의 프로필 이미지 URL */
    private String profileImageUrl;

    /** 공고 누적 조회수 */
    private Integer viewCount;

    /**
     * 엔티티 객체와 클라이언트의 언어 설정(lang)을 기반으로 DTO를 생성합니다.
     *
     * @param entity 구인 공고 기반 엔티티 (BaseEntity)
     * @param lang   클라이언트 언어 설정 (예: "ko", "ja")
     * @param source 데이터 출처 지역
     */
    public JobDetailDTO(BaseEntity entity, String lang, String source) {
        // 공통 데이터 매핑
        this.id = entity.getId();
        this.source = source;
        this.contactPhone = entity.getContactPhone();
        this.imgUrls = entity.getImgUrls();
        this.address = entity.getAddress();

        // 클라이언트 언어 감지 ("ja"인 경우 true)
        boolean isJp = "ja".equalsIgnoreCase(lang);

        // 다국어 데이터 매핑
        this.title = resolveText(isJp, entity.getTitleJp(), entity.getTitle());
        this.companyName = resolveText(isJp, entity.getCompanyNameJp(), entity.getCompanyName());
        this.wage = resolveText(isJp, entity.getWageJp(), entity.getWage());
        this.position = resolveText(isJp, entity.getPositionJp(), entity.getPosition());

        // 상세 내용 및 추가 정보 매핑
        this.jobDescription = resolveText(isJp, entity.getJobDescriptionJp(), entity.getJobDescription());
        this.body = entity.getBody();
        this.notes = resolveText(isJp, entity.getNotesJp(), entity.getNotes());
        this.viewCount = entity.getViewCount();

        // 하위 엔티티 타입 검사 및 좌표, 작성자 정보 매핑
        if (entity instanceof OsakaGeocodedEntity) {
            OsakaGeocodedEntity osaka = (OsakaGeocodedEntity) entity;
            this.lat = osaka.getLat();
            this.lng = osaka.getLng();

            if (osaka.getUser() != null) {
                this.userId = osaka.getUser().getUserId();
                this.managerName = osaka.getUser().getNickname();
                if (osaka.getUser().getProfileImage() != null) {
                    this.profileImageUrl = osaka.getUser().getProfileImage().getFileUrl();
                }
            }
        } else if (entity instanceof TokyoGeocodedEntity) {
            TokyoGeocodedEntity tokyo = (TokyoGeocodedEntity) entity;
            this.lat = tokyo.getLat();
            this.lng = tokyo.getLng();

            if (tokyo.getUser() != null) {
                this.userId = tokyo.getUser().getUserId();
                this.managerName = tokyo.getUser().getNickname();
                if (tokyo.getUser().getProfileImage() != null) {
                    this.profileImageUrl = tokyo.getUser().getProfileImage().getFileUrl();
                }
            }
        } else {
            // 위치 좌표 정보가 없는 엔티티의 경우 null 처리
            this.lat = null;
            this.lng = null;
        }
    }

    /**
     * 클라이언트 언어 설정과 데이터 존재 여부에 따라 적절한 다국어 텍스트를 반환합니다.
     *
     * @param isJp   일본어 환경 여부
     * @param jpText 매핑된 일본어 텍스트
     * @param krText 매핑된 한국어 텍스트
     * @return 언어 환경에 맞게 선택된 텍스트
     */
    private String resolveText(boolean isJp, String jpText, String krText) {
        if (isJp && hasText(jpText)) {
            return jpText;
        }
        return krText;
    }

    /**
     * 문자열의 null 여부 및 공백 여부를 검증합니다.
     *
     * @param str 검증할 문자열
     * @return 유효한 텍스트가 존재하면 true
     */
    private boolean hasText(String str) {
        return str != null && !str.isBlank();
    }
}