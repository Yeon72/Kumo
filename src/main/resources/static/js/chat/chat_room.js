/**
 * 웹소켓 통신 및 DOM 엘리먼트 전역 변수
 */
var stompClient = null;
var roomId = document.getElementById("roomId").value;
var myId = document.getElementById("myId").value;
var msgArea = document.getElementById("msgArea");
var lastChatDate = null;

/**
 * 현재 HTML의 lang 속성에 따라 날짜 포맷 로케일을 설정합니다.
 * @type {string}
 */
const currentLang = document.documentElement.lang === 'ja' ? 'ja-JP' : 'ko-KR';

/**
 * 부모 창(메인 사이트)의 다크모드 클래스 변경 상태를 감지하여
 * 현재 iframe 화면(채팅방)의 다크모드를 실시간으로 동기화합니다.
 */
function syncDarkMode() {
    try {
        if (window.parent.document.body.classList.contains('dark-mode')) {
            document.body.classList.add('dark-mode');
        } else {
            document.body.classList.remove('dark-mode');
        }
    } catch (e) {
        console.log("iframe 다크모드 동기화 대기 중...");
    }
}

syncDarkMode();

try {
    const observer = new MutationObserver(syncDarkMode);
    observer.observe(window.parent.document.body, { attributes: true, attributeFilter: ['class'] });
} catch (e) {
    console.log("MutationObserver 연결 실패 (단독 실행 모드)");
}

connect();

/**
 * 서버의 웹소켓 엔드포인트에 접속하고 채팅방 토픽을 구독합니다.
 * 입장 시 자동으로 읽음 처리 신호를 전송하며, 연결이 끊어질 경우 자동 재연결을 시도합니다.
 */
function connect() {
    var socket = new SockJS('/ws-stomp');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('[WebSocket] 방 입장 완료: ' + frame);

        stompClient.subscribe('/sub/chat/room/' + roomId, function (messageOutput) {
            showMessage(JSON.parse(messageOutput.body));
        });

        scrollToBottom();

        setTimeout(sendReadSignal, 300);

    }, function (error) {
        console.error('[WebSocket] ' + (window.CHAT_LANG ? window.CHAT_LANG.reconnecting : '웹소켓 연결 끊김! 재연결 시도...'), error);
        setTimeout(connect, 3000);
    });
}

/**
 * 메모리 누수 방지를 위해 웹소켓 연결을 안전하게 수동 해제합니다.
 */
function disconnect() {
    if (stompClient !== null && stompClient.connected) {
        stompClient.disconnect(function () {
            console.log("[WebSocket] 연결 안전하게 해제됨 (메모리 누수 방지)");
        });
    }
}

window.addEventListener('beforeunload', disconnect);

let baseInputHeight = 0;

/**
 * 사용자가 입력한 메시지를 웹소켓을 통해 서버로 전송합니다.
 * 전송 후 텍스트 영역의 크기를 기본값으로 초기화합니다.
 */
function sendMessage() {
    var msgInput = document.getElementById("msgInput");
    var messageContent = msgInput.value.trim();

    if (messageContent && stompClient) {
        var chatMessage = {
            roomId: roomId,
            senderId: myId,
            content: messageContent,
            messageType: 'TEXT'
        };
        stompClient.send("/pub/chat/message", {}, JSON.stringify(chatMessage));

        msgInput.value = '';

        if (baseInputHeight === 0) baseInputHeight = msgInput.scrollHeight;
        msgInput.style.height = baseInputHeight + 'px';
        msgInput.style.overflowY = 'hidden';
        msgInput.focus();
    }
}

/**
 * 수신된 채팅 메시지 데이터를 DOM에 렌더링합니다.
 * 메시지 타입(TEXT, IMAGE, FILE, READ)에 따라 UI를 다르게 구성하며,
 * 날짜 변경 시 다국어 포맷이 적용된 구분선(Divider)을 삽입합니다.
 *
 * @param {Object} message 렌더링할 메시지 데이터 객체
 */
function showMessage(message) {
    if (message.messageType === 'READ') {
        if (message.senderId != myId) {
            document.querySelectorAll('.unread-count').forEach(el => el.remove());
            console.log("[Chat] 상대방이 메시지를 읽음 처리했습니다.");
        }
        return;
    }

    var today = new Date();
    var dateOptions = { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' };
    var currentDate = today.toLocaleDateString(currentLang, dateOptions);

    if (lastChatDate !== currentDate) {
        var dateDiv = document.createElement('div');
        dateDiv.className = "date-divider";
        dateDiv.innerHTML = `<span class="date-divider-text">${currentDate}</span>`;
        msgArea.appendChild(dateDiv);
        lastChatDate = currentDate;
    }

    var isMe = (message.senderId == myId);
    var div = document.createElement('div');
    var timeString = message.createdAt;

    var openFileText = window.CHAT_LANG ? window.CHAT_LANG.openFile : '파일 열기';

    var finalContentHtml = "";
    if (message.messageType === 'IMAGE') {
        finalContentHtml = `<img src="${message.content}" class="chat-image" 
                    style="max-width: 200px; border-radius: 10px; margin-top: 5px;"
                    onclick="openImageModal(this.src)">`;
    }
    else if (message.messageType === 'FILE') {
        const rawPath = message.content;
        const fileName = rawPath.includes('_') ? rawPath.split('_').pop() : rawPath;
        const ext = fileName.split('.').pop().toLowerCase();

        let iconClass = 'fa-file';
        let iconColor = '#95a5a6';

        if (ext === 'pdf') { iconClass = 'fa-file-pdf'; iconColor = '#ff6b6b'; }
        else if (ext === 'xlsx' || ext === 'xls') { iconClass = 'fa-file-excel'; iconColor = '#2ecc71'; }
        else if (ext === 'docx' || ext === 'doc') { iconClass = 'fa-file-word'; iconColor = '#4a90e2'; }
        else if (ext === 'txt') { iconClass = 'fa-file-lines'; iconColor = '#f1c40f'; }

        finalContentHtml = `
        <div class="file-bubble" data-url="${message.content}" onclick="window.open(this.getAttribute('data-url'))" style="cursor: pointer;">
            <div class="file-icon-box" style="color: ${iconColor};"><i class="fa-solid ${iconClass}"></i></div>
            <div class="file-info-box">
                <div class="file-display-name">${fileName}</div>
                <div class="file-display-sub">${openFileText}</div>
            </div>
        </div>`;
    } else {
        finalContentHtml = `<div class="msg-bubble">${message.content}</div>`;
    }

    if (isMe) {
        div.className = "msg-row me";
        div.innerHTML = `<span class="msg-time">${timeString}</span><span class="unread-count">1</span>${finalContentHtml}`;
    } else {
        div.className = "msg-row other";
        var oppImgUrl = document.getElementById("opponentImg").value;
        div.innerHTML = `<img src="${oppImgUrl}" class="profile-img" style="object-fit: cover;">${finalContentHtml}<span class="msg-time">${timeString}</span>`;
    }

    msgArea.appendChild(div);
    scrollToBottom();

    if (message.senderId != myId && message.messageType !== 'READ') {
        if (typeof sendReadSignal === 'function') {
            sendReadSignal();
        }
    }
}

/**
 * 텍스트 입력 영역(textarea)의 내용 길이에 맞춰 높이를 동적으로 조절합니다.
 *
 * @param {HTMLElement} textarea 조절 대상 텍스트 영역 요소
 */
function autoResize(textarea) {
    if (baseInputHeight === 0) {
        baseInputHeight = textarea.scrollHeight;
    }

    if (textarea.value.trim() === '') {
        textarea.value = '';
        textarea.style.height = baseInputHeight + 'px';
        textarea.style.overflowY = 'hidden';
        return;
    }

    let scrollY = window.scrollY;

    textarea.style.height = 'auto';
    let newHeight = textarea.scrollHeight;
    let maxHeight = 120;

    if (newHeight > maxHeight) {
        textarea.style.height = maxHeight + 'px';
        textarea.style.overflowY = 'auto';
    } else {
        textarea.style.height = newHeight + 'px';
        textarea.style.overflowY = 'hidden';
    }

    window.scrollTo(0, scrollY);
}

/**
 * Shift 키 조합 없이 Enter 키 입력 시 메시지를 전송하도록 제어합니다.
 *
 * @param {KeyboardEvent} e 키보드 이벤트 객체
 */
function handleEnter(e) {
    if (e.isComposing || e.keyCode === 229) return;
    if (e.key === 'Enter') {
        if (!e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    }
}

/**
 * 이미지 파일을 서버에 업로드하고, 성공 시 이미지 URL을 포함한 메시지를 전송합니다.
 */
function uploadImage() {
    var fileInput = document.getElementById('fileInput');
    var file = fileInput.files[0];
    if (file) {
        var formData = new FormData();
        formData.append("file", file);
        fetch('/chat/upload', {
            method: 'POST',
            body: formData
        })
            .then(response => response.text())
            .then(imageUrl => {
                if (imageUrl.includes("실패")) {
                    alert(window.CHAT_LANG ? window.CHAT_LANG.uploadFail : "사진 업로드 실패");
                    return;
                }
                var chatMessage = {
                    roomId: roomId,
                    senderId: myId,
                    content: imageUrl,
                    messageType: 'IMAGE'
                };
                stompClient.send("/pub/chat/message", {}, JSON.stringify(chatMessage));
            });
        fileInput.value = '';
    }
}

/**
 * 문서 파일을 서버에 업로드하고, 성공 시 파일 URL을 포함한 메시지를 전송합니다.
 */
function uploadFile() {
    var fileInput = document.getElementById('docFileInput');
    var file = fileInput.files[0];

    if (file) {
        var formData = new FormData();
        formData.append("file", file);

        fetch('/chat/upload', {
            method: 'POST',
            body: formData
        })
            .then(response => response.text())
            .then(fileUrl => {
                if (fileUrl.includes("실패")) {
                    alert(window.CHAT_LANG ? window.CHAT_LANG.uploadFail : "파일 업로드 실패");
                    return;
                }
                var chatMessage = {
                    roomId: roomId,
                    senderId: myId,
                    content: fileUrl,
                    messageType: 'FILE'
                };
                stompClient.send("/pub/chat/message", {}, JSON.stringify(chatMessage));
            })
            .catch(err => console.error("업로드 에러:", err));

        fileInput.value = '';
    }
}

/**
 * 채팅창의 스크롤을 최하단으로 부드럽게 이동시킵니다.
 * 렌더링 딜레이를 고려하여 두 번 호출됩니다.
 */
function scrollToBottom() {
    setTimeout(function () {
        msgArea.scrollTop = msgArea.scrollHeight;
    }, 150);
    setTimeout(function () {
        msgArea.scrollTop = msgArea.scrollHeight;
    }, 500);
}

const modalImg = document.getElementById("imageModal");
const modalMain = document.getElementById("mainPlusMenu");
const modalTemp = document.getElementById("templateMenu");

function openImageModal(src) {
    document.getElementById("modalImage").src = src;
    modalImg.showModal();
}
function closeImageModal() { modalImg.close(); }

function openMainMenu() { modalMain.showModal(); }
function closeMainMenu() { modalMain.close(); }

function openTemplateMenu() { modalTemp.showModal(); }
function closeTemplateMenu() { modalTemp.close(); }

function openSubMenu(type) {
    closeMainMenu();
    if (type === 'template') openTemplateMenu();
}

/**
 * 선택된 템플릿 텍스트를 입력창에 삽입하고 포커스를 줍니다.
 *
 * @param {string} text 삽입할 템플릿 문자열
 */
function insertText(text) {
    const inputField = document.getElementById('msgInput');
    if (inputField) {
        inputField.value = text;
        inputField.focus();
        if (typeof autoResize === 'function') autoResize(inputField);
    }
    closeTemplateMenu();
}

/**
 * 상대방의 메시지를 열람했음을 알리는 READ 신호를 웹소켓으로 발송합니다.
 */
function sendReadSignal() {
    if (stompClient && stompClient.connected) {
        var readMessage = {
            roomId: roomId,
            senderId: myId,
            messageType: 'READ'
        };
        stompClient.send("/pub/chat/read", {}, JSON.stringify(readMessage));
    }
}

[modalImg, modalMain, modalTemp].forEach(m => {
    m.addEventListener('click', (e) => {
        if (e.target.nodeName === 'DIALOG') m.close();
    });
});

let isTranslating = false;

/**
 * 채팅방 내 모든 메시지를 분석하여 한글과 일본어를 각각 교차 번역합니다.
 * 번역 API를 통해 텍스트를 변환하며, 이미 번역된 항목은 제외합니다.
 */
async function translateAllMessages() {
    if (isTranslating) return;

    console.log("[Translation] 교차 번역 프로세스 시작");

    const bubbles = document.querySelectorAll('.msg-bubble');

    const toJapaneseQueue = [];
    const toKoreanQueue = [];

    bubbles.forEach(bubble => {
        if (!bubble.querySelector('.translated-text')) {

            let originalText = '';
            bubble.childNodes.forEach(node => {
                if (node.nodeType === Node.TEXT_NODE) {
                    originalText += node.nodeValue;
                }
            });

            const txt = originalText.trim();
            if (txt) {
                const hasKorean = /[ㄱ-ㅎ|ㅏ-ㅣ|가-힣]/.test(txt);

                if (hasKorean) {
                    toJapaneseQueue.push({ text: txt, element: bubble });
                } else {
                    toKoreanQueue.push({ text: txt, element: bubble });
                }
            }
        }
    });

    if (toJapaneseQueue.length === 0 && toKoreanQueue.length === 0) {
        console.warn("[Translation] 번역할 메시지가 없습니다.");
        return;
    }

    isTranslating = true;

    try {
        if (toKoreanQueue.length > 0) {
            console.log(`[Translation] 일본어 -> 한국어 번역 요청 중... (${toKoreanQueue.length}건)`);
            const resKO = await fetch('/api/translate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    text: toKoreanQueue.map(item => item.text),
                    target_lang: 'KO'
                })
            });
            if (resKO.ok) {
                const dataKO = await resKO.json();
                if (dataKO && dataKO.translations) {
                    dataKO.translations.forEach((t, i) => {
                        appendTranslation(toKoreanQueue[i].element, t.text);
                    });
                }
            }
        }

        if (toJapaneseQueue.length > 0) {
            console.log(`[Translation] 한국어 -> 일본어 번역 요청 중... (${toJapaneseQueue.length}건)`);
            const resJA = await fetch('/api/translate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    text: toJapaneseQueue.map(item => item.text),
                    target_lang: 'JA'
                })
            });
            if (resJA.ok) {
                const dataJA = await resJA.json();
                if (dataJA && dataJA.translations) {
                    dataJA.translations.forEach((t, i) => {
                        appendTranslation(toJapaneseQueue[i].element, t.text);
                    });
                }
            }
        }

        console.log("[Translation] 모든 교차 번역이 완료되었습니다.");
    } catch (err) {
        console.error("[Translation] 번역 중 에러 발생:", err);
        alert(window.CHAT_LANG && window.CHAT_LANG.translateFail ? window.CHAT_LANG.translateFail : "번역 처리 중 문제가 발생했습니다.");
    } finally {
        isTranslating = false;
    }
}

/**
 * 원본 메시지 버블 내부에 번역된 텍스트 엘리먼트를 추가로 렌더링합니다.
 *
 * @param {HTMLElement} bubble 대상 메시지 버블 DOM 요소
 * @param {string} translatedText 번역된 텍스트 문자열
 */
function appendTranslation(bubble, translatedText) {
    if (bubble.querySelector('.translated-text')) return;

    const hr = document.createElement('hr');
    hr.style.margin = '5px 0';
    hr.style.border = '0.5px solid rgba(0,0,0,0.1)';

    const div = document.createElement('div');
    div.className = 'translated-text';
    div.style.fontSize = '0.85em';
    div.style.color = '#555';
    div.innerText = '🌐 ' + translatedText;

    bubble.appendChild(hr);
    bubble.appendChild(div);
}

document.addEventListener("DOMContentLoaded", function () {
    var dividers = document.querySelectorAll('.date-divider-text');
    if (dividers.length > 0) {
        lastChatDate = dividers[dividers.length - 1].innerText.trim();
    }

    const translateBtn = document.querySelector('.header-translate-btn');
    if (translateBtn) {
        translateBtn.onclick = translateAllMessages;
        console.log("[Init] 번역 버튼 이벤트 연결 완료");
    } else {
        console.error("[Init] 번역 버튼(.header-translate-btn)을 찾을 수 없습니다.");
    }

    document.querySelectorAll('.file-bubble').forEach(bubble => {
        const rawNameDiv = bubble.querySelector('.raw-file-name');
        const displayNameDiv = bubble.querySelector('.file-display-name');
        const iconElement = bubble.querySelector('.file-icon-box i');
        const iconBox = bubble.querySelector('.file-icon-box');

        if (rawNameDiv && displayNameDiv && iconElement) {
            const rawPath = rawNameDiv.innerText;
            const fileName = rawPath.includes('_') ? rawPath.split('_').pop() : rawPath;
            displayNameDiv.innerText = fileName;

            const ext = fileName.split('.').pop().toLowerCase();
            iconElement.className = 'fa-solid';

            if (ext === 'pdf') {
                iconElement.classList.add('fa-file-pdf');
                iconBox.style.color = '#ff6b6b';
            } else if (ext === 'xlsx' || ext === 'xls') {
                iconElement.classList.add('fa-file-excel');
                iconBox.style.color = '#2ecc71';
            } else if (ext === 'docx' || ext === 'doc') {
                iconElement.classList.add('fa-file-word');
                iconBox.style.color = '#4a90e2';
            } else if (ext === 'txt') {
                iconElement.classList.add('fa-file-lines');
                iconBox.style.color = '#f1c40f';
            } else {
                iconElement.classList.add('fa-file');
                iconBox.style.color = '#95a5a6';
            }
        }
    });

    const backBtn = document.querySelector('.header-back-btn');
    if (backBtn) {
        backBtn.addEventListener('click', function () {
            if (typeof disconnect === 'function') {
                disconnect();
            }
        });
    }
});