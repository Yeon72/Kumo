/**
 * FindPw.js
 * 비밀번호 찾기 페이지의 다국어 텍스트 처리, 이메일 인증 및 폼 검증을 담당하는 프론트엔드 스크립트입니다.
 */

/**
 * HTML 내부에 숨겨진 요소로부터 다국어 메시지 문자열을 추출하여 반환합니다.
 *
 * @param {string} id 메시지 값을 가지고 있는 대상 DOM 요소의 식별자
 * @returns {string} 추출된 메시지 텍스트 (요소가 없으면 빈 문자열 반환)
 */
function getMsg(id) {
    const el = document.getElementById(id);
    return el ? el.value : "";
}

/**
 * 특정 필드 하단에 에러 메시지를 표시하고, 대상 입력 필드에 에러 스타일을 적용한 뒤 포커스를 이동시킵니다.
 *
 * @param {string} fieldId 에러 상태를 표시할 대상 입력 필드의 식별자
 * @param {string} [msg] 화면에 출력할 다국어 에러 메시지
 */
function showError(fieldId, msg) {
    const errorEl = document.getElementById('err-' + fieldId);
    const inputEl = document.getElementById(fieldId);

    if(errorEl) {
        errorEl.style.display = 'block';
        if(msg) errorEl.innerText = msg;
    }

    if(inputEl) {
        inputEl.classList.add('input-error');
        inputEl.focus();
    }
}

/**
 * 특정 필드 하단에 노출된 에러 메시지를 숨기고 입력 필드의 에러 스타일을 초기화합니다.
 *
 * @param {string} fieldId 에러 상태를 해제할 입력 필드의 식별자
 */
function clearError(fieldId) {
    const errorEl = document.getElementById('err-' + fieldId);
    const inputEl = document.getElementById(fieldId);

    if(errorEl) errorEl.style.display = 'none';
    if(inputEl) inputEl.classList.remove('input-error');

    if(fieldId === 'email' || fieldId === 'authCode') {
        const verifyErr = document.getElementById('err-verify');
        if(verifyErr) verifyErr.style.display = 'none';
    }
}

/**
 * 사용자 역할(구직자/구인자) 탭 버튼의 활성화 상태를 변경하고 숨겨진 역할 필드의 값을 갱신합니다.
 *
 * @param {string} role 선택된 역할 문자열 ('SEEKER' 또는 'RECRUITER')
 */
function selectRole(role) {
    const tabs = document.querySelectorAll('.tab-btn');
    tabs.forEach(btn => btn.classList.remove('active'));

    if(role === 'SEEKER') {
        tabs[0].classList.add('active');
    } else {
        tabs[1].classList.add('active');
    }

    document.getElementById('roleInput').value = role;
}

/**
 * 연락처 입력란에 입력된 숫자 문자열에 하이픈(-) 기호를 자동으로 삽입합니다.
 *
 * @param {HTMLInputElement} target 하이픈이 삽입될 연락처 입력 필드 DOM 요소
 */
function autoHyphen(target) {
    clearError('contact');
    target.value = target.value
        .replace(/[^0-9]/g, '')
        .replace(/^(\d{0,3})(\d{0,4})(\d{0,4})$/g, "$1-$2-$3")
        .replace(/(\-{1,2})$/g, "");
}

/**
 * 입력된 폼 정보(이름, 연락처, 이메일, 역할)의 유효성을 검사한 후 서버에 인증 메일 발송을 요청합니다.
 * 정보가 일치하지 않거나 오류가 발생할 경우 해당 필드에 에러 메시지를 출력합니다.
 */
function sendMail() {
    const name = document.getElementById('name').value.trim();
    const contact = document.getElementById('contact').value.trim();
    const emailInput = document.getElementById('email');
    const email = emailInput.value.trim();
    const role = document.getElementById('roleInput').value;

    if(!name) {
        showError('name', getMsg('msg-name-empty'));
        return;
    } else {
        clearError('name');
    }

    if(!contact) {
        showError('contact', getMsg('msg-contact-empty'));
        return;
    } else {
        clearError('contact');
    }

    if(!email) {
        showError('email', getMsg('msg-email-empty'));
        return;
    }

    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailPattern.test(email)) {
        showError('email', getMsg('msg-email-invalid'));
        return;
    }

    const btn = document.getElementById('btn-email-send');
    btn.disabled = true;
    const originalText = btn.innerText;
    btn.innerText = getMsg('msg-btn-sending');

    const requestData = {
        name,
        contact,
        email,
        role
    };

    fetch('/api/mail/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestData)
    })
        .then(async res => {
            if (res.ok) {
                document.getElementById('auth-box').style.display = 'flex';
            } else {
                const errorCode = await res.text();

                if (errorCode === "USER_NOT_FOUND") {
                    showError('email', getMsg('msg-fail-default'));
                } else if (errorCode === "EMPTY_EMAIL") {
                    showError('email', getMsg('msg-email-empty'));
                } else {
                    showError('email', getMsg('msg-server-error'));
                }
            }
        })
        .catch(err => {
            console.error(err);
            showError('email', getMsg('msg-server-error'));
        })
        .finally(() => {
            btn.disabled = false;
            btn.innerText = originalText;
        });
}

/**
 * 사용자가 입력한 인증번호의 일치 여부를 검증하기 위해 서버에 검사 요청을 전송합니다.
 * 검증 완료 시 추가 입력을 방지하기 위해 이메일 필드와 버튼 상태를 갱신합니다.
 */
function checkCode() {
    const inputCodeEl = document.getElementById('authCode');
    const inputCode = inputCodeEl.value.trim();

    if(!inputCode) {
        showError('authCode', getMsg('msg-auth-code-empty'));
        return;
    }

    fetch('/api/mail/check', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code: inputCode })
    })
        .then(res => res.json())
        .then(isMatch => {
            if(isMatch == true) {
                document.getElementById('auth-box').style.display = 'none';
                clearError('authCode');

                document.getElementById('auth-success-msg').style.display = 'block';
                document.getElementById('isVerified').value = "true";

                const verifyErr = document.getElementById('err-verify');
                if(verifyErr) verifyErr.style.display = 'none';

                document.getElementById('email').readOnly = true;
                const sendBtn = document.getElementById('btn-email-send');
                sendBtn.disabled = true;
                sendBtn.innerText = getMsg('msg-btn-complete');
            } else {
                showError('authCode', getMsg('msg-auth-code-mismatch'));
            }
        })
        .catch(err => {
            console.error(err);
            showError('authCode', getMsg('msg-auth-error'));
        });
}

/**
 * 비밀번호 재설정 페이지로 이동하기 전, 이름, 연락처 및 인증번호 검증 완료 여부를 최종 확인합니다.
 * 모든 검증이 완료된 경우 폼 데이터를 POST 방식으로 제출합니다.
 */
function goToChangePw() {
    const isVerified = document.getElementById('isVerified').value;
    const name = document.getElementById('name').value.trim();
    const contact = document.getElementById('contact').value.trim();

    if(!name) {
        showError('name', getMsg('msg-name-empty'));
        return;
    } else {
        clearError('name');
    }

    if(!contact) {
        showError('contact', getMsg('msg-contact-empty'));
        return;
    } else {
        clearError('contact');
    }

    if(isVerified !== "true") {
        const verifyErr = document.getElementById('err-verify');
        if(verifyErr) verifyErr.style.display = 'block';
        document.getElementById('email').focus();
        return;
    }

    const form = document.getElementById('findPwForm');
    form.action = "/changePw";
    form.method = "POST";
    form.submit();
}