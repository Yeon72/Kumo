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

/**
 * 사용자(주로 구인자)의 캘린더 일정(Schedule)을 추가, 조회, 수정, 삭제하고,
 * 프론트엔드 라이브러리 규격에 맞게 데이터를 매핑하는 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    /**
     * 고유 식별자를 통해 단일 일정 정보를 조회하며, 보안을 위해 해당 일정이
     * 요청한 사용자(본인)의 소유인지 검증합니다.
     *
     * @param scheduleId 조회할 대상 일정 식별자
     * @param email      조회를 요청한 사용자의 이메일 계정
     * @return 조회된 일정 엔티티
     * @throws RuntimeException 일정을 찾을 수 없거나 열람 권한이 없을 경우 발생
     */
    @Transactional(readOnly = true)
    public ScheduleEntity getScheduleById(Long scheduleId, String email) {
        ScheduleEntity schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));

        if (!schedule.getUser().getEmail().equals(email)) {
            throw new RuntimeException("권한이 없습니다.");
        }
        return schedule;
    }

    /**
     * 사용자가 등록 요청한 신규 일정을 저장하거나 기존 일정 데이터를 수정(갱신)합니다.
     * 식별자(PK)가 존재할 경우 JPA의 더티 체킹(Dirty Checking)을 통해 Update 로직을 수행합니다.
     *
     * @param schedule 신규 등록 또는 수정할 데이터가 담긴 일정 엔티티
     * @param email    저장을 요청한 사용자의 이메일 계정
     */
    @Transactional
    public void saveSchedule(ScheduleEntity schedule, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        if (schedule.getScheduleId() != null) {
            ScheduleEntity existing = scheduleRepository.findById(schedule.getScheduleId())
                    .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));
            existing.setTitle(schedule.getTitle());
            existing.setDescription(schedule.getDescription());
            existing.setStartAt(schedule.getStartAt());
            existing.setEndAt(schedule.getEndAt());
            existing.setColorCode(schedule.getColorCode());
        } else {
            schedule.setUser(user);
            scheduleRepository.save(schedule);
        }
    }

    /**
     * 특정 사용자(구인자) 계정에 등록된 전체 일정 목록을 조회합니다.
     *
     * @param email 조회를 요청한 사용자 이메일 계정
     * @return 해당 사용자의 전체 일정 엔티티 리스트
     */
    @Transactional(readOnly = true)
    public List<ScheduleEntity> getSchedulesByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        return scheduleRepository.findByUser(user);
    }

    /**
     * 특정 일정 내역을 데이터베이스에서 완전히 삭제합니다.
     *
     * @param id 삭제할 일정의 고유 식별자
     */
    public void deleteSchedule(Long id) {
        scheduleRepository.deleteById(id);
    }

    /**
     * 특정 사용자의 전체 일정을 프론트엔드의 FullCalendar 라이브러리가 요구하는
     * JSON Key-Value 규격에 맞춰 Map 컬렉션으로 변환하여 반환합니다.
     *
     * @param email 조회를 요청한 사용자 이메일 계정
     * @return FullCalendar 파싱 규격에 맞춰진 일정 데이터 맵 리스트
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCalendarEvents(String email) {
        List<ScheduleEntity> schedules = getSchedulesByEmail(email);

        return schedules.stream().map(schedule -> {
            Map<String, Object> event = new HashMap<>();
            event.put("id", schedule.getScheduleId());
            event.put("title", schedule.getTitle());

            event.put("start", schedule.getStartAt());
            event.put("end", schedule.getEndAt());

            event.put("color", schedule.getColorCode());

            event.put("description", schedule.getDescription());

            return event;
        }).collect(Collectors.toList());
    }
}