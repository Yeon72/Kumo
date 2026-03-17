/**
 * DOMContentLoaded 이벤트 리스너
 * 스카우트 제의 삭제 버튼의 클릭 이벤트를 바인딩하고, 비동기(Fetch) 통신을 통한
 * 삭제 처리 및 성공 시 UI 애니메이션 제어 로직을 초기화합니다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const L = window.SCOUT_LANG || {};

    /**
     * 현재 페이지의 다크모드 활성화 여부를 감지하여,
     * SweetAlert2 팝업에 적용할 커스텀 CSS 클래스 객체를 동적으로 반환합니다.
     *
     * @returns {Object} SweetAlert2 설정 옵션에 스프레드 연산자로 병합할 CSS 클래스 객체
     */
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

/**
 * 특정 구인자(Recruiter)와의 스카우트 전용 1:1 채팅방을 개설하고 플로팅 채팅창을 활성화합니다.
 * 일반 공고 지원 경로와 구분하기 위해 더미 식별자(dummyJobId, dummySource)를 서버로 함께 전달합니다.
 *
 * @param {string|number} recruiterId 채팅방을 개설할 대상 구인자의 고유 식별자
 */
function openScoutChat(recruiterId) {
    const chatContainer = document.getElementById('floatingChatContainer');
    const chatFrame = document.getElementById('floatingChatFrame');

    if (!chatContainer || !chatFrame) {
        alert("채팅 모듈을 불러올 수 없습니다. 화면을 새로고침해주세요.");
        return;
    }

    const currentLang = window.getKumoLang();

    const dummyJobId = 0;
    const dummySource = 'SCOUT';

    chatFrame.src = `/chat/create?recruiterId=${recruiterId}&jobPostId=${dummyJobId}&jobSource=${dummySource}&lang=${currentLang}`;

    chatContainer.style.display = 'flex';
    chatContainer.classList.remove('minimized');
}