package net.kumo.kumo.domain.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;

/**
 * 관리자 대시보드에 렌더링될 통계 데이터를 담는 DTO 클래스입니다.
 */
@Getter
@Builder
public class AdminDashboardDTO {

    private long totalUsers;
    private long newUsers;
    private long totalPosts;
    private long newPosts;

    /**
     * 주간 공고 등록 수 통계 데이터
     * K: 날짜 문자열 (yyyy-MM-dd), V: 등록 건수
     */
    private Map<String, Long> weeklyPostStats;

    /**
     * 오사카 지역별 공고 수 통계 데이터
     * K: 구 이름, V: 등록 건수
     */
    private Map<String, Long> osakaWardStats;

    /**
     * 도쿄 지역별 공고 수 통계 데이터
     * K: 구 이름, V: 등록 건수
     */
    private Map<String, Long> tokyoWardStats;

    /**
     * 월별 신규 회원 수 통계 데이터
     * K: 월 이름, V: 가입자 수
     */
    private Map<String, Long> monthlyUserStats;
}