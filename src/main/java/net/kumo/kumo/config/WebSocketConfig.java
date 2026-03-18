package net.kumo.kumo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 실시간 통신을 위한 WebSocket 및 STOMP 메시지 브로커 설정 클래스입니다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 클라이언트가 웹소켓 서버에 연결하기 위한 STOMP 엔드포인트를 등록합니다.
     *
     * @param registry StompEndpointRegistry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*") // CORS 이슈 방지를 위한 모든 도메인 접근 허용
                .withSockJS(); // 구형 브라우저 환경을 위한 SockJS Fallback 지원
    }

    /**
     * 메시지 라우팅을 담당하는 메시지 브로커를 구성합니다.
     *
     * @param registry MessageBrokerRegistry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 메시지를 구독(Subscribe)할 때 사용하는 프리픽스 설정
        registry.enableSimpleBroker("/sub");

        // 클라이언트가 서버로 메시지를 발행(Publish)할 때 사용하는 프리픽스 설정
        registry.setApplicationDestinationPrefixes("/pub");
    }
}