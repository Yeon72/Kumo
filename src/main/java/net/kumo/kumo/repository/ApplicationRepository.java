package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.ApplicationEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {

    // 1. 중복 지원 방지용: 해당 구직자가 이 공고에 이미 지원했는지 여부 확인
    boolean existsByTargetSourceAndTargetPostIdAndSeeker(String targetSource, Long targetPostId, UserEntity seeker);

    // 2. 구직자용: 본인의 지원 내역 목록 가져오기 (마이페이지용, 최신순)
    List<ApplicationEntity> findBySeekerOrderByAppliedAtDesc(UserEntity seeker);

    // 3. 구인자용: 특정 공고에 지원한 지원자 목록 가져오기 (최신순)
    List<ApplicationEntity> findByTargetSourceAndTargetPostIdOrderByAppliedAtDesc(String targetSource, Long targetPostId);

    List<ApplicationEntity> findByTargetSourceAndTargetPostIdIn(String targetSource, List<Long> targetPostIds);
}