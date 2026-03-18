package net.kumo.kumo.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import net.kumo.kumo.domain.dto.ScheduleDTO;
import net.kumo.kumo.domain.entity.ScheduleEntity;
import net.kumo.kumo.security.AuthenticatedUser;
import net.kumo.kumo.service.ScheduleService;

/**
 * 캘린더 기능(일정 조회, 등록, 수정, 삭제)에 대한 비동기 요청을 처리하는 API Controller 입니다.
 */
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarApiController {

    private final ScheduleService scheduleService;

    /**
     * 새로운 캘린더 일정을 저장합니다.
     *
     * @param dto  저장할 일정 정보를 담은 DTO 객체
     * @param user 현재 인증된 사용자(구인자) 정보
     * @return 성공 여부를 나타내는 상태 코드
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveEvent(@RequestBody ScheduleDTO dto,
                                       @AuthenticationPrincipal AuthenticatedUser user) {

        ScheduleEntity entity = new ScheduleEntity();
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setStartAt(LocalDateTime.parse(dto.getStart()));
        entity.setEndAt(LocalDateTime.parse(dto.getEnd()));
        entity.setColorCode(dto.getColor());

        scheduleService.saveSchedule(entity, user.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * 현재 로그인된 사용자의 모든 캘린더 일정을 조회합니다.
     * FullCalendar 라이브러리 포맷에 맞춘 이벤트 리스트를 반환합니다.
     *
     * @param user 현재 인증된 사용자 정보
     * @return 캘린더 이벤트 맵 객체의 리스트
     */
    @GetMapping("/events")
    public List<Map<String, Object>> getEvents(@AuthenticationPrincipal AuthenticatedUser user) {
        return scheduleService.getCalendarEvents(user.getUsername());
    }

    /**
     * 특정 캘린더 일정을 삭제합니다.
     * 휴지통 영역으로의 드래그 앤 드롭 이벤트를 통해 주로 호출됩니다.
     *
     * @param id 삭제할 일정의 식별자(ID)
     * @return 처리 결과 메시지를 포함한 응답 객체
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteCalendarEvent(@PathVariable("id") Long id) {
        try {
            scheduleService.deleteSchedule(id);
            return ResponseEntity.ok("일정이 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("일정 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 기존에 등록된 캘린더 일정을 수정합니다.
     * 모달창을 통한 상세 내용 변경 및 캘린더 내 드래그 앤 드롭(기간 변경) 시 호출됩니다.
     *
     * @param dto  수정할 내용이 담긴 일정 DTO 객체
     * @param user 현재 인증된 사용자 정보
     * @return 처리 결과 상태 코드
     */
    @PostMapping("/update")
    public ResponseEntity<?> updateEvent(@RequestBody ScheduleDTO dto,
                                         @AuthenticationPrincipal AuthenticatedUser user) {

        try {
            ScheduleEntity entity = new ScheduleEntity();
            entity.setScheduleId(dto.getScheduleId());
            entity.setTitle(dto.getTitle());
            entity.setDescription(dto.getDescription());
            entity.setStartAt(LocalDateTime.parse(dto.getStart()));
            entity.setEndAt(LocalDateTime.parse(dto.getEnd()));
            entity.setColorCode(dto.getColor());

            scheduleService.saveSchedule(entity, user.getUsername());

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.status(500).body("일정 수정에 실패했습니다: " + e.getMessage());
        }
    }
}