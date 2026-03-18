package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.SeekerDocumentEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 구직자가 업로드한 이력서 첨부 서류 및 포트폴리오(Document) 메타데이터에 대한
 * 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface SeekerDocumentRepository extends JpaRepository<SeekerDocumentEntity, Long> {

    /**
     * 특정 사용자 엔티티를 기반으로 업로드된 모든 증빙 서류 목록을 조회합니다.
     *
     * @param user 조회를 요청한 사용자 엔티티
     * @return 해당 사용자의 서류 엔티티 리스트
     */
    List<SeekerDocumentEntity> findByUser(UserEntity user);

    /**
     * 특정 사용자 식별자를 기반으로 업로드된 전체 증빙 서류 목록을 조회합니다.
     *
     * @param userId 조회를 요청한 사용자 식별자
     * @return 해당 사용자의 서류 엔티티 리스트
     */
    List<SeekerDocumentEntity> findAllByUser_UserId(Long userId);

    /**
     * 특정 사용자 식별자를 기반으로 업로드된 증빙 서류 목록을 조회합니다.
     *
     * @param userId 조회를 요청한 사용자 식별자
     * @return 해당 사용자의 서류 엔티티 리스트
     */
    List<SeekerDocumentEntity> findByUser_UserId(Long userId);

    /**
     * 특정 사용자가 업로드한 모든 서류 내역을 데이터베이스에서 일괄 삭제합니다. (이력서 갱신 또는 회원 탈퇴 시 사용)
     *
     * @param user 삭제할 대상 사용자 엔티티
     */
    @Modifying
    @Transactional
    void deleteByUser(UserEntity user);
}