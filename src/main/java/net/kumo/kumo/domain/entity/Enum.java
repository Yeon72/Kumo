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
		// [구직자용]
		APP_COMPLETED,   // 구인 신청 완료
		APP_PASSED,      // 합격 알림
		APP_FAILED,      // 불합격 알림
		SCOUT_OFFER,     // 스카우트 제의
		JOB_CLOSED,      // 지원 중인 공고 마감 알림

		// [구인자용]
		NEW_APPLICANT,   // 신규 지원자 발생
		TODAY_SCHEDULE,  // 오늘의 일정 알림

		// [공통]
		NEW_CHAT,        // 새로운 채팅 알림
		NOTICE,          // 시스템 공지
		REPORT_RESULT    // 신고 처리 결과
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
