/**
 * jobPosting.js
 * 구인자의 신규 공고 등록 페이지의 프론트엔드 비즈니스 로직을 담당합니다.
 * 이미지 다중 업로드 미리보기 기능, 연락처 입력 폼의 자동 하이픈(-) 포맷팅 및 정규식 검증 기능을 수행합니다.
 */

document.addEventListener('DOMContentLoaded', () => {
    const jobPostForm = document.getElementById('jobPostForm');
    const fileUpload = document.getElementById('file-upload');
    const contactInput = document.getElementById('contactPhone');

    /**
     * 파일 업로드 변경 이벤트
     * 사용자가 선택한 이미지 파일들을 읽어와(FileReader) 화면(previewArea)에 썸네일로 즉시 미리 보여줍니다.
     */
    if (fileUpload) {
        fileUpload.addEventListener('change', function () {
            const previewArea = document.getElementById('previewArea');
            previewArea.innerHTML = '';

            Array.from(this.files).forEach(file => {
                const reader = new FileReader();
                reader.onload = function (e) {
                    const wrapper = document.createElement('div');
                    wrapper.style.cssText = 'position:relative; width:80px; height:80px;';

                    const img = document.createElement('img');
                    img.src = e.target.result;
                    img.style.cssText = 'width:80px; height:80px; object-fit:cover; border-radius:10px; border:2px solid #e5e8eb;';

                    wrapper.appendChild(img);
                    previewArea.appendChild(wrapper);
                };
                reader.readAsDataURL(file);
            });
        });
    }

    /**
     * 연락처 입력 필드 제어
     * 키보드 입력을 감지하여 숫자 및 필수 기능 키만 허용하며,
     * 입력된 값의 길이에 따라 일본 전화번호 규격에 맞춰 하이픈(-)을 자동으로 삽입합니다.
     */
    if (contactInput) {
        contactInput.addEventListener('keydown', function (e) {
            const allowed = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab'];
            if (!/[0-9]/.test(e.key) && !allowed.includes(e.key)) {
                e.preventDefault();
            }
        });

        contactInput.addEventListener('input', function () {
            let val = this.value.replace(/[^0-9]/g, "");
            let formatted = "";
            if (val.length <= 3) {
                formatted = val;
            } else if (val.length <= 7) {
                formatted = val.slice(0, 3) + "-" + val.slice(3);
            } else {
                formatted = val.length === 10
                    ? val.slice(0, 3) + "-" + val.slice(3, 6) + "-" + val.slice(6)
                    : val.slice(0, 3) + "-" + val.slice(3, 7) + "-" + val.slice(7, 11);
            }
            this.value = formatted;

            document.getElementById('error_contact').style.display = 'none';
            contactInput.style.borderColor = '';
        });
    }

    /**
     * 폼 제출(Submit) 이벤트
     * 1. 마감일시(년,월,일,시간) 데이터가 DOM에 존재할 경우 ISO 형식(YYYY-MM-DDTHH:mm)으로 조합합니다.
     * 2. 연락처(contactPhone)의 빈 값 및 정규식 위반 여부를 검증하고, 오류 시 제출을 차단합니다.
     */
    if (jobPostForm) {
        jobPostForm.addEventListener('submit', function (e) {

            // 1. 마감일 조합 로직 (DOM 요소 존재 시에만 동작)
            const yEl = document.getElementById('dl-year');
            const mEl = document.getElementById('dl-month');
            const dEl = document.getElementById('dl-day');
            const tEl = document.getElementById('dl-time');
            const hiddenDl = document.getElementById('hiddenDeadline');

            if (yEl && mEl && dEl && hiddenDl) {
                const y = yEl.value;
                const m = mEl.value.padStart(2, '0');
                const d = dEl.value.padStart(2, '0');
                const t = tEl ? tEl.value : '';

                if (y && m && d) {
                    const time = t ? 'T' + t : '';
                    hiddenDl.value = `${y}-${m}-${d}${time}`;
                }
            }

            // 2. 연락처 유효성 검사 로직
            if (contactInput) {
                const val = contactInput.value;
                const regex = /^0\d{1,4}-\d{1,4}-\d{4}$/;
                const errorBox = document.getElementById('error_contact');
                const errorMsg = document.getElementById('error_contact_msg');

                if (!val) {
                    e.preventDefault();
                    errorMsg.textContent = contactErrors.empty;
                    errorBox.style.display = 'block';
                    contactInput.style.borderColor = '#e53e3e';
                    contactInput.focus();
                    return;
                }

                if (!regex.test(val)) {
                    e.preventDefault();
                    errorMsg.textContent = contactErrors.invalid;
                    errorBox.style.display = 'block';
                    contactInput.style.borderColor = '#e53e3e';
                    contactInput.focus();
                    return;
                }

                errorBox.style.display = 'none';
                contactInput.style.borderColor = '';
            }
        });
    }
});

/**
 * 모달 등 다른 뷰에서 회사를 선택했을 때 폼 내부의 hidden input 값과 UI 탭 상태를 갱신합니다.
 * @param {string|number} companyId 선택된 회사의 고유 식별자
 * @param {HTMLElement} btnElement 클릭된 회사 선택 버튼 요소
 */
function selectCompanyForPost(companyId, btnElement) {
    const selectedCompanyInput = document.getElementById('selectedCompanyId');
    if (selectedCompanyInput) {
        selectedCompanyInput.value = companyId;
    }

    document.querySelectorAll('#companySelectTabs .nav-pill').forEach(tab => {
        tab.classList.remove('active');
    });

    if (btnElement) {
        btnElement.classList.add('active');
    }
}