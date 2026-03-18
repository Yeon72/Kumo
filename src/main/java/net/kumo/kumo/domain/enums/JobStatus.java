package net.kumo.kumo.domain.enums;

/**
 * 구인 공고의 현재 모집 상태를 정의하는 열거형(Enum)입니다.
 */
public enum JobStatus {
    /** 모집 중 (기본 상태) */
    RECRUITING,

    /** 모집 마감 */
    CLOSED,

    /** 관리자에 의한 차단 (신고 누적 등 규정 위반) */
    BLOCKED
}