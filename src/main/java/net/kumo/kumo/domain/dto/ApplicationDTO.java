package net.kumo.kumo.domain.dto;

import lombok.*;
import net.kumo.kumo.domain.entity.ApplicationEntity;
import net.kumo.kumo.domain.entity.Enum.ApplicationStatus;

import java.time.format.DateTimeFormatter;

// 파일은 하나지만, 용도별로 쪼개진 완벽한 DTO 세트!
public class ApplicationDTO {

    // ==========================================
    // 1. [요청] 구직자가 지원할 때 사용
    // ==========================================
    @Getter @Setter
    public static class ApplyRequest {
        private Long targetPostId;
        private String targetSource;
    }

    // ==========================================
    // 🌟 2. [응답] 구인자가 지원자 목록을 볼 때 사용
    // ==========================================
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class ApplicantResponse {

        // [1] 지원서 상태 관리용 핵심 식별자
        private Long appId;
        private ApplicationStatus status;
        private String appliedAt;

        // [2] 구직자(UserEntity) 정보 (연관관계를 통해 즉시 추출!)
        private Long seekerId;
        private String seekerName;
        private String seekerEmail;
        private String seekerContact;

        // [3] 어떤 공고인지 알려주는 정보
        private String targetSource;
        private Long targetPostId;
        private String jobTitle; // 서비스(Service) 단에서 DB 조회 후 예쁘게 채워줄 제목

        public static ApplicantResponse from(ApplicationEntity entity, String fetchedJobTitle) {

            // 이름 조합 로직 (유저 엔티티 구조에 맞게 커스텀. 한자 성+이름이 없다면 닉네임 사용 등)
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
                    // 날짜를 프론트에서 보기 편한 문자열로 변환
                    .appliedAt(entity.getAppliedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))

                    // UserEntity와 연결되어 있으므로 getSeeker()로 한 방에 정보 추출!
                    .seekerId(entity.getSeeker().getUserId())
                    .seekerName(fullName)
                    .seekerEmail(entity.getSeeker().getEmail())
                    .seekerContact(entity.getSeeker().getContact())

                    // 공고 정보
                    .targetSource(entity.getTargetSource())
                    .targetPostId(entity.getTargetPostId())
                    .jobTitle(fetchedJobTitle)
                    .build();
        }
    }
}