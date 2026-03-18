package net.kumo.kumo.domain.dto;

import lombok.*;
import net.kumo.kumo.domain.entity.ApplicationEntity;
import net.kumo.kumo.domain.entity.Enum.ApplicationStatus;

import java.time.format.DateTimeFormatter;

/**
 * 구인 공고 지원(Application)과 관련된 요청 및 응답 데이터를 관리하는 래퍼(Wrapper) DTO 클래스입니다.
 */
public class ApplicationDTO {

    /**
     * 구직자가 특정 공고에 지원할 때 사용하는 요청 DTO입니다.
     */
    @Getter @Setter
    public static class ApplyRequest {
        private Long targetPostId;
        private String targetSource;
    }

    /**
     * 구인자가 지원자 목록을 조회할 때 반환되는 응답 DTO입니다.
     */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class ApplicantResponse {

        private Long appId;
        private ApplicationStatus status;
        private String appliedAt;

        private Long seekerId;
        private String seekerName;
        private String seekerEmail;
        private String seekerContact;

        private String targetSource;
        private Long targetPostId;
        private String jobTitle;

        /**
         * ApplicationEntity를 기반으로 클라이언트 반환용 ApplicantResponse DTO를 생성합니다.
         * 사용자 정보(이름, 이메일 등)는 연관 관계를 통해 매핑되며, 날짜는 지정된 포맷 문자열로 변환됩니다.
         *
         * @param entity          변환할 지원 내역 엔티티
         * @param fetchedJobTitle 조회된 구인 공고의 제목
         * @return 생성된 ApplicantResponse DTO 객체
         */
        public static ApplicantResponse from(ApplicationEntity entity, String fetchedJobTitle) {

            String fullName = "";
            if (entity.getSeeker().getNameKanjiSei() != null || entity.getSeeker().getNameKanjiMei() != null) {
                fullName = (entity.getSeeker().getNameKanjiSei() != null ? entity.getSeeker().getNameKanjiSei() : "")
                        + (entity.getSeeker().getNameKanjiMei() != null ? entity.getSeeker().getNameKanjiMei() : "");
            } else {
                fullName = entity.getSeeker().getNickname();
            }

            return ApplicantResponse.builder()
                    .appId(entity.getId())
                    .status(entity.getStatus())
                    .appliedAt(entity.getAppliedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .seekerId(entity.getSeeker().getUserId())
                    .seekerName(fullName)
                    .seekerEmail(entity.getSeeker().getEmail())
                    .seekerContact(entity.getSeeker().getContact())
                    .targetSource(entity.getTargetSource())
                    .targetPostId(entity.getTargetPostId())
                    .jobTitle(fetchedJobTitle)
                    .build();
        }
    }
}