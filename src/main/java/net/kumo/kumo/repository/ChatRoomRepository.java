package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {

    // 1. 기존에 있던 메서드들 (이름 뒤에 _ 가 붙은 방식)
    Optional<ChatRoomEntity> findBySeeker_UserIdAndRecruiter_UserId(Long seekerId, Long recruiterId);

    List<ChatRoomEntity> findBySeeker_UserId(Long seekerId);

    List<ChatRoomEntity> findByRecruiter_UserId(Long recruiterId);

    // 2. [추가된 정석 메서드] 목록 조회를 위해 꼭 필요합니다!
    // Seeker 혹은 Recruiter 둘 중 하나라도 내 ID와 일치하는 방을 다 가져옵니다.
    List<ChatRoomEntity> findBySeekerUserIdOrRecruiterUserId(Long seekerId, Long recruiterId);
}