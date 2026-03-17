/**
 * DOMContentLoaded 이벤트 리스너
 * 구직자(Seeker)의 입사 지원 내역 취소 이벤트 및 비동기 통신 로직을 바인딩합니다.
 * 다크모드 상태에 맞춘 사용자 확인 팝업(SweetAlert2) 렌더링과 취소 성공 시의 DOM 애니메이션 제어 기능을 포함합니다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const cancelButtons = document.querySelectorAll('.btn-cancel-app');

    cancelButtons.forEach(btn => {
        btn.addEventListener('click', function () {
            const appId = this.getAttribute('data-id');
            const row = this.closest('tr');
            const isDark = document.body.classList.contains('dark-mode');
            const theme = {
                background: isDark ? '#2a2b2e' : '#fff',
                color: isDark ? '#e3e5e8' : '#333'
            };

            Swal.fire({
                icon: 'question',
                title: historyMsg.cancelConfirmTitle,
                showCancelButton: true,
                confirmButtonColor: '#ff6b6b',
                cancelButtonColor: '#aaa',
                confirmButtonText: historyMsg.cancelConfirmBtn,
                cancelButtonText: historyMsg.cancelBack,
                width: '360px',
                ...theme
            }).then((result) => {
                if (!result.isConfirmed) return;

                fetch('/Seeker/history/cancel', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: `appId=${appId}`
                })
                    .then(response => response.text())
                    .then(res => {
                        if (res === "success") {
                            row.style.transition = 'opacity 0.3s, transform 0.3s';
                            row.style.opacity = '0';
                            row.style.transform = 'translateX(20px)';
                            setTimeout(() => row.remove(), 300);
                        } else {
                            Swal.fire({
                                icon: 'error',
                                title: historyMsg.errorTitle,
                                text: historyMsg.cancelError,
                                confirmButtonColor: '#7db4e6',
                                ...theme
                            });
                        }
                    })
                    .catch(error => {
                        console.error('Error:', error);
                        Swal.fire({
                            icon: 'error',
                            title: historyMsg.errorTitle,
                            text: historyMsg.serverError,
                            confirmButtonColor: '#7db4e6',
                            ...theme
                        });
                    });
            });
        });
    });
});