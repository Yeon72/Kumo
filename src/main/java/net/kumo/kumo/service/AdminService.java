package net.kumo.kumo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.AdminDashboardDTO;
import net.kumo.kumo.domain.dto.JobSummaryDTO;
import net.kumo.kumo.domain.dto.ReportDTO;
import net.kumo.kumo.domain.dto.UserManageDTO;
import net.kumo.kumo.domain.entity.*;
import net.kumo.kumo.domain.entity.Enum;
import net.kumo.kumo.repository.*;
import net.kumo.kumo.domain.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    // 크롤링 데이터 리포지토리 (4종)
    private final OsakaGeocodedRepository osakaGeoRepo;
    private final TokyoGeocodedRepository tokyoGeoRepo;
    private final OsakaNoGeocodedRepository osakaNoRepo;
    private final TokyoNoGeocodedRepository tokyoNoRepo;

    // 신고/유저 리포지토리
    private final ReportRepository reportRepo;
    private final UserRepository userRepo;
    private final LoginHistoryRepository loginHistoryRepo;

    /**
     * 최근 로그인 로그 50개 가져오기
     */
    @Transactional(readOnly = true)
    public List<LoginHistoryEntity> getRecentLoginLogs() {
        return loginHistoryRepo.findAll(Sort.by(Sort.Direction.DESC, "attemptTime"))
                .stream()
                .limit(50)
                .collect(Collectors.toList());
    }

    // 전체 유저 가져오기 (DTO 사용)
    @Transactional(readOnly = true)
    public Page<UserManageDTO> getAllUsers(String lang, String searchType, String keyword, String role, String status, Pageable pageable) {

        // 1. 전체 유저 조회
        List<UserEntity> allUsers = userRepo.findAll();

        // 2. 스트림 필터링
        List<UserManageDTO> filteredList = allUsers.stream()
                .map(UserManageDTO::new)
                // (1) 역할(Role) 필터
                .filter(dto -> {
                    if (role == null || role.isBlank()) return true;
                    return role.equalsIgnoreCase(dto.getRole());
                })
                // (2) 상태(Status) 필터 (ACTIVE / INACTIVE)
                .filter(dto -> {
                    if (status == null || status.isBlank()) return true;
                    return status.equalsIgnoreCase(dto.getStatus());
                })
                // (3) 검색어 필터
                .filter(dto -> {
                    if (keyword == null || keyword.isBlank()) return true;
                    String k = keyword.toLowerCase();
                    if ("email".equals(searchType)) {
                        return dto.getEmail().toLowerCase().contains(k);
                    } else {
                        // 닉네임 또는 실명 검색
                        return dto.getNickname().toLowerCase().contains(k) ||
                                dto.getName().toLowerCase().contains(k);
                    }
                })
                // (4) 정렬 (최신 가입순)
                .sorted((a, b) -> b.getJoinedAt().compareTo(a.getJoinedAt()))
                .collect(Collectors.toList());

        // 3. 페이지네이션 처리
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredList.size());

        if (start > filteredList.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, filteredList.size());
        }

        List<UserManageDTO> pagedContent = filteredList.subList(start, end);
        return new PageImpl<>(pagedContent, pageable, filteredList.size());
    }

    // 유저 통계 가져오기
    // 유저 통계 가져오기
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. 전체 회원 수
        long total = userRepo.count();

        // 2. 신규 회원 (신규 기준을 1일 전 자정 기준으로 통일)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0);
        long newUsers = userRepo.countByCreatedAtAfter(sevenDaysAgo);

        // 3. 활동 중인 회원 (Active)
        long active = userRepo.countByIsActiveTrue();

        // 4. 비활성 회원
        long inactive = total - active;

        stats.put("totalUsers", total);
        stats.put("newUsers", newUsers);
        stats.put("activeUsers", active);
        stats.put("inactiveUsers", inactive);

        return stats;
    }

    /**
     * 유저 권한(Role) 및 상태(Status) 수정
     */
    @Transactional
    public void updateUserRoleAndStatus(Long userId, String roleStr, String statusStr) {
        // 1. 유저 조회
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. ID: " + userId));

        // 2. 권한(Role) 변경
        if (roleStr != null && !roleStr.isBlank()) {
            user.setRole(Enum.UserRole.valueOf(roleStr.toUpperCase()));
        }

        // 3. 상태(Status -> isActive) 변경
        if (statusStr != null && !statusStr.isBlank()) {
            boolean isActive = "ACTIVE".equalsIgnoreCase(statusStr);
            user.setActive(isActive); // UserEntity의 isActive 필드 업데이트
        }
    }

    /**
     * 유저 단건 삭제
     */
    @Transactional
    public void deleteUser(Long userId) {
        // [주의] 만약 이 유저가 작성한 공고(Post)나 신고(Report) 내역이 있다면,
        // DB 제약조건(FK) 때문에 에러가 날 수 있습니다.
        // 필요하다면 연관된 데이터를 먼저 삭제하거나, 삭제 대신 '탈퇴 상태(isActive=false)'로 처리하는 것을 권장합니다.

        userRepo.deleteById(userId);
    }

    // 전체 공고 통합 조회 (Lang 적용)
    @Transactional(readOnly = true)
    public Page<JobSummaryDTO> getAllJobSummaries(String lang, String searchType, String keyword, String status, Pageable pageable) {
        List<JobSummaryDTO> unifiedList = new ArrayList<>();

        // 1. 데이터 통합 (기존 코드)
        unifiedList.addAll(osakaGeoRepo.findAll().stream().map(e -> new JobSummaryDTO(e, lang, "OSAKA")).toList());
        unifiedList.addAll(tokyoGeoRepo.findAll().stream().map(e -> new JobSummaryDTO(e, lang, "TOKYO")).toList());
        unifiedList.addAll(osakaNoRepo.findAll().stream().map(e -> new JobSummaryDTO(e, lang, "OSAKA_NO")).toList());
        unifiedList.addAll(tokyoNoRepo.findAll().stream().map(e -> new JobSummaryDTO(e, lang, "TOKYO_NO")).toList());

        // 2. 필터링 및 정렬 (기존 코드)
        List<JobSummaryDTO> filteredList = unifiedList.stream()
                .filter(dto -> {
                    if (status == null || status.isBlank()) return true;
                    String dtoStatus = dto.getStatus() != null ? dto.getStatus() : "RECRUITING";
                    return status.equals(dtoStatus);
                })
                .filter(dto -> {
                    if (keyword == null || keyword.isBlank()) return true;
                    String k = keyword.toLowerCase();
                    if ("region".equals(searchType)) {
                        boolean isOsaka = k.contains("오사카") || k.contains("osaka") || k.contains("大阪");
                        boolean isTokyo = k.contains("도쿄") || k.contains("tokyo") || k.contains("東京");
                        if (isOsaka) return dto.getSource().contains("OSAKA");
                        if (isTokyo) return dto.getSource().contains("TOKYO");
                        return false;
                    } else {
                        return dto.getTitle() != null && dto.getTitle().toLowerCase().contains(k);
                    }
                })
                .sorted((a, b) -> {
                    String timeA = a.getWriteTime();
                    String timeB = b.getWriteTime();
                    if (timeB == null) return -1;
                    if (timeA == null) return 1;
                    return timeB.compareTo(timeA);
                })
                .collect(Collectors.toList());

        // 3. [추가] 페이지네이션 적용 (List -> Page 변환)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredList.size());

        // 요청한 페이지가 전체 개수보다 크면 빈 리스트 반환
        if (start > filteredList.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, filteredList.size());
        }

        // 부분 리스트 생성
        List<JobSummaryDTO> pagedContent = filteredList.subList(start, end);

        return new PageImpl<>(pagedContent, pageable, filteredList.size());
    }

    /**
     * 공고 상태(Status) 수정
     */
    @Transactional
    public void updatePostStatus(String source, Long id, String statusStr) {
        // 1. 상태값 Enum 변환 (RECRUITING, CLOSED 등)
        JobStatus newStatus = JobStatus.valueOf(statusStr.toUpperCase());

        // 2. source에 따라 해당하는 테이블(Repository)에서 데이터 조회 및 수정
        if ("OSAKA".equals(source)) {
            var post = osakaGeoRepo.findById(id).orElseThrow();
            post.setStatus(newStatus);
        }
        else if ("TOKYO".equals(source)) {
            var post = tokyoGeoRepo.findById(id).orElseThrow();
            post.setStatus(newStatus);
        }
        else if ("OSAKA_NO".equals(source)) {
            var post = osakaNoRepo.findById(id).orElseThrow();
            post.setStatus(newStatus);
        }
        else if ("TOKYO_NO".equals(source)) {
            var post = tokyoNoRepo.findById(id).orElseThrow();
            post.setStatus(newStatus);
        }
        else {
            throw new IllegalArgumentException("유효하지 않은 공고 출처입니다: " + source);
        }

        // @Transactional 덕분에 setter만 호출해도 DB에 자동 반영(Dirty Checking)됩니다.
    }

    // =================================================================
    // [수정] 신고 목록 조회 (페이징 지원 및 Page 반환으로 변경)
    // =================================================================
    @Transactional(readOnly = true)
    public Page<ReportDTO> getAllReports(String lang, Pageable pageable) {
        Page<ReportEntity> entities = reportRepo.findAll(pageable);
        boolean isJp = "ja".equalsIgnoreCase(lang);

        return entities.map(entity -> {
            ReportDTO dto = ReportDTO.fromEntity(entity);

            if (entity.getReporter() != null) {
                dto.setReporterEmail(entity.getReporter().getEmail());
            } else {
                dto.setReporterEmail(isJp ? "不明" : "알 수 없음");
            }

            String source = entity.getTargetSource();
            Long targetId = entity.getTargetPostId();
            String title = isJp ? "削除された求人" : "삭제된 공고";

            try {
                BaseEntity targetEntity = null;
                if ("OSAKA".equals(source)) targetEntity = osakaGeoRepo.findById(targetId).orElse(null);
                else if ("TOKYO".equals(source)) targetEntity = tokyoGeoRepo.findById(targetId).orElse(null);
                else if ("OSAKA_NO".equals(source)) targetEntity = osakaNoRepo.findById(targetId).orElse(null);
                else if ("TOKYO_NO".equals(source)) targetEntity = tokyoNoRepo.findById(targetId).orElse(null);

                if (targetEntity != null) {
                    title = (isJp && hasText(targetEntity.getTitleJp())) ? targetEntity.getTitleJp() : targetEntity.getTitle();
                } else {
                    title = title + " " + source;
                }
            } catch (Exception e) {
                log.warn("신고 대상 공고 조회 실패: ID={}, Source={}", targetId, source);
            }

            dto.setTargetPostTitle(title);
            return dto;
        });
    }

    // =================================================================
    // [새로 추가] 신고 처리 상태 변경
    // =================================================================
    @Transactional
    public void updateReportStatus(Long reportId, String statusStr) {
        ReportEntity report = reportRepo.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고 내역입니다. ID: " + reportId));

        report.updateStatus(statusStr.toUpperCase());
    }

    // =================================================================
    // 3. 공고 일괄 삭제 (수정됨: 연관된 신고 내역 선처리)
    // =================================================================
    @Transactional
    public void deleteMixedPosts(List<String> mixedIds) {
        if (mixedIds == null || mixedIds.isEmpty()) return;

        for (String mixedId : mixedIds) {
            try {
                int lastUnderscore = mixedId.lastIndexOf('_');
                if (lastUnderscore == -1) continue;

                String source = mixedId.substring(0, lastUnderscore);
                Long id = Long.parseLong(mixedId.substring(lastUnderscore + 1));

                // [추가] 1. 외래키(FK)가 없으므로 공고를 지우기 전에 이 공고를 타겟으로 하는 신고 내역을 먼저 삭제
                // (만약 ReportRepository에 deleteByTargetSourceAndTargetPostId 메서드가 없다면 만들어주셔야 합니다)
                // reportRepo.deleteByTargetSourceAndTargetPostId(source, id);

                // 2. 공고 삭제 처리
                switch (source) {
                    case "OSAKA" -> osakaGeoRepo.deleteById(id);
                    case "TOKYO" -> tokyoGeoRepo.deleteById(id);
                    case "OSAKA_NO" -> osakaNoRepo.deleteById(id);
                    case "TOKYO_NO" -> tokyoNoRepo.deleteById(id);
                    default -> log.warn("알 수 없는 Source: {}", source);
                }
            } catch (Exception e) {
                log.error("삭제 처리 중 오류 발생: {}", mixedId, e);
            }
        }
    }

    // =================================================================
    // 4. 신고 내역 삭제
    // =================================================================
    @Transactional
    public void deleteReports(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            reportRepo.deleteAllById(ids);
        }
    }

    // =================================================================
    // 5. 대시보드 데이터
    // =================================================================
    @Transactional(readOnly = true)
    public AdminDashboardDTO getDashboardData() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(6).withHour(0).withMinute(0);

        long totalPosts = osakaGeoRepo.count() + tokyoGeoRepo.count()
                + osakaNoRepo.count() + tokyoNoRepo.count();

        long newPosts = osakaGeoRepo.countByCreatedAtAfter(sevenDaysAgo)
                + tokyoGeoRepo.countByCreatedAtAfter(sevenDaysAgo)
                + osakaNoRepo.countByCreatedAtAfter(sevenDaysAgo)
                + tokyoNoRepo.countByCreatedAtAfter(sevenDaysAgo);

        long newUsers = userRepo.countByCreatedAtAfter(sevenDaysAgo);

        List<BaseEntity> recentPosts = new ArrayList<>();
        recentPosts.addAll(osakaGeoRepo.findByCreatedAtAfter(sevenDaysAgo));
        recentPosts.addAll(tokyoGeoRepo.findByCreatedAtAfter(sevenDaysAgo));
        recentPosts.addAll(osakaNoRepo.findByCreatedAtAfter(sevenDaysAgo));
        recentPosts.addAll(tokyoNoRepo.findByCreatedAtAfter(sevenDaysAgo));

        Map<String, Long> weeklyStats = recentPosts.stream()
                .collect(Collectors.groupingBy(
                        post -> post.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_DATE),
                        Collectors.counting()
                ));
        weeklyStats = fillMissingDates(weeklyStats, 7);

        Map<String, Long> osakaWards = listToMap(osakaGeoRepo.countByWard());
        Map<String, Long> tokyoWards = listToMap(tokyoGeoRepo.countByWard());

        // 1. 기존의 mockUserStats 관련 코드를 지웁니다.

        // 2. 최근 6개월 치 진짜 DB 데이터 가져오기 및 월별 그룹화 (수정된 코드)
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(5).withDayOfMonth(1).withHour(0).withMinute(0);
        List<UserEntity> recentUsers = userRepo.findByCreatedAtAfter(sixMonthsAgo);

        // 연-월(예: 2026-01, 2026-02) 기준으로 그룹화하여 카운트
        Map<String, Long> realMonthlyStats = recentUsers.stream()
                .collect(Collectors.groupingBy(
                        user -> user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        TreeMap::new, // 시간순 정렬을 위해 TreeMap 사용
                        Collectors.counting()
                ));

        return AdminDashboardDTO.builder()
                .totalUsers(userRepo.count())
                .newUsers(newUsers)
                .totalPosts(totalPosts)
                .newPosts(newPosts)
                .weeklyPostStats(weeklyStats)
                .osakaWardStats(osakaWards)
                .tokyoWardStats(tokyoWards)
                .monthlyUserStats(realMonthlyStats) // <-- 진짜 DB 데이터 맵핑!
                .build();
    }

    // --- Helper Methods ---
    private Map<String, Long> listToMap(List<Object[]> list) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : list) {
            String key = (String) row[0];
            Long val = (Long) row[1];
            if (key != null) map.put(key, val);
        }
        return map;
    }

    private Map<String, Long> fillMissingDates(Map<String, Long> data, int days) {
        Map<String, Long> sorted = new TreeMap<>(data);
        LocalDate today = LocalDate.now();
        for (int i = 0; i < days; i++) {
            String date = today.minusDays(i).format(DateTimeFormatter.ISO_DATE);
            sorted.putIfAbsent(date, 0L);
        }
        return sorted;
    }

    private boolean hasText(String str) {
        return str != null && !str.isBlank();
    }
}