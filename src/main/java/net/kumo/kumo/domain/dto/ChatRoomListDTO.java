package net.kumo.kumo.domain.dto;

import lombok.*;

@Data // Getter, Setter, ToString 등
@Builder // .builder() 사용 가능하게 함
@NoArgsConstructor // 기본 생성자
@AllArgsConstructor // 모든 필드 생성자 (Builder 사용 시 필수)
public class ChatRoomListDTO {
    private Long roomId;
    private String opponentNickname;
    private String lastMessage;
    private String lastTime;
    private boolean hasUnread;
}