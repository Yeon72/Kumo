package net.kumo.kumo.domain.dto;

import lombok.*;
import net.kumo.kumo.domain.entity.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ResumeResponseDTO {

    // 1:1 관계 데이터
    private ProfileDTO profile;
    private ConditionDTO condition;

    // 1:N 관계 데이터 (리스트)
    private List<CareerDTO> careers;
    private EducationDTO educations;
    private List<CertificateDTO> certificates;
    private List<LanguageDTO> languages;
    private List<DocumentDTO> documents;

    // ==========================================
    // 🌟 내부 정적 클래스 (Nested Static Class) 세팅
    // ==========================================

    @Getter @Builder
    public static class ProfileDTO {
        private String careerType;
        private String selfPr;

        public static ProfileDTO from(SeekerProfileEntity entity) {
            if (entity == null) return null;

            // EXPERIENCED -> "경력", NEWCOMER -> "신입" 등으로 변환 로직 추가 가능
            String typeStr = "NEWCOMER".equals(entity.getCareerType()) ? "신입" : "경력";

            return ProfileDTO.builder()
                    .careerType(typeStr)
                    .selfPr(entity.getSelfPr())
                    .build();
        }
    }

    @Getter @Builder
    public static class ConditionDTO {
        private String desiredJob;
        private String salaryType;
        private String desiredSalary;

        public static ConditionDTO from(SeekerDesiredConditionEntity entity) {
            if (entity == null) return null;

            String sType = entity.getSalaryType();
            if ("HOURLY".equals(sType)) sType = "시급";
            else if ("MONTHLY".equals(sType)) sType = "월급";
            else if ("YEARLY".equals(sType)) sType = "연봉";

            return ConditionDTO.builder()
                    .desiredJob(entity.getDesiredJob())
                    .salaryType(sType)
                    .desiredSalary(entity.getDesiredSalary())
                    .build();
        }
    }

    @Getter @Builder
    public static class CareerDTO {
        private String companyName;
        private String department;
        private String startDate;
        private String endDate;
        private String description;

        public static CareerDTO from(SeekerCareerEntity entity) {
            if (entity == null) return null;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

            return CareerDTO.builder()
                    .companyName(entity.getCompanyName())
                    .department(entity.getDepartment())
                    .startDate(entity.getStartDate() != null ? entity.getStartDate().format(formatter) : "")
                    .endDate(entity.getEndDate() != null ? entity.getEndDate().format(formatter) : "재직중")
                    .description(entity.getDescription())
                    .build();
        }
    }

    @Getter @Builder
    public static class EducationDTO {
        private String schoolName;
        private String major;
        private String status;

        public static EducationDTO from(SeekerEducationEntity entity) {
            if (entity == null) return null;
            return EducationDTO.builder()
                    .schoolName(entity.getSchoolName())
                    .major(entity.getMajor())
                    .status(entity.getStatus()) // 프론트에서 GRADUATED 등으로 판별
                    .build();
        }
    }

    @Getter @Builder
    public static class CertificateDTO {
        private String certName;
        private String acquisitionYear;
        private String issuer;

        public static CertificateDTO from(SeekerCertificateEntity entity) {
            if (entity == null) return null;
            return CertificateDTO.builder()
                    .certName(entity.getCertName())
                    .acquisitionYear(entity.getAcquisitionYear())
                    .issuer(entity.getIssuer())
                    .build();
        }
    }

    @Getter @Builder
    public static class LanguageDTO {
        private String language;
        private String level;

        public static LanguageDTO from(SeekerLanguageEntity entity) {
            if (entity == null) return null;
            return LanguageDTO.builder()
                    .language(entity.getLanguage())
                    .level(entity.getLevel())
                    .build();
        }
    }

    @Getter @Builder
    public static class DocumentDTO {
        private String fileName;
        private String fileUrl;

        public static DocumentDTO from(SeekerDocumentEntity entity) {
            if (entity == null) return null;
            return DocumentDTO.builder()
                    .fileName(entity.getFileName())
                    .fileUrl(entity.getFileUrl())
                    .build();
        }
    }
}