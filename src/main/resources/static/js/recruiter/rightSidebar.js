/**
 * rightSidebar.js
 * 구인자 우측 사이드바의 채팅 목록 열기 및 읽지 않은 메시지 뱃지 카운트 갱신 로직을 담당합니다.
 */

/**
 * 전역 채팅 목록이 포함된 플로팅 채팅창을 활성화하여 화면에 표시합니다.
 * 필요한 DOM 요소가 존재하지 않을 경우 에러를 로깅하고 사용자에게 알림을 띄웁니다.
 */
function openGlobalChatList() {
    const chatContainer = document.getElementById('floatingChatContainer');
    const chatFrame = document.getElementById('floatingChatFrame');

    if (!chatContainer || !chatFrame) {
        console.error("[Sidebar] 플로팅 채팅창 HTML 요소를 찾을 수 없습니다.");
        alert("채팅창을 열 수 없습니다.");
        return;
    }

    chatFrame.src = "/chat/list";
    chatContainer.style.display = 'flex';
    chatContainer.classList.remove('minimized');
}

/**
 * 서버에 비동기 통신을 요청하여 읽지 않은 채팅 메시지 개수를 조회하고,
 * 사이드바의 알림 뱃지 UI(숫자 렌더링 및 표시 여부)를 동적으로 갱신합니다.
 */
function updateSidebarBadge() {
    fetch('/api/chat/unread-count')
        .then(res => {
            if (!res.ok) throw new Error("로그인 안됨");
            return res.json();
        })
        .then(count => {
            const badge = document.getElementById('side-unread-badge');
            if (badge) {
                if (count > 0) {
                    badge.textContent = count;
                    badge.style.display = 'flex';
                } else {
                    badge.style.display = 'none';
                }
            }
        })
        .catch(err => console.log("[Sidebar] 알림 뱃지 대기 중..."));
}

/**
 * DOMContentLoaded 이벤트 리스너
 * 페이지 로드 직후 알림 뱃지 데이터를 1회 갱신하며,
 * 이후 10초(10000ms) 주기로 백그라운드에서 자동 동기화하도록 인터벌을 설정합니다.
 */
document.addEventListener("DOMContentLoaded", function() {
    updateSidebarBadge();
    setInterval(updateSidebarBadge, 10000);
});