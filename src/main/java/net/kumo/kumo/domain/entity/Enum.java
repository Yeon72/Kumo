package net.kumo.kumo.domain.entity;

/**
 * 플랫폼 시스템 전반에서 사용되는 공통 상태 및 유형값들을
 * 안전하게 관리하기 위한 전역 열거형(Enum) 집합 클래스입니다.
 */
public class Enum {

    /** 사용자 권한 등급 */
    public enum UserRole {
        SEEKER, RECRUITER, ADMIN
    }

    /** 성별 분류 */
    public enum Gender {
        MALE, FEMALE, OTHER
    }

    /** 행정 구역 단위 분류 */
    public enum RegionType {
        PREFECTURE, CITY, WARD, TOWN_VILLAGE
    }

    /** 급여 지급 기준 유형 */
    public enum SalaryType {
        HOURLY, DAILY, MONTHLY, NEGOTIABLE
    }

    /** 공고 모집 진행 상태 */
    public enum JobStatus {
        RECRUITING, CLOSED
    }

    /** 구직자 지원 내역 처리 상태 */
    public enum ApplicationStatus {
        APPLIED, VIEWED, PASSED, FAILED
    }

    /** 신고 내역 처리 상태 */
    public enum ReportStatus {
        PENDING, RESOLVED, REJECTED
    }

    /** 웹소켓 채팅 메시지 유형 */
    public enum MessageType {
        TEXT, IMAGE, SYSTEM, FILE
    }

    /** 시스템 알림(Notification) 발생 유형 */
    public enum NotificationType {
        /** 구인 신청 완료 알림 (구직자용) */
        APP_COMPLETED,
        /** 서류 합격 알림 (구직자용) */
        APP_PASSED,
        /** 서류 불합격 알림 (구직자용) */
        APP_FAILED,
        /** 스카우트 제의 알림 (구직자용) */
        SCOUT_OFFER,
        /** 지원 중인 공고 마감 알림 (구직자용) */
        JOB_CLOSED,

        /** 신규 지원자 접수 알림 (구인자용) */
        NEW_APPLICANT,
        /** 금일 예정된 일정 알림 (구인자용) */
        TODAY_SCHEDULE,

        /** 신규 채팅 수신 알림 (공통) */
        NEW_CHAT,
        /** 시스템 전역 공지 알림 (공통) */
        NOTICE,
        /** 접수된 신고 처리 결과 알림 (공통) */
        REPORT_RESULT
    }

    /** 소셜 로그인 연동 제공자 */
    public enum SocialProvider {
        LINE, GOOGLE
    }

    /** 구직 희망 근무 기간 설정값 */
    public enum DesiredPeriod {
        LESS_THAN_1_MONTH,
        ONE_TO_THREE_MONTHS,
        THREE_TO_SIX_MONTHS,
        OVER_6_MONTHS,
        LONG_TERM
    }
}