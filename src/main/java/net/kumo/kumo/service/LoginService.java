package net.kumo.kumo.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.ChangeNewPWDTO;
import net.kumo.kumo.domain.dto.FindIdDTO;
import net.kumo.kumo.domain.dto.JoinRecruiterDTO;
import net.kumo.kumo.domain.dto.JoinSeekerDTO;
import net.kumo.kumo.domain.entity.Enum;
import net.kumo.kumo.domain.entity.EvidenceFileEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 회원가입(구직자/구인자), 계정 찾기, 비밀번호 변경 및
 * 회원 탈퇴 시 발생하는 연쇄 데이터 삭제 로직을 총괄하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LoginService {

    private final UserRepository userRepository;
    private final EvidenceFileRepository fileRepository;
    private final PasswordEncoder passwordEncoder;

    private final JobPostingRepository jobPostingRepository;
    private final OsakaGeocodedRepository osakaGeocodedRepository;
    private final TokyoGeocodedRepository tokyoGeocodedRepository;
    private final CompanyRepository companyRepository;
    private final SeekerProfileRepository seekerProfileRepository;
    private final SeekerEducationRepository seekerEducationRepository;
    private final SeekerCareerRepository seekerCareerRepository;
    private final SeekerCertificateRepository seekerCertificateRepository;
    private final SeekerLanguageRepository seekerLanguageRepository;
    private final SeekerDesiredConditionRepository seekerDesiredConditionRepository;
    private final SeekerDocumentRepository seekerDocumentRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationRepository notificationRepository;
    private final ScrapRepository scrapRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScoutOfferRepository scoutOfferRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final ProfileImageRepository profileImageRepository;
    private final CompanyImageRepository companyImageRepository;
    private final ReportRepository reportRepository;

    /**
     * 구인자(Recruiter)의 신규 회원가입을 처리하고, 연관된 사업자 증빙 서류를 DB에 저장합니다.
     * 초기 가입 시 관리자의 승인이 필요하므로 활성화 상태(isActive)는 false로 고정됩니다.
     *
     * @param dto            구인자 회원가입 폼 데이터가 담긴 DTO
     * @param savedFileNames 스토리지에 저장된 증빙 서류의 파일명 리스트
     */
    public void joinRecruiter(JoinRecruiterDTO dto, List<String> savedFileNames) {
        UserEntity e = UserEntity.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .nickname(dto.getNickname())
                .nameKanjiMei(dto.getNameKanjiMei())
                .nameKanjiSei(dto.getNameKanjiSei())
                .nameKanaMei(dto.getNameKanaMei())
                .nameKanaSei(dto.getNameKanaSei())
                .gender("M".equals(dto.getGender()) ? Enum.Gender.MALE : Enum.Gender.FEMALE)
                .birthDate(dto.getBirthDate())
                .contact(dto.getContact())
                .zipCode(dto.getZipCode())
                .addressMain(dto.getAddressMain())
                .addressDetail(dto.getAddressDetail())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .addrPrefecture(dto.getAddrPrefecture())
                .addrCity(dto.getAddrCity())
                .addrTown(dto.getAddrTown())
                .role(Enum.UserRole.RECRUITER)
                .joinPath(dto.getJoinPath())
                .adReceive(dto.isAdReceive())
                .isActive(false)
                .build();

        UserEntity savedUser = userRepository.save(e);

        if (savedFileNames != null && !savedFileNames.isEmpty()) {
            for (String fileName : savedFileNames) {
                EvidenceFileEntity fileEntity = EvidenceFileEntity.builder()
                        .fileName(fileName)
                        .fileType("EVIDENCE")
                        .user(savedUser)
                        .build();

                fileRepository.save(fileEntity);
            }
        }
    }

    /**
     * 구직자(Seeker)의 신규 회원가입을 처리합니다.
     * 구직자는 별도의 가입 승인 절차가 없으므로 활성화 상태(isActive)가 true로 설정됩니다.
     *
     * @param dto 구직자 회원가입 폼 데이터가 담긴 DTO
     */
    public void insertSeeker(JoinSeekerDTO dto) {
        UserEntity e = UserEntity.builder()
                .role(Enum.UserRole.SEEKER)
                .birthDate(dto.getBirthDate())
                .password(passwordEncoder.encode(dto.getPassword()))
                .gender("M".equals(dto.getGender()) ? Enum.Gender.MALE : Enum.Gender.FEMALE)
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .nameKanjiSei(dto.getNameKanjiSei())
                .nameKanjiMei(dto.getNameKanjiMei())
                .nameKanaSei(dto.getNameKanaSei())
                .nameKanaMei(dto.getNameKanaMei())
                .contact(dto.getContact())
                .zipCode(dto.getZipCode())
                .addressMain(dto.getAddressMain())
                .addressDetail(dto.getAddressDetail())
                .addrPrefecture(dto.getAddrPrefecture())
                .addrCity(dto.getAddrCity())
                .addrTown(dto.getAddrTown())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .joinPath(dto.getJoinPath())
                .adReceive(dto.isAdReceive())
                .isActive(true)
                .build();

        userRepository.save(e);
    }

    /**
     * 회원가입 시 특정 이메일이 이미 시스템에 존재하는지 확인합니다.
     *
     * @param email 중복 검사할 이메일 주소
     * @return 중복 여부 (존재 시 true)
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 회원가입 시 특정 닉네임이 이미 시스템에 존재하는지 확인합니다.
     *
     * @param nickname 중복 검사할 닉네임
     * @return 중복 여부 (존재 시 true)
     */
    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    /**
     * 비밀번호 찾기 단계에서 입력된 정보(이름, 연락처, 이메일, 가입유형)가
     * DB의 계정 정보와 일치하는지(본인 인증)를 검증합니다.
     *
     * @param name    사용자의 한자 성+이름 (공백 무시)
     * @param contact 사용자의 연락처
     * @param email   검증 대상 이메일
     * @param role    계정 권한 유형 (SEEKER / RECRUITER)
     * @return 정보 일치 여부
     */
    public boolean emailVerify(String name, String contact, String email, String role) {
        Enum.UserRole userRole = Enum.UserRole.valueOf(role);
        String cleanName = name.replace(" ", "").replace("　", "");
        return userRepository.existsByEmailAndFullNameAndContactAndRole(
                email,
                cleanName,
                contact,
                userRole
        );
    }

    /**
     * 사용자의 실명, 연락처, 가입 유형을 기반으로 가입된 이메일 계정을 찾아 반환합니다.
     *
     * @param dto 계정 찾기에 필요한 식별 정보가 담긴 DTO
     * @return 일치하는 사용자의 이메일 (존재하지 않거나 파라미터가 유효하지 않을 경우 null)
     */
    public String findId(FindIdDTO dto) {
        String cleanName = dto.getName().replace(" ", "").replace("　", "");
        String cleanContact = dto.getContact().trim();
        Enum.UserRole role;
        try {
            role = Enum.UserRole.valueOf(dto.getRole());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
        return userRepository.findEmailByKanjiNameAndContact(cleanName, cleanContact, role)
                .orElse(null);
    }

    /**
     * 인증을 거친 사용자의 비밀번호를 새로운 암호로 재설정합니다.
     *
     * @param dto 재설정 대상 이메일과 신규 평문 비밀번호가 담긴 DTO
     * @throws IllegalArgumentException 해당 이메일을 가진 계정이 존재하지 않을 때 발생
     */
    @Transactional
    public void ChangeNewPW(ChangeNewPWDTO dto) {
        UserEntity entity = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        String encodedPassWord = passwordEncoder.encode(dto.getPassword());
        entity.setPassword(encodedPassWord);
    }

    /**
     * 사용자의 회원 탈퇴 요청을 처리합니다.
     * 계정 권한(구직자/구인자)에 따라 연관된 모든 하위 데이터(공고, 이력서, 지원 내역, 알림, 채팅 등)를
     * 데이터베이스 제약 조건 오류 없이 일괄 강제 삭제합니다.
     *
     * @param email       탈퇴 요청한 사용자의 이메일
     * @param rawPassword 본인 인증을 위한 평문 비밀번호
     * @return 비밀번호 일치 및 탈퇴 처리 성공 여부 (비밀번호 불일치 시 false)
     * @throws RuntimeException 사용자를 찾을 수 없을 때 발생
     */
    @Transactional
    public boolean deleteAccount(String email, String rawPassword) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            return false;
        }

        log.info("회원 탈퇴 데이터 일괄 삭제 시작: {}, ID: {}", email, user.getUserId());

        notificationRepository.deleteByUser(user);
        scrapRepository.deleteByUserId(user.getUserId());
        loginHistoryRepository.deleteByEmail(user.getEmail());
        profileImageRepository.deleteByUser(user);
        scheduleRepository.deleteByUser(user);
        reportRepository.deleteByReporter(user);

        chatMessageRepository.deleteBySender(user);
        chatRoomRepository.deleteBySeekerOrRecruiter(user, user);

        if (user.getRole() == Enum.UserRole.RECRUITER) {
            jobPostingRepository.deleteByUser(user);
            osakaGeocodedRepository.deleteByUser(user);
            tokyoGeocodedRepository.deleteByUser(user);
            scoutOfferRepository.deleteByRecruiter(user);
            companyImageRepository.deleteByUser(user);
            companyRepository.deleteByUser(user);
            fileRepository.deleteByUser(user);
        } else if (user.getRole() == Enum.UserRole.SEEKER) {
            applicationRepository.deleteBySeeker(user);
            scoutOfferRepository.deleteBySeeker(user);
            seekerEducationRepository.deleteByUser(user);
            seekerCareerRepository.deleteByUser(user);
            seekerCertificateRepository.deleteByUser(user);
            seekerLanguageRepository.deleteByUser(user);
            seekerDesiredConditionRepository.deleteByUser(user);
            seekerDocumentRepository.deleteByUser(user);
            seekerProfileRepository.deleteByUser(user);
        }

        userRepository.delete(user);

        log.info("회원 탈퇴 처리 및 연쇄 삭제 완료: {}", email);
        return true;
    }
}