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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LoginService {
	
	private final UserRepository userRepository;
	private final EvidenceFileRepository fileRepository;
	private final PasswordEncoder passwordEncoder;
	
	// 연쇄 삭제를 위한 추가 리포지토리 주입
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
	
	public void joinRecruiter(JoinRecruiterDTO dto, List<String> savedFileNames) {
		UserEntity e = UserEntity.builder()
				.email(dto.getEmail())
				.password(passwordEncoder.encode(dto.getPassword()))
				.nickname(dto.getNickname())
				.nameKanjiMei(dto.getNameKanjiMei())
				.nameKanjiSei(dto.getNameKanjiSei())
				.nameKanaMei(dto.getNameKanaMei())
				.nameKanaSei(dto.getNameKanaSei())
				.gender("M".equals(dto.getGender()) ? Enum.Gender.MALE : Enum.Gender.FEMALE )
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
	
	public void insertSeeker(JoinSeekerDTO dto) {
		UserEntity e= UserEntity.builder().
				role(Enum.UserRole.SEEKER).
				birthDate(dto.getBirthDate()).
				password(passwordEncoder.encode(dto.getPassword())).
				gender("M".equals(dto.getGender()) ? Enum.Gender.MALE : Enum.Gender.FEMALE ).
				email(dto.getEmail())
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
	
	public boolean existsByEmail(String email) {
		return userRepository.existsByEmail(email);
	}
	
	public boolean existsByNickname(String nickname) {
		return userRepository.existsByNickname(nickname);
	}
	
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
	
	@Transactional
	public void ChangeNewPW(ChangeNewPWDTO dto) {
		UserEntity entity = userRepository.findByEmail(dto.getEmail()).orElseThrow(()->new IllegalArgumentException("존재하지않은 회원"));
		String encodedPassWord = passwordEncoder.encode(dto.getPassword());
		entity.setPassword(encodedPassWord);
	}
	
	/**
	 * 회원 탈퇴 처리
	 * UserId와 연결된 모든 데이터를 강제로 삭제합니다.
	 */
	@Transactional
	public boolean deleteAccount(String email, String rawPassword) {
		UserEntity user = userRepository.findByEmail(email).orElseThrow(()-> new RuntimeException("회원없어요"));
		
		if(!passwordEncoder.matches(rawPassword, user.getPassword())){
			return false;
		}
		
		log.info("회원 탈퇴 데이터 삭제 시작: {}, ID: {}", email, user.getUserId());
		
		// 1. 공통 데이터 삭제
		notificationRepository.deleteByUser(user);
		scrapRepository.deleteByUserId(user.getUserId());
		loginHistoryRepository.deleteByEmail(user.getEmail());
		profileImageRepository.deleteByUser(user);
		scheduleRepository.deleteByUser(user);
		reportRepository.deleteByReporter(user);
		
		// 2. 채팅 관련 삭제 (메시지 -> 방 순서)
		chatMessageRepository.deleteBySender(user);
		chatRoomRepository.deleteBySeekerOrRecruiter(user, user);
		
		// 3. 역할별 데이터 강제 삭제
		if (user.getRole() == Enum.UserRole.RECRUITER) {
			// 구인자: 작성한 모든 공고(JobPosting, OsakaGeocoded, TokyoGeocoded) 삭제
			jobPostingRepository.deleteByUser(user);
			osakaGeocodedRepository.deleteByUser(user);
			tokyoGeocodedRepository.deleteByUser(user);
			
			// 보낸 스카우트 제의 삭제
			scoutOfferRepository.deleteByRecruiter(user);
			
			// 회사 정보 및 관련 이미지 삭제
			companyImageRepository.deleteByUser(user);
			companyRepository.deleteByUser(user);
			
			// 증빙 서류 삭제
			fileRepository.deleteByUser(user);
			
		} else if (user.getRole() == Enum.UserRole.SEEKER) {
			// 구직자: 지원 내역 삭제
			applicationRepository.deleteBySeeker(user);
			
			// 받은 스카우트 제의 삭제
			scoutOfferRepository.deleteBySeeker(user);
			
			// 이력서 및 모든 하위 정보(학력, 경력, 자격증 등) 삭제
			seekerEducationRepository.deleteByUser(user);
			seekerCareerRepository.deleteByUser(user);
			seekerCertificateRepository.deleteByUser(user);
			seekerLanguageRepository.deleteByUser(user);
			seekerDesiredConditionRepository.deleteByUser(user);
			seekerDocumentRepository.deleteByUser(user);
			seekerProfileRepository.deleteByUser(user);
		}
		
		// 4. 마지막으로 사용자 엔티티 삭제
		userRepository.delete(user);
		
		log.info("회원 탈퇴 처리 완료: {}", email);
		return true;
	}
}
