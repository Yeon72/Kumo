package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 플랫폼에 가입한 모든 사용자(구직자, 구인자, 관리자)의 핵심 식별 정보,
 * 개인 신상, 접근 권한 및 연관된 하위 엔티티 내역을 통합 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    /** 로그인 시 식별자로 사용되는 고유 이메일 */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(length = 50)
    private String nickname;

    /** 플랫폼 내 사용자 권한 (SEEKER, RECRUITER, ADMIN) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enum.UserRole role;

    @Column(name = "name_kanji_sei", nullable = false, length = 50)
    private String nameKanjiSei;

    @Column(name = "name_kanji_mei", nullable = false, length = 50)
    private String nameKanjiMei;

    @Column(name = "name_kana_sei", nullable = false, length = 50)
    private String nameKanaSei;

    @Column(name = "name_kana_mei", nullable = false, length = 50)
    private String nameKanaMei;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    /** 성별 (MALE, FEMALE, OTHER) */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Enum.Gender gender;

    @Column(length = 20, unique = true)
    private String contact;

    /** 사용자 프로필 이미지 매핑 (순환 참조 방지 적용) */
    @ToString.Exclude
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private ProfileImageEntity profileImage;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "address_main", length = 255)
    private String addressMain;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "addr_prefecture", length = 50)
    private String addrPrefecture;

    @Column(name = "addr_city", length = 50)
    private String addrCity;

    @Column(name = "addr_town", length = 50)
    private String addrTown;

    @Column(columnDefinition = "DECIMAL(10, 8)")
    private Double latitude;

    @Column(columnDefinition = "DECIMAL(11, 8)")
    private Double longitude;

    /** 사용자 가입 경로 등 메타 데이터 */
    @Column(name = "join_path", length = 50)
    private String joinPath;

    /** 마케팅 및 광고 정보 수신 동의 여부 */
    @Column(name = "ad_receive")
    @Builder.Default
    private boolean adReceive = false;

    /** 계정 활성화 여부 (정지/탈퇴 처리 시 false) */
    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    /** 소셜 로그인 제공자 정보 (Google, Line 등) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Enum.SocialProvider socialProvider;

    /** 소셜 연동 고유 식별자 */
    @Column(name = "social_id", length = 100, nullable = true)
    private String socialId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 계정 보안을 위한 로그인 연속 실패 횟수 (성공 시 초기화됨) */
    @Column(name = "login_fail_count", nullable = false)
    @Builder.Default
    private int loginFailCount = 0;

    /** 마지막 로그인 실패 발생 일시 */
    @Column(name = "last_fail_at")
    private LocalDateTime lastFailAt;

    /**
     * 로그인 실패 시 실패 횟수를 증가시키고 발생 시간을 갱신합니다.
     */
    public void increaseFailCount() {
        this.loginFailCount++;
        this.lastFailAt = LocalDateTime.now();
    }

    /**
     * 로그인 성공 시 보안 관련 실패 횟수와 타임스탬프를 초기화합니다.
     */
    public void resetFailCount() {
        this.loginFailCount = 0;
        this.lastFailAt = null;
    }

    /** 구인자(Recruiter) 계정에 등록된 사업장(회사) 목록 매핑 */
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompanyEntity> companies = new ArrayList<>();

    /** 사용자 계정에 등록된 파일/증빙 서류 목록 매핑 (순환 참조 방지 적용) */
    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvidenceFileEntity> evidenceFiles = new ArrayList<>();
}