package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.Enum;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 시스템에 가입된 모든 사용자(User) 계정 정보에 대한
 * 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * 이메일을 기반으로 사용자 계정 정보를 조회합니다.
     *
     * @param email 조회할 사용자의 이메일 주소
     * @return 조건에 부합하는 사용자 엔티티
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * 특정 닉네임이 시스템에 이미 존재하는지(중복 여부) 확인합니다.
     *
     * @param nickname 중복 검사할 닉네임
     * @return 중복 여부 (존재 시 true)
     */
    boolean existsByNickname(String nickname);

    /**
     * 특정 이메일이 시스템에 이미 존재하는지(중복 여부) 확인합니다.
     *
     * @param email 중복 검사할 이메일
     * @return 중복 여부 (존재 시 true)
     */
    boolean existsByEmail(String email);

    /**
     * 사용자의 실명(한자 성+이름 조합)과 연락처, 권한 정보를 기반으로
     * 등록된 이메일 계정을 찾아 반환합니다. (아이디 찾기 기능)
     *
     * @param fullName 사용자의 성과 이름을 결합한 전체 실명
     * @param contact  사용자의 연락처
     * @param role     사용자의 가입 권한 (SEEKER, RECRUITER 등)
     * @return 조건에 일치하는 사용자의 이메일 (존재하지 않을 경우 Optional.empty 반환)
     */
    @Query("SELECT u.email FROM UserEntity u " +
            "WHERE CONCAT(IFNULL(u.nameKanjiSei, ''), IFNULL(u.nameKanjiMei, '')) = :fullName " +
            "AND u.contact = :contact " +
            "AND u.role = :role")
    Optional<String> findEmailByKanjiNameAndContact(
            @Param("fullName") String fullName,
            @Param("contact") String contact,
            @Param("role") Enum.UserRole role);

    /**
     * 이메일, 실명, 연락처, 권한 정보가 모두 일치하는 사용자가 존재하는지 확인합니다.
     * (비밀번호 찾기 시 본인 인증 기능)
     *
     * @param email    확인할 사용자의 이메일 주소
     * @param fullName 사용자의 전체 실명
     * @param contact  사용자의 연락처
     * @param role     사용자의 권한
     * @return 사용자 존재 및 정보 일치 여부
     */
    @Query("""
            SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
            FROM UserEntity u
            WHERE u.email = :email
              AND CONCAT(IFNULL(u.nameKanjiSei, ''), IFNULL(u.nameKanjiMei, '')) = :fullName
              AND u.contact = :contact
              AND u.role = :role
            """)
    boolean existsByEmailAndFullNameAndContactAndRole(
            @Param("email") String email,
            @Param("fullName") String fullName,
            @Param("contact") String contact,
            @Param("role") Enum.UserRole role);

    /**
     * 특정 기준 일시 이후에 가입한 신규 회원 수를 계산합니다. (대시보드 통계용)
     *
     * @param dateTime 기준 일시
     * @return 신규 가입자 수
     */
    long countByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 특정 기준 일시 이후에 가입한 신규 회원의 엔티티 목록을 조회합니다. (월별 차트 통계용)
     *
     * @param dateTime 기준 일시
     * @return 신규 가입 사용자 리스트
     */
    List<UserEntity> findByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 현재 계정이 활성화(isActive = true)된 회원의 총 수를 계산합니다.
     *
     * @return 활성화된 회원 수
     */
    long countByIsActiveTrue();

    /**
     * 현재 계정이 비활성화(정지 또는 탈퇴, isActive = false)된 회원의 총 수를 계산합니다.
     *
     * @return 비활성화된 회원 수
     */
    long countByIsActiveFalse();
}