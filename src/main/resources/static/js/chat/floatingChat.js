/* --- src/main/resources/static/js/chat/floatingChat.js --- */

// 1. 최소화 토글
function toggleMinimizeChat() {
    const container = document.getElementById('floatingChatContainer');
    if (container) container.classList.toggle('minimized');
}

// 2. 창 닫기 (웹소켓 연결도 프레임 비우면서 자동 해제됨)
function closeFloatingChat() {
    const container = document.getElementById('floatingChatContainer');
    const chatFrame = document.getElementById('floatingChatFrame');

    if (container) container.style.display = 'none';
    if (chatFrame) chatFrame.src = '';
}

// 3. 전역 채팅 목록 열기 (사이드바 버튼 등에서 호출)
function openGlobalChatList() {
    const chatContainer = document.getElementById('floatingChatContainer');
    const chatFrame = document.getElementById('floatingChatFrame');

    if (!chatContainer || !chatFrame) {
        console.error("🚨 채팅창 요소를 찾을 수 없습니다.");
        return;
    }

    chatFrame.src = "/chat/list";
    chatContainer.style.display = 'flex';
    chatContainer.classList.remove('minimized');
}

// 4. 마우스 드래그 기능 세팅
document.addEventListener('DOMContentLoaded', () => {
    const chatContainer = document.getElementById('floatingChatContainer');
    const chatHeader = document.getElementById('floatingChatHeader');

    if (!chatContainer || !chatHeader) return;

    let isDragging = false;
    let dragOffsetX, dragOffsetY;

    chatHeader.addEventListener('mousedown', (e) => {
        isDragging = true;
        const rect = chatContainer.getBoundingClientRect();
        dragOffsetX = e.clientX - rect.left;
        dragOffsetY = e.clientY - rect.top;

        chatContainer.style.transition = 'none'; // 드래그 중 애니메이션 끄기
        document.getElementById('floatingChatFrame').style.pointerEvents = 'none'; // iframe 클릭 간섭 방지
    });

    document.addEventListener('mousemove', (e) => {
        if (!isDragging) return;

        let newX = e.clientX - dragOffsetX;
        let newY = e.clientY - dragOffsetY;

        chatContainer.style.bottom = 'auto';
        chatContainer.style.right = 'auto';
        chatContainer.style.left = newX + 'px';
        chatContainer.style.top = newY + 'px';
    });

    document.addEventListener('mouseup', () => {
        if (isDragging) {
            isDragging = false;
            chatContainer.style.transition = 'height 0.3s ease'; // 애니메이션 복구
            document.getElementById('floatingChatFrame').style.pointerEvents = 'auto';
        }
    });
});