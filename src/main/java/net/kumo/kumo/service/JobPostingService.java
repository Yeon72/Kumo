package net.kumo.kumo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import net.kumo.kumo.domain.dto.ApplicationDTO;
import net.kumo.kumo.domain.dto.JobApplicantGroupDTO;
import net.kumo.kumo.domain.dto.JobManageListDTO;
import net.kumo.kumo.domain.dto.JobPostingRequestDTO;
import net.kumo.kumo.domain.dto.ResumeResponseDTO;
import net.kumo.kumo.domain.entity.ApplicationEntity;
import net.kumo.kumo.domain.entity.CompanyEntity;
import net.kumo.kumo.domain.entity.OsakaGeocodedEntity;
import net.kumo.kumo.domain.entity.SeekerCareerEntity;
import net.kumo.kumo.domain.entity.SeekerCertificateEntity;
import net.kumo.kumo.domain.entity.SeekerDesiredConditionEntity;
import net.kumo.kumo.domain.entity.SeekerDocumentEntity;
import net.kumo.kumo.domain.entity.SeekerEducationEntity;
import net.kumo.kumo.domain.entity.SeekerLanguageEntity;
import net.kumo.kumo.domain.entity.SeekerProfileEntity;
import net.kumo.kumo.domain.entity.TokyoGeocodedEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.domain.enums.JobStatus;
import net.kumo.kumo.repository.ApplicationRepository;
import net.kumo.kumo.repository.CompanyRepository;
import net.kumo.kumo.repository.OsakaGeocodedRepository;
import net.kumo.kumo.repository.SeekerCareerRepository;
import net.kumo.kumo.repository.SeekerCertificateRepository;
import net.kumo.kumo.repository.SeekerDesiredConditionRepository;
import net.kumo.kumo.repository.SeekerDocumentRepository;
import net.kumo.kumo.repository.SeekerEducationRepository;
import net.kumo.kumo.repository.SeekerLanguageRepository;
import net.kumo.kumo.repository.SeekerProfileRepository;
import net.kumo.kumo.repository.TokyoGeocodedRepository;
import net.kumo.kumo.repository.UserRepository;

/**
 * 구인자가 직접 시스템 내부에 등록하는 로컬 구인 공고(Job Posting)의 생성, 조회, 수정, 삭제 및
 * 관련 지원자 관리 비즈니스 로직을 총괄하는 서비스 클래스입니다.
 * 다국어 파싱 및 지역별(도쿄/오사카) 데이터베이스 테이블 분기 처리 로직을 포함합니다.
 */
@Service
@RequiredArgsConstructor
public class JobPostingService {

    private final OsakaGeocodedRepository osakaGeocodedRepository;
    private final TokyoGeocodedRepository tokyoGeocodedRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    private final SeekerProfileRepository seekerProfileRepository;
    private final SeekerDesiredConditionRepository conditionRepository;
    private final SeekerCareerRepository careerRepository;
    private final SeekerEducationRepository educationRepository;
    private final SeekerCertificateRepository certificateRepository;
    private final SeekerLanguageRepository languageRepository;
    private final SeekerDocumentRepository documentRepository;
    private final NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Value("${file.upload.dir}")
    private String uploadDir;

    /**
     * 클라이언트가 업로드한 이미지 파일을 서버 스토리지에 저장하고, 웹 접근 경로를 반환합니다.
     *
     * @param file 업로드된 다중 파트 파일 객체
     * @return 저장된 파일의 웹 URL 경로 (실패 시 null 반환)
     */
    private String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        try {
            java.io.File directory = new java.io.File(uploadDir);
            if (!directory.exists()) directory.mkdirs();

            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String savedName = java.util.UUID.randomUUID().toString() + extension;

            java.io.File dest = new java.io.File(uploadDir + savedName);
            file.transferTo(dest);

            return "/images/uploadFile/" + savedName;
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 구인자가 작성한 신규 구인 공고를 데이터베이스에 등록합니다.
     * 첨부 이미지 저장 및 주소 체계에 따른 도쿄/오사카 지역 분기 처리를 수행합니다.
     *
     * @param dto    공고 폼 데이터가 담긴 DTO
     * @param images 첨부된 다중 이미지 파일 목록
     * @param user   작성자(구인자) 엔티티
     */
    @Transactional
    public void saveJobPosting(JobPostingRequestDTO dto, List<MultipartFile> images, UserEntity user) {

        CompanyEntity company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다. ID: " + dto.getCompanyId()));

        String companyName = company.getBizName();
        String address = (company.getAddressMain() != null ? company.getAddressMain() : "")
                + (company.getAddressDetail() != null ? " " + company.getAddressDetail() : "");
        Double lat = company.getLatitude() != null ? company.getLatitude().doubleValue() : 0.0;
        Double lng = company.getLongitude() != null ? company.getLongitude().doubleValue() : 0.0;

        String prefJp = company.getAddrPrefecture();
        String cityJp = company.getAddrCity();
        String wardJp = company.getAddrTown();

        String imgUrls = "";
        if (images != null && !images.isEmpty()) {
            imgUrls = images.stream()
                    .filter(f -> !f.isEmpty())
                    .map(this::saveImage)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.joining(","));
        }

        String salaryType;
        String salaryTypeJp;

        switch (dto.getSalaryType()) {
            case "HOURLY" -> { salaryType = "시급"; salaryTypeJp = "時給"; }
            case "DAILY" -> { salaryType = "일급"; salaryTypeJp = "日給"; }
            case "MONTHLY" -> { salaryType = "월급"; salaryTypeJp = "月給"; }
            case "SALARY" -> { salaryType = "연봉"; salaryTypeJp = "年収"; }
            default -> { salaryType = "미정"; salaryTypeJp = "未定"; }
        }

        String wage = (dto.getSalaryAmount() != null) ? salaryType + " " + dto.getSalaryAmount() + "엔" : "";
        String wageJp = (dto.getSalaryAmount() != null) ? salaryTypeJp + " " + dto.getSalaryAmount() + "円" : "";

        long datanum = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        java.time.format.DateTimeFormatter writeTimeFormatter = java.time.format.DateTimeFormatter
                .ofPattern("yy.MM.dd");
        String writeTime = now.format(writeTimeFormatter);

        if ("東京都".equals(prefJp)) {
            saveToTokyo(dto, user, company, companyName, address, lat, lng, prefJp, cityJp, wardJp, imgUrls, wage,
                    wageJp, datanum, now, writeTime);
        } else {
            saveToOsaka(dto, user, company, companyName, address, lat, lng, prefJp, cityJp, wardJp, imgUrls, wage,
                    wageJp, datanum, now, writeTime);
        }
    }

    private void saveToOsaka(JobPostingRequestDTO dto, UserEntity user, CompanyEntity company, String companyName,
                             String address, Double lat, Double lng, String prefJp, String cityJp, String wardJp, String imgUrls,
                             String wage, String wageJp, long datanum, LocalDateTime now, String writeTime) {
        Integer maxNo = osakaGeocodedRepository.findMaxRowNo();
        Integer nextRowNo = (maxNo == null) ? 1 : maxNo + 1;

        OsakaGeocodedEntity entity = new OsakaGeocodedEntity();
        entity.setCreatedAt(now);
        entity.setWriteTime(writeTime);
        entity.setUser(user);
        entity.setCompanyName(companyName);
        entity.setCompany(company);
        entity.setAddress(address);
        entity.setLat(lat);
        entity.setLng(lng);
        entity.setPrefectureJp(prefJp);
        entity.setCityJp(cityJp);
        entity.setWardJp(wardJp);

        entity.setSalaryType(dto.getSalaryType());
        entity.setSalaryAmount(dto.getSalaryAmount());

        entity.setRowNo(nextRowNo);
        entity.setDatanum(datanum);
        entity.setTitle(dto.getTitle());
        entity.setContactPhone(dto.getContactPhone());
        entity.setHref("/Recruiter/posting/" + datanum);
        entity.setPosition(dto.getPosition());
        entity.setJobDescription(dto.getJobDescription());
        entity.setNotes(dto.getNotes());
        entity.setWage(wage);
        entity.setWageJp(wageJp);
        entity.setImgUrls(imgUrls.isEmpty() ? null : imgUrls);
        entity.setStatus(JobStatus.RECRUITING);

        parseAddressToSixColumnsOsaka(entity, address);
        osakaGeocodedRepository.save(entity);
    }

    private void saveToTokyo(JobPostingRequestDTO dto, UserEntity user, CompanyEntity company, String companyName,
                             String address, Double lat, Double lng, String prefJp, String cityJp, String wardJp, String imgUrls,
                             String wage, String wageJp, long datanum, LocalDateTime now, String writeTime) {
        Integer maxNo = tokyoGeocodedRepository.findMaxRowNo();
        Integer nextRowNo = (maxNo == null) ? 1 : maxNo + 1;

        TokyoGeocodedEntity entity = new TokyoGeocodedEntity();
        entity.setCreatedAt(now);
        entity.setWriteTime(writeTime);
        entity.setUser(user);
        entity.setCompanyName(companyName);
        entity.setCompany(company);
        entity.setAddress(address);
        entity.setLat(lat);
        entity.setLng(lng);
        entity.setPrefectureJp(prefJp);

        entity.setSalaryType(dto.getSalaryType());
        entity.setSalaryAmount(dto.getSalaryAmount());

        entity.setRowNo(nextRowNo);
        entity.setDatanum(datanum);
        entity.setTitle(dto.getTitle());
        entity.setContactPhone(dto.getContactPhone());
        entity.setHref("/Recruiter/posting/" + datanum);
        entity.setPosition(dto.getPosition());
        entity.setJobDescription(dto.getJobDescription());
        entity.setNotes(dto.getNotes());
        entity.setWage(wage);
        entity.setWageJp(wageJp);
        entity.setImgUrls(imgUrls.isEmpty() ? null : imgUrls);
        entity.setStatus(JobStatus.RECRUITING);

        parseAddressToSixColumnsTokyo(entity, address);
        tokyoGeocodedRepository.save(entity);
    }

    private void parseAddressToSixColumnsOsaka(OsakaGeocodedEntity entity, String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank())
            return;
        String[] parts = fullAddress.split("\\s+");
        String prefJp = null, cityJp = null, wardJp = null;

        for (String part : parts) {
            if (part.endsWith("府") || part.endsWith("県"))
                prefJp = part;
            else if (part.endsWith("市"))
                cityJp = part;
            else if (part.endsWith("区"))
                wardJp = part;
        }

        entity.setPrefectureJp(prefJp);
        entity.setCityJp(cityJp);
        entity.setWardJp(wardJp);

        if ("大阪府".equals(prefJp))
            entity.setPrefectureKr("오사카부");
        if ("大阪市".equals(cityJp))
            entity.setCityKr("오사카시");
        if (wardJp != null) {
            Map<String, String> wardMap = Map.of("中央区", "주오구", "浪速区", "나니와구", "北区", "기타구");
            entity.setWardKr(wardMap.getOrDefault(wardJp, wardJp));
        }
    }

    private void parseAddressToSixColumnsTokyo(TokyoGeocodedEntity entity, String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank())
            return;
        String[] parts = fullAddress.split("\\s+");
        String prefJp = null, cityJp = null, wardJp = null;

        for (String part : parts) {
            if (part.endsWith("都"))
                prefJp = part;
            else if (part.endsWith("市"))
                cityJp = part;
            else if (part.endsWith("区"))
                wardJp = part;
        }

        entity.setPrefectureJp(prefJp);
        entity.setWardCityJp(wardJp != null ? wardJp : cityJp);

        if ("東京都".equals(prefJp))
            entity.setPrefectureKr("도쿄도");

        if (wardJp != null) {
            Map<String, String> tokyoMap = Map.ofEntries(
                    Map.entry("千代田区", "지요다구"),
                    Map.entry("中央区", "주오구"),
                    Map.entry("港区", "미나토구"),
                    Map.entry("新宿区", "신주쿠구"),
                    Map.entry("文京区", "분쿄구"),
                    Map.entry("台東区", "다이토구"),
                    Map.entry("墨田区", "스미다구"),
                    Map.entry("江東区", "고토구"),
                    Map.entry("品川区", "시나가와구"),
                    Map.entry("目黒区", "메구로구"),
                    Map.entry("大田区", "오타구"),
                    Map.entry("世田谷区", "세타가야구"),
                    Map.entry("渋谷区", "시부야구"),
                    Map.entry("中野区", "나카노구"),
                    Map.entry("杉並区", "스기나미구"),
                    Map.entry("豊島区", "도시마구"),
                    Map.entry("北区", "기타구"),
                    Map.entry("荒川区", "아라카와구"),
                    Map.entry("板橋区", "이타바시구"),
                    Map.entry("練馬区", "네리마구"),
                    Map.entry("足立区", "아다치구"),
                    Map.entry("葛飾区", "가쓰시카구"),
                    Map.entry("江戸川区", "에도가와구"),
                    Map.entry("八王子市", "하치오지시"),
                    Map.entry("町田市", "마치다시"));

            entity.setWardCityKr(tokyoMap.getOrDefault(wardJp, wardJp));
        }
    }

    /**
     * 특정 사용자(구인자)가 등록한 전체 공고 목록을 지역 구분 없이 통합하여 반환합니다.
     * 모집 상태(진행중 우선) 및 등록일(최신순)을 기준으로 정렬됩니다.
     *
     * @param email 작성자(구인자)의 이메일 계정
     * @return 통합 및 정렬된 구인 공고 DTO 리스트
     */
    public List<JobManageListDTO> getMyJobPostings(String email) {
        List<JobManageListDTO> result = new java.util.ArrayList<>();

        List<OsakaGeocodedEntity> osakaJobs = osakaGeocodedRepository.findByUser_Email(email);
        for (OsakaGeocodedEntity o : osakaJobs) {

            String displayWage = o.getWage() != null ? o.getWage()
                    .replace("HOURLY", "시급")
                    .replace("DAILY", "일급")
                    .replace("MONTHLY", "월급")
                    .replace("SALARY", "연봉") : "";

            result.add(JobManageListDTO.builder()
                    .id(o.getId())
                    .datanum(o.getDatanum())
                    .title(o.getTitle())
                    .titleJp(o.getTitleJp())
                    .regionType("오사카")
                    .wage(displayWage)
                    .wageJp(o.getWageJp())
                    .createdAt(o.getCreatedAt())
                    .status(o.getStatus() != null ? o.getStatus().name() : "RECRUITING")
                    .build());
        }

        List<TokyoGeocodedEntity> tokyoJobs = tokyoGeocodedRepository.findByUser_Email(email);
        for (TokyoGeocodedEntity t : tokyoJobs) {

            String displayWage = t.getWage() != null ? t.getWage()
                    .replace("HOURLY", "시급")
                    .replace("DAILY", "일급")
                    .replace("MONTHLY", "월급")
                    .replace("SALARY", "연봉") : "";

            result.add(JobManageListDTO.builder()
                    .id(t.getId())
                    .datanum(t.getDatanum())
                    .title(t.getTitle())
                    .titleJp(t.getTitleJp())
                    .regionType("도쿄")
                    .wage(displayWage)
                    .wageJp(t.getWageJp())
                    .createdAt(t.getCreatedAt())
                    .status(t.getStatus() != null ? t.getStatus().name() : "RECRUITING")
                    .build());
        }

        result.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        result.sort((a, b) -> {
            if (!a.getStatus().equals(b.getStatus())) {
                return a.getStatus().equals("RECRUITING") ? -1 : 1;
            }

            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        return result;
    }

    /**
     * 사용자가 등록한 특정 구인 공고를 삭제합니다. (보안 검증 포함)
     * * @param datanum 삭제할 공고의 고유 번호
     * @param region  삭제 대상 공고의 소속 지역 (TOKYO 또는 OSAKA)
     * @param email   삭제를 요청한 사용자의 이메일 계정 (검증용)
     * @throws IllegalStateException 삭제 권한이 일치하지 않을 때 발생
     */
    @Transactional
    public void deleteMyJobPosting(Long datanum, String region, String email) {
        if ("TOKYO".equalsIgnoreCase(region)) {
            TokyoGeocodedEntity entity = tokyoGeocodedRepository.findByDatanum(datanum)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도쿄 공고입니다. (datanum: " + datanum + ")"));

            if (!entity.getUser().getEmail().equals(email)) {
                throw new IllegalStateException("해당 공고를 삭제할 권한이 없습니다.");
            }

            tokyoGeocodedRepository.delete(entity);

        } else if ("OSAKA".equalsIgnoreCase(region)) {
            OsakaGeocodedEntity entity = osakaGeocodedRepository.findByDatanum(datanum)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 오사카 공고입니다. (datanum: " + datanum + ")"));

            if (!entity.getUser().getEmail().equals(email)) {
                throw new IllegalStateException("해당 공고를 삭제할 권한이 없습니다.");
            }

            osakaGeocodedRepository.delete(entity);

        } else {
            throw new IllegalArgumentException("알 수 없는 지역 정보입니다: " + region);
        }
    }

    /**
     * 공고 수정 화면을 렌더링하기 위해 특정 공고의 상세 데이터를 조회합니다.
     *
     * @param id     조회할 공고의 식별자(PK)
     * @param region 조회 대상 공고의 소속 지역 (TOKYO 또는 OSAKA)
     * @return 렌더링에 사용될 공고 데이터 DTO
     */
    public JobPostingRequestDTO getJobPostingForEdit(Long id, String region) {
        JobPostingRequestDTO dto = new JobPostingRequestDTO();

        if ("TOKYO".equalsIgnoreCase(region)) {
            TokyoGeocodedEntity e = tokyoGeocodedRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("공고를 찾을 수 없습니다."));
            dto.setDatanum(e.getDatanum());
            dto.setTitle(e.getTitle());
            dto.setTitleJp(e.getTitleJp());
            dto.setPosition(e.getPosition());
            dto.setPositionJp(e.getPositionJp());
            dto.setContactPhone(e.getContactPhone());
            dto.setContactPhoneJp(e.getContactPhoneJp());
            dto.setJobDescription(e.getJobDescription());
            dto.setJobDescriptionJp(e.getJobDescriptionJp());
            dto.setNotes(e.getNotes());
            dto.setNotesJp(e.getNotesJp());
            dto.setSalaryType(e.getSalaryType());
            dto.setSalaryAmount(e.getSalaryAmount());
            if (e.getCompany() != null)
                dto.setCompanyId(e.getCompany().getCompanyId());

        } else {
            OsakaGeocodedEntity e = osakaGeocodedRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("공고를 찾을 수 없습니다."));
            dto.setDatanum(e.getDatanum());
            dto.setTitle(e.getTitle());
            dto.setTitleJp(e.getTitleJp());
            dto.setPosition(e.getPosition());
            dto.setPositionJp(e.getPositionJp());
            dto.setContactPhone(e.getContactPhone());
            dto.setContactPhoneJp(e.getContactPhoneJp());
            dto.setJobDescription(e.getJobDescription());
            dto.setJobDescriptionJp(e.getJobDescriptionJp());
            dto.setNotes(e.getNotes());
            dto.setNotesJp(e.getNotesJp());
            dto.setSalaryType(e.getSalaryType());
            dto.setSalaryAmount(e.getSalaryAmount());
            if (e.getCompany() != null)
                dto.setCompanyId(e.getCompany().getCompanyId());
        }

        return dto;
    }

    /**
     * 사용자가 수정한 구인 공고 데이터를 데이터베이스에 반영합니다.
     *
     * @param id     수정 대상 공고의 식별자(PK)
     * @param region 수정 대상 공고의 소속 지역 (TOKYO 또는 OSAKA)
     * @param dto    수정된 공고 폼 데이터 DTO
     * @param images 추가로 업로드된 신규 이미지 파일 리스트
     */
    @Transactional
    public void updateJobPosting(Long id, String region, JobPostingRequestDTO dto, List<MultipartFile> images) {
        String imgUrls = null;
        if (images != null) {
            String joined = images.stream()
                    .filter(f -> !f.isEmpty())
                    .map(f -> "/uploads/" + f.getOriginalFilename())
                    .collect(Collectors.joining(","));
            if (!joined.isEmpty())
                imgUrls = joined;
        }

        String salaryLabel = switch (dto.getSalaryType() != null ? dto.getSalaryType() : "") {
            case "HOURLY" -> "시급";
            case "DAILY" -> "일급";
            case "MONTHLY" -> "월급";
            case "SALARY" -> "연봉";
            default -> "미정";
        };
        String salaryLabelJp = switch (dto.getSalaryType() != null ? dto.getSalaryType() : "") {
            case "HOURLY" -> "時給";
            case "DAILY" -> "日給";
            case "MONTHLY" -> "月給";
            case "SALARY" -> "年収";
            default -> "未定";
        };
        String wage = dto.getSalaryAmount() != null ? salaryLabel + " " + dto.getSalaryAmount() + "엔" : "";
        String wageJp = dto.getSalaryAmount() != null ? salaryLabelJp + " " + dto.getSalaryAmount() + "円" : "";

        if ("TOKYO".equalsIgnoreCase(region)) {
            TokyoGeocodedEntity e = tokyoGeocodedRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("공고를 찾을 수 없습니다."));
            e.setTitle(dto.getTitle());
            e.setPosition(dto.getPosition());
            e.setContactPhone(dto.getContactPhone());
            e.setJobDescription(dto.getJobDescription());
            e.setNotes(dto.getNotes());
            e.setSalaryType(dto.getSalaryType());
            e.setSalaryAmount(dto.getSalaryAmount());
            e.setWage(wage);
            e.setWageJp(wageJp);
            if (imgUrls != null)
                e.setImgUrls(imgUrls);
            if (dto.getCompanyId() != null)
                companyRepository.findById(dto.getCompanyId()).ifPresent(e::setCompany);

        } else {
            OsakaGeocodedEntity e = osakaGeocodedRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("공고를 찾을 수 없습니다."));
            e.setTitle(dto.getTitle());
            e.setPosition(dto.getPosition());
            e.setContactPhone(dto.getContactPhone());
            e.setJobDescription(dto.getJobDescription());
            e.setNotes(dto.getNotes());
            e.setSalaryType(dto.getSalaryType());
            e.setSalaryAmount(dto.getSalaryAmount());
            e.setWage(wage);
            e.setWageJp(wageJp);
            if (imgUrls != null)
                e.setImgUrls(imgUrls);
            if (dto.getCompanyId() != null)
                companyRepository.findById(dto.getCompanyId()).ifPresent(e::setCompany);
        }
    }

    /**
     * 특정 구인 공고의 모집 상태를 강제로 마감(CLOSED) 처리합니다.
     * 마감 처리 시, 해당 공고에 지원 중이던 모든 지원자에게 시스템 알림이 발송됩니다.
     *
     * @param datanum 마감 처리할 공고의 고유 번호
     * @param region  해당 공고의 지역 출처
     */
    @Transactional
    public void closeJobPosting(Long datanum, String region) {
        String title = "";
        if ("TOKYO".equalsIgnoreCase(region)) {
            TokyoGeocodedEntity entity = tokyoGeocodedRepository.findByDatanum(datanum)
                    .orElseThrow(() -> new IllegalArgumentException("공고를 찾을 수 없습니다."));
            entity.setStatus(JobStatus.CLOSED);
            title = entity.getTitle();
        } else {
            OsakaGeocodedEntity entity = osakaGeocodedRepository.findByDatanum(datanum)
                    .orElseThrow(() -> new IllegalArgumentException("공고를 찾을 수 없습니다."));
            entity.setStatus(JobStatus.CLOSED);
            title = entity.getTitle();
        }

        List<ApplicationEntity> applications = applicationRepository
                .findByTargetSourceAndTargetPostIdOrderByAppliedAtDesc(region.toUpperCase(), datanum);
        for (ApplicationEntity app : applications) {
            if (app.getStatus() == net.kumo.kumo.domain.entity.Enum.ApplicationStatus.APPLIED ||
                    app.getStatus() == net.kumo.kumo.domain.entity.Enum.ApplicationStatus.VIEWED) {

                notificationService.sendJobClosedNotification(
                        app.getSeeker(),
                        title,
                        datanum,
                        region.toUpperCase());
            }
        }
    }

    /**
     * 구인자의 대시보드(지원자 관리)에 렌더링될 공고별 지원자 요약 목록을 조회합니다.
     *
     * @param user   조회를 요청한 구인자 사용자 엔티티
     * @param sortBy 적용할 정렬 조건 (예: 지원자수 우선 등)
     * @return 그룹화된 공고 및 지원자 정보 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<JobApplicantGroupDTO> getGroupedApplicantsForRecruiter(UserEntity user, String sortBy) {
        List<JobApplicantGroupDTO> groupedList = new ArrayList<>();
        String email = user.getEmail();

        List<OsakaGeocodedEntity> osakaJobs = osakaGeocodedRepository.findByUser_Email(email);
        if (!osakaJobs.isEmpty()) {
            List<Long> osakaJobIds = osakaJobs.stream().map(OsakaGeocodedEntity::getId).toList();

            List<ApplicationEntity> osakaApps = applicationRepository.findByTargetSourceAndTargetPostIdIn("OSAKA",
                    osakaJobIds);

            Map<Long, List<ApplicationEntity>> appMap = osakaApps.stream()
                    .collect(Collectors.groupingBy(ApplicationEntity::getTargetPostId));

            for (OsakaGeocodedEntity job : osakaJobs) {
                List<ApplicationEntity> appsForThisJob = appMap.getOrDefault(job.getId(), new ArrayList<>());

                List<ApplicationDTO.ApplicantResponse> appResponses = appsForThisJob.stream()
                        .map(app -> ApplicationDTO.ApplicantResponse.from(app, job.getTitle()))
                        .sorted((a, b) -> {
                            int priorityA = getApplicantPriority(a.getStatus().name());
                            int priorityB = getApplicantPriority(b.getStatus().name());

                            if (priorityA != priorityB) {
                                return Integer.compare(priorityA, priorityB);
                            }
                            return b.getAppId().compareTo(a.getAppId());
                        })
                        .toList();

                groupedList.add(JobApplicantGroupDTO.builder()
                        .jobId(job.getId())
                        .source("OSAKA")
                        .jobTitle(job.getTitle())
                        .jobTitleJp(job.getTitleJp())
                        .status(job.getStatus() != null ? job.getStatus().name() : "RECRUITING")
                        .createdAt(job.getCreatedAt())
                        .applicantCount(appResponses.size())
                        .applicants(appResponses)
                        .build());
            }
        }

        List<TokyoGeocodedEntity> tokyoJobs = tokyoGeocodedRepository.findByUser_Email(email);
        if (!tokyoJobs.isEmpty()) {
            List<Long> tokyoJobIds = tokyoJobs.stream().map(TokyoGeocodedEntity::getId).toList();

            List<ApplicationEntity> tokyoApps = applicationRepository.findByTargetSourceAndTargetPostIdIn("TOKYO",
                    tokyoJobIds);

            Map<Long, List<ApplicationEntity>> appMap = tokyoApps.stream()
                    .collect(Collectors.groupingBy(ApplicationEntity::getTargetPostId));

            for (TokyoGeocodedEntity job : tokyoJobs) {
                List<ApplicationEntity> appsForThisJob = appMap.getOrDefault(job.getId(), new ArrayList<>());

                List<ApplicationDTO.ApplicantResponse> appResponses = appsForThisJob.stream()
                        .map(app -> ApplicationDTO.ApplicantResponse.from(app, job.getTitle()))
                        .sorted((a, b) -> {
                            int priorityA = getApplicantPriority(a.getStatus().name());
                            int priorityB = getApplicantPriority(b.getStatus().name());

                            if (priorityA != priorityB) {
                                return Integer.compare(priorityA, priorityB);
                            }
                            return b.getAppId().compareTo(a.getAppId());
                        })
                        .toList();

                groupedList.add(JobApplicantGroupDTO.builder()
                        .jobId(job.getId())
                        .source("TOKYO")
                        .jobTitle(job.getTitle())
                        .jobTitleJp(job.getTitleJp())
                        .status(job.getStatus() != null ? job.getStatus().name() : "RECRUITING")
                        .createdAt(job.getCreatedAt())
                        .applicantCount(appResponses.size())
                        .applicants(appResponses)
                        .build());
            }
        }

        groupedList.sort((a, b) -> {
            if ("applicantCount".equalsIgnoreCase(sortBy)) {
                if (a.getApplicantCount() != b.getApplicantCount()) {
                    return Integer.compare(b.getApplicantCount(), a.getApplicantCount());
                }
            }

            boolean aIsRecruiting = "RECRUITING".equals(a.getStatus());
            boolean bIsRecruiting = "RECRUITING".equals(b.getStatus());

            if (aIsRecruiting && !bIsRecruiting) return -1;
            if (!aIsRecruiting && bIsRecruiting) return 1;

            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        return groupedList;
    }

    /**
     * 지원자의 현재 심사 상태에 따른 정렬 우선순위 계수를 반환합니다.
     * 검토중(APPLIED)은 최상단, 합격(PASSED)은 중간, 불합격(FAILED)은 최하단에 배치됩니다.
     *
     * @param status 평가할 상태 문자열
     * @return 정렬에 사용될 우선순위 정수 (낮을수록 상단)
     */
    private int getApplicantPriority(String status) {
        switch (status) {
            case "APPLIED":
                return 0;
            case "PASSED":
                return 1;
            case "FAILED":
                return 2;
            default:
                return 1;
        }
    }

    /**
     * 특정 구직자의 상세 이력서 데이터를 다수의 연관 엔티티로부터 취합하여 반환합니다.
     * 데이터베이스 I/O 최적화를 위해 프록시(Proxy) 엔티티 조회를 활용합니다.
     *
     * @param seekerId 조회할 구직자(Seeker) 식별자
     * @return 종합 이력서 데이터가 포함된 응답용 DTO 객체
     */
    @Transactional(readOnly = true)
    public ResumeResponseDTO getApplicantResumeData(Long seekerId) {

        SeekerProfileEntity profileEntity = seekerProfileRepository.findByUser_UserId(seekerId).orElse(null);
        ResumeResponseDTO.ProfileDTO profileDTO = ResumeResponseDTO.ProfileDTO.from(profileEntity);

        SeekerDesiredConditionEntity conditionEntity = conditionRepository.findByUser_UserId(seekerId).orElse(null);
        ResumeResponseDTO.ConditionDTO conditionDTO = ResumeResponseDTO.ConditionDTO.from(conditionEntity);

        List<SeekerCareerEntity> careerEntities = careerRepository.findByUser_UserId(seekerId);
        List<ResumeResponseDTO.CareerDTO> careerDTOs = careerEntities.stream()
                .map(ResumeResponseDTO.CareerDTO::from)
                .collect(Collectors.toList());

        SeekerEducationEntity eduEntities = educationRepository.findByUser_UserId(seekerId);
        ResumeResponseDTO.EducationDTO eduDTO = null;
        if (eduEntities != null) {
            eduDTO = ResumeResponseDTO.EducationDTO.from(eduEntities);
        }

        List<SeekerCertificateEntity> certEntities = certificateRepository.findByUser_UserId(seekerId);
        List<ResumeResponseDTO.CertificateDTO> certDTOs = certEntities.stream()
                .map(ResumeResponseDTO.CertificateDTO::from)
                .collect(Collectors.toList());

        List<SeekerLanguageEntity> langEntities = languageRepository.findByUser_UserId(seekerId);
        List<ResumeResponseDTO.LanguageDTO> langDTOs = langEntities.stream()
                .map(ResumeResponseDTO.LanguageDTO::from)
                .collect(Collectors.toList());

        UserEntity proxyUser = userRepository.getReferenceById(seekerId);

        List<SeekerDocumentEntity> docEntities = documentRepository.findByUser(proxyUser);

        List<net.kumo.kumo.domain.dto.ResumeResponseDTO.DocumentDTO> docDTOs = docEntities.stream()
                .map(net.kumo.kumo.domain.dto.ResumeResponseDTO.DocumentDTO::from)
                .collect(Collectors.toList());

        return net.kumo.kumo.domain.dto.ResumeResponseDTO.builder()
                .profile(profileDTO)
                .condition(conditionDTO)
                .careers(careerDTOs)
                .educations(eduDTO)
                .certificates(certDTOs)
                .languages(langDTOs)
                .documents(docDTOs)
                .build();
    }

    /**
     * 특정 지원 내역(서류 심사)의 상태(합격, 불합격 등)를 갱신합니다.
     * 결과 갱신과 동시에 지원한 구직자에게 시스템 알림이 자동으로 발송됩니다.
     *
     * @param appId  갱신 대상 지원 내역 식별자
     * @param status 반영할 새로운 상태 Enum 객체
     */
    @Transactional
    public void updateApplicationStatus(Long appId, net.kumo.kumo.domain.entity.Enum.ApplicationStatus status) {
        ApplicationEntity application = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("지원서를 찾을 수 없습니다."));
        application.setStatus(status);

        if (status == net.kumo.kumo.domain.entity.Enum.ApplicationStatus.PASSED ||
                status == net.kumo.kumo.domain.entity.Enum.ApplicationStatus.FAILED) {

            String jobTitle = "공고";
            if ("TOKYO".equalsIgnoreCase(application.getTargetSource())) {
                jobTitle = tokyoGeocodedRepository.findById(application.getTargetPostId())
                        .map(TokyoGeocodedEntity::getTitle).orElse("공고");
            } else if ("OSAKA".equalsIgnoreCase(application.getTargetSource())) {
                jobTitle = osakaGeocodedRepository.findById(application.getTargetPostId())
                        .map(OsakaGeocodedEntity::getTitle).orElse("공고");
            }

            notificationService.sendAppStatusNotification(application.getSeeker(), status, jobTitle);
        }
    }
}