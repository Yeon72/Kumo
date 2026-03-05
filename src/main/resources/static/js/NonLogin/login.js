document.addEventListener("DOMContentLoaded", function () {
    
    // 🌟 KUMO 전용 작은 알림창 설정
const KumoSwal = Swal.mixin({
    width: '340px',
    padding: '1.2em',
    customClass: {
        title: 'kumo-swal-title',
        popup: 'kumo-swal-popup'
    },
    confirmButtonColor: '#7db4e6',
    cancelButtonColor: '#6c757d',
    // 다크모드 대응
    background: document.body.classList.contains('dark-mode') ? '#2a2b2e' : '#fff',
    color: document.body.classList.contains('dark-mode') ? '#e3e5e8' : '#333'
});

// 사용 예시
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
        loginForm.addEventListener('submit', function(e) {
            e.preventDefault();

            // 🌟 [보안 로직 추가] 캡차 영역이 보이고 있다면, 인증 여부 확인 필수!
            const captchaArea = document.getElementById('captchaArea');
            if (captchaArea && captchaArea.style.display !== 'none') {
                const recaptchaResponse = grecaptcha.getResponse();
                if (recaptchaResponse.length === 0) {
                    const errorMsg = (typeof loginMessages !== 'undefined' && loginMessages.captcha_error) 
                        ? loginMessages.captcha_error 
                        : "로봇이 아니라는 것을 증명하기 위해 리캡차를 체크해 주세요.";
                    alert(errorMsg);
                    return false; // 제출 중단
                }
            }

            const formData = new URLSearchParams(new FormData(loginForm));
            // 리캡차 토큰도 함께 전송 (필요 시)
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

    // 입력 시 에러 숨기기
    inputs.forEach(input => {
        input.addEventListener('input', function() {
            // SweetAlert는 자동으로 닫히므로 별도 처리 불필요
        });
    });

    // URL 파라미터 정리
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('error')) {
        window.history.replaceState({}, document.title, window.location.pathname);
    }
});

// 구글/라인 SNS 버튼 클릭
function alertSns() {
    const isDark = document.body.classList.contains('dark-mode');
    Swal.fire({
        icon: 'info',
        title: typeof loginMessages !== 'undefined' ? loginMessages.sns_alert : '서비스 준비 중입니다.',
        confirmButtonColor: '#7db4e6',
        background: isDark ? '#2a2b2e' : '#fff',
        color: isDark ? '#e3e5e8' : '#333',
        width: '360px',        // ← 크기 축소
        padding: '1.5em',      // ← 내부 여백 축소
        toast: false
    });
}
