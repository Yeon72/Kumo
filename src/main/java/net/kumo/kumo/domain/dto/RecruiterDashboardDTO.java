package net.kumo.kumo.domain.dto;

import lombok.*;
import java.util.Map;
import java.util.List;

/**
 * 구인자의 메인 대시보드 화면에 노출될 누적 통계 데이터 및
 * 차트 렌더링용 시계열 데이터를 담는 DTO 클래스입니다.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RecruiterDashboardDTO {

    /** 누적 전체 지원자 수 */
    private long totalApplicants;

    /** 이력서를 아직 열람하지 않은(미확인) 지원자 수 */
    private long unreadApplicants;

    /** 현재 등록되어 진행 중인 누적 공고 수 */
    private long totalJobs;

    /** 구인자 대시보드 또는 공고 누적 방문자 수 */
    private long totalVisits;

    /** 차트 X축 (예: ["02.25", "02.26"]) 데이터 리스트 */
    private List<String> chartLabels;

    /** 차트 Y축 (예: [5, 12]) 일자별 지원자 수 리스트 */
    private List<Long> chartData;

}