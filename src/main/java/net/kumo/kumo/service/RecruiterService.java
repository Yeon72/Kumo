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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RecruiterService {

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final SeekerProfileRepository seekerProfileRepo;
    private final SeekerService seekerService;
    private final ScoutOfferRepository scoutOfferRepo; // 🌟 추가
    private final NotificationRepository notificationRepo; // 🌟 추가
    private final OsakaGeocodedRepository osakaGeocodedRepository;
    private final TokyoGeocodedRepository tokyoGeocodedRepository;
    private final ApplicationRepository applicationRepository;
    private final MessageSource messageSource;

    /**
     * 구인자 대시보드 통계 데이터 가져오기
     */
    public net.kumo.kumo.domain.dto.RecruiterDashboardDTO getDashboardStats(String email) {
        // 1. 공고 정보 가져오기
        List<OsakaGeocodedEntity> osakaJobs = osakaGeocodedRepository.findByUser_Email(email);
        List<TokyoGeocodedEntity> tokyoJobs = tokyoGeocodedRepository.findByUser_Email(email);

        long totalJobs = osakaJobs.size() + tokyoJobs.size();
        long totalVisits = osakaJobs.stream().mapToLong(j -> j.getViewCount() != null ? j.getViewCount() : 0).sum()
                + tokyoJobs.stream().mapToLong(j -> j.getViewCount() != null ? j.getViewCount() : 0).sum();

        // 2. 지원자 정보 조회를 위한 ID 리스트 생성
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

        // 3. 차트용 데이터 (최근 7일)
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
     * 인재에게 스카우트 제의 보내기
     */
    @Transactional
    public void sendScoutOffer(String recruiterEmail, Long seekerId) {
        // 🌟 2. 현재 요청을 보낸 사용자의 언어(Locale) 정보 가져오기
        Locale currentLocale = LocaleContextHolder.getLocale();

        UserEntity recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("error.recruiter.notfound", null, currentLocale)));

        UserEntity seeker = userRepository.findById(seekerId)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("error.seeker.notfound", null, currentLocale)));

        // 이미 대기 중인 제안이 있는지 확인 (중복 제안 방지)
        if (scoutOfferRepo.existsByRecruiterAndSeekerAndStatus(recruiter, seeker,
                ScoutOfferEntity.ScoutStatus.PENDING)) {
            throw new RuntimeException(
                    messageSource.getMessage("error.scout.duplicate", null, currentLocale));
        }

        // 1. 스카우트 제안 저장
        ScoutOfferEntity offer = ScoutOfferEntity.builder()
                .recruiter(recruiter)
                .seeker(seeker)
                .status(ScoutOfferEntity.ScoutStatus.PENDING)
                .build();
        scoutOfferRepo.save(offer);

        // 2. 구직자에게 알림 생성 (기존 로직 완벽합니다!)
        NotificationEntity noti = NotificationEntity.builder()
                .user(seeker)
                .notifyType(NotificationType.SCOUT_OFFER)
                .title("noti.scout.title")
                .content(recruiter.getNickname()) // 🌟 번역 시 인자로 사용될 사장님 닉네임
                .targetUrl("/Seeker/scout")
                .isRead(false)
                .build();
        notificationRepo.save(noti);
    }

    /**
     * 스카우트 동의 & 이력서 공개한 인재 목록 불러오기
     * 
     * @return
     */
    public List<SeekerProfileEntity> getScoutedProfiles() {
        return seekerProfileRepo.findByScoutAgreeTrueAndIsPublicTrue();
    }

    /**
     * 특정 인재의 이력서 상세 정보 가져오기
     * 
     * @param userId
     * @return
     */
    public ResumeDto getTalentResume(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("인재 정보를 찾을 수 없습니다."));
        return seekerService.getResume(user.getEmail());
    }

    public UserEntity getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 유저의 프로필 이미지 경로를 업데이트합니다.
     * 
     * @param email            유저 식별용 이메일
     * @param imagePath        저장된 이미지의 웹 접근 경로
     * @param originalFileName 원본 파일명
     * @param storedFileName   UUID가 붙은 저장 파일명
     * @param fileSize         파일 크기
     */
    @org.springframework.transaction.annotation.Transactional
    public void updateProfileImage(String email, String imagePath, String originalFileName, String storedFileName,
            Long fileSize) {

        // 1. 이메일로 유저 정보를 가져옵니다.
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일을 가진 유저를 찾을 수 없습니다: " + email));

        // 🌟 2. 유저가 이미 가지고 있는 프로필 사진이 있는지 꺼내봅니다.
        ProfileImageEntity existingImage = user.getProfileImage();

        if (existingImage != null) {
            // 🟢 [Case A] 기존 프사가 있는 경우 -> 새로운 정보로 내용물만 덮어쓰기 (UPDATE)
            existingImage.setFileUrl(imagePath);
            existingImage.setOriginalFileName(originalFileName);
            existingImage.setStoredFileName(storedFileName);
            existingImage.setFileSize(fileSize);
        } else {
            // 🔵 [Case B] 기존 프사가 아예 없는 경우 -> 새로 만들어서 연결해주기 (INSERT)
            ProfileImageEntity newImage = ProfileImageEntity.builder()
                    .fileUrl(imagePath)
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .fileSize(fileSize)
                    .user(user)
                    .build();
            user.setProfileImage(newImage);
        }

        // 3. 변경 사항을 저장합니다. (JPA의 더티 체킹 덕분에 알아서 UPDATE나 INSERT 쿼리가 나갑니다!)
        userRepository.save(user);
    }

    /**
     * 회원정보 수정
     * 
     * @param dto
     */
    public void updateProfile(JoinRecruiterDTO dto) {
        UserEntity user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("해당 이메일을 가진 유저를 찾을 수 없습니다: " + dto.getEmail()));

        // 2. 새 객체를 만들지 말고, 기존 객체의 알맹이(필드)만 쏙쏙 바꿔 입힙니다!
        // (UserEntity 클래스에 @Setter 나 수정용 메서드가 있어야 합니다.)
        user.setNickname(dto.getNickname());
        user.setZipCode(dto.getZipCode());
        user.setAddressMain(dto.getAddressMain());
        user.setAddressDetail(dto.getAddressDetail());
        user.setAddrPrefecture(dto.getAddrPrefecture());
        user.setAddrCity(dto.getAddrCity());
        user.setAddrTown(dto.getAddrTown());
        user.setLatitude(dto.getLatitude());
        user.setLongitude(dto.getLongitude());

        // 🌟 [최종 검문소] DB에 저장되기 직전, user 객체에 위도/경도가 잘 꽂혀있는지 확인!
        log.info("👉 DB 저장 직전 Entity 상태: 위도={}, 경도={}", user.getLatitude(), user.getLongitude());
    }

    public long getUnreadCount(String email) {
        long totalUnread = 0;

        // 1. 도쿄 공고 ID들만 숫자 리스트로 추출
        List<TokyoGeocodedEntity> tokyoEntities = tokyoGeocodedRepository.findByUser_Email(email);
        if (tokyoEntities != null && !tokyoEntities.isEmpty()) {
            // 🌟 엔티티 봉지에서 ID(Long)만 꺼내서 리스트로 만듭니다.
            List<Long> tokyoIds = tokyoEntities.stream()
                    .map(TokyoGeocodedEntity::getDatanum) // 사장님 엔티티의 ID 필드명 확인!
                    .toList();
            totalUnread += applicationRepository.countUnreadBySourceAndPostIds("TOKYO", tokyoIds);
        }

        // 2. 오사카 공고 ID들만 숫자 리스트로 추출
        List<OsakaGeocodedEntity> osakaEntities = osakaGeocodedRepository.findByUser_Email(email);
        if (osakaEntities != null && !osakaEntities.isEmpty()) {
            // 🌟 여기도 똑같이 ID만 추출!
            List<Long> osakaIds = osakaEntities.stream()
                    .map(OsakaGeocodedEntity::getDatanum) // 필드명이 다르면 수정해주세요.
                    .toList();
            totalUnread += applicationRepository.countUnreadBySourceAndPostIds("OSAKA", osakaIds);
        }

        return totalUnread;
    }

}
