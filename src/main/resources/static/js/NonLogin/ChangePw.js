/**
 * ChangePw.js
 * 비밀번호 재설정 페이지의 프론트엔드 유효성 검사 및 에러 메시지 렌더링 로직을 담당합니다.
 */

/**
 * 영문, 숫자, 특수문자를 최소 1개 이상 포함하는 8~16자리의 비밀번호 정규식 패턴입니다.
 * @type {RegExp}
 */
const pwRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[$@$!%*#?&])[A-Za-z\d$@$!%*#?&]{8,16}$/;

/**
 * HTML 내부에 숨겨진(hidden) 입력 요소로부터 다국어 메시지 텍스트를 추출합니다.
 *
 * @param {string} id 메시지가 저장된 DOM 요소의 식별자
 * @returns {string} 요소의 value 값 (요소가 존재하지 않을 경우 빈 문자열 반환)
 */
function getMsg(id) {
    const el = document.getElementById(id);
    return el ? el.value : "";
}

/**
 * 특정 입력 필드의 하단에 에러 메시지를 표시하고, 입력 필드에 에러 스타일을 적용합니다.
 *
 * @param {string} fieldId 에러를 표시할 대상 입력 필드의 식별자
 * @param {string} msg 화면에 출력할 에러 메시지 문자열
 */
function showError(fieldId, msg) {
    const errorEl = document.getElementById('err-' + fieldId);
    const inputEl = document.getElementById(fieldId);
    if(errorEl) {
        errorEl.innerText = msg;
        errorEl.style.display = 'block';
    }
    if(inputEl) inputEl.classList.add('input-error');
}

/**
 * 특정 입력 필드의 에러 메시지를 숨기고 에러 스타일을 제거하여 UI를 초기화합니다.
 *
 * @param {string} fieldId 초기화할 대상 입력 필드의 식별자
 */
function clearError(fieldId) {
    const errorEl = document.getElementById('err-' + fieldId);
    const inputEl = document.getElementById(fieldId);
    if(errorEl) errorEl.style.display = 'none';
    if(inputEl) inputEl.classList.remove('input-error');
}

/**
 * 새 비밀번호와 비밀번호 확인 필드의 입력값을 실시간으로 비교하여,
 * 정규식 통과 여부 및 두 비밀번호의 일치 상태를 하단 통합 메시지 영역에 출력합니다.
 */
function checkMatch() {
    const pw1 = document.getElementById('newPassword').value;
    const pw2 = document.getElementById('confirmPassword').value;
    const matchMsg = document.getElementById('pw-match-msg');

    if(!pw1 && !pw2) {
        matchMsg.style.display = 'none';
        return;
    }

    if(pw1 !== pw2) {
        matchMsg.innerText = getMsg('msg-pw-mismatch');
        matchMsg.style.color = "#EA4335";
        matchMsg.style.display = 'block';
    } else {
        if(pwRegex.test(pw1)) {
            matchMsg.innerText = getMsg('msg-pw-available');
            matchMsg.style.color = "#4285F4";
            matchMsg.style.display = 'block';
        } else {
            matchMsg.innerText = getMsg('msg-pw-invalid');
            matchMsg.style.color = "#EA4335";
            matchMsg.style.display = 'block';
        }
    }
}

/**
 * 비밀번호 변경 폼을 제출하기 전, 입력 필드의 누락 여부, 정규식 통과 여부,
 * 비밀번호 일치 여부를 최종적으로 검증합니다. 모든 검증을 통과할 경우 폼을 서버로 제출합니다.
 */
function submitChange() {
    const pw1 = document.getElementById('newPassword').value;
    const pw2 = document.getElementById('confirmPassword').value;

    clearError('newPassword');
    clearError('confirmPassword');

    if(!pw1) {
        showError('newPassword', getMsg('msg-pw-empty'));
        return;
    }

    if(!pw2) {
        showError('confirmPassword', getMsg('msg-confirm-empty'));
        return;
    }

    if(!pwRegex.test(pw1)) {
        showError('newPassword', getMsg('msg-pw-invalid'));
        return;
    }

    if(pw1 !== pw2) {
        showError('confirmPassword', getMsg('msg-pw-mismatch'));
        return;
    }

    document.getElementById('changePwForm').submit();
}