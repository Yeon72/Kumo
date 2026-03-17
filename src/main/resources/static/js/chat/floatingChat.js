/**
 * 현재 애플리케이션의 다국어(언어) 설정 값을 감지하여 반환하는 전역 함수입니다.
 * URL 파라미터, HTML lang 속성, 쿠키 값을 순차적으로 확인하여 언어 코드를 결정합니다.
 *
 * @returns {string} 감지된 언어 코드 (예: 'ja', 'kr')
 */
window.getKumoLang = function() {
    const urlLang = new URLSearchParams(window.location.search).get('lang');
    if (urlLang) return urlLang;

    const htmlLang = document.documentElement.lang;
    if (htmlLang === 'ja') return 'ja';

    const cookieLang = document.cookie.split('; ').find(row => row.startsWith('lang='))?.split('=')[1];
    if (cookieLang) return cookieLang;

    return 'kr';
};

/**
 * 플로팅 채팅창의 최소화(Minimize) 상태를 토글합니다.
 */
function toggleMinimizeChat() {
    const container = document.getElementById('floatingChatContainer');
    if (container) container.classList.toggle('minimized');
}

/**
 * 플로팅 채팅창을 완전히 닫고, 내부 iframe의 소스(src)를 초기화하여 메모리를 확보합니다.
 */
function closeFloatingChat() {
    const container = document.getElementById('floatingChatContainer');
    const chatFrame = document.getElementById('floatingChatFrame');

    if (container) container.style.display = 'none';
    if (chatFrame) chatFrame.src = '';
}

/**
 * 전역 채팅 목록 화면을 플로팅 창의 iframe 내부에 로드하고 화면에 표시합니다.
 * 현재 클라이언트의 다국어 설정 코드를 URL 파라미터로 함께 전달합니다.
 */
function openGlobalChatList() {
    const chatContainer = document.getElementById('floatingChatContainer');
    const chatFrame = document.getElementById('floatingChatFrame');

    if (!chatContainer || !chatFrame) return;

    const currentLang = window.getKumoLang();

    chatFrame.src = `/chat/list?lang=${currentLang}`;
    chatContainer.style.display = 'flex';
    chatContainer.classList.remove('minimized');
}

/**
 * DOMContentLoaded 이벤트 리스너
 * 플로팅 채팅창 헤더를 마우스로 클릭하여 화면 내에서 자유롭게 드래그 앤 드롭으로
 * 이동시킬 수 있도록 좌표 계산 및 마우스 이벤트를 바인딩합니다.
 */
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

        chatContainer.style.transition = 'none';
        document.getElementById('floatingChatFrame').style.pointerEvents = 'none';
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
            chatContainer.style.transition = 'height 0.3s ease';
            document.getElementById('floatingChatFrame').style.pointerEvents = 'auto';
        }
    });
});