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

/**
 * 플랫폼 전역의 시스템 알림(Notification) 생성, 다국어 처리 기반 조회,
 * 열람 상태 변경 및 내역 삭제 로직을 총괄하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MessageSource messageSource;

    /**
     * 특정 사용자가 수신한 알림 목록 전체를 최신순으로 조회하며,
     * 클라이언트의 로케일 설정에 맞춰 다국어 번역을 적용한 DTO 형태로 변환하여 반환합니다.
     *
     * @param username 조회를 요청한 사용자의 계정(이메일)
     * @param locale   다국어 번역에 적용할 현재 로케일
     * @return 번역이 완료된 시스템 알림 DTO 리스트
     */
    public List<NotificationResponseDTO> getDtoList(String username, Locale locale) {
        UserEntity entity = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        List<NotificationEntity> notifications = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(entity.getUserId());

        return notifications.stream()
                .map(notif -> {
                    String translatedTitle;
                    String translatedContent;
                    Object[] args = new Object[]{notif.getContent()};

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

    /**
     * 구직자의 지원서 심사 결과(합격 또는 불합격)에 대한 알림을 생성하여 발송합니다.
     *
     * @param seeker   알림을 수신할 구직자 엔티티
     * @param status   처리된 지원 상태(PASSED, FAILED)
     * @param jobTitle 지원 대상 공고의 제목
     */
    public void sendAppStatusNotification(UserEntity seeker, Enum.ApplicationStatus status, String jobTitle) {
        Enum.NotificationType type = (status == Enum.ApplicationStatus.PASSED) ?
                Enum.NotificationType.APP_PASSED : Enum.NotificationType.APP_FAILED;

        createNotification(seeker, type, null, jobTitle, "/Seeker/history");
    }

    /**
     * 구직자가 특정 공고에 입사 지원을 성공적으로 완료했을 때 시스템 알림을 생성하여 발송합니다.
     *
     * @param seeker   알림을 수신할 구직자 엔티티
     * @param jobTitle 지원 대상 공고의 제목
     * @param postId   지원 대상 공고 식별자
     * @param source   지원 대상 공고 데이터 출처
     */
    public void sendAppCompletedNotification(UserEntity seeker, String jobTitle, Long postId, String source) {
        createNotification(seeker, Enum.NotificationType.APP_COMPLETED, null, jobTitle,
                "/map/jobs/detail?id=" + postId + "&source=" + source);
    }

    /**
     * 작성한 공고에 새로운 구직자가 지원했을 때 구인자에게 시스템 알림을 생성하여 발송합니다.
     *
     * @param recruiter      알림을 수신할 구인자 엔티티
     * @param seekerNickname 새롭게 지원한 구직자의 닉네임
     */
    public void sendNewApplicantNotification(UserEntity recruiter, String seekerNickname) {
        createNotification(recruiter, Enum.NotificationType.NEW_APPLICANT, null, seekerNickname, "/Recruiter/ApplicantInfo");
    }

    /**
     * 구직자가 지원 중인 공고의 모집 상태가 마감(CLOSED)으로 변경되었을 때 알림을 생성하여 발송합니다.
     *
     * @param seeker   알림을 수신할 구직자 엔티티
     * @param jobTitle 마감된 공고의 제목
     * @param postId   마감된 공고 식별자
     * @param source   마감된 공고 데이터 출처
     */
    public void sendJobClosedNotification(UserEntity seeker, String jobTitle, Long postId, String source) {
        createNotification(seeker, Enum.NotificationType.JOB_CLOSED, null, jobTitle,
                "/map/jobs/detail?id=" + postId + "&source=" + source);
    }

    /**
     * 작성한 공고에 대하여 사용자 신고가 접수되었을 때 구인자에게 시스템 알림을 생성하여 발송합니다.
     *
     * @param recruiter 알림을 수신할 구인자 엔티티
     * @param jobTitle  신고가 접수된 공고의 제목
     * @param postId    신고 대상 공고 식별자
     * @param source    신고 대상 공고 데이터 출처
     */
    public void sendReportNotification(UserEntity recruiter, String jobTitle, Long postId, String source) {
        createNotification(recruiter, Enum.NotificationType.REPORT_RESULT, null, jobTitle,
                "/Recruiter/editJobPosting?id=" + postId + "&region=" + source);
    }

    /**
     * 알림 엔티티를 조립하고 데이터베이스에 최종 영속화하는 내부 공통 로직입니다.
     * 데이터베이스 제약 조건 충돌 방지를 위해 명시된 제목이 없을 경우 유형의 이름을 기본 제목으로 저장합니다.
     *
     * @param user      알림을 수신할 사용자 엔티티
     * @param type      알림 발생 유형 열거형
     * @param title     알림의 제목 (null 허용)
     * @param content   알림의 상세 내용
     * @param targetUrl 알림 클릭 시 이동할 URL 경로
     */
    private void createNotification(UserEntity user, Enum.NotificationType type, String title, String content, String targetUrl) {
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

    /**
     * 특정 사용자가 수신한 모든 알림의 상태를 일괄 '읽음(true)' 처리합니다.
     *
     * @param username 상태를 갱신할 사용자의 계정(이메일)
     */
    @Transactional
    public void markAllAsRead(String username) {
        UserEntity user = userRepository.findByEmail(username).orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        notificationRepository.markAllAsRead(user.getUserId());
    }

    /**
     * 특정 사용자가 수신한 전체 알림 내역을 데이터베이스에서 일괄 삭제합니다.
     *
     * @param username 내역을 삭제할 사용자의 계정(이메일)
     */
    @Transactional
    public void deleteAllNotifications(String username) {
        UserEntity user = userRepository.findByEmail(username).orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        notificationRepository.deleteAllByUserId(user.getUserId());
    }

    /**
     * 특정 알림 내역 1건을 삭제합니다. 알림의 소유자와 삭제 요청자가 일치하는지 권한 검증을 수행합니다.
     *
     * @param id       삭제할 알림 식별자
     * @param username 권한을 검증할 사용자의 계정(이메일)
     * @throws SecurityException 권한이 유효하지 않을 때 발생
     */
    public void deleteNotification(Long id, String username) {
        NotificationEntity notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 알림이 존재하지 않습니다."));
        if (!notification.getUser().getEmail().equals(username)) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }
        notificationRepository.delete(notification);
    }

    /**
     * 특정 사용자가 수신한 알림 중 아직 열람하지 않은 알림의 개수를 계산합니다.
     *
     * @param username 조회를 요청한 사용자의 계정(이메일)
     * @return 미열람 알림 개수
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public long countUnreadNotifications(String username) {
        UserEntity user = userRepository.findByEmail(username).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepository.countByUser_UserIdAndIsReadFalse(user.getUserId());
    }

    /**
     * 특정 알림 내역 1건의 상태를 '읽음(true)'으로 개별 갱신합니다.
     *
     * @param id       상태를 갱신할 알림 식별자
     * @param username 권한을 검증할 사용자의 계정(이메일)
     * @throws SecurityException 권한이 유효하지 않을 때 발생
     */
    @Transactional
    public void markAsRead(Long id, String username) {
        NotificationEntity notification = notificationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("알림 없음"));
        if (!notification.getUser().getEmail().equals(username)) {
            throw new SecurityException("권한 없음");
        }
        notification.setRead(true);
    }
}