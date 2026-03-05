/* --- src/main/resources/static/js/recruiter/rightSidebar.js --- */

// 1. 🌟 사이드바 전역 채팅 목록 열기 함수 (조원분 코드 유지)
function openGlobalChatList() {
    const chatContainer = document.getElementById('floatingChatContainer');
    const chatFrame = document.getElementById('floatingChatFrame');

    if (!chatContainer || !chatFrame) {
        console.error("🚨 플로팅 채팅창 HTML 요소가 현재 페이지에 없습니다.");
        alert("채팅창을 열 수 없습니다.");
        return;
    }

    chatFrame.src = "/chat/list";
    chatContainer.style.display = 'flex';
    chatContainer.classList.remove('minimized');
}

// 2. 🔴 빨간 뱃지 숫자 업데이트 함수
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
        .catch(err => console.log("알림 뱃지 대기 중..."));
}

// 3. 🚀 페이지 로드 시 실행 및 주기적 갱신
document.addEventListener("DOMContentLoaded", function() {
    updateSidebarBadge();
    // 10초마다 자동으로 숫자를 체크합니다.
    setInterval(updateSidebarBadge, 10000);
});