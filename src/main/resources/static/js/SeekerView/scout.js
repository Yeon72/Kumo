/* scout.js */
document.addEventListener('DOMContentLoaded', () => {
    // 삭제 버튼 이벤트 리스너 등록
    const deleteButtons = document.querySelectorAll('.btn-delete-scout');

    deleteButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const scoutId = this.getAttribute('data-id');
            const card = this.closest('.scout-card');

            // 💡 팁: 다국어 적용을 원하시면 HTML에서 window.SCOUT_LANG = { confirmDelete: ... } 등을
            // 만들어서 가져오게 하시면 좋습니다!
            if (!confirm("이 제안을 목록에서 삭제하시겠습니까?")) return;

            // 🌟 Vanilla JS Fetch API 사용
            fetch('/Seeker/scout/delete', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `scoutId=${scoutId}`
            })
                .then(response => {
                    if (response.ok) {
                        // 삭제 성공 시 애니메이션 후 제거
                        card.style.opacity = '0';
                        card.style.transform = 'scale(0.95)';
                        setTimeout(() => {
                            card.remove();
                            // 목록이 비었는지 확인
                            const remainingCards = document.querySelectorAll('.scout-card');
                            if (remainingCards.length === 0) {
                                const list = document.querySelector('.scout-list');
                                // 빈 목록 텍스트도 다국어 처리를 위해 HTML에 숨겨둔 텍스트를 복사하거나 변수를 쓰는 것을 권장합니다.
                                list.innerHTML = '<div class="text-center py-5"><p class="text-muted">받은 스카우트 제의가 없습니다.</p></div>';
                            }
                        }, 300);
                    } else {
                        alert("삭제 중 오류가 발생했습니다.");
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert("서버와 통신 중 오류가 발생했습니다.");
                });
        });
    });
});

// ==========================================================
// 🌟 [추가됨] 스카우트 전용 1:1 채팅 열기 함수
// ==========================================================
function openScoutChat(recruiterId) {
    const chatContainer = document.getElementById('floatingChatContainer');
    const chatFrame = document.getElementById('floatingChatFrame');

    if (!chatContainer || !chatFrame) {
        // 다국어 처리가 필요하다면 이 부분도 변수 처리하시면 됩니다.
        alert("채팅 모듈을 불러올 수 없습니다. 화면을 새로고침해주세요.");
        return;
    }

    // 1. 부모 창(현재 스카우트 현황 페이지)의 언어 설정을 그대로 가져옵니다. (없으면 kr)
    const currentLang = window.getKumoLang();

    // 2. 백엔드의 /chat/create 주소는 jobPostId와 jobSource를 필수로 요구합니다.
    // 스카우트는 특정 공고에서 누른 것이 아니므로 식별을 위해 0과 'SCOUT'라는 임시 값을 보냅니다.
    const dummyJobId = 0;
    const dummySource = 'SCOUT';

    // 3. iframe 주소를 새 채팅방 생성 주소로 바꿔치기 (lang 꼬리표 부착 완벽!)
    chatFrame.src = `/chat/create?recruiterId=${recruiterId}&jobPostId=${dummyJobId}&jobSource=${dummySource}&lang=${currentLang}`;

    // 4. 숨겨져 있던 채팅창을 짠! 하고 나타나게 합니다.
    chatContainer.style.display = 'flex';
    chatContainer.classList.remove('minimized');
}