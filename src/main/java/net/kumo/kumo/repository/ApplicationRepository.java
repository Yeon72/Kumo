package net.kumo.kumo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.kumo.kumo.domain.entity.ApplicationEntity;
import net.kumo.kumo.domain.entity.UserEntity;

@Repository
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {

        // 1. 중복 지원 방지용: 해당 구직자가 이 공고에 이미 지원했는지 여부 확인
        boolean existsByTargetSourceAndTargetPostIdAndSeeker(String targetSource, Long targetPostId, UserEntity seeker);

        // 2. 구직자용: 본인의 지원 내역 목록 가져오기 (마이페이지용, 최신순)
        List<ApplicationEntity> findBySeekerOrderByAppliedAtDesc(UserEntity seeker);

        // 3. 구인자용: 특정 공고에 지원한 지원자 목록 가져오기 (최신순)
        List<ApplicationEntity> findByTargetSourceAndTargetPostIdOrderByAppliedAtDesc(String targetSource,
                        Long targetPostId);

        List<ApplicationEntity> findByTargetSourceAndTargetPostIdIn(String targetSource, List<Long> targetPostIds);

        // 🌟 이 착한 쿼리를 꼭 넣어주세요! (a.status = 'APPLIED' 조건도 문자열이 아닌 Enum 형태로 깔끔하게 처리했습니다.)
        @Query("SELECT COUNT(a) FROM ApplicationEntity a " +
                        "WHERE a.targetSource = :source " +
                        "AND a.targetPostId IN :postIds " +
                        "AND a.status = net.kumo.kumo.domain.entity.Enum.ApplicationStatus.APPLIED")
        long countUnreadBySourceAndPostIds(
                        @Param("source") String source,
                        @Param("postIds") List<Long> postIds);
}