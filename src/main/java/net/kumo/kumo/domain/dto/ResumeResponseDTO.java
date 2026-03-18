package net.kumo.kumo.domain.dto;

import lombok.*;
import net.kumo.kumo.domain.entity.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 구직자의 이력서 상세 정보를 구인자 또는 구직자 본인에게 렌더링하기 위해
 * 다수의 엔티티 정보를 통합하여 반환하는 응답 전용 복합 DTO 클래스입니다.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ResumeResponseDTO {

    private ProfileDTO profile;
    private ConditionDTO condition;

    private List<CareerDTO> careers;
    private EducationDTO educations;
    private List<CertificateDTO> certificates;
    private List<LanguageDTO> languages;
    private List<DocumentDTO> documents;

    /**
     * 구직자의 기본 프로필 요약 정보를 담는 내부 DTO 클래스입니다.
     */
    @Getter @Builder
    public static class ProfileDTO {
        private String careerType;
        private String selfPr;

        public static ProfileDTO from(SeekerProfileEntity entity) {
            if (entity == null) return null;

            String typeStr = "NEWCOMER".equals(entity.getCareerType()) ? "신입" : "경력";

            return ProfileDTO.builder()
                    .careerType(typeStr)
                    .selfPr(entity.getSelfPr())
                    .build();
        }
    }

    /**
     * 구직자의 희망 근무 조건을 담는 내부 DTO 클래스입니다.
     */
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

    /**
     * 구직자의 과거 경력 사항(직장 이력) 정보를 담는 내부 DTO 클래스입니다.
     */
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

    /**
     * 구직자의 최종 학력 정보를 담는 내부 DTO 클래스입니다.
     */
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
                    .status(entity.getStatus())
                    .build();
        }
    }

    /**
     * 구직자의 취득 자격증 정보를 담는 내부 DTO 클래스입니다.
     */
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

    /**
     * 구직자의 어학 능력 정보를 담는 내부 DTO 클래스입니다.
     */
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

    /**
     * 구직자가 등록한 포트폴리오 및 증빙 서류 파일의 메타데이터를 담는 내부 DTO 클래스입니다.
     */
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