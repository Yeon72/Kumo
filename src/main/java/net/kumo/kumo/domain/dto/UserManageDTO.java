package net.kumo.kumo.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kumo.kumo.domain.entity.ProfileImageEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자(Admin) 대시보드의 회원 관리 페이지에서
 * 개별 회원의 상세 정보 및 상태를 렌더링하기 위한 DTO 클래스입니다.
 */
@Getter
@NoArgsConstructor
public class UserManageDTO {

    private Long id;
    private String email;
    private String nickname;
    private String name;

    /** 사용자 권한 (SEEKER, RECRUITER, ADMIN) */
    private String role;

    /** 사용자 활성화 상태 (ACTIVE, INACTIVE) */
    private String status;

    /** 사용자 프로필 이미지 정보 */
    private ProfileImageEntity profileImage;

    private String joinedAt;
    private String lastActive;

    /** 구인자 회원의 사업자 증빙 서류 URL 목록 */
    private List<String> evidenceUrls;

    /**
     * UserEntity를 기반으로 관리자 뷰 렌더링을 위한 UserManageDTO를 생성합니다.
     * 날짜 포맷팅 및 연관된 증빙 서류 파일의 URL 추출 로직을 포함합니다.
     *
     * @param user 매핑할 대상 회원 엔티티 객체
     */
    public UserManageDTO(UserEntity user) {
        this.id = user.getUserId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.name = user.getNameKanjiSei() + " " + user.getNameKanjiMei();

        if (user.getRole() != null) {
            this.role = user.getRole().name();
        } else {
            this.role = "SEEKER";
        }

        this.status = user.isActive() ? "ACTIVE" : "INACTIVE";

        this.profileImage = user.getProfileImage();
        if (this.profileImage == null || this.profileImage.getFileUrl() == null || this.profileImage.getFileUrl().isEmpty()) {
            this.profileImage = new ProfileImageEntity();
            this.profileImage.setFileUrl("/uploads/default_profile.png");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        if (user.getCreatedAt() != null) {
            this.joinedAt = user.getCreatedAt().format(formatter);
        } else {
            this.joinedAt = "-";
        }

        if (user.getUpdatedAt() != null) {
            this.lastActive = user.getUpdatedAt().format(formatter);
        } else {
            this.lastActive = "-";
        }

        if (user.getEvidenceFiles() != null && !user.getEvidenceFiles().isEmpty()) {
            this.evidenceUrls = user.getEvidenceFiles().stream()
                    .filter(file -> "EVIDENCE".equals(file.getFileType()))
                    .map(file -> "/images/uploadFile/" + file.getFileName())
                    .collect(Collectors.toList());
        }
    }
}