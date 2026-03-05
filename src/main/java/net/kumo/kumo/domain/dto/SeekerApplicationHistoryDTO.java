package net.kumo.kumo.domain.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeekerApplicationHistoryDTO {
    private Long appId;
    private String targetSource;
    private Long targetPostId;
    private String title;
    private String businessName;
    private String location;
    private String wage;
    private String wageJp;
    private String contact;
    private String manager;
    private LocalDateTime appliedAt;
    private String status;
}
