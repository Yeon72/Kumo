/**
 * 관리자 전용 실시간 서버 로그(DataOps) 스트리밍 스크립트입니다.
 * 웹소켓(SockJS + STOMP)을 이용하여 서버 로그를 실시간으로 수신하고 화면에 렌더링합니다.
 */

/**
 * STOMP 클라이언트 인스턴스를 저장하는 전역 변수
 * @type {Object|null}
 */
let stompClient = null;

document.addEventListener('DOMContentLoaded', function() {
    const logOutput = document.getElementById('logOutput');
    const cursor = document.getElementById('cursor');

    /**
     * 서버의 웹소켓 엔드포인트에 연결을 시도하고,
     * 시스템 로그 스트리밍 토픽('/topic/admin/logs')을 구독합니다.
     */
    function connectWebSocket() {
        const socket = new SockJS('/ws-stomp');
        stompClient = Stomp.over(socket);

        stompClient.debug = null;

        stompClient.connect({}, function (frame) {
            console.log('Admin Log WebSocket Connected: ' + frame);

            stompClient.subscribe('/topic/admin/logs', function (message) {
                appendLog(message.body);
            });

            appendLog("<span class='info'>[SYSTEM]</span> 실시간 서버 로그 수신을 시작합니다...");
        }, function(error) {
            console.error("WebSocket Connection Error: ", error);
            appendLog("<span class='red'>[ERROR]</span> 로그 서버와 연결이 끊어졌습니다. 새로고침 해주세요.");
        });
    }

    /**
     * 수신된 로그 데이터를 DOM에 삽입하여 화면에 렌더링합니다.
     * 브라우저 렌더링 부하 및 메모리 누수를 방지하기 위해 최대 1,000줄의 로그만 유지하며,
     * 새로운 로그가 추가될 때마다 스크롤을 최하단으로 자동 이동시킵니다.
     *
     * @param {string} htmlContent 서버로부터 수신된 HTML 텍스트 포맷의 로그 문자열
     */
    function appendLog(htmlContent) {
        if (!logOutput || !cursor) return;

        const newLogLine = document.createElement('div');
        newLogLine.innerHTML = htmlContent;
        logOutput.insertBefore(newLogLine, cursor);

        if (logOutput.childElementCount > 1000) {
            logOutput.removeChild(logOutput.firstChild);
        }

        logOutput.scrollTop = logOutput.scrollHeight;
    }

    connectWebSocket();
});