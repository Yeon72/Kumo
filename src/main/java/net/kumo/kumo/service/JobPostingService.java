package net.kumo.kumo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.kumo.kumo.domain.dto.ApplicationDTO;
import net.kumo.kumo.domain.dto.JobApplicantGroupDTO;
import net.kumo.kumo.domain.entity.*;
import net.kumo.kumo.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import net.kumo.kumo.domain.dto.JobManageListDTO;
import net.kumo.kumo.domain.dto.JobPostingRequestDTO;
import net.kumo.kumo.domain.dto.JobApplicantGroupDTO;
import net.kumo.kumo.domain.dto.ApplicationDTO;
import net.kumo.kumo.domain.entity.ApplicationEntity;
import net.kumo.kumo.domain.enums.JobStatus;
import net.kumo.kumo.repository.CompanyRepository;
import net.kumo.kumo.repository.OsakaGeocodedRepository;
import net.kumo.kumo.repository.TokyoGeocodedRepository;

@Service
@RequiredArgsConstructor
public class JobPostingService {

    private final OsakaGeocodedRepository osakaGeocodedRepository;
    private final TokyoGeocodedRepository tokyoGeocodedRepository; // 🌟 도쿄 레포지토리 추가
    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional
    public void saveJobPosting(JobPostingRequestDTO dto, List<MultipartFile> images, UserEntity user) {

        // 1. 단 한 번만! 회사 객체를 가져옵니다. (중복 조회 제거)
        CompanyEntity company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다. ID: " + dto.getCompanyId()));

        String companyName = company.getBizName();
        String address = (company.getAddressMain() != null ? company.getAddressMain() : "")
                + (company.getAddressDetail() != null ? " " + company.getAddressDetail() : "");
        Double lat = company.getLatitude() != null ? company.getLatitude().doubleValue() : 0.0;
        Double lng = company.getLongitude() != null ? company.getLongitude().doubleValue() : 0.0;

        String prefJp = company.getAddrPrefecture(); // 🌟 "東京都" 또는 "大阪府"
        String cityJp = company.getAddrCity();
        String wardJp = company.getAddrTown();

        // 2. 이미지 URL 처리
        String imgUrls = "";
        if (images != null && !images.isEmpty()) {
            imgUrls = images.stream()
                    .filter(f -> !f.isEmpty())
                    .map(f -> "/uploads/" + f.getOriginalFilename())
                    .collect(Collectors.joining(","));
        }

        // 급여 부분 임시 변수
        String salaryType;
        String salaryTypeJp;

        // 급여 기준 별 임시 변수 저장
        switch (dto.getSalaryType()) {
            case "HOURLY":
                salaryType = "시급";
                salaryTypeJp = "時給";
                break;

            case "DAILY":
                salaryType = "일급";
                salaryTypeJp = "日給";
                break;

            case "MONTHLY":
                salaryType = "월급";
                salaryTypeJp = "月給";
                break;

            case "SALARY":
                salaryType = "연봉";
                salaryTypeJp = "年収";
                break;

            default:
                salaryType = "미정";
                salaryTypeJp = "未定";
                break;
        }

        // 3. 급여 문자열 및 공통 데이터 세팅
        String wage = (dto.getSalaryType() != null && dto.getSalaryAmount() != null)
                ? salaryType + " " + dto.getSalaryAmount() + "엔"
                : "";

        String wageJp = (dto.getSalaryType() != null && dto.getSalaryAmount() != null)
                ? salaryTypeJp + " " + dto.getSalaryAmount() + "円"
                : "";

        long datanum = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        java.time.format.DateTimeFormatter writeTimeFormatter = java.time.format.DateTimeFormatter
                .ofPattern("yy.MM.dd");
        String writeTime = now.format(writeTimeFormatter);

        // 🌟🌟 4. [핵심] 도쿄 vs 오사카 분기 처리 🌟🌟
        if ("東京都".equals(prefJp)) {
            saveToTokyo(dto, user, company, companyName, address, lat, lng, prefJp, cityJp, wardJp, imgUrls, wage,
                    wageJp, datanum, now, writeTime);
        } else {
            // 기본값은 오사카로 처리 (大阪府이거나 다른 지역일 경우 일단 오사카 DB로)
            saveToOsaka(dto, user, company, companyName, address, lat, lng, prefJp, cityJp, wardJp, imgUrls, wage,
                    wageJp, datanum, now, writeTime);
        }
    }

    // ==========================================
    // 🚅 오사카 저장 로직 (기존 로직 분리)
    // ==========================================
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

        // 🌟 [추가] 수정 시 입력창에 다시 뿌려주기 위해 원본 데이터 저장!
        entity.setSalaryType(dto.getSalaryType()); // "HOURLY" 등 저장
        entity.setSalaryAmount(dto.getSalaryAmount()); // 1200 등 저장

        entity.setRowNo(nextRowNo);
        entity.setDatanum(datanum);
        entity.setTitle(dto.getTitle());
        entity.setContactPhone(dto.getContactPhone());
        entity.setHref("/Recruiter/posting/" + datanum);
        entity.setPosition(dto.getPosition());
        entity.setJobDescription(dto.getJobDescription());
        entity.setBody(dto.getBody());
        entity.setWage(wage);
        entity.setWageJp(wageJp);
        entity.setImgUrls(imgUrls.isEmpty() ? null : imgUrls);
        entity.setStatus(JobStatus.RECRUITING);

        parseAddressToSixColumnsOsaka(entity, address);
        osakaGeocodedRepository.save(entity);
    }

    // ==========================================
    // 🚅 도쿄 저장 로직 (신규 추가)
    // ==========================================
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

        // 🌟 [추가] 수정 시 입력창에 다시 뿌려주기 위해 원본 데이터 저장!
        entity.setSalaryType(dto.getSalaryType()); // "HOURLY" 등 저장
        entity.setSalaryAmount(dto.getSalaryAmount()); // 1200 등 저장

        entity.setRowNo(nextRowNo);
        entity.setDatanum(datanum);
        entity.setTitle(dto.getTitle());
        entity.setContactPhone(dto.getContactPhone());
        entity.setHref("/Recruiter/posting/" + datanum);
        entity.setPosition(dto.getPosition());
        entity.setJobDescription(dto.getJobDescription());
        entity.setBody(dto.getBody());
        entity.setWage(wage);
        entity.setWageJp(wageJp);
        entity.setImgUrls(imgUrls.isEmpty() ? null : imgUrls);
        entity.setStatus(JobStatus.RECRUITING);

        parseAddressToSixColumnsTokyo(entity, address);
        tokyoGeocodedRepository.save(entity);
    }

    // ==========================================
    // 🗺️ 주소 파싱 로직 (오사카/도쿄 분리)
    // ==========================================
    private void parseAddressToSixColumnsOsaka(OsakaGeocodedEntity entity, String fullAddress) {
        // ... (사장님이 쓰시던 기존 parseAddressToSixColumns 코드와 동일하게 넣으시면 됩니다)
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
                prefJp = part; // 도쿄도는 府가 아니라 都입니다!
            else if (part.endsWith("市"))
                cityJp = part;
            else if (part.endsWith("区"))
                wardJp = part;
        }

        entity.setPrefectureJp(prefJp);
        // ✅ 수정 (도쿄 엔티티 구조에 맞게 통합!)
        // 도쿄는 시/구를 wardCityJp 하나로 쓰기로 했었죠!
        entity.setWardCityJp(wardJp != null ? wardJp : cityJp);

        if ("東京都".equals(prefJp))
            entity.setPrefectureKr("도쿄도");
        // 도쿄의 주요 구 번역 세팅
        // 2. 한국어 세팅 (setWardKr 대신 setWardCityKr 사용!)
        // 🗺️ 도쿄 23구 전체 번역 매핑 (Map.ofEntries 사용)
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
                    // 필요하다면 도쿄도의 주요 시(市)도 아래처럼 계속 추가할 수 있습니다!
                    Map.entry("八王子市", "하치오지시"),
                    Map.entry("町田市", "마치다시"));

            // 매핑된 한국어 구 이름이 있으면 넣고, 없으면 일본어 원본 그대로 저장!
            entity.setWardCityKr(tokyoMap.getOrDefault(wardJp, wardJp));
        }
    }

    /**
     * 특정 유저(이메일)의 도쿄 + 오사카 공고를 합쳐서 반환 (최신순 정렬)
     */
    public List<JobManageListDTO> getMyJobPostings(String email) {
        List<JobManageListDTO> result = new java.util.ArrayList<>();

        // 1. 오사카 공고 가져와서 바구니에 담기
        List<OsakaGeocodedEntity> osakaJobs = osakaGeocodedRepository.findByUser_Email(email);
        for (OsakaGeocodedEntity o : osakaJobs) {

            // 🌟 1. 여기서 영어를 한글로 싹 바꿔줍니다!
            String displayWage = o.getWage() != null ? o.getWage()
                    .replace("HOURLY", "시급")
                    .replace("DAILY", "일급")
                    .replace("MONTHLY", "월급")
                    .replace("SALARY", "연봉") : "";

            result.add(JobManageListDTO.builder()
                    .id(o.getId()) // 🌟 [추가] 오사카 테이블의 진짜 id
                    .datanum(o.getDatanum())
                    .title(o.getTitle())
                    .regionType("오사카") // 라벨링
                    .wage(displayWage)
                    .wageJp(o.getWageJp())
                    .createdAt(o.getCreatedAt())
                    .status(o.getStatus() != null ? o.getStatus().name() : "RECRUITING")
                    .build());
        }

        // 2. 도쿄 공고 가져와서 바구니에 담기
        List<TokyoGeocodedEntity> tokyoJobs = tokyoGeocodedRepository.findByUser_Email(email);
        for (TokyoGeocodedEntity t : tokyoJobs) {

            // 🌟 1. 여기서 영어를 한글로 싹 바꿔줍니다!
            String displayWage = t.getWage() != null ? t.getWage()
                    .replace("HOURLY", "시급")
                    .replace("DAILY", "일급")
                    .replace("MONTHLY", "월급")
                    .replace("SALARY", "연봉") : "";

            result.add(JobManageListDTO.builder()
                    .id(t.getId()) // 🌟 [추가] 도쿄 테이블의 진짜 id
                    .datanum(t.getDatanum())
                    .title(t.getTitle())
                    .regionType("도쿄") // 라벨링
                    .wage(displayWage)
                    .wageJp(t.getWageJp())
                    .createdAt(t.getCreatedAt())
                    .status(t.getStatus() != null ? t.getStatus().name() : "RECRUITING")
                    .build());
        }

        // 3. 🌟 두 리스트를 합친 후, 등록일(createdAt) 기준 '최신순(내림차순)' 정렬!
        result.sort((a, b) -> {
            if (a.getCreatedAt() == null)
                return 1;
            if (b.getCreatedAt() == null)
                return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        result.sort((a, b) -> {
            // 1. 상태 기준 정렬: RECRUITING(모집중)이 CLOSED(마감)보다 앞으로 오게 함
            if (!a.getStatus().equals(b.getStatus())) {
                // RECRUITING 이면 -1(앞으로), CLOSED 이면 1(뒤로)
                return a.getStatus().equals("RECRUITING") ? -1 : 1;
            }

            // 2. 상태가 같다면 최신 등록일 순으로 정렬
            if (a.getCreatedAt() == null)
                return 1;
            if (b.getCreatedAt() == null)
                return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        return result;
    }

    /**
     * 🌟 [완전 복구] 특정 유저의 공고 삭제 로직 (보안 검증 포함)
     * 
     * @param datanum : 공고 고유 번호
     * @param region  : TOKYO 또는 OSAKA
     * @param email   : 현재 로그인한 유저의 이메일 (검증용)
     */
    @Transactional
    public void deleteMyJobPosting(Long datanum, String region, String email) {
        if ("TOKYO".equalsIgnoreCase(region)) {
            // 1. 도쿄 테이블에서 데이터 조회
            TokyoGeocodedEntity entity = tokyoGeocodedRepository.findByDatanum(datanum)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도쿄 공고입니다. (datanum: " + datanum + ")"));

            // 2. [보안 핵심] 작성자와 현재 로그인 유저가 일치하는지 확인
            if (!entity.getUser().getEmail().equals(email)) {
                throw new IllegalStateException("해당 공고를 삭제할 권한이 없습니다.");
            }

            // 3. 검증 통과 시 삭제
            tokyoGeocodedRepository.delete(entity);

        } else if ("OSAKA".equalsIgnoreCase(region)) {
            // 1. 오사카 테이블에서 데이터 조회
            OsakaGeocodedEntity entity = osakaGeocodedRepository.findByDatanum(datanum)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 오사카 공고입니다. (datanum: " + datanum + ")"));

            // 2. [보안 핵심] 작성자 검증
            if (!entity.getUser().getEmail().equals(email)) {
                throw new IllegalStateException("해당 공고를 삭제할 권한이 없습니다.");
            }

            // 3. 검증 통과 시 삭제
            osakaGeocodedRepository.delete(entity);

        } else {
            throw new IllegalArgumentException("알 수 없는 지역 정보입니다: " + region);
        }
    }

    /**
     * 수정용 공고 데이터 단일 조회
     */
    public JobPostingRequestDTO getJobPostingForEdit(Long id, String region) {
        JobPostingRequestDTO dto = new JobPostingRequestDTO();

        if ("TOKYO".equalsIgnoreCase(region)) {
            TokyoGeocodedEntity e = tokyoGeocodedRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("공고를 찾을 수 없습니다."));
            dto.setDatanum(e.getDatanum());
            dto.setTitle(e.getTitle());
            dto.setPosition(e.getPosition());
            dto.setContactPhone(e.getContactPhone());
            dto.setJobDescription(e.getJobDescription());
            dto.setBody(e.getBody());
            dto.setSalaryType(e.getSalaryType());
            dto.setSalaryAmount(e.getSalaryAmount());
            if (e.getCompany() != null)
                dto.setCompanyId(e.getCompany().getCompanyId());

        } else {
            OsakaGeocodedEntity e = osakaGeocodedRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("공고를 찾을 수 없습니다."));
            dto.setDatanum(e.getDatanum());
            dto.setTitle(e.getTitle());
            dto.setPosition(e.getPosition());
            dto.setContactPhone(e.getContactPhone());
            dto.setJobDescription(e.getJobDescription());
            dto.setBody(e.getBody());
            dto.setSalaryType(e.getSalaryType());
            dto.setSalaryAmount(e.getSalaryAmount());
            if (e.getCompany() != null)
                dto.setCompanyId(e.getCompany().getCompanyId());
        }

        return dto;
    }

    /**
     * 공고 수정
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
            e.setBody(dto.getBody());
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
            e.setBody(dto.getBody());
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

    @Transactional
    public void closeJobPosting(Long datanum, String region) {
        if ("TOKYO".equalsIgnoreCase(region)) {
            TokyoGeocodedEntity entity = tokyoGeocodedRepository.findByDatanum(datanum)
                    .orElseThrow(() -> new IllegalArgumentException("공고를 찾을 수 없습니다."));
            entity.setStatus(JobStatus.CLOSED); // 🌟 상태를 마감으로 변경!
        } else {
            OsakaGeocodedEntity entity = osakaGeocodedRepository.findByDatanum(datanum)
                    .orElseThrow(() -> new IllegalArgumentException("공고를 찾을 수 없습니다."));
            entity.setStatus(JobStatus.CLOSED);
        }
    }

    // ==========================================
    // 🌟 [NEW] 지원자 관리 탭 : 내 공고별 지원자 목록 가져오기
    // ==========================================
    @Transactional(readOnly = true)
    public List<JobApplicantGroupDTO> getGroupedApplicantsForRecruiter(UserEntity user) {
        List<JobApplicantGroupDTO> groupedList = new ArrayList<>();
        String email = user.getEmail();

        // ------------------------------------------
        // 1. 오사카 공고 조회 및 지원자 매핑
        // ------------------------------------------
        List<OsakaGeocodedEntity> osakaJobs = osakaGeocodedRepository.findByUser_Email(email);
        if (!osakaJobs.isEmpty()) {
            List<Long> osakaJobIds = osakaJobs.stream().map(OsakaGeocodedEntity::getId).toList();

            // 이 구인자의 오사카 공고들에 지원한 모든 지원서 한 번에 조회
            List<ApplicationEntity> osakaApps = applicationRepository.findByTargetSourceAndTargetPostIdIn("OSAKA", osakaJobIds);

            // 공고 ID(targetPostId)를 기준으로 지원서들을 그룹화 (Map 형태로 분리)
            Map<Long, List<ApplicationEntity>> appMap = osakaApps.stream()
                    .collect(Collectors.groupingBy(ApplicationEntity::getTargetPostId));

            // 각 공고별로 DTO 조립
            for (OsakaGeocodedEntity job : osakaJobs) {
                // 해당 공고에 달린 지원서 리스트 꺼내기 (없으면 빈 리스트)
                List<ApplicationEntity> appsForThisJob = appMap.getOrDefault(job.getId(), new ArrayList<>());

                // 엔티티 -> DTO 변환 및 최신 지원순 정렬
                List<ApplicationDTO.ApplicantResponse> appResponses = appsForThisJob.stream()
                        .map(app -> ApplicationDTO.ApplicantResponse.from(app, job.getTitle()))
                        .sorted((a, b) -> b.getAppId().compareTo(a.getAppId()))
                        .toList();

                groupedList.add(JobApplicantGroupDTO.builder()
                        .jobId(job.getId())
                        .source("OSAKA")
                        .jobTitle(job.getTitle())
                        .status(job.getStatus() != null ? job.getStatus().name() : "RECRUITING")
                        .createdAt(job.getCreatedAt())
                        .applicantCount(appResponses.size())
                        .applicants(appResponses) // 🌟 지원자 목록 쏙!
                        .build());
            }
        }

        // ------------------------------------------
        // 2. 도쿄 공고 조회 및 지원자 매핑
        // ------------------------------------------
        List<TokyoGeocodedEntity> tokyoJobs = tokyoGeocodedRepository.findByUser_Email(email);
        if (!tokyoJobs.isEmpty()) {
            List<Long> tokyoJobIds = tokyoJobs.stream().map(TokyoGeocodedEntity::getId).toList();

            // 도쿄 공고 지원서 조회
            List<ApplicationEntity> tokyoApps = applicationRepository.findByTargetSourceAndTargetPostIdIn("TOKYO", tokyoJobIds);

            Map<Long, List<ApplicationEntity>> appMap = tokyoApps.stream()
                    .collect(Collectors.groupingBy(ApplicationEntity::getTargetPostId));

            for (TokyoGeocodedEntity job : tokyoJobs) {
                List<ApplicationEntity> appsForThisJob = appMap.getOrDefault(job.getId(), new ArrayList<>());

                List<ApplicationDTO.ApplicantResponse> appResponses = appsForThisJob.stream()
                        .map(app -> ApplicationDTO.ApplicantResponse.from(app, job.getTitle()))
                        .sorted((a, b) -> b.getAppId().compareTo(a.getAppId()))
                        .toList();

                groupedList.add(JobApplicantGroupDTO.builder()
                        .jobId(job.getId())
                        .source("TOKYO")
                        .jobTitle(job.getTitle())
                        .status(job.getStatus() != null ? job.getStatus().name() : "RECRUITING")
                        .createdAt(job.getCreatedAt())
                        .applicantCount(appResponses.size())
                        .applicants(appResponses) // 🌟 지원자 목록 쏙!
                        .build());
            }
        }

        // ------------------------------------------
        // 3. 최신 공고가 아코디언 맨 위에 뜨도록 정렬
        // ------------------------------------------
        groupedList.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        return groupedList;
    }
}