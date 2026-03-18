package net.kumo.kumo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 클라이언트(프론트엔드)로 시스템 알림 데이터를 전달하기 위한 DTO 클래스입니다.
 * JSON 직렬화 시 발생할 수 있는 필드명 매핑 이슈를 방지하기 위해 @JsonProperty를 명시적으로 선언합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDTO {

    /** 알림 고유 식별자 */
    @JsonProperty("notificationId")
    private Long notificationId;

    /** * 알림 열람 여부
     * Lombok의 boolean Getter 네이밍 규칙(isRead)과 JSON 직렬화 간의 불일치를 방지합니다.
     */
    @JsonProperty("read")
    private boolean isRead;

    private String title;
    private String content;
    private String type;

    /** 알림 클릭 시 리다이렉트될 목적지 URL */
    private String targetUrl;

    @JsonProperty("createdAt")
    private String createdAt;
}