/**
 * editJobPosting.js
 * 구인자 공고 수정 페이지의 프론트엔드 비즈니스 로직을 담당합니다.
 * 연락처 입력 필드의 자동 포맷팅(하이픈 추가), 유효성 검사,
 * 그리고 비동기 통신(Fetch)을 통한 공고 마감 기능을 수행합니다.
 */

document.addEventListener("DOMContentLoaded", () => {

    const contactInput = document.getElementById('contactPhone');
    const jobEditForm = document.getElementById('jobEditForm');

    /**
     * 연락처 입력 필드(keydown 이벤트)
     * 사용자가 숫자 및 허용된 제어 키(백스페이스, 탭 등)만 입력할 수 있도록 입력을 제한합니다.
     */
    if (contactInput) {
        contactInput.addEventListener('keydown', function (e) {
            const allowed = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab'];
            if (!/[0-9]/.test(e.key) && !allowed.includes(e.key)) {
                e.preventDefault();
            }
        });

        /**
         * 연락처 입력 필드(input 이벤트)
         * 사용자가 입력한 숫자의 길이에 맞춰 하이픈(-)을 자동으로 삽입(포맷팅)합니다.
         */
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

            // 값이 입력되면 에러 메시지 초기화
            document.getElementById('error_contact').style.display = 'none';
            contactInput.style.borderColor = '';
        });
    }

    /**
     * 폼 제출(submit) 이벤트
     * 서버로 데이터를 전송하기 전 연락처 필드의 빈 값 여부 및 정규식 기반 유효성 검사를 수행합니다.
     */
    if (jobEditForm) {
        jobEditForm.addEventListener('submit', function (e) {
            const val = contactInput ? contactInput.value : '';
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
        });
    }
});

/**
 * [마감하기] 버튼 클릭 시 호출됩니다.
 * SweetAlert2를 통해 사용자에게 최종 확인을 받은 후,
 * Fetch API를 이용하여 서버에 해당 공고의 마감 처리를 비동기로 요청합니다.
 */
function closePosting() {
    const datanum = document.querySelector('input[name="datanum"]').value;
    const region = document.querySelector('input[name="region"]').value;

    Swal.fire({
        title: msgCloseTitle,
        text: msgCloseText,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#333d4b',
        confirmButtonText: msgBtnClose,
        cancelButtonText: msgBtnCancel
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(`/Recruiter/closeJobPosting?datanum=${datanum}&region=${region}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            })
                .then(response => response.text())
                .then(data => {
                    if (data === "success") {
                        Swal.fire({ title: msgOkTitle, text: msgOkText, icon: 'success' })
                            .then(() => { location.href = '/Recruiter/JobManage'; });
                    } else {
                        Swal.fire(msgErrorTitle, msgErrorFail, 'error');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    Swal.fire(msgErrorTitle, msgErrorComm, 'error');
                });
        }
    });
}