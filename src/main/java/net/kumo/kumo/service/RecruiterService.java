package net.kumo.kumo.service;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.JoinRecruiterDTO;
import net.kumo.kumo.domain.dto.ResumeDto;
import net.kumo.kumo.domain.entity.ApplicationEntity;
import net.kumo.kumo.domain.entity.Enum;
import net.kumo.kumo.domain.entity.Enum.NotificationType;
import net.kumo.kumo.domain.entity.NotificationEntity;
import net.kumo.kumo.domain.entity.OsakaGeocodedEntity;
import net.kumo.kumo.domain.entity.ProfileImageEntity;
import net.kumo.kumo.domain.entity.ScoutOfferEntity;
import net.kumo.kumo.domain.entity.SeekerProfileEntity;
import net.kumo.kumo.domain.entity.TokyoGeocodedEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.ApplicationRepository;
import net.kumo.kumo.repository.NotificationRepository;
import net.kumo.kumo.repository.OsakaGeocodedRepository;
import net.kumo.kumo.repository.ScheduleRepository;
import net.kumo.kumo.repository.ScoutOfferRepository;
import net.kumo.kumo.repository.SeekerProfileRepository;
import net.kumo.kumo.repository.TokyoGeocodedRepository;
import net.kumo.kumo.repository.UserRepository;

/**
 * 구인자(Recruiter) 전용 대시보드 통계 집계, 스카우트 제의 발송,
 * 이력서 열람 및 프로필 정보 갱신과 같은 핵심 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RecruiterService {

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final SeekerProfileRepository seekerProfileRepo;
    private final SeekerService seekerService;
    private final ScoutOfferRepository scoutOfferRepo;
    private final NotificationRepository notificationRepo;
    private final OsakaGeocodedRepository osakaGeocodedRepository;
    private final TokyoGeocodedRepository tokyoGeocodedRepository;
    private final ApplicationRepository applicationRepository;
    private final MessageSource messageSource;

    /**
     * 구인자의 메인 대시보드 렌더링에 필요한 통계 데이터(등록된 총 공고 수, 누적 지원자 수,
     * 미열람 지원자 수, 주간 지원자 변동 차트 등)를 집계하여 반환합니다.
     *
     * @param email 통계를 조회할 구인자 계정(이메일)
     * @return 대시보드 통계 데이터 DTO
     */
    public net.kumo.kumo.domain.dto.RecruiterDashboardDTO getDashboardStats(String email) {
        List<OsakaGeocodedEntity> osakaJobs = osakaGeocodedRepository.findByUser_Email(email);
        List<TokyoGeocodedEntity> tokyoJobs = tokyoGeocodedRepository.findByUser_Email(email);

        long totalJobs = osakaJobs.size() + tokyoJobs.size();
        long totalVisits = osakaJobs.stream().mapToLong(j -> j.getViewCount() != null ? j.getViewCount() : 0).sum()
                + tokyoJobs.stream().mapToLong(j -> j.getViewCount() != null ? j.getViewCount() : 0).sum();

        List<Long> osakaIds = osakaJobs.stream().map(OsakaGeocodedEntity::getId).toList();
        List<Long> tokyoIds = tokyoJobs.stream().map(TokyoGeocodedEntity::getId).toList();

        List<ApplicationEntity> allApps = new java.util.ArrayList<>();
        if (!osakaIds.isEmpty()) {
            allApps.addAll(applicationRepository.findByTargetSourceAndTargetPostIdIn("OSAKA", osakaIds));
        }
        if (!tokyoIds.isEmpty()) {
            allApps.addAll(applicationRepository.findByTargetSourceAndTargetPostIdIn("TOKYO", tokyoIds));
        }

        long totalApplicants = allApps.size();
        long unreadApplicants = allApps.stream()
                .filter(a -> a.getStatus() == Enum.ApplicationStatus.APPLIED)
                .count();

        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.List<String> labels = new java.util.ArrayList<>();
        java.util.List<Long> data = new java.util.ArrayList<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MM.dd");

        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate date = today.minusDays(i);
            labels.add(date.format(formatter));

            long count = allApps.stream()
                    .filter(a -> a.getAppliedAt().toLocalDate().equals(date))
                    .count();
            data.add(count);
        }

        return net.kumo.kumo.domain.dto.RecruiterDashboardDTO.builder()
                .totalApplicants(totalApplicants)
                .unreadApplicants(unreadApplicants)
                .totalJobs(totalJobs)
                .totalVisits(totalVisits)
                .chartLabels(labels)
                .chartData(data)
                .build();
    }

    /**
     * 특정 구직자(인재)에게 스카우트 제의를 발송하고, 해당 구직자에게 시스템 수신 알림을 생성합니다.
     * 중복 제안 방지 로직과 다국어 에러 메시지 처리가 포함되어 있습니다.
     *
     * @param recruiterEmail 스카우트 제의를 발송하는 구인자 계정
     * @param seekerId       제의를 수신할 대상 구직자의 식별자
     * @throws RuntimeException 대상 사용자를 찾을 수 없거나, 이미 진행 중인 스카우트 제의가 존재할 경우 발생
     */
    @Transactional
    public void sendScoutOffer(String recruiterEmail, Long seekerId) {
        Locale currentLocale = LocaleContextHolder.getLocale();

        UserEntity recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("error.recruiter.notfound", null, currentLocale)));

        UserEntity seeker = userRepository.findById(seekerId)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("error.seeker.notfound", null, currentLocale)));

        if (scoutOfferRepo.existsByRecruiterAndSeekerAndStatus(recruiter, seeker,
                ScoutOfferEntity.ScoutStatus.PENDING)) {
            throw new RuntimeException(
                    messageSource.getMessage("error.scout.duplicate", null, currentLocale));
        }

        ScoutOfferEntity offer = ScoutOfferEntity.builder()
                .recruiter(recruiter)
                .seeker(seeker)
                .status(ScoutOfferEntity.ScoutStatus.PENDING)
                .build();
        scoutOfferRepo.save(offer);

        NotificationEntity noti = NotificationEntity.builder()
                .user(seeker)
                .notifyType(NotificationType.SCOUT_OFFER)
                .title("noti.scout.title")
                .content(recruiter.getNickname())
                .targetUrl("/Seeker/scout")
                .isRead(false)
                .build();
        notificationRepo.save(noti);
    }

    /**
     * 인재 탐색 페이지 렌더링을 위해, 스카우트 제의 수신에 동의하고 이력서를 공개 상태로 설정한
     * 전체 구직자 프로필 목록을 조회합니다.
     *
     * @return 공개 상태인 구직자 프로필 엔티티 리스트
     */
    public List<SeekerProfileEntity> getScoutedProfiles() {
        return seekerProfileRepo.findByScoutAgreeTrueAndIsPublicTrue();
    }

    /**
     * 인재 탐색 후 열람을 위한 특정 구직자의 전체 이력서 상세 정보를 조회하여 반환합니다.
     *
     * @param userId 조회 대상 구직자의 식별자
     * @return 통합 이력서 데이터 DTO
     * @throws RuntimeException 대상 구직자를 찾을 수 없을 때 발생
     */
    public ResumeDto getTalentResume(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("인재 정보를 찾을 수 없습니다."));
        return seekerService.getResume(user.getEmail());
    }

    /**
     * 주어진 이메일 계정을 기반으로 구인자(본인)의 엔티티 정보를 조회합니다.
     *
     * @param email 조회를 요청한 사용자의 이메일 계정
     * @return 사용자 엔티티
     * @throws RuntimeException 사용자를 찾을 수 없을 때 발생
     */
    public UserEntity getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 구인자의 프로필 이미지 정보 및 스토리지 경로를 갱신합니다.
     * 기존 등록된 이미지가 존재할 경우 Update 로직을, 존재하지 않을 경우 Insert 로직을 분기하여 처리합니다.
     *
     * @param email            변경 대상 구인자의 이메일 계정
     * @param imagePath        이미지 렌더링을 위한 웹 접근 경로 URL
     * @param originalFileName 클라이언트로부터 업로드된 원본 파일명
     * @param storedFileName   서버 스토리지에 중복 방지를 위해 생성된 UUID 파일명
     * @param fileSize         파일의 물리적 크기
     */
    @org.springframework.transaction.annotation.Transactional
    public void updateProfileImage(String email, String imagePath, String originalFileName, String storedFileName,
                                   Long fileSize) {

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일을 가진 유저를 찾을 수 없습니다: " + email));

        ProfileImageEntity existingImage = user.getProfileImage();

        if (existingImage != null) {
            existingImage.setFileUrl(imagePath);
            existingImage.setOriginalFileName(originalFileName);
            existingImage.setStoredFileName(storedFileName);
            existingImage.setFileSize(fileSize);
        } else {
            ProfileImageEntity newImage = ProfileImageEntity.builder()
                    .fileUrl(imagePath)
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .fileSize(fileSize)
                    .user(user)
                    .build();
            user.setProfileImage(newImage);
        }

        userRepository.save(user);
    }

    /**
     * 구인자의 마이페이지 설정에 따라 기본 계정 및 연락처/위치 정보를 갱신합니다.
     *
     * @param dto 갱신할 내용이 담긴 폼 DTO
     * @throws RuntimeException 갱신 대상 사용자를 찾을 수 없을 때 발생
     */
    public void updateProfile(JoinRecruiterDTO dto) {
        UserEntity user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("해당 이메일을 가진 유저를 찾을 수 없습니다: " + dto.getEmail()));

        user.setNickname(dto.getNickname());
        user.setZipCode(dto.getZipCode());
        user.setAddressMain(dto.getAddressMain());
        user.setAddressDetail(dto.getAddressDetail());
        user.setAddrPrefecture(dto.getAddrPrefecture());
        user.setAddrCity(dto.getAddrCity());
        user.setAddrTown(dto.getAddrTown());
        user.setLatitude(dto.getLatitude());
        user.setLongitude(dto.getLongitude());

        log.info("DB 저장 직전 Entity 상태: 위도={}, 경도={}", user.getLatitude(), user.getLongitude());
    }

    /**
     * 글로벌 네비게이션 헤더 뱃지 등에 표시하기 위해, 특정 구인자가 등록한 전체 공고에
     * 새롭게 접수된 미확인(APPLIED) 신규 지원자의 총합을 계산합니다.
     *
     * @param email 조회를 요청한 구인자 계정
     * @return 미확인 지원자의 총 개수
     */
    public long getUnreadCount(String email) {
        long totalUnread = 0;

        List<TokyoGeocodedEntity> tokyoEntities = tokyoGeocodedRepository.findByUser_Email(email);
        if (tokyoEntities != null && !tokyoEntities.isEmpty()) {
            List<Long> tokyoIds = tokyoEntities.stream()
                    .map(TokyoGeocodedEntity::getDatanum)
                    .toList();
            totalUnread += applicationRepository.countUnreadBySourceAndPostIds("TOKYO", tokyoIds);
        }

        List<OsakaGeocodedEntity> osakaEntities = osakaGeocodedRepository.findByUser_Email(email);
        if (osakaEntities != null && !osakaEntities.isEmpty()) {
            List<Long> osakaIds = osakaEntities.stream()
                    .map(OsakaGeocodedEntity::getDatanum)
                    .toList();
            totalUnread += applicationRepository.countUnreadBySourceAndPostIds("OSAKA", osakaIds);
        }

        return totalUnread;
    }
}