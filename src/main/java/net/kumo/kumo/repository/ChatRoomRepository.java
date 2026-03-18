package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.ChatRoomEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 채팅방(ChatRoom) 세션 관리에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {

    /**
     * 구직자, 구인자, 대상 공고 식별자 및 출처 정보를 모두 충족하는 특정 채팅방을 조회합니다.
     * 기존 채팅방 존재 여부 확인 및 입장을 위해 사용됩니다.
     *
     * @param seekerId     참여 중인 구직자 식별자
     * @param recruiterId  참여 중인 구인자 식별자
     * @param targetPostId 연결된 대상 공고 식별자
     * @param targetSource 대상 공고의 데이터 출처
     * @return 조건에 부합하는 채팅방 엔티티 (없을 경우 Optional.empty 반환)
     */
    Optional<ChatRoomEntity> findBySeeker_UserIdAndRecruiter_UserIdAndTargetPostIdAndTargetSource(
            Long seekerId, Long recruiterId, Long targetPostId, String targetSource
    );

    /**
     * 특정 사용자가 구직자 또는 구인자 자격으로 참여 중인 모든 채팅방 목록을 조회합니다. (JPQL 기반)
     *
     * @param userId 참여 여부를 확인할 사용자 식별자
     * @return 사용자가 참여 중인 채팅방 리스트
     */
    @Query("SELECT r FROM ChatRoomEntity r WHERE r.seeker.userId = :userId OR r.recruiter.userId = :userId")
    List<ChatRoomEntity> findChatRoomsByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자가 구직자 또는 구인자 자격으로 참여 중인 모든 채팅방 목록을 조회합니다. (메서드 네이밍 기반)
     *
     * @param seekerId    구직자로서의 사용자 식별자
     * @param recruiterId 구인자로서의 사용자 식별자
     * @return 사용자가 참여 중인 채팅방 리스트
     */
    List<ChatRoomEntity> findBySeekerUserIdOrRecruiterUserId(Long seekerId, Long recruiterId);

    /**
     * 특정 사용자가 구직자 또는 구인자로 소속된 모든 채팅방 세션을 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user  구직자로서 확인할 사용자 엔티티
     * @param user1 구인자로서 확인할 사용자 엔티티 (통상적으로 user와 동일 객체 전달)
     */
    void deleteBySeekerOrRecruiter(UserEntity user, UserEntity user1);
}