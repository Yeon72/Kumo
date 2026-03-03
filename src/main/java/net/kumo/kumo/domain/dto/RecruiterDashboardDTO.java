package net.kumo.kumo.domain.dto;

import lombok.*;
import java.util.Map;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RecruiterDashboardDTO {
    private long totalApplicants;
    private long unreadApplicants;
    private long totalJobs;
    private long totalVisits;
    
    // 차트용 데이터: 날짜별 지원자 수 (최근 7일)
    private List<String> chartLabels; // ["02.25", "02.26", ...]
    private List<Long> chartData;     // [5, 12, 8, ...]
}
