package net.kumo.kumo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.JobDetailDTO;
import net.kumo.kumo.domain.dto.ScrapDTO;
import net.kumo.kumo.service.ScrapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * 구인 공고 스크랩(찜하기) 관련 비동기 요청을 처리하는 API Controller 클래스입니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/scraps")
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    /**
     * 특정 공고에 대한 사용자의 스크랩(찜) 상태를 토글(추가 또는 취소) 처리합니다.
     *
     * @param scrapDTO  스크랩 대상 공고 식별자 및 출처 정보가 담긴 DTO
     * @param principal 현재 인증된 사용자 정보
     * @return 갱신된 스크랩 상태 여부를 포함한 DTO 응답
     * @throws IllegalArgumentException 인증되지 않은 사용자의 접근 시 발생
     */
    @PostMapping
    public ResponseEntity<ScrapDTO> toggleScrap(@RequestBody ScrapDTO scrapDTO, Principal principal) {
        if (principal == null) {
            log.warn("인증되지 않은 사용자의 스크랩 토글 시도 차단");
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String loginEmail = principal.getName();
        boolean isScraped = scrapService.toggleScrap(scrapDTO, loginEmail);

        scrapDTO.setScraped(isScraped);
        return ResponseEntity.ok(scrapDTO);
    }

    /**
     * 현재 로그인한 사용자가 스크랩(찜)한 구인 공고 리스트를 조회합니다.
     *
     * @param principal 현재 인증된 사용자 정보
     * @param lang      클라이언트 언어 설정 (기본값: kr)
     * @return 사용자가 스크랩한 공고의 상세 정보 리스트(JobDetailDTO)
     * @throws IllegalArgumentException 인증되지 않은 사용자의 접근 시 발생
     */
    @GetMapping
    public ResponseEntity<List<JobDetailDTO>> getScrapedJobs(
            Principal principal,
            @RequestParam(defaultValue = "kr") String lang) {

        if (principal == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String loginEmail = principal.getName();
        List<JobDetailDTO> scrapedJobs = scrapService.getScrapedJobsList(loginEmail, lang);

        log.info("사용자 스크랩 리스트 조회 완료 - 계정: {}, 조회 건수: {}", loginEmail, scrapedJobs.size());

        return ResponseEntity.ok(scrapedJobs);
    }
}