document.addEventListener("DOMContentLoaded", function () {
    const modal = document.getElementById('profileModal');
    const btnOpenModal = document.getElementById('btnOpenModal');
    const btnCloseX = document.querySelector('.close-btn');
    const btnCancel = document.getElementById('btnCancel');
    const btnSave = document.getElementById('btnSaveImage');

    const fileInput = document.getElementById('fileInput');
    const modalPreview = document.getElementById('modalPreview');
    const currentProfileImg = document.getElementById('currentProfileImg');
    const fileNameSpan = document.getElementById('fileName');

    // [1] 프로필 모달 열기 & 초기화 (Seeker 로직 유지: 현재 이미지 미리보기에 반영)
    if (btnOpenModal) {
        btnOpenModal.addEventListener('click', function () {
            modal.style.display = "flex";
            if (currentProfileImg && modalPreview) {
                modalPreview.src = currentProfileImg.src;
            }
            fileInput.value = '';
            if (fileNameSpan) {
                fileNameSpan.innerText = typeof msg !== 'undefined' ? msg.fileNone : '선택된 파일 없음';
                fileNameSpan.style.color = "#888";
            }
        });
    }

    const closeModal = () => {
        modal.classList.add('closing');
        setTimeout(() => {
            modal.style.display = "none";
            modal.classList.remove('closing');
        }, 250);
    };

    if (btnCloseX) btnCloseX.addEventListener('click', closeModal);
    if (btnCancel) btnCancel.addEventListener('click', closeModal);

    // 바깥 클릭 시 닫기
    window.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
    });

    // [2] 파일 선택 시 (미리보기 & 파일명 표시)
    if (fileInput) {
        fileInput.addEventListener('change', function (e) {
            const file = e.target.files[0];
            if (file) {
                if (fileNameSpan) {
                    fileNameSpan.innerText = file.name;
                    fileNameSpan.style.color = "#333";
                }
                const reader = new FileReader();
                reader.onload = function (evt) {
                    if (modalPreview) modalPreview.src = evt.target.result;
                };
                reader.readAsDataURL(file);
            }
        });
    }

    // [3] 서버 전송 (저장 버튼 - Recruiter의 Swal UI 적용)
    if (btnSave) {
        btnSave.addEventListener('click', function () {
            const file = fileInput.files[0];
            if (!file) {
                Swal.fire({
                    icon: 'warning',
                    title: typeof msg !== 'undefined' ? msg.selectPhoto : '사진을 선택해주세요.'
                });
                return;
            }

            const formData = new FormData();
            formData.append("profileImage", file);

            fetch('/api/profileImage', {
                method: 'POST',
                body: formData
            })
            .then(response => {
                if (response.ok) return response.text();
                throw new Error('FAILED');
            })
            .then(newImageUrl => {
                Swal.fire({
                    icon: 'success',
                    title: typeof msg !== 'undefined' ? msg.uploadSuccess : '프로필 사진이 변경되었습니다.'
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
                    title: typeof msg !== 'undefined' ? msg.error : '에러 발생',
                    text: err.message
                });
            });
        });
    }

    // [4] 소셜 연동 알림 (LINE, Google)
    document.querySelectorAll('.sns-toggle').forEach(toggle => {
        toggle.addEventListener('click', (e) => {
            e.preventDefault();
            const isDark = document.documentElement.classList.contains('dark-mode');
            Swal.fire({
                title: typeof snsMsg !== 'undefined' ? snsMsg.snsTitle : 'Service Notice',
                text: typeof snsMsg !== 'undefined' ? snsMsg.snsText : '아직 서비스 준비 중입니다.',
                icon: 'info',
                confirmButtonColor: '#7db4e6',
                background: isDark ? '#2b2b2b' : '#fff',
                color: isDark ? '#fff' : '#333'
            });
        });
    });
});

/**
 * 회원 탈퇴 관련 함수들 (글로벌 스코프)
 */

// 1. 모달 열기
function openDeleteModal() {
    const modal = document.getElementById('deleteAccountModal');
    if (modal) {
        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden'; // 스크롤 방지
    }
}

// 2. 모달 닫기
function closeDeleteModal() {
    const modal = document.getElementById('deleteAccountModal');
    if (modal) {
        modal.classList.add('closing');
        setTimeout(() => {
            modal.style.display = 'none';
            modal.classList.remove('closing');
            document.body.style.overflow = ''; // 스크롤 복구
            
            // 입력 필드 초기화
            document.getElementById('deleteConfirmPw').value = '';
            document.getElementById('deleteConfirmPwCheck').value = '';
            document.getElementById('deleteMismatchMsg').style.display = 'none';
            document.getElementById('deleteErrorMsg').style.display = 'none';
            document.getElementById('btnConfirmDelete').disabled = true;
        }, 250);
    }
}

// 3. 입력 실시간 검증
function checkDeleteInput() {
    const pw = document.getElementById('deleteConfirmPw').value;
    const pwCheck = document.getElementById('deleteConfirmPwCheck').value;
    const mismatchMsg = document.getElementById('deleteMismatchMsg');
    const btn = document.getElementById('btnConfirmDelete');

    // 둘 다 입력되었을 때만 검사
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

// 4. 탈퇴 실행
function executeDelete() {
    const password = document.getElementById('deleteConfirmPw').value;
    const errorMsg = document.getElementById('deleteErrorMsg');
    const isDark = document.body.classList.contains('dark-mode');

    Swal.fire({
        title: typeof delMsg !== 'undefined' ? delMsg.confirmTitle : '정말 탈퇴하시겠습니까?',
        text: typeof delMsg !== 'undefined' ? delMsg.confirmText : "탈퇴 후 데이터는 복구할 수 없습니다.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: typeof delMsg !== 'undefined' ? delMsg.btnDelete : '탈퇴',
        cancelButtonText: typeof delMsg !== 'undefined' ? delMsg.btnCancel : '취소',
        background: isDark ? '#1e1e1e' : '#fff',
        color: isDark ? '#fff' : '#333'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch('/api/user/delete', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ password: password })
            })
            .then(async response => {
                if (response.ok) {
                    Swal.fire({
                        title: typeof delMsg !== 'undefined' ? delMsg.successTitle : '탈퇴 완료',
                        text: typeof delMsg !== 'undefined' ? delMsg.successText : '그동안 KUMO를 이용해 주셔서 감사합니다.',
                        icon: 'success',
                        background: isDark ? '#1e1e1e' : '#fff',
                        color: isDark ? '#fff' : '#333'
                    }).then(() => {
                        window.location.href = '/logout'; // 홈으로 이동
                    });
                } else {
                    const errorText = await response.text();
                    // 비밀번호 틀림 등의 에러 처리
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
                    background: isDark ? '#1e1e1e' : '#fff',
                    color: isDark ? '#fff' : '#333'
                });
            });
        }
    });
}
