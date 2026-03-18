/**
 * calendar.js
 * 구인자 일정 관리 페이지(FullCalendar)의 프론트엔드 비즈니스 로직을 담당합니다.
 * HTML 상단에서 선언된 다국어 메시지 객체(calMsg) 및 현재 언어(CURRENT_LANG) 변수를 사용합니다.
 */

let mainCal;
let editingEventId = null;

/**
 * UTC 오차 문제를 해결하기 위해 로컬 시간을 기준으로 YYYY-MM-DD 형태의 문자열을 반환합니다.
 * @param {Date} date 변환할 날짜 객체
 * @returns {string} 포맷팅된 날짜 문자열
 */
function getLocalDateString(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

/**
 * 드래그 앤 드롭 또는 리사이즈를 통해 변경된 일정 정보를 서버에 백그라운드로 자동 저장(업데이트)합니다.
 * @param {Object} info FullCalendar 이벤트 객체 정보
 */
function silentUpdate(info) {
    function formatDateTime(d) {
        if (!d) return null;
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const h = String(d.getHours()).padStart(2, '0');
        const min = String(d.getMinutes()).padStart(2, '0');
        return `${y}-${m}-${day}T${h}:${min}:00`;
    }

    const data = {
        scheduleId: parseInt(info.event.id),
        title: info.event.title,
        description: info.event.extendedProps.description || "",
        start: formatDateTime(info.event.start),
        end: info.event.end ? formatDateTime(info.event.end) : formatDateTime(info.event.start),
        color: info.event.backgroundColor || info.event.color || "#7db4e6"
    };

    $.ajax({
        url: '/api/calendar/update',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function () {
            updateAll(getLocalDateString(info.event.start), mainCal);
        },
        error: function (err) {
            console.error(err);
            Swal.fire(calMsg.errorTitle, calMsg.errorMove, 'error');
            info.revert();
        }
    });
}

/**
 * DOMContentLoaded 이후 FullCalendar 초기화 및 시간 선택(select) 요소의 동적 옵션을 생성합니다.
 */
window.addEventListener('load', function () {
    function fillSelect(id, max, defaultVal) {
        const el = document.getElementById(id);
        if (!el) return;
        for (let i = 0; i <= max; i++) {
            const opt = document.createElement('option');
            const v = String(i).padStart(2, '0');
            opt.value = v;
            opt.text = v;
            if (i === defaultVal) opt.selected = true;
            el.appendChild(opt);
        }
    }

    fillSelect('startHour', 23, 9);
    fillSelect('startMin', 59, 0);
    fillSelect('endHour', 23, 10);
    fillSelect('endMin', 59, 0);

    const mainEl = document.getElementById('calendar');
    const dropZone = document.getElementById('deleteDropZone');

    if (mainEl && typeof FullCalendar !== "undefined") {
        mainCal = new FullCalendar.Calendar(mainEl, {
            initialView: 'dayGridMonth',
            fixedWeekCount: false,
            locale: typeof CURRENT_LANG !== 'undefined' ? CURRENT_LANG : 'ko',
            headerToolbar: { left: 'prev,next today', center: 'title', right: 'dayGridMonth,timeGridWeek,timeGridDay' },
            slotMinTime: '06:00:00',
            slotMaxTime: '23:00:00',
            allDaySlot: false,
            expandRows: true,
            events: '/api/calendar/events',
            dayMaxEvents: 2,
            moreLinkContent: '...',
            editable: true,

            eventsSet: function () {
                const selectedDate = document.getElementById('scheduleDate')?.value;
                if (selectedDate) updateAll(selectedDate, mainCal);
            },

            eventDrop: function (info) { silentUpdate(info); },
            eventResize: function (info) { silentUpdate(info); },

            eventDragStart: function () {
                if (dropZone) dropZone.classList.add('drag-over');
            },

            eventDragStop: function (info) {
                if (!dropZone) return;
                const rect = dropZone.getBoundingClientRect();
                const x = info.jsEvent.clientX;
                const y = info.jsEvent.clientY;

                if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
                    Swal.fire({
                        title: calMsg.dragDeleteTitle,
                        text: `'${info.event.title}' ${calMsg.dragDeleteText}`,
                        icon: 'warning',
                        showCancelButton: true,
                        confirmButtonColor: '#dc3545',
                        cancelButtonColor: '#6c757d',
                        confirmButtonText: calMsg.dragDeleteConfirm,
                        cancelButtonText: calMsg.cancel
                    }).then((result) => {
                        if (result.isConfirmed) deleteScheduleById(info.event.id);
                    });
                }
                dropZone.classList.remove('drag-over');
            },

            dateClick: function (info) { handleDateSelection(info.dateStr, info.dayEl); },
            eventClick: function (info) {
                openEditModal(info.event);
                info.jsEvent.preventDefault();
            },
            moreLinkClick: function (info) {
                const dateStr = getLocalDateString(info.date);
                handleDateSelection(dateStr, info.el.closest('.fc-daygrid-day'));
                return false;
            }
        });
        mainCal.render();
    }

    $('#startHour, #startMin, #endHour, #endMin').on('change', function () {
        const startH = String($('#startHour').val()).padStart(2, '0');
        const startM = String($('#startMin').val()).padStart(2, '0');
        const endH = String($('#endHour').val()).padStart(2, '0');
        const endM = String($('#endMin').val()).padStart(2, '0');
        const startVal = `${startH}:${startM}`;
        const endVal = `${endH}:${endM}`;
        if (startVal >= endVal) {
            $('.custom-time-input').addClass('is-invalid-time');
        } else {
            $('.custom-time-input').removeClass('is-invalid-time');
        }
    });
});

/**
 * 캘린더에서 특정 날짜의 셀을 클릭했을 때의 동작을 처리합니다.
 * 선택 효과를 부여하고 사이드바의 일정을 갱신합니다.
 * @param {string} dateStr 선택된 날짜 문자열 (YYYY-MM-DD)
 * @param {HTMLElement} dayEl 클릭된 날짜의 DOM 요소
 */
function handleDateSelection(dateStr, dayEl) {
    document.querySelectorAll('.fc-daygrid-day').forEach(el => el.classList.remove('selected-day'));
    if (dayEl) dayEl.classList.add('selected-day');
    const dateInput = document.getElementById('scheduleDate');
    if (dateInput) dateInput.value = dateStr;
    updateAll(dateStr, mainCal);
}

/**
 * 신규 스케줄 등록 모달창을 띄우기 위해 폼 및 버튼 상태를 초기화합니다.
 */
function prepareNewSchedule() {
    editingEventId = null;
    const selectedDate = $('#scheduleDate').val();
    $('#scheduleForm')[0].reset();
    $('#scheduleDate').val(selectedDate ? selectedDate : getLocalDateString(new Date()));
    $('#btnDelete').hide();
    $('.btn-modal-submit').text(calMsg.titleAdd);
    $('.modal-title').text(calMsg.titleAdd);
    $('#startHour').val('09');
    $('#startMin').val('00');
    $('#endHour').val('10');
    $('#endMin').val('00');
}

/**
 * 기존 스케줄을 수정하기 위해 클릭한 이벤트의 데이터를 모달 폼에 바인딩합니다.
 * @param {Object} event FullCalendar 이벤트 객체
 */
function openEditModal(event) {
    editingEventId = event.id ? parseInt(event.id) : null;
    $('#scheduleTitle').val(event.title);
    $('#scheduleDescription').val(event.extendedProps.description || "");

    const start = event.start;
    const dateStr = getLocalDateString(start);
    const timeStr = String(start.getHours()).padStart(2, '0') + ":" + String(start.getMinutes()).padStart(2, '0');

    $('#scheduleDate').val(dateStr);
    const [sh, sm] = timeStr.split(':');
    $('#startHour').val(sh);
    $('#startMin').val(sm);
    $('#btnDelete').show();
    $('.btn-modal-submit').text(calMsg.btnSubmitEdit);
    $('.modal-title').text(calMsg.titleEdit);

    if (event.end) {
        const endH = String(event.end.getHours()).padStart(2, '0');
        const endM = String(event.end.getMinutes()).padStart(2, '0');
        $('#endHour').val(endH);
        $('#endMin').val(endM);
    }

    const eventColor = event.backgroundColor || event.color || "#7db4e6";
    $('.color-dot').removeClass('active');
    $(`.color-dot[data-color="${eventColor}"]`).addClass('active');

    $('#scheduleModal').modal('show');
}

/**
 * 시간(시/분) 입력란에 사용자가 직접 숫자를 입력할 경우, 범위(0~23, 0~59)에 맞춰 자동 보정합니다.
 */
$(document).on('input', '#startHour, #endHour', function () {
    let v = parseInt($(this).val()) || 0;
    if (v > 23) v = 23;
    $(this).val(String(v).padStart(2, '0'));
});
$(document).on('input', '#startMin, #endMin', function () {
    let v = parseInt($(this).val()) || 0;
    if (v > 59) v = 59;
    $(this).val(String(v).padStart(2, '0'));
});

/**
 * 모달 창에서 폼 데이터를 수집하여 스케줄 신규 등록 또는 수정 API를 호출합니다.
 */
function saveSchedule() {
    const title = $('#scheduleTitle').val();
    const date = $('#scheduleDate').val();
    const startH = String($('#startHour').val()).padStart(2, '0');
    const startM = String($('#startMin').val()).padStart(2, '0');
    const endH = String($('#endHour').val()).padStart(2, '0');
    const endM = String($('#endMin').val()).padStart(2, '0');
    const startVal = `${startH}:${startM}`;
    const endVal = `${endH}:${endM}`;

    if (!title || !date) {
        Swal.fire(calMsg.errorTitle, calMsg.errorRequired, 'warning');
        return;
    }

    if (startVal >= endVal) {
        Swal.fire({ icon: 'warning', title: 'Oops!', text: calMsg.errorTimeRange, confirmButtonColor: '#7db4e6' });
        return;
    }

    const data = {
        scheduleId: editingEventId,
        title: title,
        description: $('#scheduleDescription').val(),
        start: date + "T" + (startVal || "09:00") + ":00",
        end: date + "T" + (endVal || "10:00") + ":00",
        color: $('.color-dot.active').data('color') || "#7db4e6"
    };

    $.ajax({
        url: editingEventId ? '/api/calendar/update' : '/api/calendar/save',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function () {
            $('#scheduleModal').modal('hide');
            mainCal.refetchEvents();
            updateAll(date, mainCal);
            Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: calMsg.saveSuccess, showConfirmButton: false, timer: 1500 });
        },
        error: function () {
            Swal.fire(calMsg.errorTitle, calMsg.errorSave, 'error');
        }
    });
}

/**
 * 수정 모달 창 내의 '삭제' 버튼 클릭 시, 사용자에게 최종 확인을 받습니다.
 */
function deleteSchedule() {
    if (!editingEventId) return;
    Swal.fire({
        title: calMsg.deleteTitle,
        text: calMsg.deleteText,
        icon: 'error',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: calMsg.deleteConfirm,
        cancelButtonText: calMsg.cancel
    }).then((result) => {
        if (result.isConfirmed) deleteScheduleById(editingEventId);
    });
}

/**
 * 실제 서버로 스케줄 삭제 요청을 보내고, 화면(캘린더 및 사이드바)을 갱신합니다.
 * @param {number|string} id 삭제할 일정의 고유 식별자
 */
function deleteScheduleById(id) {
    if (!id) return;
    $.ajax({
        url: '/api/calendar/delete/' + id,
        type: 'DELETE',
        success: function () {
            $('#scheduleModal').modal('hide');
            mainCal.refetchEvents();
            updateAll($('#scheduleDate').val() || getLocalDateString(new Date()), mainCal);
            Swal.fire({ title: calMsg.deleteSuccess, text: calMsg.deleteSuccessText, icon: 'success', confirmButtonColor: '#7db4e6' });
        },
        error: function () {
            Swal.fire(calMsg.errorTitle, calMsg.errorDelete, 'error');
        }
    });
}

/**
 * 특정 날짜에 해당하는 일정 목록을 필터링하여 우측 사이드바 패널에 렌더링합니다.
 * (전역 함수로 선언되어 다른 모듈에서도 호출 가능합니다.)
 * @param {string} date 대상 날짜 문자열 (YYYY-MM-DD)
 * @param {Object} calApi FullCalendar 인스턴스 객체
 */
window.updateAll = function (date, calApi) {
    const container = document.getElementById('event-list-container');
    const titleEl = document.getElementById('selected-date-title');
    if (titleEl) titleEl.innerText = date + calMsg.scheduleTitle;

    let evs = calApi.getEvents().filter(e => getLocalDateString(e.start) === date);
    evs.sort((a, b) => a.start - b.start);
    container.innerHTML = evs.length ? '' : `<div class="empty-state">${calMsg.noSchedule}</div>`;

    evs.forEach(e => {
        const card = document.createElement("div");
        card.className = "sidebar-card";
        card.style.setProperty("border-left-color", e.backgroundColor || e.color || "#7db4e6", "important");
        card.style.cursor = "pointer";
        card.onclick = () => openEditModal(e);

        const hours = String(e.start.getHours()).padStart(2, '0');
        const minutes = String(e.start.getMinutes()).padStart(2, '0');
        card.innerHTML = `
            <div style="display: flex; align-items: center; gap: 12px; width: 100%;">
              <div class="event-item-title" style="margin: 0; font-size: 0.9rem; font-weight: 700;">${e.title}</div>
              <div class="small text-muted"><i class="bi bi-clock me-1"></i>${hours}:${minutes}</div>
            </div>`;
        container.appendChild(card);
    });
};

/**
 * 모달 창 내의 색상 선택기(Color Dot) 클릭 이벤트를 처리합니다.
 */
$(document).on('click', '.color-dot', function () {
    $('.color-dot').removeClass('active');
    $(this).addClass('active');
});