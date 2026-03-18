/**
 * 전역 다국어 설정 변수 (HTML 템플릿에서 주입, 기본값: 'ko')
 * @type {string}
 */
const currentLang = window.CURRENT_LANG || 'ko';

/**
 * 다국어(언어) 설정을 변경하고 페이지를 새로고침합니다.
 *
 * @param {string} lang 변경할 언어 코드
 */
function changeLanguage(lang) {
    const url = new URL(window.location.href);
    url.searchParams.set('lang', lang);
    window.location.href = url.toString();
}

/**
 * 활성화된 탭 버튼 및 탭 콘텐츠 화면을 전환합니다.
 *
 * @param {string} tabName 전환할 대상 탭의 식별자
 */
function switchTab(tabName) {
    document.querySelectorAll('.tab-item').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    document.getElementById('tab-btn-' + tabName).classList.add('active');
    document.getElementById('tab-content-' + tabName).classList.add('active');
}

/**
 * 특정 이름을 가진 모든 체크박스의 선택 상태를 일괄 변경합니다.
 *
 * @param {string} name 대상 체크박스 요소의 name 속성값
 * @param {boolean} isChecked 적용할 체크 상태 (true: 선택, false: 해제)
 */
function toggleAll(name, isChecked) {
    document.querySelectorAll(`input[name="${name}"]`).forEach(cb => cb.checked = isChecked);
}

/**
 * 구인 공고 수정을 위한 모달 창을 열고, 기존 데이터를 폼에 세팅합니다.
 *
 * @param {string} source 대상 공고의 데이터 출처 (예: OSAKA, TOKYO)
 * @param {string|number} id 대상 공고의 고유 식별자
 * @param {string} status 대상 공고의 현재 상태 (예: RECRUITING, CLOSED)
 */
function openEditModal(source, id, status) {
    document.getElementById('editPostSource').value = source;
    document.getElementById('editPostId').value = id;
    document.getElementById('editPostStatus').value = status || 'RECRUITING';
    document.getElementById('editPostModal').style.display = 'flex';
}

/**
 * 구인 공고 수정 모달 창을 닫습니다.
 */
function closeEditModal() {
    document.getElementById('editPostModal').style.display = 'none';
}

/**
 * 폼에 입력된 데이터를 바탕으로 구인 공고 상태 수정 요청(API)을 전송합니다.
 */
function submitEdit() {
    const source = document.getElementById('editPostSource').value;
    const id = document.getElementById('editPostId').value;
    const status = document.getElementById('editPostStatus').value;

    const msg = currentLang === 'ja' ? "修正しますか？" : "수정하시겠습니까?";
    if(confirm(msg)) {
        fetch('/admin/post/edit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ source: source, id: id, status: status })
        }).then(res => {
            if (res.ok) {
                alert(currentLang === 'ja' ? "修正が完了しました。" : "수정이 완료되었습니다.");
                location.reload();
            } else {
                alert(currentLang === 'ja' ? "修正に失敗しました。" : "수정에 실패했습니다.");
            }
        }).catch(error => {
            console.error('Error:', error);
            alert("Network Error");
        });

        closeEditModal();
    }
}

/**
 * 신고 내역 확인 및 상태 변경을 위한 모달 창을 열고 데이터를 세팅합니다.
 *
 * @param {string|number} id 신고 내역 고유 식별자
 * @param {string} email 신고자 이메일 계정
 * @param {string} category 신고 사유 카테고리
 * @param {string} desc 신고 상세 내용
 * @param {string} status 현재 신고 처리 상태
 * @param {string} targetSource 신고 대상 공고의 데이터 출처
 * @param {string|number} targetPostId 신고 대상 공고의 고유 식별자
 */
function openReportModal(id, email, category, desc, status, targetSource, targetPostId) {
    document.getElementById('reportIdVal').value = id;
    document.getElementById('reportEmailVal').innerText = email;
    document.getElementById('reportCategoryVal').innerText = category;
    document.getElementById('reportDescVal').innerText = desc;
    document.getElementById('reportStatusSelect').value = status || 'PENDING';

    document.getElementById('reportTargetSource').value = targetSource || '';
    document.getElementById('reportTargetPostId').value = targetPostId || '';

    document.getElementById('reportModal').style.display = 'flex';
}

/**
 * 신고 확인 및 상태 변경 모달 창을 닫습니다.
 */
function closeReportModal() {
    document.getElementById('reportModal').style.display = 'none';
}

/**
 * 모달에 설정된 데이터를 바탕으로 신고 내역의 처리 상태 변경 요청(API)을 전송합니다.
 * 상태가 '차단됨(BLOCKED)'으로 변경될 경우, 원본 게시글과 해당 신고 내역의 일괄 삭제 로직을 수행합니다.
 */
function submitReportStatus() {
    const reportId = document.getElementById('reportIdVal').value;
    const newStatus = document.getElementById('reportStatusSelect').value;
    const targetSource = document.getElementById('reportTargetSource').value;
    const targetPostId = document.getElementById('reportTargetPostId').value;

    const msg = currentLang === 'ja' ? "申告状態を変更しますか？" : "신고 상태를 변경하시겠습니까?";
    if(confirm(msg)) {

        // 상태가 'BLOCKED'인 경우 대상 게시글 및 신고 내역을 일괄 삭제 처리합니다.
        if (newStatus === 'BLOCKED') {
            const deleteMsg = currentLang === 'ja'
                ? "「遮断済み」として処理し、この申告内容と掲示物を削除しますか？"
                : "'차단됨'으로 처리하며, 이 신고 내역과 원본 게시글을 리스트에서 완전히 삭제하시겠습니까?";

            if (confirm(deleteMsg)) {
                // 1. 원본 게시글 삭제 처리
                if (targetSource && targetPostId) {
                    fetch('/admin/post/delete', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ ids: [targetSource + "_" + targetPostId] })
                    });
                }

                // 2. 신고 내역 삭제 API 호출
                fetch('/admin/report/delete', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ ids: [parseInt(reportId)] })
                }).then(res => {
                    if(res.ok) {
                        alert(currentLang === 'ja' ? "遮断および削除が完了しました。" : "차단 및 삭제 처리가 완료되었습니다.");
                        location.reload();
                    } else {
                        alert("Error during deletion");
                    }
                });

                closeReportModal();
                return;
            }
        }

        fetch('/admin/report/edit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: reportId, status: newStatus })
        }).then(res => {
            if(res.ok) {
                alert(currentLang === 'ja' ? "処理が完了しました。" : "처리가 완료되었습니다.");
                const url = new URL(window.location.href);
                url.searchParams.set('tab', 'report');
                window.location.href = url.toString();
            } else {
                alert(currentLang === 'ja' ? "処理に失敗しました。" : "처리에 실패했습니다.");
            }
        }).catch(error => {
            console.error('Error:', error);
            alert("Network Error");
        });

        closeReportModal();
    }
}

/**
 * 단일 구인 공고 항목에 대한 삭제 요청(API)을 전송합니다.
 *
 * @param {string} source 대상 공고의 데이터 출처
 * @param {string|number} id 대상 공고의 고유 식별자
 */
function deleteOnePost(source, id) {
    const msg = currentLang === 'ja' ? "本当にこの求人を削除しますか？" : "정말 이 공고를 삭제하시겠습니까?";
    if(!confirm(msg)) return;

    fetch('/admin/post/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ids: [source + "_" + id] })
    }).then(res => {
        if (res.ok) { location.reload(); }
        else { alert(currentLang === 'ja' ? "削除に失敗しました。" : "삭제 실패"); }
    });
}

/**
 * 현재 활성화된 탭(공고 목록 또는 신고 목록)에 따라
 * 다중 선택된 항목들의 일괄 삭제 요청(API)을 전송합니다.
 */
function deleteSelectedItems() {
    const isReportTab = document.getElementById('tab-content-report').classList.contains('active');
    const targetName = isReportTab ? 'reportIds' : 'postIds';
    const checkedBoxes = document.querySelectorAll(`input[name="${targetName}"]:checked`);

    if (checkedBoxes.length === 0) {
        alert(currentLang === 'ja' ? "削除する項目を選択してください。" : "삭제할 항목을 선택해주세요.");
        return;
    }

    const ids = Array.from(checkedBoxes).map(cb => isReportTab ? parseInt(cb.value) : cb.value);
    if (!confirm(currentLang === 'ja' ? "選択した項目を削除しますか？" : "선택한 항목을 삭제하시겠습니까?")) return;

    fetch(isReportTab ? '/admin/report/delete' : '/admin/post/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ids: ids })
    }).then(res => {
        if (res.ok) { location.reload(); }
        else { alert("Error"); }
    });
}