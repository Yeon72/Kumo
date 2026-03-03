// ==========================================
// 1. 전역(Global) 함수: HTML의 onclick에서 직접 호출하는 함수들 (회원 탈퇴 모달)
// ==========================================

// 모달 열기
function openDeleteModal() {
    document.getElementById('deleteAccountModal').style.display = 'flex';
    document.getElementById('deleteConfirmPw').value = ''; // 비밀번호 입력창 초기화
    document.getElementById('deleteConfirmPwCheck').value = ''; // 비밀번호 확인창 초기화
    document.getElementById('btnConfirmDelete').disabled = true; // 탈퇴 버튼 비활성화
    document.getElementById('deleteMismatchMsg').style.display = 'none'; // 불일치 에러 숨김
    document.getElementById('deleteErrorMsg').style.display = 'none'; // 서버 에러 숨김
}

// 모달 닫기
function closeDeleteModal() {
    document.getElementById('deleteAccountModal').style.display = 'none';
}

// 비밀번호 일치 여부 실시간 검사 (두 비밀번호가 같아야 버튼 활성화)
function checkDeleteInput() {
    const pw = document.getElementById('deleteConfirmPw').value;
    const pwCheck = document.getElementById('deleteConfirmPwCheck').value;
    const deleteBtn = document.getElementById('btnConfirmDelete');
    const mismatchMsg = document.getElementById('deleteMismatchMsg');
    const serverErrorMsg = document.getElementById('deleteErrorMsg');

    // 타이핑을 시작하면 기존 서버 에러 메시지는 숨김
    if(serverErrorMsg) serverErrorMsg.style.display = 'none';

    // 둘 다 한 글자 이상 입력되었을 때만 비교 시작
    if (pw.length > 0 && pwCheck.length > 0) {
        if (pw === pwCheck) {
            if(mismatchMsg) mismatchMsg.style.display = 'none';
            deleteBtn.disabled = false; // 일치하면 활성화!
        } else {
            if(mismatchMsg) mismatchMsg.style.display = 'block';
            deleteBtn.disabled = true; // 불일치하면 비활성화
        }
    } else {
        if(mismatchMsg) mismatchMsg.style.display = 'none';
        deleteBtn.disabled = true;
    }
}

// 탈퇴 실행 (서버로 데이터 전송)
function executeDelete() {
    const password = document.getElementById('deleteConfirmPw').value;
    const errorMsg = document.getElementById('deleteErrorMsg');


    // AJAX (Fetch API) 요청
    fetch('/api/user/delete', { // 실제 백엔드 컨트롤러 URL에 맞게 수정하세요.
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            // CSRF 토큰이 필요하다면 여기에 추가
        },
        body: JSON.stringify({ password: password})
    })
    .then(response => {
        if (response.ok) {
            alert("회원 탈퇴가 정상적으로 처리되었습니다. 그동안 이용해 주셔서 감사합니다.");
            window.location.href = "/logout"; // 성공 시 로그아웃 및 메인으로 이동
        } else if (response.status === 401 || response.status === 400) {
            // 비밀번호 틀림
            if(errorMsg) {
                errorMsg.style.display = 'block';
                errorMsg.innerText = "비밀번호가 일치하지 않습니다.";
            }
        } else {
            alert("오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert("서버와 통신 중 문제가 발생했습니다.");
    });
}


// ==========================================
// 2. DOMContentLoaded: 화면이 모두 로드된 후 실행 (프로필 사진, 스위치 등)
// ==========================================
document.addEventListener("DOMContentLoaded", function () {
    const modal = document.getElementById('profileModal');
    const btnOpenModal = document.getElementById('btnOpenModal');
    const btnCloseX = document.querySelector('.close-btn');
    const btnCancel = document.querySelector('.btn-cancel');
    const btnSave = document.getElementById('btnSaveImage');

    const fileInput = document.getElementById('fileInput');
    const modalPreview = document.getElementById('modalPreview');
    const currentProfileImg = document.getElementById('currentProfileImg');
    const fileNameSpan = document.getElementById('fileName');

    // [1] 프로필 모달 열기 & 초기화
    if (btnOpenModal) {
        btnOpenModal.addEventListener('click', function () {
            modal.classList.add('show');
            if (currentProfileImg && modalPreview) modalPreview.src = currentProfileImg.src;
            fileInput.value = '';
            if (fileNameSpan) {
                fileNameSpan.innerText = msg.fileNone; // "ファイルが選択されていません"
                fileNameSpan.style.color = "#888";
            }
        });
    }

    const closeModal = () => modal.classList.remove('show');
    if (btnCloseX) btnCloseX.addEventListener('click', closeModal);
    if (btnCancel) btnCancel.addEventListener('click', closeModal);

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

    // [3] 서버 전송 (저장 버튼)
    if (btnSave) {
        btnSave.addEventListener('click', function () {
            if (!fileInput.files[0]) {
                alert(msg.selectPhoto); // "変更する写真を選択してください。"
                return;
            }

            const formData = new FormData();
            formData.append("profileImage", fileInput.files[0]);

            fetch('/api/profileImage', {
                method: 'POST',
                body: formData
            })
                .then(response => {
                    if (response.ok) return response.text();
                    throw new Error('FAILED');
                })
                .then(newImageUrl => {
                    alert(msg.uploadSuccess); // "プロフィール写真が変更されました。"
                    if (newImageUrl && currentProfileImg) currentProfileImg.src = newImageUrl;
                    closeModal();
                })
                .catch(err => alert(msg.error + err.message)); // "エラーが発生しました: ..."
        });
    }

    // [4] 소셜 연동 알림 (LINE, Google) - alert → Swal로 교체
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