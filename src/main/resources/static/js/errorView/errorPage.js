/**
 * errorPage.js
 * 에러 페이지의 UI 상호작용 및 내비게이션 로직을 담당합니다.
 */

document.addEventListener("DOMContentLoaded", () => {
    /**
     * '이전 페이지' 버튼 클릭 시 브라우저 히스토리를 한 단계 뒤로 되돌립니다.
     */
    const btnGoBack = document.getElementById("btnGoBack");
    if (btnGoBack) {
        btnGoBack.addEventListener("click", () => {
            window.history.back();
        });
    }
});