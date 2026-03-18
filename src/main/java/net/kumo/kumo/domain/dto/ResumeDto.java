package net.kumo.kumo.domain.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 구직자의 이력서 작성 폼(Form)에서 입력한 복합적인 이력 데이터를
 * 서버로 전달받기 위한 래퍼(Wrapper) DTO 클래스입니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class ResumeDto {

    /* 1. 학력사항 */
    private String educationLevel;
    private String educationStatus;
    private String schoolName;

    /* 2. 경력사항 (다중 입력 리스트 형태) */
    /** 경력 유형 (EXPERIENCED 또는 NEWCOMER) */
    private String careerType;

    private List<String> companyName;
    private List<String> startYear;
    private List<String> startMonth;
    private List<String> endYear;
    private List<String> endMonth;
    private List<String> jobDuties;

    /* 3. 희망 근무 조건 */
    private String desiredLocation1;
    private String desiredLocation2;
    private String desiredJob;
    private String salaryType;

    /** 희망 급여 금액 (빈 값 처리를 유연하게 하기 위해 String 타입 사용) */
    private String desiredSalary;
    private String desiredPeriod;

    /* 4. 자격증 (다중 입력 리스트 형태) */
    private List<String> certName;
    private List<String> certPublisher;
    private List<String> certYear;

    /* 5. 어학 능력 (다중 입력 리스트 형태) */
    private List<String> languageName;

    /** 어학 수준 (ADVANCED, INTERMEDIATE, BEGINNER) */
    private List<String> languageLevel;

    /* 6. 개인 설정 및 기타 정보 */
    private Boolean contactPublic;
    private Boolean resumePublic;
    private Boolean scoutAgree;

    private String selfIntroduction;

    /* 7. 증빙 서류 및 포트폴리오 첨부 파일 */
    /** 클라이언트로부터 업로드된 다중 파일 객체 목록 */
    private List<MultipartFile> portfolioFiles;

    /** 이미 업로드되어 저장된 파일의 URL 문자열 목록 */
    private List<String> portfolioFileUrls;
}