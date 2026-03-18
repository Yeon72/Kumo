/**
 * admin_sidebar.js
 * 관리자 사이드바의 뷰 제어 및 다국어(언어) 설정 변경 기능을 담당합니다.
 */

/**
 * 현재 URL의 'lang' 파라미터를 교체하여 관리자 페이지의 언어를 변경하고 새로고침합니다.
 *
 * @param {string} newLang 변경할 대상 언어 코드 (예: 'ko', 'ja')
 */
function changeAdminLanguage(newLang) {
    const url = new URL(window.location.href);
    url.searchParams.set('lang', newLang);
    window.location.href = url.toString();
}