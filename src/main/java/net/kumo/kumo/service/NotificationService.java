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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MessageSource messageSource;

    /**
     * 알림 목록 조회 및 다국어 번역 (JS로 전달될 최종 형태)
     */
    public List<NotificationResponseDTO> getDtoList(String username, Locale locale) {
        UserEntity entity = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        List<NotificationEntity> notifications = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(entity.getUserId());

        return notifications.stream()
                .map(notif -> {
                    String translatedTitle;
                    String translatedContent;
                    Object[] args = new Object[]{notif.getContent()}; // content에 저장된 값을 인자로 활용

                    // Enum 타입에 따라 메시지 구성 결정
                    switch (notif.getNotifyType()) {
                        case APP_PASSED:
                            translatedTitle = messageSource.getMessage("notify.app.pass.title", null, "서류 합격 안내", locale);
                            translatedContent = messageSource.getMessage("notify.app.pass.content", args, "축하합니다! 합격하셨습니다.", locale);
                            break;
                        case APP_FAILED:
                            translatedTitle = messageSource.getMessage("notify.app.fail.title", null, "서류 전형 결과 안내", locale);
                            translatedContent = messageSource.getMessage("notify.app.fail.content", args, "아쉽지만 이번에는 불합격하셨습니다.", locale);
                            break;
                        case APP_COMPLETED:
                            translatedTitle = messageSource.getMessage("notify.app.completed.title", null, "구인 신청 완료", locale);
                            translatedContent = messageSource.getMessage("notify.app.completed.content", args, "구인 신청이 성공적으로 접수되었습니다.", locale);
                            break;
                        case JOB_CLOSED:
                            translatedTitle = messageSource.getMessage("notify.job.closed.title", null, "지원 공고 마감", locale);
                            translatedContent = messageSource.getMessage("notify.job.closed.content", args, "지원하신 공고가 마감되었습니다.", locale);
                            break;
                        case NEW_APPLICANT:
                            translatedTitle = messageSource.getMessage("notify.new.applicant.title", null, "신규 지원자 발생", locale);
                            translatedContent = messageSource.getMessage("notify.new.applicant.content", args, "새로운 지원자가 발생했습니다.", locale);
                            break;
                        case SCOUT_OFFER:
                            translatedTitle = messageSource.getMessage("noti.scout.title", null, "스카우트 제의", locale);
                            translatedContent = messageSource.getMessage("noti.scout.content", args, "스카우트 제의가 도착했습니다.", locale);
                            break;
                        case REPORT_RESULT:
                            translatedTitle = messageSource.getMessage("notify.report.result.title", null, "신고 접수 안내", locale);
                            translatedContent = messageSource.getMessage("notify.report.result.content", args, "공고에 대한 신고가 접수되었습니다.", locale);
                            break;
                        default:
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

    // --- 알림 생성 전용 메서드들 (서비스에서 호출) ---

    /**
     * 1. 지원 상태 변경 알림 (합격/불합격)
     */
    public void sendAppStatusNotification(UserEntity seeker, Enum.ApplicationStatus status, String jobTitle) {
        Enum.NotificationType type = (status == Enum.ApplicationStatus.PASSED) ? 
                Enum.NotificationType.APP_PASSED : Enum.NotificationType.APP_FAILED;
        
        createNotification(seeker, type, null, jobTitle, "/Seeker/history");
    }

    /**
     * 2. 구인 신청 완료 알림 (구직자용)
     */
    public void sendAppCompletedNotification(UserEntity seeker, String jobTitle, Long postId, String source) {
        createNotification(seeker, Enum.NotificationType.APP_COMPLETED, null, jobTitle, 
                "/map/jobs/detail?id=" + postId + "&source=" + source);
    }

    /**
     * 3. 신규 지원자 발생 알림 (구인자용)
     */
    public void sendNewApplicantNotification(UserEntity recruiter, String seekerNickname) {
        createNotification(recruiter, Enum.NotificationType.NEW_APPLICANT, null, seekerNickname, "/Recruiter/ApplicantInfo");
    }

    /**
     * 4. 공고 마감 알림 (구직자용)
     */
    public void sendJobClosedNotification(UserEntity seeker, String jobTitle, Long postId, String source) {
        createNotification(seeker, Enum.NotificationType.JOB_CLOSED, null, jobTitle, 
                "/map/jobs/detail?id=" + postId + "&source=" + source);
    }

    /**
     * 5. 공고 신고 접수 및 수정 요청 알림 (구인자용)
     */
    public void sendReportNotification(UserEntity recruiter, String jobTitle, Long postId, String source) {
        createNotification(recruiter, Enum.NotificationType.REPORT_RESULT, null, jobTitle, 
                "/Recruiter/editJobPosting?id=" + postId + "&region=" + source);
    }

    /**
     * 공통 알림 저장 로직
     */
    private void createNotification(UserEntity user, Enum.NotificationType type, String title, String content, String targetUrl) {
        // DB의 NOT NULL 제약조건 충돌을 방지하기 위해 title이 null이면 타입명을 기본값으로 저장
        String finalTitle = (title != null) ? title : type.name();
        
        NotificationEntity notification = NotificationEntity.builder()
                .user(user)
                .notifyType(type)
                .title(finalTitle) 
                .content(content)
                .targetUrl(targetUrl)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String username) {
        UserEntity user = userRepository.findByEmail(username).orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        notificationRepository.markAllAsRead(user.getUserId());
    }

    @Transactional
    public void deleteAllNotifications(String username) {
        UserEntity user = userRepository.findByEmail(username).orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        notificationRepository.deleteAllByUserId(user.getUserId());
    }

    public void deleteNotification(Long id, String username) {
        NotificationEntity notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 알림이 존재하지 않습니다."));
        if (!notification.getUser().getEmail().equals(username)) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }
        notificationRepository.delete(notification);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public long countUnreadNotifications(String username) {
        UserEntity user = userRepository.findByEmail(username).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepository.countByUser_UserIdAndIsReadFalse(user.getUserId());
    }

    @Transactional
    public void markAsRead(Long id, String username) {
        NotificationEntity notification = notificationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("알림 없음"));
        if (!notification.getUser().getEmail().equals(username)) {
            throw new SecurityException("권한 없음");
        }
        notification.setRead(true);
    }
}
