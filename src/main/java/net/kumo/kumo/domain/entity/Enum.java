package net.kumo.kumo.domain.entity;

public class Enum {
	public enum UserRole {
		SEEKER, RECRUITER, ADMIN
	}

	public enum Gender {
		MALE, FEMALE, OTHER
	}

	public enum RegionType {
		PREFECTURE, CITY, WARD, TOWN_VILLAGE
	}

	public enum SalaryType {
		HOURLY, DAILY, MONTHLY, NEGOTIABLE
	}

	public enum JobStatus {
		RECRUITING, CLOSED
	}

	public enum ApplicationStatus {
		APPLIED, VIEWED, PASSED, FAILED
	}

	public enum ReportStatus {
		PENDING, RESOLVED, REJECTED
	}

	public enum MessageType {
		TEXT, IMAGE, SYSTEM, FILE
	}

	public enum NotificationType {
		// 구직자(Seeker) 대상
		APP_STATUS, // 지원 상태 변동
		INTERVIEW_REMIND, // 면접 일정 리마인드
		SCRAP_CLOSING, // 스크랩 공고 마감 임박
		JOB_RECOMMEND, // 맞춤 공고 추천

		// 구인자(Recruiter) 대상
		NEW_APPLICANT, // 신규 지원자 발생
		SCHEDULE_CONFIRMED, // 면접 일정 확정
		POST_EXPIRED, // 공고 마감 알림

		// 공통 및 시스템
		NEW_CHAT, // 새 메시지
		NOTICE, // 공지사항
		SECURITY, // 계정 보안
		REPORT_RESULT // 신고 처리 결과
	}

	public enum SocialProvider {
		LINE, GOOGLE
	}
	
	public enum DesiredPeriod {
		LESS_THAN_1_MONTH,
		ONE_TO_THREE_MONTHS,
		THREE_TO_SIX_MONTHS,
		OVER_6_MONTHS,
		LONG_TERM
	}
	
	
}
