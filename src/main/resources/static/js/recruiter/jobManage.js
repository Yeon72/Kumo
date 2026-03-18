/**
 * jobManage.js
 * 구인자의 공고 관리 페이지의 프론트엔드 비즈니스 로직을 담당합니다.
 * 공고 관리 모달 호출, 공고 삭제 처리(Fetch), 테이블 열 클릭 시의 클라이언트 사이드 정렬 기능을 수행합니다.
 */

/**
 * 테이블 내 모집중(RECRUITING) 상태인 공고의 기어(톱니바퀴) 버튼을 클릭했을 때 호출되며,
 * 해당 공고의 지역 및 제목 정보를 다국어 처리에 맞게 모달창에 세팅하고 표시합니다.
 *
 * @param {number|string} id 공고의 고유 식별자 (상세 보기용)
 * @param {number|string} datanum 공고의 데이터 번호 (삭제/수정용)
 * @param {string} regionType 한글/일어 형태의 지역 텍스트 ('도쿄', '東京', '오사카' 등)
 * @param {string} title 기본(한글) 공고 제목
 * @param {string} titleJp 일본어 공고 제목
 */
function openJobModal(id, datanum, regionType, title, titleJp) {
    let sourceEn = '';
    let displayRegion = '';

    if (regionType === '도쿄' || regionType === '東京' || regionType === 'Tokyo') {
        sourceEn = 'TOKYO';
        displayRegion = regionTokyo;
    } else {
        sourceEn = 'OSAKA';
        displayRegion = regionOsaka;
    }

    document.getElementById('modalRegionBadge').innerText = displayRegion;

    const finalTitle = (langCode === 'ja' && titleJp && titleJp.trim() !== '') ? titleJp : title;
    document.getElementById('modalJobTitle').innerText = finalTitle;

    document.getElementById('btnViewJob').onclick = () => {
        location.href = `/map/jobs/detail?id=${id}&source=${sourceEn}&lang=${langCode}`;
    };

    document.getElementById('btnEditJob').onclick = () => location.href = `/Recruiter/editJobPosting?id=${id}&region=${sourceEn}`;

    document.getElementById('btnDeleteJob').onclick = () => deletePosting(datanum, sourceEn);

    const modal = new bootstrap.Modal(document.getElementById('jobManageModal'));
    modal.show();
}

/**
 * 공고를 완전히 삭제하기 전 SweetAlert2를 통해 확인 창을 띄우고,
 * 승인 시 Fetch API를 이용해 서버에 비동기 삭제 요청을 보냅니다.
 *
 * @param {number|string} datanum 삭제할 공고의 데이터 번호
 * @param {string} sourceEn 영문 지역 코드 ('TOKYO' 또는 'OSAKA')
 */
function deletePosting(datanum, sourceEn) {
    Swal.fire({
        title: msgDelTitle,
        text: msgDelText,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#ff4d4f',
        confirmButtonText: msgBtnDel,
        cancelButtonText: msgBtnCancel
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(`/Recruiter/api/recruiter/postings?datanum=${datanum}&region=${sourceEn}`, {
                method: 'DELETE',
            })
                .then(response => {
                    if (response.ok) {
                        Swal.fire({
                            title: msgDelOk,
                            text: msgDelOkText,
                            icon: 'success',
                            confirmButtonColor: '#7db4e6'
                        }).then(() => location.reload());
                    } else {
                        response.text().then(msg => Swal.fire(msgError, msg, 'error'));
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    Swal.fire(msgCommError, msgCommFail, 'error');
                });
        }
    });
}

/**
 * DOMContentLoaded 이벤트 리스너
 * 테이블 헤더 클릭 시, 선택된 열의 데이터를 기준으로 테이블 행을
 * 클라이언트 사이드에서 오름차순/내림차순으로 즉시 정렬합니다.
 * (모집중 상태가 우선 노출되도록 0순위 가중치를 부여합니다.)
 */
document.addEventListener('DOMContentLoaded', () => {
    const table = document.querySelector('.kumo-table');
    if (!table) return;

    const headers = table.querySelectorAll('th');
    const tbody = table.querySelector('tbody');

    headers.forEach((header, index) => {
        if (index === 1 || index === 4 || index === 5) return;

        header.addEventListener('click', () => {
            const rows = Array.from(tbody.querySelectorAll('tr:not(.text-center)'));
            const isAscending = header.classList.contains('sort-asc');

            headers.forEach(h => h.classList.remove('sort-asc', 'sort-desc'));

            rows.sort((rowA, rowB) => {
                const statusA = rowA.querySelector('.status-badge').classList.contains('status-ing') ? 0 : 1;
                const statusB = rowB.querySelector('.status-badge').classList.contains('status-ing') ? 0 : 1;

                if (statusA !== statusB) {
                    return statusA - statusB;
                }

                let cellA = rowA.children[index].innerText.trim();
                let cellB = rowB.children[index].innerText.trim();

                if (index === 2) {
                    cellA = parseInt(cellA.replace(/[^0-9]/g, '')) || 0;
                    cellB = parseInt(cellB.replace(/[^0-9]/g, '')) || 0;
                }
                else if (index === 3) {
                    cellA = new Date(cellA);
                    cellB = new Date(cellB);
                }

                if (cellA < cellB) return isAscending ? 1 : -1;
                if (cellA > cellB) return isAscending ? -1 : 1;
                return 0;
            });

            header.classList.toggle('sort-asc', !isAscending);
            header.classList.toggle('sort-desc', isAscending);

            rows.forEach(row => tbody.appendChild(row));
        });
    });
});