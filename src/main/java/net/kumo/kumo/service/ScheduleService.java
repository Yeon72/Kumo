package net.kumo.kumo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import net.kumo.kumo.domain.entity.ScheduleEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.ScheduleRepository;
import net.kumo.kumo.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    /**
     * 단일 일정 조회
     * 
     * @param scheduleId
     * @param email
     * @return
     */
    @Transactional(readOnly = true)
    public ScheduleEntity getScheduleById(Long scheduleId, String email) {
        ScheduleEntity schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));

        // 본인 일정인지 검증
        if (!schedule.getUser().getEmail().equals(email)) {
            throw new RuntimeException("권한이 없습니다.");
        }
        return schedule;
    }

    /**
     * 일정 수정
     * 
     * @param schedule
     * @param email
     */
    @Transactional // ← 이게 없으면 dirty checking 안 됨!
    public void saveSchedule(ScheduleEntity schedule, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        if (schedule.getScheduleId() != null) {
            ScheduleEntity existing = scheduleRepository.findById(schedule.getScheduleId()) // ← 오타 수정
                    .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));
            existing.setTitle(schedule.getTitle());
            existing.setDescription(schedule.getDescription());
            existing.setStartAt(schedule.getStartAt());
            existing.setEndAt(schedule.getEndAt());
            existing.setColorCode(schedule.getColorCode());
            // @Transactional 있으면 save() 없어도 자동 update ✅
        } else {
            schedule.setUser(user);
            scheduleRepository.save(schedule);
        }
    }

    /**
     * 현재 리크루터의 모든 일정 가져오기
     */
    @Transactional(readOnly = true)
    public List<ScheduleEntity> getSchedulesByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        return scheduleRepository.findByUser(user);
    }

    /**
     * 일정 삭제
     */
    public void deleteSchedule(Long id) {
        scheduleRepository.deleteById(id);
    }

    /**
     * FullCalendar 전용 포맷으로 변환하여 가져오기
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCalendarEvents(String email) {
        // 1. 해당 유저의 모든 일정을 가져옴
        List<ScheduleEntity> schedules = getSchedulesByEmail(email);

        // 2. FullCalendar가 요구하는 Key값으로 변환 (엔티티 -> Map)
        return schedules.stream().map(schedule -> {
            Map<String, Object> event = new HashMap<>();
            event.put("id", schedule.getScheduleId());
            event.put("title", schedule.getTitle());

            // ⚠️ FullCalendar는 'start'와 'end'라는 이름을 사용합니다.
            event.put("start", schedule.getStartAt());
            event.put("end", schedule.getEndAt());

            // 🎨 이미지에서 선택했던 색상 적용
            event.put("color", schedule.getColorCode());

            // 추가 정보 (상세설명 등)
            event.put("description", schedule.getDescription());

            return event;
        }).collect(Collectors.toList());
    }
}