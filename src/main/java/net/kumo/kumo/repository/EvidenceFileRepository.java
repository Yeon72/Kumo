package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.EvidenceFileEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사업자 등록증 등 사용자 증빙 서류 메타데이터에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
public interface EvidenceFileRepository extends JpaRepository<EvidenceFileEntity, Long> {

    /**
     * 특정 사용자가 업로드한 파일 중, 특정 파일 타입(예: EVIDENCE)에 해당하는 파일 목록을 조회합니다.
     *
     * @param user     조회를 요청한 사용자 엔티티
     * @param fileType 조회할 파일의 논리적 분류 타입
     * @return 조건에 부합하는 증빙 서류 엔티티 리스트
     */
    List<EvidenceFileEntity> findByUserAndFileType(UserEntity user, String fileType);

    /**
     * 특정 사용자가 업로드한 모든 증빙 서류 내역을 데이터베이스에서 일괄 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 대상 사용자 엔티티
     */
    @Modifying
    @Transactional
    void deleteByUser(UserEntity user);
}