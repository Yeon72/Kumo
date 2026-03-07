/* scout.js */
document.addEventListener('DOMContentLoaded', () => {
    const L = window.SCOUT_LANG || {};

    // ✅ 다크모드 감지해서 SweetAlert2 옵션 자동 적용
    function getSwalTheme() {
        return document.body.classList.contains('dark-mode') ? {
            customClass: {
                popup:         'swal-dark-popup',
                title:         'swal-dark-title',
                htmlContainer: 'swal-dark-text',
                confirmButton: 'swal-dark-confirm',
                cancelButton:  'swal-dark-cancel'
            }
        } : {};
    }

    document.querySelectorAll('.btn-delete-scout').forEach(btn => {
        btn.addEventListener('click', function () {
            const scoutId = this.getAttribute('data-id');
            const card = this.closest('.scout-card');

            Swal.fire({
                ...getSwalTheme(),
                title: L.confirmTitle || '삭제 확인',
                text: L.confirmDelete || '이 제안을 목록에서 삭제하시겠습니까?',
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#7db4e6',
                cancelButtonColor: '#aaaaaa',
                confirmButtonText: L.confirmBtn || '삭제',
                cancelButtonText: L.cancelBtn || '취소'
            }).then(result => {
                if (!result.isConfirmed) return;

                fetch('/Seeker/scout/delete', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: `scoutId=${scoutId}`
                })
                .then(response => {
                    if (response.ok) {
                        Swal.fire({
                            ...getSwalTheme(),
                            icon: 'success',
                            title: L.successMsg || '삭제되었습니다.',
                            timer: 1200,
                            showConfirmButton: false
                        }).then(() => {
                            card.style.opacity = '0';
                            card.style.transform = 'scale(0.95)';
                            card.style.transition = 'all 0.3s ease';
                            setTimeout(() => {
                                card.remove();
                                if (document.querySelectorAll('.scout-card').length === 0) {
                                    document.querySelector('.scout-list').innerHTML =
                                        `<div class="text-center py-5"><p class="text-muted">${L.emptyMsg || '받은 스카우트 제의가 없습니다.'}</p></div>`;
                                }
                            }, 300);
                        });
                    } else {
                        Swal.fire({
                            ...getSwalTheme(),
                            icon: 'error',
                            title: L.errorMsg || '삭제 중 오류가 발생했습니다.'
                        });
                    }
                })
                .catch(() => {
                    Swal.fire({
                        ...getSwalTheme(),
                        icon: 'error',
                        title: L.networkError || '서버와 통신 중 오류가 발생했습니다.'
                    });
                });
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