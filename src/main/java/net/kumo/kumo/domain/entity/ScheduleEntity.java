package net.kumo.kumo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자(주로 구인자)의 캘린더 일정 정보를 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleId;

    /** 일정을 소유하고 있는 사용자 정보 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    /** 캘린더에 표시될 일정 제목 */
    @Column(nullable = false)
    private String title;

    /** 일정에 대한 상세 설명 본문 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 일정 시작 일시 */
    @Column(nullable = false, name = "start_at")
    private LocalDateTime startAt;

    /** 일정 종료 일시 */
    @Column(nullable = false, name = "end_at")
    private LocalDateTime endAt;

    /** 캘린더 UI 렌더링을 위한 헥스(Hex) 색상 코드 (예: #ff6b6b) */
    @Column(name = "color_code")
    private String colorCode;

}