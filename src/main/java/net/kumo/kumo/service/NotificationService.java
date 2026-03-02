package net.kumo.kumo.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.NotificationResponseDTO;
import net.kumo.kumo.domain.entity.Enum;
import net.kumo.kumo.domain.entity.NotificationEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.NotificationRepository;
import net.kumo.kumo.repository.UserRepository;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {
	private  final NotificationRepository notificationRepository;
	private  final UserRepository userRepository;
	private final MessageSource messageSource; // 다국어 처리를 위해 필요
	
	public List<NotificationResponseDTO> getDtoList(String username, Locale locale) {
		UserEntity entity = userRepository.findByEmail(username)
				.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
		Long userId = entity.getUserId();
		List<NotificationEntity> notifications = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
		
		// 3. 엔티티 리스트를 DTO 리스트로 변환 (메시지 키 번역 포함)
		return notifications.stream()
				.map(notif -> {
					String translatedTitle;
					String translatedContent;

					if (notif.getNotifyType() == Enum.NotificationType.SCOUT_OFFER) {
						// 🌟 스카우트 제의는 특별 처리 (content에 저장된 닉네임을 인자로 사용)
						translatedTitle = messageSource.getMessage("noti.scout.title", null, "새로운 스카우트 제의!", locale);
						translatedContent = messageSource.getMessage("noti.scout.content", new Object[]{notif.getContent()}, locale);
					} else {
						// DB에 저장된 메시지 키를 실제 언어로 번역 (키가 없으면 원문 그대로 출력)
						translatedTitle = messageSource.getMessage(notif.getTitle(), null, notif.getTitle(), locale);
						translatedContent = messageSource.getMessage(notif.getContent(), null, notif.getContent(), locale);
					}
					
					return NotificationResponseDTO.builder()
							.notificationId(notif.getId())
							.type(notif.getNotifyType().name())
							.title(translatedTitle)
							.content(translatedContent)
							.targetUrl(notif.getTargetUrl())
							.isRead(notif.isRead())
							.createdAt(notif.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
							.build();
				})
				.collect(Collectors.toList());
		
	}
	
	@Transactional
	public void markAllAsRead(String username) {
		UserEntity user = userRepository.findByEmail(username)
				.orElseThrow(() -> new IllegalArgumentException("유저 없음"));
		
		notificationRepository.markAllAsRead(user.getUserId());
	}
	
	
	@Transactional // ★ 삭제 작업은 트랜잭션 필수! (없으면 에러 남)
	public void deleteAllNotifications(String username) {
		// 1. 유저 찾기
		UserEntity user = userRepository.findByEmail(username)
				.orElseThrow(() -> new IllegalArgumentException("유저 없음"));
		
		// 2. 해당 유저의 모든 알림 삭제
		notificationRepository.deleteAllByUserId(user.getUserId());
	}
	
	public void deleteNotification(Long id, String username) {
		// 1. 알림 조회 (없으면 에러)
		NotificationEntity notification = notificationRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("해당 알림이 존재하지 않습니다."));
		
		// 2. 소유자 체크 (남의 알림 삭제 방지)
		// username(이메일)과 알림 주인의 이메일이 같은지 확인
		if (!notification.getUser().getEmail().equals(username)) {
			throw new SecurityException("삭제 권한이 없습니다.");
		}
		
		// 3. 삭제 수행
		notificationRepository.delete(notification);
	}
	
	// 안 읽은 개수 조회 서비스
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public long countUnreadNotifications(String username) {
		UserEntity user = userRepository.findByEmail(username)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
		return notificationRepository.countByUser_UserIdAndIsReadFalse(user.getUserId());
	}

	@Transactional
	public void markAsRead(Long id, String username) {
		NotificationEntity notification = notificationRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("알림 없음"));
		if (!notification.getUser().getEmail().equals(username)) {
			throw new SecurityException("권한 없음");
		}
		notification.setRead(true);
	}
}
