package net.kumo.kumo.domain.dto;


import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class ResumeDto {
	// ==========================================
	// 1. 학력사항
	// ==========================================
	private String educationLevel;
	private String educationStatus;
	private String schoolName;
	
	// ==========================================
	// 2. 경력사항 (단일 & 다중 리스트)
	// ==========================================
	private String careerType; // EXPERIENCED or NEWCOMER
	
	// 🌟 폼이 여러 개 추가될 수 있으므로 List로 받습니다.
	private List<String> companyName;
	private List<String> startYear;
	private List<String> startMonth;
	private List<String> endYear;
	private List<String> endMonth;
	private List<String> jobDuties;
	
	// ==========================================
	// 3. 희망근무조건
	// ==========================================
	private String desiredLocation1;
	private String desiredLocation2;
	private String desiredJob;
	private String salaryType;
	private String desiredSalary; // Integer -> String으로 변경하여 빈 값 등 예외 처리 유연화
	private String desiredPeriod;
	
	// ==========================================
	// 4. 자격증 (다중 리스트)
	// ==========================================
	private List<String> certName;
	private List<String> certPublisher;
	private List<String> certYear;
	
	// ==========================================
	// 5. 어학 능력 (다중 리스트)
	// ==========================================
	private List<String> languageName;
	private List<String> languageLevel; // ADVANCED, INTERMEDIATE, BEGINNER
	
	// ==========================================
	// 6. 설정 및 기타 (체크박스 & 토글)
	// ==========================================
	private Boolean contactPublic; // true or false
	private Boolean resumePublic;  // true or false
	private Boolean scoutAgree;    // 체크박스 (체크하면 true)
	
	private String selfIntroduction; // 자기소개
	
	// ==========================================
	// 7. 증빙서류 (파일 업로드)
	// ==========================================
	// 🌟 multiple 속성이 있으므로 List<MultipartFile> 로 받아야 합니다!
	private List<MultipartFile> portfolioFiles;
	
	// 🌟 이미 업로드된 파일들의 URL을 담기 위한 리스트
	private List<String> portfolioFileUrls;
}
