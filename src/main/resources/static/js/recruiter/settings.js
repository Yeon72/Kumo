/**
 * DOMContentLoaded 이벤트 리스너
 * 프로필 이미지 변경 모달의 제어(열기/닫기, 파일 미리보기, 서버 업로드) 및
 * SNS 연동 토글 버튼의 알림 이벤트를 초기화하고 바인딩합니다.
 */
document.addEventListener("DOMContentLoaded", function () {
    const modal = document.getElementById('profileModal');
    const btnOpenModal = document.getElementById('btnOpenModal');
    const btnCloseX = document.querySelector('.close-btn');
    const btnCancel = document.getElementById('cancelProfileBtn');
    const btnSave = document.getElementById('saveProfileBtn');

    const fileInput = document.getElementById('fileInput');
    const profilePreview = document.getElementById('profilePreview');
    const currentProfileImg = document.getElementById('currentProfileImg');
    const fileNameSpan = document.getElementById('fileName');

    if (btnOpenModal) {
        btnOpenModal.addEventListener('click', function () {
            modal.style.display = "flex";
            if (currentProfileImg && profilePreview) {
                profilePreview.src = currentProfileImg.src;
            }
            fileInput.value = '';
            if (fileNameSpan) {
                fileNameSpan.innerText = typeof settingsMsg !== 'undefined' ? settingsMsg.fileNone : '선택된 파일 없음';
            }
        });
    }

    /**
     * 프로필 모달에 닫기 애니메이션 클래스를 부여하고, 지정된 지연 시간 후 화면에서 숨깁니다.
     */
    const closeModal = () => {
        modal.classList.add('closing');
        setTimeout(() => {
            modal.style.display = "none";
            modal.classList.remove('closing');
        }, 250);
    };

    if (btnCloseX) btnCloseX.addEventListener('click', closeModal);
    if (btnCancel) btnCancel.addEventListener('click', closeModal);

    window.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
    });

    if (fileInput) {
        fileInput.addEventListener('change', function (e) {
            const file = e.target.files[0];
            if (file) {
                if (fileNameSpan) {
                    fileNameSpan.innerText = file.name;
                }
                const reader = new FileReader();
                reader.onload = function (evt) {
                    if (profilePreview) profilePreview.src = evt.target.result;
                };
                reader.readAsDataURL(file);
            }
        });
    }

    if (btnSave) {
        btnSave.addEventListener('click', function () {
            const file = fileInput.files[0];
            if (!file) {
                Swal.fire({
                    icon: 'warning',
                    title: typeof settingsMsg !== 'undefined' ? settingsMsg.noPhoto : '사진을 선택해주세요.'
                });
                return;
            }

            const formData = new FormData();
            formData.append("profileImage", file);

            fetch('/Recruiter/UploadProfile', {
                method: 'POST',
                body: formData
            })
                .then(response => {
                    if (response.ok) return response.text();
                    throw new Error('UPLOAD_FAILED');
                })
                .then(newImageUrl => {
                    Swal.fire({
                        icon: 'success',
                        title: typeof settingsMsg !== 'undefined' ? settingsMsg.uploadSuccess : '프로필 사진이 업로드 되었습니다.'
                    }).then(() => {
                        if (newImageUrl && currentProfileImg) {
                            currentProfileImg.src = newImageUrl;
                        }
                        closeModal();
                    });
                })
                .catch(err => {
                    Swal.fire({
                        icon: 'error',
                        title: typeof settingsMsg !== 'undefined' ? settingsMsg.uploadFail : '업로드 실패',
                        text: err.message
                    });
                });
        });
    }

    document.querySelectorAll('.sns-toggle').forEach(toggle => {
        toggle.addEventListener('click', (e) => {
            e.preventDefault();
            const isDark = document.body.classList.contains('dark-mode');
            Swal.fire({
                title: typeof snsMsg !== 'undefined' ? snsMsg.snsTitle : 'Service Notice',
                text: typeof snsMsg !== 'undefined' ? snsMsg.snsText : '아직 서비스 준비 중입니다.',
                icon: 'info',
                confirmButtonColor: '#7db4e6',
                background: isDark ? '#2a2b2e' : '#fff',
                color: isDark ? '#e3e5e8' : '#333'
            });
        });
    });

});

/**
 * 회원 탈퇴 진행을 위한 확인 모달 창을 엽니다.
 * 배경 스크롤을 방지하기 위해 body의 overflow 속성을 조작합니다.
 */
function openDeleteModal() {
    const modal = document.getElementById('deleteAccountModal');
    if (modal) {
        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }
}

/**
 * 회원 탈퇴 확인 모달 창을 닫고, 내부에 입력된 비밀번호 데이터 및
 * 에러 메시지 상태, 버튼 활성화 상태를 초기화합니다.
 */
function closeDeleteModal() {
    const modal = document.getElementById('deleteAccountModal');
    if (modal) {
        modal.classList.add('closing');
        setTimeout(() => {
            modal.style.display = 'none';
            modal.classList.remove('closing');
            document.body.style.overflow = '';

            document.getElementById('deleteConfirmPw').value = '';
            document.getElementById('deleteConfirmPwCheck').value = '';
            document.getElementById('deleteMismatchMsg').style.display = 'none';
            document.getElementById('deleteErrorMsg').style.display = 'none';
            document.getElementById('btnConfirmDelete').disabled = true;
        }, 250);
    }
}

/**
 * 탈퇴 모달 내의 비밀번호 및 비밀번호 확인 입력 필드 값을 실시간으로 비교하여,
 * 일치 여부에 따라 경고 메시지 노출 및 탈퇴 실행 버튼의 활성화 상태를 제어합니다.
 */
function checkDeleteInput() {
    const pw = document.getElementById('deleteConfirmPw').value;
    const pwCheck = document.getElementById('deleteConfirmPwCheck').value;
    const mismatchMsg = document.getElementById('deleteMismatchMsg');
    const btn = document.getElementById('btnConfirmDelete');

    if (pw && pwCheck) {
        if (pw === pwCheck) {
            mismatchMsg.style.display = 'none';
            btn.disabled = false;
        } else {
            mismatchMsg.style.display = 'block';
            btn.disabled = true;
        }
    } else {
        mismatchMsg.style.display = 'none';
        btn.disabled = true;
    }
}

/**
 * 사용자에게 탈퇴에 대한 최종 확인(SweetAlert2)을 받은 후,
 * 입력된 비밀번호와 함께 서버에 계정 완전 삭제 비동기 요청을 전송합니다.
 * 성공 시 로그아웃 엔드포인트로 리다이렉트합니다.
 */
function executeDelete() {
    const password = document.getElementById('deleteConfirmPw').value;
    const errorMsg = document.getElementById('deleteErrorMsg');
    const isDark = document.body.classList.contains('dark-mode');

    Swal.fire({
        title: typeof delMsg !== 'undefined' ? delMsg.confirmTitle : '정말 탈퇴하시겠습니까?',
        text: typeof delMsg !== 'undefined' ? delMsg.confirmText : '탈퇴 후 데이터는 복구할 수 없습니다.',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: typeof delMsg !== 'undefined' ? delMsg.btnDelete : '탈퇴',
        cancelButtonText: typeof delMsg !== 'undefined' ? delMsg.btnCancel : '취소',
        background: isDark ? '#2a2b2e' : '#fff',
        color: isDark ? '#e3e5e8' : '#333'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch('/api/user/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ password: password })
            })
                .then(async response => {
                    if (response.ok) {
                        Swal.fire({
                            title: typeof delMsg !== 'undefined' ? delMsg.successTitle : '탈퇴 완료',
                            text: typeof delMsg !== 'undefined' ? delMsg.successText : '그동안 KUMO를 이용해 주셔서 감사합니다.',
                            icon: 'success',
                            background: isDark ? '#2a2b2e' : '#fff',
                            color: isDark ? '#e3e5e8' : '#333'
                        }).then(() => {
                            window.location.href = '/logout';
                        });
                    } else {
                        const errorText = await response.text();
                        errorMsg.innerText = errorText || (typeof delMsg !== 'undefined' ? delMsg.errorText : '비밀번호가 일치하지 않거나 오류가 발생했습니다.');
                        errorMsg.style.display = 'block';
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    Swal.fire({
                        title: typeof delMsg !== 'undefined' ? delMsg.errorTitle : '오류',
                        text: typeof delMsg !== 'undefined' ? delMsg.errorText : '서버와의 통신 중 오류가 발생했습니다.',
                        icon: 'error',
                        background: isDark ? '#2a2b2e' : '#fff',
                        color: isDark ? '#e3e5e8' : '#333'
                    });
                });
        }
    });
}