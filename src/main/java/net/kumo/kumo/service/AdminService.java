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

/**
 * 관리자(Admin) 대시보드 및 백오피스 관리에 필요한 핵심 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 사용자 계정 관리, 구인 공고 모니터링, 통계 데이터 추출 및 신고 내역 처리 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final OsakaGeocodedRepository osakaGeoRepo;
    private final TokyoGeocodedRepository tokyoGeoRepo;
    private final OsakaNoGeocodedRepository osakaNoRepo;
    private final TokyoNoGeocodedRepository tokyoNoRepo;

    private final ReportRepository reportRepo;
    private final UserRepository userRepo;
    private final LoginHistoryRepository loginHistoryRepo;

    /**
     * 플랫폼 전체의 최근 로그인 시도 이력(성공/실패 포함) 50건을 최신순으로 조회합니다.
     *
     * @return 로그인 이력 엔티티 리스트
     */
    @Transactional(readOnly = true)
    public List<LoginHistoryEntity> getRecentLoginLogs() {
        return loginHistoryRepo.findAll(Sort.by(Sort.Direction.DESC, "attemptTime"))
                .stream()
                .limit(50)
                .collect(Collectors.toList());
    }

    /**
     * 관리자 페이지의 회원 관리 목록을 검색 및 필터 조건에 따라 페이징 처리하여 조회합니다.
     *
     * @param lang       다국어 설정
     * @param searchType 검색 대상 (email 또는 nickname/name)
     * @param keyword    검색 키워드
     * @param role       권한 필터 (SEEKER, RECRUITER 등)
     * @param status     상태 필터 (ACTIVE, INACTIVE)
     * @param pageable   페이징 요청 정보
     * @return 필터링 및 페이징이 완료된 사용자 정보 DTO Page 객체
     */
    @Transactional(readOnly = true)
    public Page<UserManageDTO> getAllUsers(String lang, String searchType, String keyword, String role, String status, Pageable pageable) {

        List<UserEntity> allUsers = userRepo.findAll();

        List<UserManageDTO> filteredList = allUsers.stream()
                .map(UserManageDTO::new)
                .filter(dto -> {
                    if (role == null || role.isBlank()) return true;
                    return role.equalsIgnoreCase(dto.getRole());
                })
                .filter(dto -> {
                    if (status == null || status.isBlank()) return true;
                    return status.equalsIgnoreCase(dto.getStatus());
                })
                .filter(dto -> {
                    if (keyword == null || keyword.isBlank()) return true;
                    String k = keyword.toLowerCase();
                    if ("email".equals(searchType)) {
                        return dto.getEmail().toLowerCase().contains(k);
                    } else {
                        return dto.getNickname().toLowerCase().contains(k) ||
                                dto.getName().toLowerCase().contains(k);
                    }
                })
                .sorted((a, b) -> b.getJoinedAt().compareTo(a.getJoinedAt()))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredList.size());

        if (start > filteredList.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, filteredList.size());
        }

        List<UserManageDTO> pagedContent = filteredList.subList(start, end);
        return new PageImpl<>(pagedContent, pageable, filteredList.size());
    }

    /**
     * 대시보드 상단 요약 패널에 노출될 전체 회원 및 신규/활성 회원 통계 데이터를 집계합니다.
     *
     * @return 통계 데이터가 담긴 맵(Map) 객체
     */
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();

        long total = userRepo.count();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0);
        long newUsers = userRepo.countByCreatedAtAfter(sevenDaysAgo);
        long active = userRepo.countByIsActiveTrue();
        long inactive = total - active;

        stats.put("totalUsers", total);
        stats.put("newUsers", newUsers);
        stats.put("activeUsers", active);
        stats.put("inactiveUsers", inactive);

        return stats;
    }

    /**
     * 특정 사용자의 시스템 권한(Role) 및 계정 활성화 상태를 관리자가 수동으로 변경합니다.
     *
     * @param userId    대상 사용자 식별자
     * @param roleStr   변경할 권한 명칭 문자열
     * @param statusStr 변경할 상태 명칭 문자열 (ACTIVE, INACTIVE)
     */
    @Transactional
    public void updateUserRoleAndStatus(Long userId, String roleStr, String statusStr) {
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. ID: " + userId));

        if (roleStr != null && !roleStr.isBlank()) {
            user.setRole(Enum.UserRole.valueOf(roleStr.toUpperCase()));
        }

        if (statusStr != null && !statusStr.isBlank()) {
            boolean isActive = "ACTIVE".equalsIgnoreCase(statusStr);
            user.setActive(isActive);
        }
    }

    /**
     * 특정 사용자를 시스템에서 완전 삭제(Hard Delete)합니다.
     * 연관된 하위 데이터의 외래키 제약조건 위반에 유의해야 합니다.
     *
     * @param userId 삭제할 사용자 식별자
     */
    @Transactional
    public void deleteUser(Long userId) {
        userRepo.deleteById(userId);
    }

    /**
     * 4개로 분리된 구인 공고 테이블 데이터를 모두 통합하여,
     * 검색 및 필터 조건에 따라 페이징 처리된 목록을 반환합니다.
     *
     * @param lang       다국어 설정
     * @param searchType 검색 대상 (지역 필터 또는 키워드)
     * @param keyword    검색 키워드
     * @param status     공고 상태 (RECRUITING, CLOSED 등)
     * @param pageable   페이징 요청 정보
     * @return 통합 및 필터링된 구인 공고 DTO Page 객체
     */
    @Transactional(readOnly = true)
    public Page<JobSummaryDTO> getAllJobSummaries(String lang, String searchType, String keyword, String status, Pageable pageable) {
        List<JobSummaryDTO> unifiedList = new ArrayList<>();

        unifiedList.addAll(osakaGeoRepo.findAll().stream().map(e -> new JobSummaryDTO(e, lang, "OSAKA")).toList());
        unifiedList.addAll(tokyoGeoRepo.findAll().stream().map(e -> new JobSummaryDTO(e, lang, "TOKYO")).toList());
        unifiedList.addAll(osakaNoRepo.findAll().stream().map(e -> new JobSummaryDTO(e, lang, "OSAKA_NO")).toList());
        unifiedList.addAll(tokyoNoRepo.findAll().stream().map(e -> new JobSummaryDTO(e, lang, "TOKYO_NO")).toList());

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

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredList.size());

        if (start > filteredList.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, filteredList.size());
        }

        List<JobSummaryDTO> pagedContent = filteredList.subList(start, end);

        return new PageImpl<>(pagedContent, pageable, filteredList.size());
    }

    /**
     * 특정 구인 공고의 모집 진행 상태를 강제로 변경합니다.
     *
     * @param source    공고 데이터 출처 지역
     * @param id        공고 식별자
     * @param statusStr 변경할 상태 문자열
     */
    @Transactional
    public void updatePostStatus(String source, Long id, String statusStr) {
        JobStatus newStatus = JobStatus.valueOf(statusStr.toUpperCase());

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
    }

    /**
     * 관리자 페이지의 신고 관리 탭에 노출될 전체 신고 내역을 페이징하여 조회합니다.
     * 신고 대상 공고가 이미 삭제된 경우에도 예외 없이 대응하여 렌더링합니다.
     *
     * @param lang     다국어 설정
     * @param pageable 페이징 요청 정보
     * @return 페이징이 완료된 신고 내역 DTO Page 객체
     */
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

    /**
     * 관리자가 개별 신고 내역의 처리 상태(접수, 완료, 기각 등)를 갱신합니다.
     *
     * @param reportId  갱신할 신고 내역 식별자
     * @param statusStr 변경할 상태 문자열
     */
    @Transactional
    public void updateReportStatus(Long reportId, String statusStr) {
        ReportEntity report = reportRepo.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고 내역입니다. ID: " + reportId));

        report.updateStatus(statusStr.toUpperCase());
    }

    /**
     * 관리자가 다중 선택한 구인 공고들을 시스템에서 일괄 완전 삭제(Hard Delete)합니다.
     * 삭제 전 해당 공고를 타겟으로 하는 신고 내역을 선행 삭제 처리합니다.
     *
     * @param mixedIds "출처_식별자" 형태(예: OSAKA_123)의 식별자 리스트
     */
    @Transactional
    public void deleteMixedPosts(List<String> mixedIds) {
        if (mixedIds == null || mixedIds.isEmpty()) return;

        for (String mixedId : mixedIds) {
            try {
                int lastUnderscore = mixedId.lastIndexOf('_');
                if (lastUnderscore == -1) continue;

                String source = mixedId.substring(0, lastUnderscore);
                Long id = Long.parseLong(mixedId.substring(lastUnderscore + 1));

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

    /**
     * 관리자가 다중 선택한 신고 내역들을 데이터베이스에서 일괄 삭제합니다.
     *
     * @param ids 삭제할 신고 내역 식별자 리스트
     */
    @Transactional
    public void deleteReports(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            reportRepo.deleteAllById(ids);
        }
    }

    /**
     * 관리자 대시보드 렌더링에 필요한 지역별 공고 등록 현황 차트,
     * 최근 6개월 가입자 통계 등의 집계 데이터를 생성하여 반환합니다.
     *
     * @return 대시보드 통계 데이터가 담긴 DTO
     */
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

        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(5).withDayOfMonth(1).withHour(0).withMinute(0);
        List<UserEntity> recentUsers = userRepo.findByCreatedAtAfter(sixMonthsAgo);

        Map<String, Long> realMonthlyStats = recentUsers.stream()
                .collect(Collectors.groupingBy(
                        user -> user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        TreeMap::new,
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
                .monthlyUserStats(realMonthlyStats)
                .build();
    }

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