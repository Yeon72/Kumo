/**
 * 어드민 실시간 서버 로그 (DataOps) 스크립트
 */

let stompClient = null;

// HTML 문서(DOM)가 완전히 로드된 후 실행되도록 이벤트 리스너 사용
document.addEventListener('DOMContentLoaded', function() {
    const logOutput = document.getElementById('logOutput');
    const cursor = document.getElementById('cursor');

    function connectWebSocket() {
        // 서버의 웹소켓 엔드포인트 연결 (/ws-stomp)
        const socket = new SockJS('/ws-stomp');
        stompClient = Stomp.over(socket);

        // 개발자 도구에 STOMP 내부 동작 로그가 너무 많이 뜨는 것을 방지
        stompClient.debug = null;

        stompClient.connect({}, function (frame) {
            console.log('Admin Log WebSocket Connected: ' + frame);

            // 서버가 /topic/admin/logs 로 쏘는 메시지를 계속 기다림(구독)
            stompClient.subscribe('/topic/admin/logs', function (message) {
                appendLog(message.body);
            });

            appendLog("<span class='info'>[SYSTEM]</span> 실시간 서버 로그 수신을 시작합니다...");
        }, function(error) {
            console.error("WebSocket Connection Error: ", error);
            appendLog("<span class='red'>[ERROR]</span> 로그 서버와 연결이 끊어졌습니다. 새로고침 해주세요.");
        });
    }

    function appendLog(htmlContent) {
        if (!logOutput || !cursor) return;

        // 커서 앞에 새로운 로그(div) 삽입
        const newLogLine = document.createElement('div');
        newLogLine.innerHTML = htmlContent;
        logOutput.insertBefore(newLogLine, cursor);

        // 로그가 1000줄이 넘어가면 맨 윗줄부터 지워서 브라우저 메모리 폭발 방지
        if (logOutput.childElementCount > 1000) {
            logOutput.removeChild(logOutput.firstChild);
        }

        // 새 로그가 찍힐 때마다 스크롤을 맨 아래로 자동 이동
        logOutput.scrollTop = logOutput.scrollHeight;
    }

    // 스크립트가 로드되면 바로 웹소켓 연결 시작
    connectWebSocket();
});