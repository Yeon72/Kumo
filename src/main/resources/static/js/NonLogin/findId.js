/**
 * FindId.js
 * 아이디 찾기 페이지의 프론트엔드 유효성 검사 및 뷰 전환, 데이터 비동기 요청 로직을 담당합니다.
 */

/**
 * 현재 선택된 사용자 역할 상태 (구직자/구인자)
 * @type {string}
 */
let currentRole = 'SEEKER';

/**
 * 연락처 입력 형식 검증을 위한 정규식 패턴 (예: 010-0000-0000)
 * @type {RegExp}
 */
const contactRegex = /^0\d{1,4}-\d{1,4}-\d{4}$/;

/**
 * 사용자 역할(구직자/구인자) 탭을 선택하고 화면을 초기화합니다.
 *
 * @param {string} role 선택된 역할 문자열 ('SEEKER' 또는 'RECRUITER')
 */
function selectRole(role) {
    currentRole = role;

    const tabs = document.querySelectorAll('.tab-btn');
    tabs.forEach(btn => btn.classList.remove('active'));

    if(role === 'SEEKER') {
        tabs[0].classList.add('active');
    } else {
        tabs[1].classList.add('active');
    }

    const roleInput = document.getElementById('roleInput');
    if(roleInput) roleInput.value = role;

    resetDisplay();
}

/**
 * 결과창, 에러 메시지 창 및 입력 필드를 초기 상태로 되돌립니다.
 */
function resetDisplay() {
    const resultBox = document.getElementById('result-box');
    const errorMsg = document.getElementById('error-msg');
    if(resultBox) resultBox.style.display = 'none';
    if(errorMsg) errorMsg.style.display = 'none';

    document.getElementById('name').value = '';
    document.getElementById('contact').value = '';

    clearError('name');
    clearError('contact');
}

/**
 * 연락처 입력 시 숫자만 추출하여 하이픈(-)을 자동으로 삽입합니다.
 * 입력 도중 에러 상태를 해제하여 UX를 개선합니다.
 *
 * @param {HTMLInputElement} target 이벤트가 발생한 입력 필드 요소
 */
function autoHyphen(target) {
    clearError('contact');

    let val = target.value.replace(/[^0-9]/g, "");
    let formatted = "";

    if (val.length <= 3) {
        formatted = val;
    } else if (val.length <= 7) {
        formatted = val.slice(0, 3) + "-" + val.slice(3);
    } else {
        if (val.length === 10) {
            formatted = val.slice(0, 3) + "-" + val.slice(3, 6) + "-" + val.slice(6);
        } else {
            formatted = val.slice(0, 3) + "-" + val.slice(3, 7) + "-" + val.slice(7, 11);
        }
    }
    target.value = formatted;
}

/**
 * 특정 입력 필드의 에러 테두리를 제거하고 관련 에러 메시지를 숨깁니다.
 *
 * @param {string} fieldId 에러를 초기화할 입력 필드의 식별자
 */
function clearError(fieldId) {
    const inputEl = document.getElementById(fieldId);

    if(inputEl) inputEl.classList.remove('input-error');

    if (fieldId === 'contact') {
        const emptyErr = document.getElementById('err-contact-empty');
        const invalidErr = document.getElementById('err-contact-invalid');
        if(emptyErr) emptyErr.style.display = 'none';
        if(invalidErr) invalidErr.style.display = 'none';
    } else {
        const errorEl = document.getElementById('err-' + fieldId);
        if(errorEl) errorEl.style.display = 'none';
    }
}

/**
 * 특정 입력 필드에 에러 테두리를 추가하고 상황에 맞는 에러 메시지를 표시합니다.
 *
 * @param {string} fieldId 에러를 표시할 입력 필드의 식별자
 * @param {string} [type='empty'] 표시할 에러의 유형 ('empty' 또는 'invalid')
 */
function showError(fieldId, type = 'empty') {
    const inputEl = document.getElementById(fieldId);

    if(inputEl) {
        inputEl.classList.add('input-error');
        inputEl.focus();
    }

    if (fieldId === 'contact') {
        if (type === 'empty') {
            document.getElementById('err-contact-empty').style.display = 'block';
            document.getElementById('err-contact-invalid').style.display = 'none';
        } else if (type === 'invalid') {
            document.getElementById('err-contact-empty').style.display = 'none';
            document.getElementById('err-contact-invalid').style.display = 'block';
        }
    } else {
        const errorEl = document.getElementById('err-' + fieldId);
        if(errorEl) errorEl.style.display = 'block';
    }
}

/**
 * 이름과 연락처 필드의 유효성을 검사한 후, 서버에 아이디 찾기 비동기(Fetch) 요청을 전송합니다.
 * 응답 결과에 따라 숨겨진 메시지(Hidden Input)를 참조하여 결과창 또는 에러창을 렌더링합니다.
 */
function findId() {
    const nameInput = document.getElementById('name');
    const contactInput = document.getElementById('contact');

    const name = nameInput.value.trim();
    const contact = contactInput.value.trim();

    if (!name) {
        showError('name');
        return;
    } else {
        clearError('name');
    }

    if (!contact) {
        showError('contact', 'empty');
        return;
    }
    else if (!contactRegex.test(contact)) {
        showError('contact', 'invalid');
        return;
    }
    else {
        clearError('contact');
    }

    const requestData = {
        name: name,
        contact: contact,
        role: currentRole
    };

    fetch('/api/findId', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestData)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            const resultBox = document.getElementById('result-box');
            const errorMsg = document.getElementById('error-msg');

            if (data.status === 'success') {
                if(errorMsg) errorMsg.style.display = 'none';
                if(resultBox) resultBox.style.display = 'block';

                document.getElementById('found-email').innerText = data.email;
            } else {
                if(resultBox) resultBox.style.display = 'none';
                if(errorMsg) errorMsg.style.display = 'block';

                const errorText = document.getElementById('error-text-content');
                if(errorText) {
                    const defaultMsgInput = document.getElementById('msg-fail-default');
                    const messageToShow = defaultMsgInput ? defaultMsgInput.value : (data.message || "일치하는 정보가 없습니다.");

                    errorText.innerText = messageToShow;
                }
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showError('name');
            const errEl = document.getElementById('err-name');
            if(errEl) errEl.innerText = "서버 통신 중 오류가 발생했습니다.";
        });
}