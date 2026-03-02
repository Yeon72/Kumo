package net.kumo.kumo.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.NotificationResponseDTO;
import net.kumo.kumo.domain.entity.NotificationEntity;
import net.kumo.kumo.service.NotificationService;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationController {
	
	private final NotificationService notificationService;
	private final MessageSource messageSource;
	
	
	@GetMapping("/api/notifications")
	@ResponseBody
	public ResponseEntity<List<NotificationResponseDTO>> getNotifications(@AuthenticationPrincipal UserDetails user, Locale locale){
		
		List<NotificationResponseDTO> dtoList = notificationService.getDtoList(user.getUsername(),locale);
		
		return ResponseEntity.ok(dtoList);
	
	}
	
	// [추가] 모두 읽음 처리 API
	@PatchMapping("/api/notifications/read-all")
	public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetails user) {
		notificationService.markAllAsRead(user.getUsername());
		return ResponseEntity.ok().build();
	}
	
	// [기존] 전체 삭제 API (DELETE)
	@DeleteMapping("/api/notifications")
	public ResponseEntity<Void> deleteAllNotifications(@AuthenticationPrincipal UserDetails user) {
		notificationService.deleteAllNotifications(user.getUsername());
		return ResponseEntity.ok().build();
	}
	
	// [1] 개별 삭제 (ID 받음) - ★ 이게 없어서 에러 났을 확률 99%
	@DeleteMapping("/api/notifications/{id}")
	public ResponseEntity<Void> deleteNotification(
			@AuthenticationPrincipal UserDetails user,
			@PathVariable Long id) {
		
		// 서비스에 "이 ID 지워줘" 요청
		notificationService.deleteNotification(id, user.getUsername());
		
		return ResponseEntity.ok().build();
	}
	
	// 안 읽은 알림 개수 조회 API
	@GetMapping("/api/notifications/unread-count")
	public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal UserDetails user) {
		long count = notificationService.countUnreadNotifications(user.getUsername());
		return ResponseEntity.ok(count);
	}

	// [추가] 알림 개별 읽음 처리 API
	@PatchMapping("/api/notifications/{id}/read")
	public ResponseEntity<Void> markAsRead(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
		notificationService.markAsRead(id, user.getUsername());
		return ResponseEntity.ok().build();
	}
	
	
	
}
