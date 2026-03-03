package net.kumo.kumo.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.JoinSeekerDTO;
import net.kumo.kumo.domain.dto.ResumeDto;
import net.kumo.kumo.domain.dto.SeekerMyPageDTO;
import net.kumo.kumo.domain.entity.*;
import net.kumo.kumo.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SeekerService {
    private final UserRepository userRepository;
    private final ProfileImageRepository profileImageRepository;
    private final SeekerProfileRepository profileRepo;
    private final SeekerDesiredConditionRepository conditionRepo;
    private final SeekerEducationRepository educationRepo;
    private final SeekerCareerRepository careerRepo;
    private final SeekerCertificateRepository certificateRepo;
    private final SeekerLanguageRepository languageRepo;
    private final SeekerDocumentRepository seekerDocumentRepository; // 🌟 증빙서류 레포지토리 추가

    @Value("${file.upload.dir}")
    private String uploadDir; // application.properties에서 가져옴 (C:/KumoUpload/)

    private final String EVIDENCE_FOLDER = "evidenceFiles/"; // 🌟 증빙서류 폴더명

    public SeekerMyPageDTO getDTO(String username) {
        UserEntity userEntity = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. email=" + username));
        return SeekerMyPageDTO.EntityToDto(userEntity);
    }

    public String updateProfileImage(String username, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        UserEntity userentity = userRepository.findByEmail(username).orElseThrow(() -> new IllegalArgumentException("해당유저없음"));

        String profileFolder = "profileImage/";
        String absolutePath = uploadDir + profileFolder;

        File folder = new File(absolutePath);
        if (!folder.exists()) { folder.mkdirs(); }

        String originalFileName = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String saveFileName = uuid + "_" + originalFileName;

        File saveFile = new File(absolutePath, saveFileName);
        file.transferTo(saveFile);

        String fileUrl = "/uploads/" + profileFolder + saveFileName;

        ProfileImageEntity existingImage = userentity.getProfileImage();
        if (existingImage != null) {
            File oldFile = new File(absolutePath, existingImage.getStoredFileName());
            if (oldFile.exists()) { oldFile.delete(); }
            existingImage.setOriginalFileName(file.getOriginalFilename());
            existingImage.setStoredFileName(saveFileName);
            existingImage.setFileUrl(fileUrl);
            existingImage.setFileSize(file.getSize());
        } else {
            ProfileImageEntity newImage = ProfileImageEntity.builder()
                    .originalFileName(file.getOriginalFilename())
                    .storedFileName(saveFileName)
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .user(userentity)
                    .build();
            userentity.setProfileImage(newImage);
            profileImageRepository.save(newImage);
        }
        return fileUrl;
		
		
    }

    public void updateProfile(JoinSeekerDTO dto) {
        UserEntity user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("해당 이메일을 가진 유저를 찾을 수 없습니다: " + dto.getEmail()));

        user.setNickname(dto.getNickname());
        user.setZipCode(dto.getZipCode());
        user.setAddressMain(dto.getAddressMain());
        user.setAddressDetail(dto.getAddressDetail());
        user.setAddrPrefecture(dto.getAddrPrefecture());
        user.setAddrCity(dto.getAddrCity());
        user.setAddrTown(dto.getAddrTown());
        user.setLatitude(dto.getLatitude());
        user.setLongitude(dto.getLongitude());

        userRepository.save(user);
    }

    @Transactional
    public void saveResume(ResumeDto dto, String username) {
        UserEntity user = userRepository.findByEmail(username).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // ==========================================
        // 0. 기존 데이터 삭제
        // ==========================================
        profileRepo.deleteByUser(user);
        conditionRepo.deleteByUser(user);
        profileRepo.flush();
        conditionRepo.flush();

        educationRepo.deleteByUser(user);
        careerRepo.deleteByUser(user);
        certificateRepo.deleteByUser(user);
        languageRepo.deleteByUser(user);
        seekerDocumentRepository.deleteByUser(user); // 🌟 기존 증빙서류 정보 삭제
        
        educationRepo.flush();
        careerRepo.flush();
        seekerDocumentRepository.flush();

        // ==========================================
        // 1. 프로필 기본 정보 저장
        // ==========================================
        SeekerProfileEntity profile = SeekerProfileEntity.builder()
                .user(user)
                .careerType(dto.getCareerType())
                .selfPr(dto.getSelfIntroduction())
                .contactPublic(dto.getContactPublic() != null ? dto.getContactPublic() : false)
                .isPublic(dto.getResumePublic() != null ? dto.getResumePublic() : false)
                .scoutAgree(dto.getScoutAgree() != null ? dto.getScoutAgree() : false)
                .build();
        profileRepo.save(profile);

        // ==========================================
        // 2. 희망근무조건 저장
        // ==========================================
        SeekerDesiredConditionEntity condition = SeekerDesiredConditionEntity.builder()
                .user(user)
                .locationPrefecture(dto.getDesiredLocation1())
                .locationWard(dto.getDesiredLocation2())
                .desiredJob(dto.getDesiredJob())
                .salaryType(dto.getSalaryType())
                .desiredSalary(dto.getDesiredSalary())
                .desiredPeriod(dto.getDesiredPeriod())
                .build();
        conditionRepo.save(condition);

        // ==========================================
        // 3. 학력사항 저장
        // ==========================================
        if (dto.getSchoolName() != null && !dto.getSchoolName().trim().isEmpty()) {
            SeekerEducationEntity education = SeekerEducationEntity.builder()
                    .user(user)
                    .educationLevel(dto.getEducationLevel())
                    .schoolName(dto.getSchoolName())
                    .status(dto.getEducationStatus())
                    .build();
            educationRepo.save(education);
        }

        // ==========================================
        // 4. 경력사항 저장
        // ==========================================
        if ("EXPERIENCED".equals(dto.getCareerType()) && dto.getCompanyName() != null) {
            List<SeekerCareerEntity> careerList = new ArrayList<>();
            int size = dto.getCompanyName().size();

            for (int i = 0; i < size; i++) {
                String compName = dto.getCompanyName().get(i);
                if (compName == null || compName.trim().isEmpty()) continue;

                if (i >= dto.getStartYear().size() || i >= dto.getEndYear().size()) break;

                LocalDate startDate = parseDate(dto.getStartYear().get(i), dto.getStartMonth().get(i));
                LocalDate endDate = parseDate(dto.getEndYear().get(i), dto.getEndMonth().get(i));

                if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                    log.warn("날짜 역전 감지 ({}): 시작일({})이 종료일({})보다 늦습니다. 교체합니다.", compName, startDate, endDate);
                    LocalDate temp = startDate; startDate = endDate; endDate = temp;
                }

                careerList.add(SeekerCareerEntity.builder()
                        .user(user)
                        .companyName(compName)
                        .startDate(startDate)
                        .endDate(endDate)
                        .description(dto.getJobDuties() != null && dto.getJobDuties().size() > i ? dto.getJobDuties().get(i) : "")
                        .build());
            }
            careerRepo.saveAll(careerList);
        }

        // ==========================================
        // 5. 자격증 저장
        // ==========================================
        if (dto.getCertName() != null) {
            List<SeekerCertificateEntity> certList = new ArrayList<>();
            for (int i = 0; i < dto.getCertName().size(); i++) {
                String certName = dto.getCertName().get(i);
                if (certName == null || certName.trim().isEmpty()) continue;

                String publisher = (dto.getCertPublisher() != null && dto.getCertPublisher().size() > i) ? dto.getCertPublisher().get(i) : "";
                String certYear = (dto.getCertYear() != null && dto.getCertYear().size() > i) ? dto.getCertYear().get(i) : "";

                certList.add(SeekerCertificateEntity.builder()
                        .user(user).certName(certName).issuer(publisher).acquisitionYear(certYear).build());
            }
            certificateRepo.saveAll(certList);
        }

        // ==========================================
        // 6. 어학 능력 저장
        // ==========================================
        if (dto.getLanguageName() != null) {
            List<SeekerLanguageEntity> langList = new ArrayList<>();
            for (int i = 0; i < dto.getLanguageName().size(); i++) {
                String langName = dto.getLanguageName().get(i);
                if (langName == null || langName.trim().isEmpty()) continue;

                String level = (dto.getLanguageLevel() != null && dto.getLanguageLevel().size() > i) ? dto.getLanguageLevel().get(i) : "BEGINNER";

                langList.add(SeekerLanguageEntity.builder()
                        .user(user).language(langName).level(level).build());
            }
            languageRepo.saveAll(langList);
        }

        // ==========================================
        // 7. 📁 증빙서류 저장 (파일 업로드)
        // ==========================================
        if (dto.getPortfolioFiles() != null && !dto.getPortfolioFiles().isEmpty()) {
            String absolutePath = uploadDir + EVIDENCE_FOLDER;
            File folder = new File(absolutePath);
            if (!folder.exists()) folder.mkdirs();

            List<SeekerDocumentEntity> seekerDocumentEntities = new ArrayList<>();
            for (MultipartFile file : dto.getPortfolioFiles()) {
                if (file == null || file.isEmpty()) continue;
                try {
                    String originalFileName = file.getOriginalFilename();
                    String uuid = UUID.randomUUID().toString();
                    String saveFileName = uuid + "_" + originalFileName;

                    File saveFile = new File(absolutePath, saveFileName);
                    file.transferTo(saveFile);
					
					// 가상 경로 URL 생성
					String fileUrl = "/uploads/" + EVIDENCE_FOLDER + saveFileName;

					seekerDocumentEntities.add(SeekerDocumentEntity.builder()
                            .fileName(originalFileName) // 원본명 저장
                            .fileUrl(fileUrl)           // 가상 경로 저장
                            .user(user)
                            .build());
                    log.info("증빙서류 저장 완료: {}", saveFileName);
                } catch (IOException e) {
                    log.error("파일 저장 실패: {}", e.getMessage());
                }
            }
            if (!seekerDocumentEntities.isEmpty()) {
                seekerDocumentRepository.saveAll(seekerDocumentEntities);
            }
        }

        log.info("== [ {} ] 님의 이력서 DB 저장 완료! ==", user.getNameKanjiMei());
    }

    private LocalDate parseDate(String year, String month) {
        try {
            if (year == null || year.trim().isEmpty() || month == null || month.trim().isEmpty()) return null;
            int m = Integer.parseInt(month);
            String dateStr = String.format("%s-%02d-01", year, m);
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.error("날짜 파싱 에러: {}년 {}월", year, month);
            return null;
        }
    }
}