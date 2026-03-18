/**
 * dark-theme.js
 * FOUC(Flash of Unstyled Content, 화면 깜빡임 현상)를 방지하기 위해
 * DOM과 CSS가 완전히 로드되기 전에 즉시 실행되어(IIFE) 로컬 스토리지의 테마 설정을 적용합니다.
 */
(function () {
    const savedTheme = localStorage.getItem("theme");
    if (savedTheme === "dark") {
        document.documentElement.classList.add("dark-mode");
    }
})();