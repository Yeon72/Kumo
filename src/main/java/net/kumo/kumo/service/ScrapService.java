package net.kumo.kumo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.JobDetailDTO;
import net.kumo.kumo.domain.dto.ScrapDTO;
import net.kumo.kumo.domain.entity.BaseEntity;
import net.kumo.kumo.domain.entity.ScrapEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 구직자의 구인 공고 스크랩(즐겨찾기/찜하기) 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;

    private final OsakaGeocodedRepository osakaRepo;
    private final TokyoGeocodedRepository tokyoRepo;
    private final OsakaNoGeocodedRepository osakaNoRepo;
    private final TokyoNoGeocodedRepository tokyoNoRepo;

    /**
     * 사용자의 공고 스크랩 상태를 토글(등록 또는 취소) 처리합니다.
     * 이미 스크랩된 상태라면 삭제를, 그렇지 않다면 신규 스크랩을 등록합니다.
     *
     * @param scrapDTO   스크랩 대상 공고 정보가 담긴 DTO
     * @param loginEmail 토글 요청을 수행한 로그인 사용자의 이메일 계정
     * @return true: 스크랩 등록 완료, false: 스크랩 취소 완료
     */
    @Transactional
    public boolean toggleScrap(ScrapDTO scrapDTO, String loginEmail) {

        UserEntity user = userRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Long userId = user.getUserId();
        Long jobPostId = scrapDTO.getTargetPostId();
        String source = scrapDTO.getTargetSource();

        log.info("스크랩 토글 요청 - UserId: {}, JobPostId: {}, Source: {}", userId, jobPostId, source);

        if (scrapRepository.existsByUserIdAndJobPostIdAndSource(userId, jobPostId, source)) {
            scrapRepository.deleteByUserIdAndJobPostIdAndSource(userId, jobPostId, source);
            log.info("스크랩 취소 완료");
            return false;
        } else {
            ScrapEntity newScrap = ScrapEntity.builder()
                    .userId(userId)
                    .jobPostId(jobPostId)
                    .source(source)
                    .build();
            scrapRepository.save(newScrap);
            log.info("스크랩 등록 완료");
            return true;
        }
    }

    /**
     * 공고 상세 페이지 로딩 시, 해당 사용자가 이미 공고를 스크랩했는지 여부를 단순 확인합니다.
     *
     * @param userId    확인할 사용자의 식별자
     * @param jobPostId 대상 공고의 식별자
     * @param source    대상 공고의 지역 및 데이터 출처
     * @return 스크랩 여부 (true: 이미 스크랩함)
     */
    @Transactional(readOnly = true)
    public boolean checkIsScraped(Long userId, Long jobPostId, String source) {
        return scrapRepository.existsByUserIdAndJobPostIdAndSource(userId, jobPostId, source);
    }

    /**
     * 특정 사용자의 전체 스크랩 내역을 최신순으로 조회하고,
     * 각 스크랩에 연관된 실제 공고 상세 데이터(JobDetailDTO) 리스트로 변환하여 반환합니다.
     *
     * @param loginEmail 조회를 요청한 로그인 사용자의 이메일 계정
     * @param lang       다국어 지원을 위한 언어 설정 파라미터
     * @return 사용자 스크랩 기반의 구인 공고 상세 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<JobDetailDTO> getScrapedJobsList(String loginEmail, String lang) {
        UserEntity user = userRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        List<ScrapEntity> scraps = scrapRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId());
        List<JobDetailDTO> result = new ArrayList<>();

        for (ScrapEntity scrap : scraps) {
            Long id = scrap.getJobPostId();
            String source = scrap.getSource();
            BaseEntity entity = null;

            if ("OSAKA".equalsIgnoreCase(source)) {
                entity = osakaRepo.findById(id).orElse(null);
            } else if ("TOKYO".equalsIgnoreCase(source)) {
                entity = tokyoRepo.findById(id).orElse(null);
            } else if ("OSAKA_NO".equalsIgnoreCase(source)) {
                entity = osakaNoRepo.findById(id).orElse(null);
            } else if ("TOKYO_NO".equalsIgnoreCase(source)) {
                entity = tokyoNoRepo.findById(id).orElse(null);
            }

            if (entity != null) {
                result.add(new JobDetailDTO(entity, lang, source));
            }
        }

        return result;
    }
}