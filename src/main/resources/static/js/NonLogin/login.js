/* ==========================================
   KUMO 로그인 페이지 Logic (AJAX 통합)
   ========================================== */

document.addEventListener("DOMContentLoaded", function() {

    // 1. [아이디 불러오기] 페이지 로드 시 로컬 스토리지 확인
    // 기존에 cookie를 뒤지던 로직을 localStorage로 통일했습니다.
    const savedEmail = localStorage.getItem("savedEmail");
    const emailInput = document.querySelector('input[name="email"]');
    const saveIdCheckbox = document.getElementById('saveId');

    if (savedEmail && emailInput) {
        emailInput.value = savedEmail;
        if (saveIdCheckbox) {
            saveIdCheckbox.checked = true;
        }
    }

    const loginForm = document.querySelector('form');
    const errorMsgBox = document.querySelector('.login-error-msg');
    const inputs = document.querySelectorAll('.custom-input');
    const captchaArea = document.getElementById('captchaArea');

    // 2. [로그인 제출] 비동기(AJAX) 처리
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
                    // ★ [아이디 저장 로직 녹여내기]
                    // 오타 수정: querySelector.('.saveId') -> getElementById('saveId')
                    const emailVal = emailInput.value;
                    const isSaveChecked = saveIdCheckbox ? saveIdCheckbox.checked : false;

                    if (isSaveChecked) {
                        localStorage.setItem("savedEmail", emailVal);
                    } else {
                        localStorage.removeItem("savedEmail");
                    }

                    // 🌟 서버에서 내려준 경로로 이동 (권한별 분기)
                    window.location.href = response.redirectUrl || '/';
                },

                error: function(xhr) {
                    const response = xhr.responseJSON;

                    if (errorMsgBox) {
                        const errorText = errorMsgBox.querySelector('span');
                        if (response && response.message && errorText) {
                            errorText.textContent = response.message;
                        }
                        errorMsgBox.style.display = 'flex';

                        // 캡차 노출 로직
                        if (response && response.showCaptcha) {
                            if (captchaArea) {
                                captchaArea.style.display = 'block';
                                console.warn("보안 인증(CAPTCHA)이 활성화되었습니다.");
                            }
                        }
                    }

                    // 비밀번호 입력창 초기화 및 포커스
                    const pwInput = document.querySelector('input[name="password"]');
                    if (pwInput) {
                        pwInput.value = '';
                        pwInput.focus();
                    }
                }
            });
        });
    }

    // 3. 입력 시작 시 에러 메시지 숨기기
    if (errorMsgBox) {
        inputs.forEach(input => {
            input.addEventListener('input', function() {
                errorMsgBox.style.display = 'none';
            });
        });
    }

    // 4. URL 파라미터 정리
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('error')) {
        const newUrl = window.location.pathname;
        window.history.replaceState({}, document.title, newUrl);
    }
});

/**
 * SNS 로그인 버튼 클릭 시 알림창
 */
function alertSns() {
    if (typeof loginMessages !== 'undefined' && loginMessages.sns_alert) {
        alert(loginMessages.sns_alert);
    } else {
        alert("서비스 준비 중입니다.");
    }
}