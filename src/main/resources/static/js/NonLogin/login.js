document.addEventListener("DOMContentLoaded", function () {

    /**
     * 애플리케이션 전용으로 커스터마이징된 SweetAlert2 믹스인(Mixin) 객체입니다.
     * 다크모드 상태에 따라 배경 및 텍스트 색상이 동적으로 적용됩니다.
     * @type {Object}
     */
    const KumoSwal = Swal.mixin({
        width: '340px',
        padding: '1.2em',
        customClass: {
            title: 'kumo-swal-title',
            popup: 'kumo-swal-popup'
        },
        confirmButtonColor: '#7db4e6',
        cancelButtonColor: '#6c757d',
        background: document.body.classList.contains('dark-mode') ? '#2a2b2e' : '#fff',
        color: document.body.classList.contains('dark-mode') ? '#e3e5e8' : '#333'
    });

    /**
     * SNS 로그인 기능 접근 시 서비스 준비 중 메시지를 표시하는 로컬 헬퍼 함수입니다.
     */
    function alertSns() {
        KumoSwal.fire({
            icon: 'info',
            title: loginMessages.sns_alert
        });
    }

    const savedEmail = localStorage.getItem("savedEmail");
    const emailInput = document.querySelector('input[name="email"]');
    const saveIdCheckbox = document.getElementById('saveId');

    if (savedEmail && emailInput) {
        emailInput.value = savedEmail;
        if (saveIdCheckbox) saveIdCheckbox.checked = true;
    }

    const loginForm = document.querySelector('form');
    const inputs = document.querySelectorAll('.custom-input');
    const captchaArea = document.getElementById('captchaArea');

    const isDark = () => document.body.classList.contains('dark-mode');
    const swalTheme = () => ({
        background: isDark() ? '#2a2b2e' : '#fff',
        color: isDark() ? '#e3e5e8' : '#333',
        confirmButtonColor: '#7db4e6'
    });

    if (loginForm) {
        /**
         * 로그인 폼 제출 시 이벤트를 가로채어 reCAPTCHA 인증 상태를 검증하고,
         * 비동기(AJAX) 로그인 요청을 수행합니다.
         * 응답에 따라 아이디 저장 처리 및 에러 팝업/캡차 활성화를 제어합니다.
         */
        loginForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const captchaArea = document.getElementById('captchaArea');
            if (captchaArea && captchaArea.style.display !== 'none') {
                const recaptchaResponse = grecaptcha.getResponse();
                if (recaptchaResponse.length === 0) {
                    const errorMsg = (typeof loginMessages !== 'undefined' && loginMessages.captcha_error)
                        ? loginMessages.captcha_error
                        : "로봇이 아니라는 것을 증명하기 위해 리캡차를 체크해 주세요.";
                    alert(errorMsg);
                    return false;
                }
            }

            const formData = new URLSearchParams(new FormData(loginForm));
            const recaptchaToken = grecaptcha.getResponse();
            if (recaptchaToken) {
                formData.append('g-recaptcha-response', recaptchaToken);
            }

            $.ajax({
                url: loginForm.getAttribute('action'),
                type: 'POST',
                data: formData.toString(),
                contentType: 'application/x-www-form-urlencoded',

                success: function(response) {
                    const emailVal = emailInput.value;
                    const isSaveChecked = saveIdCheckbox ? saveIdCheckbox.checked : false;

                    if (isSaveChecked) {
                        localStorage.setItem("savedEmail", emailVal);
                    } else {
                        localStorage.removeItem("savedEmail");
                    }

                    window.location.href = response.redirectUrl || '/';
                },

                error: function(xhr) {
                    const response = xhr.responseJSON;
                    const errorText = response?.message
                        || (typeof loginMessages !== 'undefined' ? loginMessages.error_text : '아이디 또는 비밀번호가 일치하지 않습니다.');

                    Swal.fire({
                        icon: 'error',
                        title: typeof loginMessages !== 'undefined' ? loginMessages.error_title : '로그인 실패',
                        text: errorText,
                        ...swalTheme()
                    });

                    if (response?.showCaptcha && captchaArea) {
                        captchaArea.style.display = 'block';
                    }

                    const pwInput = document.querySelector('input[name="password"]');
                    if (pwInput) {
                        pwInput.value = '';
                        pwInput.focus();
                    }
                }
            });
        });
    }

    inputs.forEach(input => {
        input.addEventListener('input', function() {
        });
    });

    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('error')) {
        window.history.replaceState({}, document.title, window.location.pathname);
    }
});

/**
 * 소셜(구글, 라인) 로그인 버튼 클릭 시 호출되는 전역 함수입니다.
 * 현재 서비스 준비 중임을 알리는 팝업을 렌더링합니다.
 */
function alertSns() {
    const isDark = document.body.classList.contains('dark-mode');
    Swal.fire({
        icon: 'info',
        title: typeof loginMessages !== 'undefined' ? loginMessages.sns_alert : '서비스 준비 중입니다.',
        confirmButtonColor: '#7db4e6',
        background: isDark ? '#2a2b2e' : '#fff',
        color: isDark ? '#e3e5e8' : '#333',
        width: '360px',
        padding: '1.5em',
        toast: false
    });
}