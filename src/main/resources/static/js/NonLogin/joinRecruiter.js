/**
 * 구인자(Recruiter) 회원가입 페이지의 프론트엔드 비즈니스 로직을 담당합니다.
 * 폼 유효성 검사, 다중 파일 업로드 프리뷰, 비동기 데이터 검증(이메일/닉네임 중복),
 * 우편번호 검색 및 좌표 변환(Geocoding) 기능을 포함합니다.
 */

/**
 * 폼 유효성 검사에 사용되는 정규표현식 패턴 모음입니다.
 * @constant {Object}
 */
const regexPatterns = {
    email: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
    password: /^(?=.*[A-Za-z])(?=.*\d)(?=.*[$@$!%*#?&])[A-Za-z\d$@$!%*#?&]{8,16}$/,
    name_kanji: /^[一-龥ぁ-んァ-ヶー々〆〤]+$/,
    name_kana: /^[ァ-ヶー]+$/,
    contact: /^0\d{1,4}-\d{1,4}-\d{4}$/
};

/**
 * 사용자가 업로드를 위해 선택한 다중 파일(이미지 등) 객체들을 저장하는 전역 배열입니다.
 * @type {Array<File>}
 */
let selectedFiles = [];

/**
 * DOMContentLoaded 이벤트 리스너
 * 입력 필드 변경 감지(인증 초기화), 주소 블러 이벤트, 파일 업로드 제어,
 * 전화번호 자동 포맷팅 및 약관 전체 동의 기능을 초기화하고 바인딩합니다.
 */
document.addEventListener('DOMContentLoaded', () => {

    const emailInput = document.getElementById('email');
    if(emailInput) {
        emailInput.addEventListener('input', () => {
            document.getElementById('emailChecked').value = "false";
            document.getElementById('error_email').style.display = 'none';
        });
    }

    const nicknameInput = document.getElementById('nickname');
    if(nicknameInput) {
        nicknameInput.addEventListener('input', () => {
            document.getElementById('nicknameChecked').value = "false";
            document.getElementById('error_nickname').style.display = 'none';
        });
    }

    const addrDetail = document.getElementById('address_detail');
    if(addrDetail) {
        addrDetail.addEventListener('blur', function() {
            const mainAddr = document.getElementById('address_main').value;
            const detailAddr = this.value;
            if(mainAddr) getGeocode(mainAddr + " " + detailAddr);
        });
    }

    const evidenceFile = document.getElementById('evidenceFile');
    const fileNameDisplay = document.getElementById('fileNameDisplay');
    const btnUpload = document.getElementById('btnUpload');

    if(evidenceFile && fileNameDisplay && btnUpload) {
        const openFileSelector = () => evidenceFile.click();
        fileNameDisplay.addEventListener('click', openFileSelector);
        btnUpload.addEventListener('click', openFileSelector);
        evidenceFile.addEventListener('change', handleFileSelect);
    }

    const contactInput = document.getElementById('contact');
    if(contactInput) {
        contactInput.addEventListener('input', function() {
            let val = this.value.replace(/[^0-9]/g, "");
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
            this.value = formatted;
        });
    }

    const checkAll = document.getElementById('checkAll');
    if(checkAll) {
        checkAll.addEventListener('change', function() {
            const checkboxes = document.querySelectorAll('.terms-box input[type="checkbox"]');
            checkboxes.forEach(cb => cb.checked = this.checked);
        });
    }
});

/**
 * 사용자가 파일 입력란을 통해 파일을 선택했을 때 호출됩니다.
 * 이미지 파일 여부를 검증하고 유효한 파일을 전역 배열에 추가한 뒤 미리보기를 갱신합니다.
 *
 * @param {Event} e 파일 선택(change) 이벤트 객체
 */
function handleFileSelect(e) {
    const files = Array.from(e.target.files);
    const errorEvidence = document.getElementById('error_evidence');

    let hasInvalidFile = false;
    files.forEach(file => {
        if (!file.type.match('image.*')) {
            hasInvalidFile = true;
        } else {
            selectedFiles.push(file);
        }
    });

    if (hasInvalidFile) {
        alert("이미지 파일만 업로드 가능합니다.");
    }

    updatePreview();
    updateInputFiles();

    if (selectedFiles.length > 0 && errorEvidence) {
        errorEvidence.style.display = 'none';
    }
}

/**
 * 선택된 파일 배열을 순회하며 화면에 썸네일(미리보기) 이미지와 삭제 버튼을 동적으로 생성하여 렌더링합니다.
 */
function updatePreview() {
    const container = document.getElementById('previewContainer');
    const fileNameDisplay = document.getElementById('fileNameDisplay');

    if(!container) return;

    container.innerHTML = "";

    if (selectedFiles.length > 0) {
        container.style.display = 'flex';
        fileNameDisplay.value = `총 ${selectedFiles.length}개 파일 선택됨`;
    } else {
        container.style.display = 'none';
        fileNameDisplay.value = "";
    }

    selectedFiles.forEach((file, index) => {
        const box = document.createElement('div');
        box.className = 'preview-box';
        box.style.position = 'relative';

        const img = document.createElement('img');
        const reader = new FileReader();
        reader.onload = (e) => img.src = e.target.result;
        reader.readAsDataURL(file);

        const delBtn = document.createElement('button');
        delBtn.type = 'button';
        delBtn.className = 'btn-remove-file';
        delBtn.innerHTML = '<i class="fa-solid fa-xmark"></i>';

        delBtn.style.position = 'absolute';
        delBtn.style.top = '0';
        delBtn.style.right = '0';
        delBtn.style.backgroundColor = 'rgba(0,0,0,0.5)';
        delBtn.style.color = 'white';
        delBtn.style.border = 'none';
        delBtn.style.cursor = 'pointer';
        delBtn.style.width = '20px';
        delBtn.style.height = '20px';
        delBtn.style.display = 'flex';
        delBtn.style.alignItems = 'center';
        delBtn.style.justifyContent = 'center';

        delBtn.onclick = () => removeFile(index);

        box.appendChild(img);
        box.appendChild(delBtn);
        container.appendChild(box);
    });
}

/**
 * 미리보기 영역에서 특정 파일의 삭제 버튼을 클릭했을 때 호출됩니다.
 * 전역 배열에서 해당 파일을 제거하고 미리보기 및 실제 입력 요소를 동기화합니다.
 *
 * @param {number} index 삭제할 파일의 배열 내 인덱스
 */
function removeFile(index) {
    selectedFiles.splice(index, 1);
    updatePreview();
    updateInputFiles();
}

/**
 * 전역 배열에 저장된 파일 목록을 HTML의 실제 `<input type="file">` 요소(DataTransfer)에 동기화합니다.
 */
function updateInputFiles() {
    const evidenceFile = document.getElementById('evidenceFile');
    if(!evidenceFile) return;

    const dataTransfer = new DataTransfer();
    selectedFiles.forEach(file => {
        dataTransfer.items.add(file);
    });
    evidenceFile.files = dataTransfer.files;
}

/**
 * 입력된 우편번호를 기반으로 외부 API(zipcloud)를 호출하여 기본 주소 정보를 자동으로 채웁니다.
 */
function searchAddress() {
    const zipcode = document.getElementById('zipcode').value;
    const errorEl = document.getElementById('error_zipcode');

    if (!zipcode || zipcode.length < 7) {
        errorEl.innerText = errorMessages.zipcode_check;
        errorEl.style.display = 'block';
        return;
    }
    errorEl.style.display = 'none';

    fetch(`https://zipcloud.ibsnet.co.jp/api/search?zipcode=${zipcode}`)
        .then(response => response.json())
        .then(data => {
            if (data.status === 200 && data.results) {
                const result = data.results[0];
                document.getElementById('addr_prefecture').value = result.address1;
                document.getElementById('addr_city').value = result.address2;
                document.getElementById('addr_town').value = result.address3;

                const fullAddress = result.address1 + result.address2 + result.address3;
                document.getElementById('address_main').value = fullAddress;
                getGeocode(fullAddress);

                document.getElementById('address_detail').focus();
            } else {
                errorEl.innerText = errorMessages.search_fail;
                errorEl.style.display = 'block';
            }
        })
        .catch(() => {
            errorEl.innerText = errorMessages.search_fail;
            errorEl.style.display = 'block';
        });
}

/**
 * 결합된 주소 문자열을 Google Maps Geocoding API를 통해 위도(Latitude)와 경도(Longitude)로 변환합니다.
 *
 * @param {string} address 좌표로 변환할 전체 주소 문자열
 */
function getGeocode(address) {
    if (!window.google || !window.google.maps) return;

    const geocoder = new google.maps.Geocoder();

    geocoder.geocode({ 'address': address }, function(results, status) {
        if (status === 'OK') {
            const loc = results[0].geometry.location;
            document.getElementById('latitude').value = loc.lat();
            document.getElementById('longitude').value = loc.lng();

            const addressComponents = results[0].address_components;
            let prefecture = '';
            let city = '';
            let town = '';

            for (let i = 0; i < addressComponents.length; i++) {
                const component = addressComponents[i];
                const types = component.types;

                if (types.includes('administrative_area_level_1')) {
                    prefecture = component.long_name;
                } else if (types.includes('locality')) {
                    city = component.long_name;
                } else if (types.includes('sublocality_level_1')) {
                    town = component.long_name;
                } else if (types.includes('political') && types.includes('sublocality')) {
                    if (!town) town = component.long_name;
                }
            }

            document.getElementById('addr_prefecture').value = prefecture;
            document.getElementById('addr_city').value = city;
            document.getElementById('addr_town').value = town;

        } else {
            console.error('Geocode was not successful for the following reason: ' + status);
        }
    });
}

/**
 * 사용자가 입력한 이메일의 유효성을 검사하고, 서버에 비동기 통신을 요청하여 중복 여부를 확인합니다.
 */
function checkEmail() {
    const emailInput = document.getElementById('email');
    const email = emailInput.value.trim();

    if (!email) {
        showError('email', 'error_email', errorMessages.email_empty);
        return;
    }
    if (!regexPatterns.email.test(email)) {
        showError('email', 'error_email', errorMessages.email_invalid);
        return;
    }

    fetch('/api/check/email', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email })
    })
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(isDuplicate => {
            const errorEl = document.getElementById('error_email');

            if (isDuplicate) {
                errorEl.innerText = errorMessages.email_duplicate;
                errorEl.style.color = '#EA4335';
                errorEl.style.display = 'block';
                document.getElementById('emailChecked').value = "false";
                emailInput.focus();
            } else {
                errorEl.innerText = errorMessages.success_email;
                errorEl.style.color = "#4285F4";
                errorEl.style.display = 'block';
                document.getElementById('emailChecked').value = "true";
            }
        })
        .catch(error => {
            console.error('Error:', error);
        });
}

/**
 * 사용자가 입력한 닉네임의 빈 값 여부를 검사하고, 서버에 비동기 통신을 요청하여 중복 여부를 확인합니다.
 */
function checkNickname() {
    const nicknameInput = document.getElementById('nickname');
    const nickname = nicknameInput.value.trim();

    if (!nickname) {
        showError('nickname', 'error_nickname', errorMessages.nickname);
        return;
    }

    fetch('/api/check/nickname', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nickname: nickname })
    })
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(isDuplicate => {
            const errorEl = document.getElementById('error_nickname');

            if (isDuplicate) {
                errorEl.innerText = errorMessages.nickname_duplicate;
                errorEl.style.color = '#EA4335';
                errorEl.style.display = 'block';
                document.getElementById('nicknameChecked').value = "false";
                nicknameInput.focus();
            } else {
                errorEl.innerText = errorMessages.success_nickname;
                errorEl.style.color = "#4285F4";
                errorEl.style.display = 'block';
                document.getElementById('nicknameChecked').value = "true";
            }
        })
        .catch(error => {
            console.error('Error:', error);
        });
}

/**
 * 대상 입력 필드 하단에 에러 메시지를 표시하고 포커스를 이동시킵니다.
 *
 * @param {string} inputId 대상을 지시하는 입력 필드 식별자
 * @param {string} errorId 에러 메시지를 렌더링할 텍스트 요소 식별자
 * @param {string} msg 렌더링할 다국어 에러 메시지
 */
function showError(inputId, errorId, msg) {
    const errorEl = document.getElementById(errorId);
    if(errorEl) {
        errorEl.innerText = msg;
        errorEl.style.color = '#EA4335';
        errorEl.style.display = 'block';
    }
    if(inputId) {
        const input = document.getElementById(inputId);
        if(input) input.focus();
    }
}

/**
 * 특정 필드의 빈 값 및 정규식 일치 여부를 단일 함수로 검사합니다.
 *
 * @param {string} inputId 검사할 대상 입력 필드 식별자
 * @param {string} errorId 에러 메시지를 렌더링할 텍스트 요소 식별자
 * @param {string} emptyMsg 값이 비어있을 때 출력할 에러 메시지
 * @param {RegExp} [regex=null] 값의 형식을 검사할 정규표현식 객체
 * @param {string} [regexMsg=null] 정규식 검사 실패 시 출력할 에러 메시지
 * @returns {boolean} 검사 통과 시 true 반환
 */
function checkField(inputId, errorId, emptyMsg, regex = null, regexMsg = null) {
    const input = document.getElementById(inputId);
    if(!input) return false;

    const value = input.value.trim();
    if (!value) { showError(inputId, errorId, emptyMsg); return false; }
    if (regex && !regex.test(value)) { showError(inputId, errorId, regexMsg); return false; }
    return true;
}

/**
 * 폼 제출 시 실행되는 이벤트 리스너
 * 정의된 순서에 따라 모든 입력 필드, 중복 확인 상태, 필수 약관 동의 및
 * 증빙서류 업로드 여부를 순차적으로 검증합니다.
 */
document.getElementById('joinForm').addEventListener('submit', function(e) {

    document.querySelectorAll('.error-msg').forEach(el => {
        el.style.display = 'none';
        el.style.color = '#EA4335';
    });

    function fail(inputId, errorId, msg) {
        showError(inputId, errorId, msg);
        e.preventDefault();
        return true;
    }

    function checkAndStop(inputId, errorId, emptyMsg, regex, regexMsg) {
        if (!checkField(inputId, errorId, emptyMsg, regex, regexMsg)) {
            e.preventDefault();
            return true;
        }
        return false;
    }

    if (checkAndStop('name_kanji_sei', 'error_name_kanji_sei', errorMessages.name_kanji_sei, regexPatterns.name_kanji, errorMessages.regex_kanji)) return;
    if (checkAndStop('name_kanji_mei', 'error_name_kanji_mei', errorMessages.name_kanji_mei, regexPatterns.name_kanji, errorMessages.regex_kanji)) return;
    if (checkAndStop('name_kana_sei', 'error_name_kana_sei', errorMessages.name_kana_sei, regexPatterns.name_kana, errorMessages.regex_kana)) return;
    if (checkAndStop('name_kana_mei', 'error_name_kana_mei', errorMessages.name_kana_mei, regexPatterns.name_kana, errorMessages.regex_kana)) return;

    if (checkAndStop('nickname', 'error_nickname', errorMessages.nickname)) return;
    if (document.getElementById('nicknameChecked').value !== "true") {
        return fail('nickname', 'error_nickname', errorMessages.check_dup);
    }

    const y = document.getElementById('birth_year').value;
    const m = document.getElementById('birth_month').value;
    const d = document.getElementById('birth_day').value;
    if (!y || !m || !d) {
        return fail('birth_year', 'error_birth', errorMessages.birth);
    }

    if (checkAndStop('zipcode', 'error_zipcode', errorMessages.zipcode)) return;
    if (checkAndStop('address_detail', 'error_address_detail', errorMessages.address_detail)) return;

    if (checkAndStop('contact', 'error_contact', errorMessages.contact_empty, regexPatterns.contact, errorMessages.contact_invalid)) return;

    if (checkAndStop('email', 'error_email', errorMessages.email_empty, regexPatterns.email, errorMessages.email_invalid)) return;
    if (document.getElementById('emailChecked').value !== "true") {
        return fail('email', 'error_email', errorMessages.check_dup);
    }

    if (checkAndStop('password', 'error_password', errorMessages.pw_empty, regexPatterns.password, errorMessages.pw_invalid)) return;
    const pw = document.getElementById('password').value;
    const pwConf = document.getElementById('password_confirm').value;
    if (pw !== pwConf) {
        return fail('password_confirm', 'error_password_confirm', errorMessages.pw_mismatch);
    }

    const evidenceFile = document.getElementById('evidenceFile');
    if (evidenceFile && evidenceFile.files.length === 0) {
        return fail(null, 'error_evidence', errorMessages.evidence_empty);
    }

    if (checkAndStop('join_path', 'error_join_path', errorMessages.joinpath)) return;

    const requiredTerms = [
        'term_age',
        'term_service',
        'term_privacy',
        'term_location',
        'term_provision'
    ];

    for (const id of requiredTerms) {
        const checkbox = document.getElementById(id);
        if (!checkbox || !checkbox.checked) {
            return fail(null, 'error_terms', errorMessages.terms);
        }
    }
});