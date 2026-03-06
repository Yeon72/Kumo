/* --- src/main/resources/static/js/chat/floatingChat.js --- */

// 🌟 [1] 모든 JS 파일에서 공통으로 쓸 언어 감지 전역 함수
window.getKumoLang = function() {
    // 1순위: URL 파라미터 (?lang=ja)
    const urlLang = new URLSearchParams(window.location.search).get('lang');
    if (urlLang) return urlLang;

    // 2순위: HTML 태그의 lang 속성 (<html lang="ja">)
    const htmlLang = document.documentElement.lang;
    if (htmlLang === 'ja') return 'ja';

    // 3순위: 브라우저 쿠키 (이름이 'lang'인 쿠키)
    const cookieLang = document.cookie.split('; ').find(row => row.startsWith('lang='))?.split('=')[1];
    if (cookieLang) return cookieLang;

    // 4순위: 기본값
    return 'kr';
};

// 1. 최소화 토글
function toggleMinimizeChat() {
    const container = document.getElementById('floatingChatContainer');
    if (container) container.classList.toggle('minimized');
}

// 2. 창 닫기
function closeFloatingChat() {
    const container = document.getElementById('floatingChatContainer');
    const chatFrame = document.getElementById('floatingChatFrame');

    if (container) container.style.display = 'none';
    if (chatFrame) chatFrame.src = '';
}

// 3. 전역 채팅 목록 열기 (수정됨)
function openGlobalChatList() {
    const chatContainer = document.getElementById('floatingChatContainer');
    const chatFrame = document.getElementById('floatingChatFrame');

    if (!chatContainer || !chatFrame) return;

    // 🌟 수정된 부분: 위에서 정의한 전역 함수(getKumoLang) 호출
    const currentLang = window.getKumoLang();

    chatFrame.src = `/chat/list?lang=${currentLang}`;
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