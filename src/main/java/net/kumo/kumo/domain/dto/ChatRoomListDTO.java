package net.kumo.kumo.domain.dto;

import lombok.*;

/**
 * 사용자의 채팅방 목록(Lobby) 화면을 렌더링하기 위해 필요한
 * 개별 채팅방의 요약 정보를 담는 DTO 클래스입니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomListDTO {

    private Long roomId;
    private String opponentNickname;
    private String opponentProfileImg;
    private String lastMessage;
    private String lastTime;
    private boolean hasUnread;

}