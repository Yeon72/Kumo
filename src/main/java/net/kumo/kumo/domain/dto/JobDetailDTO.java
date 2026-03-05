package net.kumo.kumo.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kumo.kumo.domain.entity.*;

@Getter
@NoArgsConstructor
public class JobDetailDTO {
    private Long id;
    private String source; // 출처 (OSAKA, TOKYO 등)
    private String title;
    private String companyName;
    private String address;
    private String wage;
    private String contactPhone;

    private String position; // 업무
    private String jobDescription; // 업무 상세 요약
    private String body; // 상세 정보 (본문 전체)

    private String imgUrls; // 이미지
    private Double lat; // 지도용 위도
    private Double lng; // 지도용 경도

    // ★ [추가] 사장님(구인자)의 고유 ID를 담을 그릇!
    private Long userId;

    /**
     * 엔티티와 언어 설정(lang)을 받아 DTO를 생성합니다.
     */
    public JobDetailDTO(BaseEntity entity, String lang, String source) {
        // 1. 공통 데이터 매핑 (언어 무관)
        this.id = entity.getId();
        this.source = source;
        this.contactPhone = entity.getContactPhone();
        this.imgUrls = entity.getImgUrls();
        this.address = entity.getAddress();

        // 2. 언어 감지 ("ja"인 경우에만 true)
        boolean isJp = "ja".equalsIgnoreCase(lang);

        // 3. 언어별 데이터 매핑
        this.title = resolveText(isJp, entity.getTitleJp(), entity.getTitle());
        this.companyName = resolveText(isJp, entity.getCompanyNameJp(), entity.getCompanyName());
        this.wage = resolveText(isJp, entity.getWageJp(), entity.getWage());
        this.position = resolveText(isJp, entity.getPositionJp(), entity.getPosition());

        // 4. 상세 내용 (Body) 처리 로직
        String desc = resolveText(isJp, entity.getJobDescriptionJp(), entity.getJobDescription());
        String notes = resolveText(isJp, entity.getNotesJp(), entity.getNotes());

        if (hasText(desc)) {
            this.body = desc;
        } else if (hasText(notes)) {
            this.body = notes;
        } else {
            this.body = entity.getBody(); // 최후의 수단
        }

        this.jobDescription = this.body;

        // 5. ★ [수정됨] 좌표 데이터 추출 및 사장님 ID(userId) 안전하게 추출!
        if (entity instanceof OsakaGeocodedEntity) {
            OsakaGeocodedEntity osaka = (OsakaGeocodedEntity) entity;
            this.lat = osaka.getLat();
            this.lng = osaka.getLng();
            // 자식 주머니에서 사장님 번호 꺼내기
            if (osaka.getUser() != null) {
                this.userId = osaka.getUser().getUserId();
            }
        } else if (entity instanceof TokyoGeocodedEntity) {
            TokyoGeocodedEntity tokyo = (TokyoGeocodedEntity) entity;
            this.lat = tokyo.getLat();
            this.lng = tokyo.getLng();
            // 자식 주머니에서 사장님 번호 꺼내기
            if (tokyo.getUser() != null) {
                this.userId = tokyo.getUser().getUserId();
            }
        } else {
            // NoGeocoded 테이블은 좌표 없음
            this.lat = null;
            this.lng = null;
        }
    }

    /**
     * 언어 설정과 데이터 존재 여부에 따라 적절한 텍스트를 반환합니다.
     */
    private String resolveText(boolean isJp, String jpText, String krText) {
        if (isJp && hasText(jpText)) {
            return jpText;
        }
        return krText;
    }

    /**
     * 문자열이 null이 아니고 공백이 아닌지 확인합니다.
     */
    private boolean hasText(String str) {
        return str != null && !str.isBlank();
    }
}