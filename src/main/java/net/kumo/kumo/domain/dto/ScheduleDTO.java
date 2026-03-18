package net.kumo.kumo.domain.dto;

import lombok.Data;

/**
 * 캘린더 라이브러리(FullCalendar 등)와 연동하여
 * 개별 일정 정보를 프론트엔드와 주고받기 위한 DTO 클래스입니다.
 */
@Data
public class ScheduleDTO {

    /** 일정 데이터의 고유 식별자 (수정 및 삭제 시 필수) */
    private Long scheduleId;

    private String title;
    private String description;

    /** 일정 시작 일시 (형식: "yyyy-MM-ddTHH:mm") */
    private String start;

    /** 일정 종료 일시 (형식: "yyyy-MM-ddTHH:mm") */
    private String end;

    /** 캘린더 이벤트 렌더링 색상 (Hex 코드 등) */
    private String color;

}