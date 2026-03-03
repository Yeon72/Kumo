package net.kumo.kumo.service;

import lombok.RequiredArgsConstructor;
import net.kumo.kumo.domain.dto.ApplicationDTO;
import net.kumo.kumo.domain.dto.JobDetailDTO;
import net.kumo.kumo.domain.dto.JobSummaryDTO;
import net.kumo.kumo.domain.dto.ReportDTO;
import net.kumo.kumo.domain.dto.projection.JobSummaryView;
import net.kumo.kumo.domain.entity.*;
import net.kumo.kumo.domain.entity.Enum;
import net.kumo.kumo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MapService {

    private final OsakaGeocodedRepository osakaRepo;
    private final TokyoGeocodedRepository tokyoRepo;
    private final OsakaNoGeocodedRepository osakaNoRepo;
    private final TokyoNoGeocodedRepository tokyoNoRepo;

    // 신고 관련 리포지토리
    private final ReportRepository reportRepo;
    private final UserRepository userRepo; // ★ [추가] 신고자(User) 조회를 위해 필요

    // 공고 신청 리포지토리
    private final ApplicationRepository applicationRepo;

    // --- 1. 지도용 리스트 조회 ---
    @Transactional(readOnly = true)
    public List<JobSummaryDTO> getJobListInMap(Double minLat, Double maxLat, Double minLng, Double maxLng, String lang) {
        List<JobSummaryView> osakaRaw = osakaRepo.findTop300ByLatBetweenAndLngBetween(minLat, maxLat, minLng, maxLng);
        List<JobSummaryDTO> result = new ArrayList<>(osakaRaw.stream()
                // 🌟 [추가] 상태가 null(기존 데이터)이거나 'RECRUITING'인 것만 필터링!
                .filter(view -> view.getStatus() == null || "RECRUITING".equals(view.getStatus().name()))
                .map(view -> new JobSummaryDTO(view, lang, "OSAKA"))
                .toList());

        List<JobSummaryView> tokyoRaw = tokyoRepo.findTop300ByLatBetweenAndLngBetween(minLat, maxLat, minLng, maxLng);
        result.addAll(tokyoRaw.stream()
                // 🌟 [추가] 도쿄도 마찬가지로 필터링!
                .filter(view -> view.getStatus() == null || "RECRUITING".equals(view.getStatus().name()))
                .map(view -> new JobSummaryDTO(view, lang, "TOKYO"))
                .toList());

        return result;
    }

    // --- 2. 상세 페이지 조회 ---
    @Transactional(readOnly = true)
    public JobDetailDTO getJobDetail(Long id, String source, String lang) {
        BaseEntity entity = null;

        // 소스에 따라 적절한 리포지토리 선택
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

        // JobDetailDTO 생성자에 source도 함께 전달
        return new JobDetailDTO(entity, lang, source);
    }

    // --- 3. [수정] 신고 등록 ---
    @Transactional
    public void createReport(ReportDTO dto) {
        // 1. 신고자(User) 조회
        // DTO에 있는 reporterId로 실제 유저 엔티티를 찾아야 연관관계를 맺을 수 있음
        UserEntity reporter = userRepo.findById(dto.getReporterId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2. 엔티티 변환 및 저장 (변경된 DB 구조 반영)
        ReportEntity report = ReportEntity.builder()
                .reporter(reporter)            // [변경] ID 대신 UserEntity 객체 주입
                .targetPostId(dto.getTargetPostId())
                .targetSource(dto.getTargetSource()) // [변경] 이제 별도 컬럼에 저장
                .reasonCategory(dto.getReasonCategory())
                .description(dto.getDescription())   // [변경] 순수 본문만 저장 (앞에 [OSAKA] 안 붙임)
                .status("PENDING")             // 기본 상태
                .build();

        reportRepo.save(report);
    }

    // --- 4. 구인 신청(지원하기) 로직 ---
    @Transactional
    public void applyForJob(UserEntity seeker, ApplicationDTO.ApplyRequest dto) {
        // ★ 파라미터 타입 변경됨!

        // 1. 중복 지원 검사
        boolean alreadyApplied = applicationRepo.existsByTargetSourceAndTargetPostIdAndSeeker(
                dto.getTargetSource(),
                dto.getTargetPostId(),
                seeker
        );

        if (alreadyApplied) {
            throw new IllegalStateException("이미 지원하신 공고입니다.");
        }

        // 2. 지원 내역 엔티티 생성
        ApplicationEntity application = ApplicationEntity.builder()
                .targetSource(dto.getTargetSource())
                .targetPostId(dto.getTargetPostId())
                .seeker(seeker)
                .build();

        // 3. DB 저장
        applicationRepo.save(application);
    }

    // --- 5. [NEW] 공고 삭제 로직 ---
    @Transactional
    public void deleteJobPost(Long id, String source, UserEntity user) {

        // 1. source를 기반으로 해당 공고 엔티티를 찾기 (BaseEntity 상속 객체)
        BaseEntity entity = null;

        if ("OSAKA".equalsIgnoreCase(source)) {
            entity = osakaRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공고입니다."));
        } else if ("TOKYO".equalsIgnoreCase(source)) {
            entity = tokyoRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공고입니다."));
        } else {
            throw new IllegalArgumentException("잘못된 접근입니다 (Source 오류).");
        }

        // 2. 권한 검증 (가장 중요!)
        // 관리자(ADMIN)이거나, 공고의 작성자 ID와 로그인 유저 ID가 같아야만 삭제 가능
        boolean isAdmin = (user.getRole() == Enum.UserRole.ADMIN);

        // 주의: 크롤링 데이터는 작성자(userId)가 null일 수 있으므로 null 체크 필수
        boolean isRealOwner = (entity.getUserId() != null && entity.getUserId().equals(user.getUserId()));

        if (!isAdmin && !isRealOwner) {
            throw new IllegalStateException("해당 공고를 삭제할 권한이 없습니다.");
        }

        // 3. 권한이 확인되면 실제 데이터베이스에서 삭제
        if ("OSAKA".equalsIgnoreCase(source)) {
            osakaRepo.deleteById(id);
        } else if ("TOKYO".equalsIgnoreCase(source)) {
            tokyoRepo.deleteById(id);
        }

        // (참고) 만약 해당 공고에 엮인 지원내역(applications)이 있다면,
        // 테이블 설계 당시 ON DELETE CASCADE 를 걸어두지 않으셨다면
        // 여기서 applicationRepo.deleteByTargetSourceAndTargetPostId() 를 먼저 실행해 주셔야 합니다.
    }
    
    // ==========================================
    // --- 4. [NEW] 검색 리스트 조회 (JobDetailDTO 반환) ---
    // ==========================================
    // ==========================================
    // --- 4. [NEW] 검색 리스트 조회 (JobDetailDTO 반환) ---
    // ==========================================
    @Transactional(readOnly = true)
    public List<JobDetailDTO> searchJobsList(String keyword, String mainRegion, String subRegion, String lang) {
        
        List<JobDetailDTO> results = new ArrayList<>();
        
        // 1. 도쿄 검색
        if ("tokyo".equalsIgnoreCase(mainRegion)) {
            // 위치 O: 도쿄는 wardCityJp, wardCityKr 컬럼을 확인
            tokyoRepo.findAll(JobSearchSpec.searchConditions(keyword, subRegion, "wardCityJp", "wardCityKr"))
                    .stream() // 🌟 stream() 열고
                    .filter(entity -> entity.getStatus() == null || "RECRUITING".equals(entity.getStatus().name())) // 🌟 필터 추가!
                    .forEach(entity -> results.add(new JobDetailDTO(entity, lang, "TOKYO")));
            
            // 위치 X: address 컬럼에서 확인
            tokyoNoRepo.findAll(JobSearchSpec.searchConditions(keyword, subRegion, "address"))
                    .stream()
                    .filter(entity -> entity.getStatus() == null || "RECRUITING".equals(entity.getStatus().name()))
                    .forEach(entity -> results.add(new JobDetailDTO(entity, lang, "TOKYO_NO")));
        }
        // 2. 오사카 검색
        else if ("osaka".equalsIgnoreCase(mainRegion)) {
            // 위치 O: 오사카는 wardJp, wardKr 컬럼을 확인
            osakaRepo.findAll(JobSearchSpec.searchConditions(keyword, subRegion, "wardJp", "wardKr"))
                    .stream()
                    .filter(entity -> entity.getStatus() == null || "RECRUITING".equals(entity.getStatus().name()))
                    .forEach(entity -> results.add(new JobDetailDTO(entity, lang, "OSAKA")));
            
            // 위치 X: address 컬럼에서 확인
            osakaNoRepo.findAll(JobSearchSpec.searchConditions(keyword, subRegion, "address"))
                    .stream()
                    .filter(entity -> entity.getStatus() == null || "RECRUITING".equals(entity.getStatus().name()))
                    .forEach(entity -> results.add(new JobDetailDTO(entity, lang, "OSAKA_NO")));
        }
        
        return results;
    }
}