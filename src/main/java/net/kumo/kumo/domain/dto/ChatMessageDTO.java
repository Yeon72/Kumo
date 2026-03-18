package net.kumo.kumo.domain.dto;

import lombok.*;

/**
 * 채팅방 내부에서 송수신되는 개별 메시지(텍스트, 이미지, 파일 등)의
 * 상세 데이터와 메타 정보를 담아 전달하기 위한 DTO 클래스입니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {

    private Long roomId;
    private Long senderId;

    /** 화면에 출력할 발신자의 닉네임 */
    private String senderNickname;

    /** 메시지 본문 (텍스트 내용 또는 파일/이미지 URL) */
    private String content;

    /** 메시지의 유형 (예: TEXT, IMAGE, FILE, READ 등) */
    private String messageType;

    private String fileName;

    /** 시/분 단위로 포맷팅된 메시지 발송 시간 (예: "17:05") */
    private String createdAt;

    /** 채팅창 내 날짜 변경선(Divider) 렌더링을 위한 포맷팅된 날짜 정보 */
    private String createdDate;

    /** 상대방의 메시지 수신(읽음) 여부 */
    private boolean isRead;

}