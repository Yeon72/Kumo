package net.kumo.kumo.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.JoinSeekerDTO;
import net.kumo.kumo.domain.dto.ResumeDto;
import net.kumo.kumo.domain.dto.SeekerApplicationHistoryDTO;
import net.kumo.kumo.domain.dto.SeekerMyPageDTO;
import net.kumo.kumo.domain.entity.ApplicationEntity;
import net.kumo.kumo.domain.entity.ProfileImageEntity;
import net.kumo.kumo.domain.entity.ScoutOfferEntity;
import net.kumo.kumo.domain.entity.SeekerCareerEntity;
import net.kumo.kumo.domain.entity.SeekerCertificateEntity;
import net.kumo.kumo.domain.entity.SeekerDesiredConditionEntity;
import net.kumo.kumo.domain.entity.SeekerDocumentEntity;
import net.kumo.kumo.domain.entity.SeekerEducationEntity;
import net.kumo.kumo.domain.entity.SeekerLanguageEntity;
import net.kumo.kumo.domain.entity.SeekerProfileEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.ApplicationRepository;
import net.kumo.kumo.repository.JobPostingRepository;
import net.kumo.kumo.repository.OsakaGeocodedRepository;
import net.kumo.kumo.repository.OsakaNoGeocodedRepository;
import net.kumo.kumo.repository.ProfileImageRepository;
import net.kumo.kumo.repository.ScoutOfferRepository;
import net.kumo.kumo.repository.SeekerCareerRepository;
import net.kumo.kumo.repository.SeekerCertificateRepository;
import net.kumo.kumo.repository.SeekerDesiredConditionRepository;
import net.kumo.kumo.repository.SeekerDocumentRepository;
import net.kumo.kumo.repository.SeekerEducationRepository;
import net.kumo.kumo.repository.SeekerLanguageRepository;
import net.kumo.kumo.repository.SeekerProfileRepository;
import net.kumo.kumo.repository.TokyoGeocodedRepository;
import net.kumo.kumo.repository.TokyoNoGeocodedRepository;
import net.kumo.kumo.repository.UserRepository;

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
    private final SeekerDocumentRepository seekerDocumentRepository;
    private final ScoutOfferRepository scoutOfferRepo;
    private final ApplicationRepository applicationRepo;
    private final TokyoGeocodedRepository tokyoRepo;
    private final OsakaGeocodedRepository osakaRepo;
    private final TokyoNoGeocodedRepository tokyoNoRepo; // 🌟 추가
    private final OsakaNoGeocodedRepository osakaNoRepo; // 🌟 추가
    private final JobPostingRepository jobPostingRepo;

    /**
     * 구직자의 지원 내역 가져오기
     */
    public List<SeekerApplicationHistoryDTO> getApplicationHistory(String email) {
        UserEntity seeker = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        List<ApplicationEntity> applications = applicationRepo.findBySeekerOrderByAppliedAtDesc(seeker);
        List<SeekerApplicationHistoryDTO> history = new ArrayList<>();

        for (ApplicationEntity app : applications) {
            SeekerApplicationHistoryDTO dto = SeekerApplicationHistoryDTO.builder()
                    .appId(app.getId())
                    .targetSource(app.getTargetSource())
                    .targetPostId(app.getTargetPostId())
                    .appliedAt(app.getAppliedAt())
                    .status(app.getStatus().name())
                    .build();

            // 🌟 MapService와 동일하게 findById(PK) 기준으로 조회
            String source = app.getTargetSource().toUpperCase();
            Long postId = app.getTargetPostId();

            try {
                if ("TOKYO".equals(source)) {
                    tokyoRepo.findById(postId).ifPresent(job -> {
                        dto.setTitle(job.getTitle());
                        dto.setBusinessName(job.getCompanyName());
                        dto.setLocation(job.getAddress());
                        dto.setWage(job.getWage());
                        dto.setWageJp(job.getWageJp());
                        dto.setContact(job.getContactPhone());
                        dto.setManager("담당자");
                    });
                } else if ("OSAKA".equals(source)) {
                    osakaRepo.findById(postId).ifPresent(job -> {
                        dto.setTitle(job.getTitle());
                        dto.setBusinessName(job.getCompanyName());
                        dto.setLocation(job.getAddress());
                        dto.setWage(job.getWage());
                        dto.setWageJp(job.getWageJp());
                        dto.setContact(job.getContactPhone());
                        dto.setManager("담당자");
                    });
                } else if ("TOKYO_NO".equals(source)) {
                    tokyoNoRepo.findById(postId).ifPresent(job -> {
                        dto.setTitle(job.getTitle());
                        dto.setBusinessName(job.getCompanyName());
                        dto.setLocation(job.getAddress());
                        dto.setWage(job.getWage());
                        dto.setContact(job.getContactPhone());
                        dto.setManager("담당자");
                    });
                } else if ("OSAKA_NO".equals(source)) {
                    osakaNoRepo.findById(postId).ifPresent(job -> {
                        dto.setTitle(job.getTitle());
                        dto.setBusinessName(job.getCompanyName());
                        dto.setLocation(job.getAddress());
                        dto.setWage(job.getWage());
                        dto.setContact(job.getContactPhone());
                        dto.setManager("담당자");
                    });
                } else if ("KUMO".equals(source)) {
                    jobPostingRepo.findById(postId).ifPresent(job -> {
                        dto.setTitle(job.getTitle());
                        dto.setBusinessName(job.getCompany() != null ? job.getCompany().getBizName() : "-");
                        dto.setLocation(job.getWorkAddress());
                        dto.setWage(job.getSalaryAmount() != null ? job.getSalaryAmount().toString() : "-");
                        dto.setContact(job.getUser().getContact());
                        dto.setManager(job.getUser().getNickname());
                    });
                }
            } catch (Exception e) {
                log.error("지원 내역 상세 정보 로드 실패 (Source: {}, PostID: {}): {}", source, postId, e.getMessage());
                dto.setTitle("삭제된 공고입니다.");
            }

            history.add(dto);
        }
        return history;
    }

    /**
     * 지원 취소하기
     */
    @Transactional
    public void cancelApplication(Long appId, String email) {
        ApplicationEntity app = applicationRepo.findById(appId)
                .orElseThrow(() -> new RuntimeException("지원 내역을 찾을 수 없습니다."));

        if (!app.getSeeker().getEmail().equals(email)) {
            throw new RuntimeException("취소 권한이 없습니다.");
        }

        applicationRepo.delete(app);
    }

    /**
     * 구직자가 받은 스카우트 제안 목록 불러오기
     */
    public List<ScoutOfferEntity> getScoutOffers(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return scoutOfferRepo.findBySeekerOrderByCreatedAtDesc(user);
    }

    @Value("${file.upload.dir}")
    private String uploadDir; // application.properties에서 가져옴

    private final String EVIDENCE_FOLDER = "evidenceFiles/";

    public SeekerMyPageDTO getDTO(String username) {
        UserEntity userEntity = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. email=" + username));
        return SeekerMyPageDTO.EntityToDto(userEntity);
    }

    public String updateProfileImage(String username, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        UserEntity userentity = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("해당유저없음"));
        String profileFolder = "profileImage/";
        String absolutePath = uploadDir + profileFolder;
        File folder = new File(absolutePath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String originalFileName = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String saveFileName = uuid + "_" + originalFileName;
        File saveFile = new File(absolutePath, saveFileName);
        file.transferTo(saveFile);
        String fileUrl = "/uploads/" + profileFolder + saveFileName;
        ProfileImageEntity existingImage = userentity.getProfileImage();
        if (existingImage != null) {
            existingImage.setOriginalFileName(file.getOriginalFilename());
            existingImage.setStoredFileName(saveFileName);
            existingImage.setFileUrl(fileUrl);
            existingImage.setFileSize(file.getSize());
        } else {
            ProfileImageEntity newImage = ProfileImageEntity.builder()
                    .originalFileName(file.getOriginalFilename()).storedFileName(saveFileName)
                    .fileUrl(fileUrl).fileSize(file.getSize()).user(userentity).build();
            userentity.setProfileImage(newImage);
            profileImageRepository.save(newImage);
        }
        return fileUrl;
    }

    public void updateProfile(JoinSeekerDTO dto) {
        UserEntity user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다."));
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResumeDto getResume(String username) {
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        SeekerProfileEntity profile = profileRepo.findByUser_UserId(user.getUserId()).orElse(null);
        SeekerDesiredConditionEntity condition = conditionRepo.findByUser_UserId(user.getUserId()).orElse(null);
        SeekerEducationEntity education = educationRepo.findByUser_UserId(user.getUserId());
        List<SeekerCareerEntity> careers = careerRepo.findByUser_UserId(user.getUserId());
        List<SeekerCertificateEntity> certificates = certificateRepo.findByUser_UserId(user.getUserId());
        List<SeekerLanguageEntity> languages = languageRepo.findByUser_UserId(user.getUserId());
        List<SeekerDocumentEntity> documents = seekerDocumentRepository.findByUser_UserId(user.getUserId());

        ResumeDto dto = new ResumeDto();
        if (profile != null) {
            dto.setCareerType(profile.getCareerType());
            dto.setSelfIntroduction(profile.getSelfPr());
            dto.setContactPublic(profile.getContactPublic());
            dto.setResumePublic(profile.getIsPublic());
            dto.setScoutAgree(profile.getScoutAgree());
        }
        if (condition != null) {
            dto.setDesiredLocation1(condition.getLocationPrefecture());
            dto.setDesiredLocation2(condition.getLocationWard());
            dto.setDesiredJob(condition.getDesiredJob());
            dto.setSalaryType(condition.getSalaryType());
            dto.setDesiredSalary(condition.getDesiredSalary());
            dto.setDesiredPeriod(condition.getDesiredPeriod());
        }
        if (education != null) {
            dto.setEducationLevel(education.getEducationLevel());
            dto.setEducationStatus(education.getStatus());
            dto.setSchoolName(education.getSchoolName());
        }
        if (careers != null && !careers.isEmpty()) {
            dto.setCompanyName(new ArrayList<>());
            dto.setStartYear(new ArrayList<>());
            dto.setStartMonth(new ArrayList<>());
            dto.setEndYear(new ArrayList<>());
            dto.setEndMonth(new ArrayList<>());
            dto.setJobDuties(new ArrayList<>());
            for (SeekerCareerEntity career : careers) {
                dto.getCompanyName().add(career.getCompanyName());
                if (career.getStartDate() != null) {
                    dto.getStartYear().add(String.valueOf(career.getStartDate().getYear()));
                    dto.getStartMonth().add(String.format("%02d", career.getStartDate().getMonthValue()));
                } else {
                    dto.getStartYear().add("");
                    dto.getStartMonth().add("");
                }
                if (career.getEndDate() != null) {
                    dto.getEndYear().add(String.valueOf(career.getEndDate().getYear()));
                    dto.getEndMonth().add(String.format("%02d", career.getEndDate().getMonthValue()));
                } else {
                    dto.getEndYear().add("");
                    dto.getEndMonth().add("");
                }
                dto.getJobDuties().add(career.getDescription());
            }
        }
        if (certificates != null && !certificates.isEmpty()) {
            dto.setCertName(new ArrayList<>());
            dto.setCertPublisher(new ArrayList<>());
            dto.setCertYear(new ArrayList<>());
            for (SeekerCertificateEntity cert : certificates) {
                dto.getCertName().add(cert.getCertName());
                dto.getCertPublisher().add(cert.getIssuer());
                dto.getCertYear().add(cert.getAcquisitionYear());
            }
        }
        if (languages != null && !languages.isEmpty()) {
            dto.setLanguageName(new ArrayList<>());
            dto.setLanguageLevel(new ArrayList<>());
            for (SeekerLanguageEntity lang : languages) {
                dto.getLanguageName().add(lang.getLanguage());
                dto.getLanguageLevel().add(lang.getLevel());
            }
        }
        if (documents != null && !documents.isEmpty()) {
            dto.setPortfolioFileUrls(new ArrayList<>());
            for (SeekerDocumentEntity doc : documents) {
                dto.getPortfolioFileUrls().add(doc.getFileUrl());
            }
        }
        return dto;
    }

    @Transactional
    public void saveResume(ResumeDto dto, String username) {
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        profileRepo.deleteByUser(user);
        conditionRepo.deleteByUser(user);
        educationRepo.deleteByUser(user);
        careerRepo.deleteByUser(user);
        certificateRepo.deleteByUser(user);
        languageRepo.deleteByUser(user);
        boolean hasNewFiles = dto.getPortfolioFiles() != null && !dto.getPortfolioFiles().isEmpty()
                && dto.getPortfolioFiles().stream().anyMatch(f -> !f.isEmpty());
        if (hasNewFiles) {
            seekerDocumentRepository.deleteByUser(user);
        }
        profileRepo.flush();
        conditionRepo.flush();
        educationRepo.flush();
        careerRepo.flush();
        seekerDocumentRepository.flush();

        SeekerProfileEntity profile = SeekerProfileEntity.builder()
                .user(user).careerType(dto.getCareerType()).selfPr(dto.getSelfIntroduction())
                .contactPublic(dto.getContactPublic() != null ? dto.getContactPublic() : false)
                .isPublic(dto.getResumePublic() != null ? dto.getResumePublic() : false)
                .scoutAgree(dto.getScoutAgree() != null ? dto.getScoutAgree() : false).build();
        profileRepo.save(profile);

        SeekerDesiredConditionEntity condition = SeekerDesiredConditionEntity.builder()
                .user(user).locationPrefecture(dto.getDesiredLocation1()).locationWard(dto.getDesiredLocation2())
                .desiredJob(dto.getDesiredJob()).salaryType(dto.getSalaryType())
                .desiredSalary(dto.getDesiredSalary()).desiredPeriod(dto.getDesiredPeriod()).build();
        conditionRepo.save(condition);

        if (dto.getSchoolName() != null && !dto.getSchoolName().trim().isEmpty()) {
            educationRepo.save(SeekerEducationEntity.builder().user(user).educationLevel(dto.getEducationLevel())
                    .schoolName(dto.getSchoolName()).status(dto.getEducationStatus()).build());
        }

        if ("EXPERIENCED".equals(dto.getCareerType()) && dto.getCompanyName() != null) {
            for (int i = 0; i < dto.getCompanyName().size(); i++) {
                String compName = dto.getCompanyName().get(i);
                if (compName == null || compName.trim().isEmpty())
                    continue;
                LocalDate sd = parseDate(dto.getStartYear().get(i), dto.getStartMonth().get(i));
                LocalDate ed = parseDate(dto.getEndYear().get(i), dto.getEndMonth().get(i));
                careerRepo.save(SeekerCareerEntity.builder().user(user).companyName(compName).startDate(sd).endDate(ed)
                        .description(dto.getJobDuties().get(i)).build());
            }
        }

        if (dto.getCertName() != null) {
            for (int i = 0; i < dto.getCertName().size(); i++) {
                if (dto.getCertName().get(i) == null || dto.getCertName().get(i).trim().isEmpty())
                    continue;
                certificateRepo.save(SeekerCertificateEntity.builder().user(user).certName(dto.getCertName().get(i))
                        .issuer(dto.getCertPublisher().get(i)).acquisitionYear(dto.getCertYear().get(i)).build());
            }
        }

        if (dto.getLanguageName() != null) {
            for (int i = 0; i < dto.getLanguageName().size(); i++) {
                if (dto.getLanguageName().get(i) == null || dto.getLanguageName().get(i).trim().isEmpty())
                    continue;
                languageRepo.save(SeekerLanguageEntity.builder().user(user).language(dto.getLanguageName().get(i))
                        .level(dto.getLanguageLevel().get(i)).build());
            }
        }

        if (dto.getPortfolioFiles() != null && !dto.getPortfolioFiles().isEmpty()) {
            String ap = uploadDir + EVIDENCE_FOLDER;
            File f = new File(ap);
            if (!f.exists())
                f.mkdirs();
            for (MultipartFile file : dto.getPortfolioFiles()) {
                if (file == null || file.isEmpty())
                    continue;
                try {
                    String sfn = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                    file.transferTo(new File(ap, sfn));
                    seekerDocumentRepository.save(SeekerDocumentEntity.builder().fileName(file.getOriginalFilename())
                            .fileUrl("/uploads/" + EVIDENCE_FOLDER + sfn).user(user).build());
                } catch (IOException e) {
                    log.error("저장실패: {}", e.getMessage());
                }
            }
        }
    }

    private LocalDate parseDate(String year, String month) {
        try {
            if (year == null || year.isEmpty() || month == null || month.isEmpty())
                return null;
            return LocalDate.parse(String.format("%s-%02d-01", year, Integer.parseInt(month)));
        } catch (Exception e) {
            return null;
        }
    }
}
