package net.kumo.kumo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.NotificationResponseDTO;
import net.kumo.kumo.service.NotificationService;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

/**
 * 사용자별 시스템 알림(Notification)의 조회, 상태 변경, 삭제 처리를 담당하는 Controller 클래스입니다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final MessageSource messageSource;

    /**
     * 현재 인증된 사용자의 모든 알림 목록을 다국어 설정에 맞게 조회합니다.
     *
     * @param user   현재 인증된 사용자 정보
     * @param locale 클라이언트 로케일 정보
     * @return 알림 정보 리스트를 포함한 ResponseEntity
     */
    @GetMapping("/api/notifications")
    @ResponseBody
    public ResponseEntity<List<NotificationResponseDTO>> getNotifications(
            @AuthenticationPrincipal UserDetails user,
            Locale locale) {
        List<NotificationResponseDTO> dtoList = notificationService.getDtoList(user.getUsername(), locale);
        return ResponseEntity.ok(dtoList);
    }

    /**
     * 현재 인증된 사용자의 모든 알림을 '읽음' 상태로 일괄 처리합니다.
     *
     * @param user 현재 인증된 사용자 정보
     * @return 처리 성공 상태를 포함한 ResponseEntity
     */
    @PatchMapping("/api/notifications/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetails user) {
        notificationService.markAllAsRead(user.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 알림의 상태를 '읽음'으로 단건 처리합니다.
     *
     * @param id   상태를 변경할 알림 식별자
     * @param user 현재 인증된 사용자 정보
     * @return 처리 성공 상태를 포함한 ResponseEntity
     */
    @PatchMapping("/api/notifications/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        notificationService.markAsRead(id, user.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * 현재 인증된 사용자의 아직 읽지 않은 알림 개수를 반환합니다.
     *
     * @param user 현재 인증된 사용자 정보
     * @return 읽지 않은 알림 개수(Long)를 포함한 ResponseEntity
     */
    @GetMapping("/api/notifications/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal UserDetails user) {
        long count = notificationService.countUnreadNotifications(user.getUsername());
        return ResponseEntity.ok(count);
    }

    /**
     * 현재 인증된 사용자의 모든 알림 내역을 시스템에서 삭제합니다.
     *
     * @param user 현재 인증된 사용자 정보
     * @return 처리 성공 상태를 포함한 ResponseEntity
     */
    @DeleteMapping("/api/notifications")
    public ResponseEntity<Void> deleteAllNotifications(@AuthenticationPrincipal UserDetails user) {
        notificationService.deleteAllNotifications(user.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 알림 내역을 시스템에서 단건 삭제합니다.
     *
     * @param user 현재 인증된 사용자 정보
     * @param id   삭제할 알림 식별자
     * @return 처리 성공 상태를 포함한 ResponseEntity
     */
    @DeleteMapping("/api/notifications/{id}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        notificationService.deleteNotification(id, user.getUsername());
        return ResponseEntity.ok().build();
    }
}