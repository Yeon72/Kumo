package net.kumo.kumo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.ApplicationDTO;
import net.kumo.kumo.domain.dto.JobDetailDTO;
import net.kumo.kumo.domain.dto.JobSummaryDTO;
import net.kumo.kumo.domain.dto.ReportDTO;
import net.kumo.kumo.domain.dto.projection.JobSummaryView;
import net.kumo.kumo.domain.entity.ApplicationEntity;
import net.kumo.kumo.domain.entity.BaseEntity;
import net.kumo.kumo.domain.entity.Enum;
import net.kumo.kumo.domain.entity.OsakaGeocodedEntity;
import net.kumo.kumo.domain.entity.ReportEntity;
import net.kumo.kumo.domain.entity.TokyoGeocodedEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.ApplicationRepository;
import net.kumo.kumo.repository.JobSearchSpec;
import net.kumo.kumo.repository.OsakaGeocodedRepository;
import net.kumo.kumo.repository.OsakaNoGeocodedRepository;
import net.kumo.kumo.repository.ReportRepository;
import net.kumo.kumo.repository.TokyoGeocodedRepository;
import net.kumo.kumo.repository.TokyoNoGeocodedRepository;
import net.kumo.kumo.repository.UserRepository;

/**
 * 지도 기반의 구인 공고 검색, 위치별 목록 조회, 상세 정보 열람 및
 * 구인 신청, 신고 기능 등 핵심 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MapService {

    private final OsakaGeocodedRepository osakaRepo;
    private final TokyoGeocodedRepository tokyoRepo;
    private final OsakaNoGeocodedRepository osakaNoRepo;
    private final TokyoNoGeocodedRepository tokyoNoRepo;

    private final ReportRepository reportRepo;
    private final UserRepository userRepo;

    private final ApplicationRepository applicationRepo;
    private final NotificationService notificationService;

    /**
     * 클라이언트의 지도 뷰포트(위경도 범위) 내에 존재하는 구인 공고 목록을 조회합니다.
     * 모집 중(RECRUITING)이거나 상태가 명시되지 않은 기존 공고만 필터링하여 반환합니다.
     *
     * @param minLat 최소 위도
     * @param maxLat 최대 위도
     * @param minLng 최소 경도
     * @param maxLng 최대 경도
     * @param lang   다국어 지원을 위한 언어 설정
     * @return 뷰포트 범위 내에 존재하는 공고 요약 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<JobSummaryDTO> getJobListInMap(Double minLat, Double maxLat, Double minLng, Double maxLng,
                                               String lang) {
        List<JobSummaryView> osakaRaw = osakaRepo.findTop300ByLatBetweenAndLngBetween(minLat, maxLat, minLng, maxLng);
        List<JobSummaryDTO> result = new ArrayList<>(osakaRaw.stream()
                .filter(view -> view.getStatus() == null || "RECRUITING".equals(view.getStatus().name()))
                .map(view -> new JobSummaryDTO(view, lang, "OSAKA"))
                .toList());

        List<JobSummaryView> tokyoRaw = tokyoRepo.findTop300ByLatBetweenAndLngBetween(minLat, maxLat, minLng, maxLng);
        result.addAll(tokyoRaw.stream()
                .filter(view -> view.getStatus() == null || "RECRUITING".equals(view.getStatus().name()))
                .map(view -> new JobSummaryDTO(view, lang, "TOKYO"))
                .toList());

        return result;
    }

    /**
     * 특정 구인 공고의 상세 정보를 조회하고, 해당 공고의 누적 조회수를 1 증가시킵니다.
     *
     * @param id     조회할 공고의 고유 식별자
     * @param source 공고의 데이터 출처 지역 (예: OSAKA, TOKYO 등)
     * @param lang   다국어 지원을 위한 언어 설정
     * @return 공고 상세 정보가 담긴 DTO
     */
    @Transactional
    public JobDetailDTO getJobDetail(Long id, String source, String lang) {
        BaseEntity entity = null;

        if ("OSAKA".equalsIgnoreCase(source)) {
            entity = osakaRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공고입니다."));
        } else if ("TOKYO".equalsIgnoreCase(source)) {
            entity = tokyoRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공고입니다."));
        } else if ("OSAKA_NO".equalsIgnoreCase(source)) {
            entity = osakaNoRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공고입니다."));
        } else if ("TOKYO_NO".equalsIgnoreCase(source)) {
            entity = tokyoNoRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공고입니다."));
        } else {
            throw new IllegalArgumentException("잘못된 접근입니다 (Source 오류).");
        }

        entity.addViewCount();
        log.info("조회수 증가 완료: {}", entity.getViewCount());

        return new JobDetailDTO(entity, lang, source);
    }

    /**
     * 특정 구인 공고에 대한 사용자의 신고 내역을 데이터베이스에 등록합니다.
     * 등록이 완료되면 해당 공고의 작성자(구인자)에게 신고 접수 안내 시스템 알림을 발송합니다.
     *
     * @param dto 신고자 및 신고 대상 정보가 담긴 DTO
     */
    @Transactional
    public void createReport(ReportDTO dto) {
        UserEntity reporter = userRepo.findById(dto.getReporterId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        ReportEntity report = ReportEntity.builder()
                .reporter(reporter)
                .targetPostId(dto.getTargetPostId())
                .targetSource(dto.getTargetSource())
                .reasonCategory(dto.getReasonCategory())
                .description(dto.getDescription())
                .status("PENDING")
                .build();

        reportRepo.save(report);

        String jobTitle = "공고";
        UserEntity recruiter = null;
        if ("OSAKA".equalsIgnoreCase(dto.getTargetSource())) {
            OsakaGeocodedEntity job = osakaRepo.findById(dto.getTargetPostId()).orElse(null);
            if (job != null) {
                recruiter = job.getUser();
                jobTitle = job.getTitle();
            }
        } else if ("TOKYO".equalsIgnoreCase(dto.getTargetSource())) {
            TokyoGeocodedEntity job = tokyoRepo.findById(dto.getTargetPostId()).orElse(null);
            if (job != null) {
                recruiter = job.getUser();
                jobTitle = job.getTitle();
            }
        }

        if (recruiter != null) {
            notificationService.sendReportNotification(
                    recruiter,
                    jobTitle,
                    dto.getTargetPostId(),
                    dto.getTargetSource());
        }
    }

    /**
     * 구직자가 특정 구인 공고에 입사 지원을 요청할 때 수행되는 비즈니스 로직입니다.
     * 중복 지원 여부를 검증한 후, 구직자 본인과 해당 공고의 작성자 양측에 각각 알림을 발송합니다.
     *
     * @param seeker 지원을 요청한 구직자 사용자 엔티티
     * @param dto    지원 대상 공고 정보가 담긴 DTO
     * @throws IllegalStateException 사용자가 해당 공고에 이미 지원한 내역이 존재할 경우 발생
     */
    @Transactional
    public void applyForJob(UserEntity seeker, ApplicationDTO.ApplyRequest dto) {
        boolean alreadyApplied = applicationRepo.existsByTargetSourceAndTargetPostIdAndSeeker(
                dto.getTargetSource(),
                dto.getTargetPostId(),
                seeker);

        if (alreadyApplied) {
            throw new IllegalStateException("이미 지원하신 공고입니다.");
        }

        ApplicationEntity application = ApplicationEntity.builder()
                .targetSource(dto.getTargetSource())
                .targetPostId(dto.getTargetPostId())
                .seeker(seeker)
                .build();

        applicationRepo.save(application);

        String jobTitle = "공고";
        UserEntity recruiter = null;
        if ("OSAKA".equalsIgnoreCase(dto.getTargetSource())) {
            OsakaGeocodedEntity job = osakaRepo.findById(dto.getTargetPostId()).orElse(null);
            if (job != null) {
                jobTitle = job.getTitle();
                recruiter = job.getUser();
            }
        } else if ("TOKYO".equalsIgnoreCase(dto.getTargetSource())) {
            TokyoGeocodedEntity job = tokyoRepo.findById(dto.getTargetPostId()).orElse(null);
            if (job != null) {
                jobTitle = job.getTitle();
                recruiter = job.getUser();
            }
        }

        notificationService.sendAppCompletedNotification(seeker, jobTitle, dto.getTargetPostId(),
                dto.getTargetSource());

        if (recruiter != null) {
            notificationService.sendNewApplicantNotification(recruiter, seeker.getNickname());
        }
    }

    /**
     * 시스템에 등록된 특정 구인 공고를 완전히 삭제(Hard Delete)합니다.
     * 관리자 권한을 가졌거나, 공고를 등록한 사용자 본인일 경우에만 삭제가 허용됩니다.
     *
     * @implNote 해당 공고와 연관된 지원 내역(Application)이 존재할 경우, 데이터베이스의 외래키 Cascade 제약
     * 조건이 없으면 무결성 오류가 발생할 수 있으므로 연관 데이터 선행 삭제 처리가 필요할 수 있습니다.
     *
     * @param id     삭제할 공고의 고유 식별자
     * @param source 삭제 대상 공고의 데이터 출처 지역
     * @param user   삭제 요청을 시도하는 사용자 엔티티
     * @throws IllegalStateException 삭제 권한이 유효하지 않을 경우 발생
     */
    @Transactional
    public void deleteJobPost(Long id, String source, UserEntity user) {

        BaseEntity entity = null;

        if ("OSAKA".equalsIgnoreCase(source)) {
            entity = osakaRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공고입니다."));
        } else if ("TOKYO".equalsIgnoreCase(source)) {
            entity = tokyoRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공고입니다."));
        } else {
            throw new IllegalArgumentException("잘못된 접근입니다 (Source 오류).");
        }

        boolean isAdmin = (user.getRole() == Enum.UserRole.ADMIN);
        boolean isRealOwner = (entity.getUserId() != null && entity.getUserId().equals(user.getUserId()));

        if (!isAdmin && !isRealOwner) {
            throw new IllegalStateException("해당 공고를 삭제할 권한이 없습니다.");
        }

        if ("OSAKA".equalsIgnoreCase(source)) {
            osakaRepo.deleteById(id);
        } else if ("TOKYO".equalsIgnoreCase(source)) {
            tokyoRepo.deleteById(id);
        }
    }

    /**
     * 사용자가 입력한 검색 키워드 및 지역 필터 조건에 부합하는 구인 공고 목록을 검색합니다.
     * 모집 진행 중(RECRUITING)인 공고만을 동적 쿼리(Specification)를 통해 조회하여 반환합니다.
     *
     * @param keyword    사용자가 검색한 텍스트 키워드 (제목, 회사명 등)
     * @param mainRegion 메인 지역 구분 필터 (tokyo 또는 osaka)
     * @param subRegion  세부 행정구역 지역 구분 필터
     * @param lang       다국어 지원을 위한 언어 설정
     * @return 검색 조건에 일치하는 구인 공고 상세 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<JobDetailDTO> searchJobsList(String keyword, String mainRegion, String subRegion, String lang) {

        List<JobDetailDTO> results = new ArrayList<>();

        if ("tokyo".equalsIgnoreCase(mainRegion)) {
            tokyoRepo.findAll(JobSearchSpec.searchConditions(keyword, subRegion, "wardCityJp", "wardCityKr"))
                    .stream()
                    .filter(entity -> entity.getStatus() == null || "RECRUITING".equals(entity.getStatus().name()))
                    .forEach(entity -> results.add(new JobDetailDTO(entity, lang, "TOKYO")));

            tokyoNoRepo.findAll(JobSearchSpec.searchConditions(keyword, subRegion, "address"))
                    .stream()
                    .filter(entity -> entity.getStatus() == null || "RECRUITING".equals(entity.getStatus().name()))
                    .forEach(entity -> results.add(new JobDetailDTO(entity, lang, "TOKYO_NO")));
        }
        else if ("osaka".equalsIgnoreCase(mainRegion)) {
            osakaRepo.findAll(JobSearchSpec.searchConditions(keyword, subRegion, "wardJp", "wardKr"))
                    .stream()
                    .filter(entity -> entity.getStatus() == null || "RECRUITING".equals(entity.getStatus().name()))
                    .forEach(entity -> results.add(new JobDetailDTO(entity, lang, "OSAKA")));

            osakaNoRepo.findAll(JobSearchSpec.searchConditions(keyword, subRegion, "address"))
                    .stream()
                    .filter(entity -> entity.getStatus() == null || "RECRUITING".equals(entity.getStatus().name()))
                    .forEach(entity -> results.add(new JobDetailDTO(entity, lang, "OSAKA_NO")));
        }

        return results;
    }
}