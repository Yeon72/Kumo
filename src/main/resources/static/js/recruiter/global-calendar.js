document.addEventListener("DOMContentLoaded", function () {
    const currentLang = document.documentElement.lang || "ko";

    /**
     * 메인 캘린더(FullCalendar) 인스턴스를 초기화하고 화면에 렌더링합니다.
     * 월별, 주별, 일별 보기 및 일정 클릭 이벤트를 지원합니다.
     */
    const calendarEl = document.getElementById("calendar");
    if (calendarEl && typeof FullCalendar !== "undefined") {
        try {
            const calendar = new FullCalendar.Calendar(calendarEl, {
                height: 'auto',
                expandRows: true,
                initialView: "dayGridMonth",
                headerToolbar: {
                    left: "prev,next today",
                    center: "title",
                    right: "dayGridMonth,timeGridWeek,timeGridDay,listWeek",
                },
                locale: currentLang,
                events: "/api/calendar/events",

                allDaySlot: true,
                slotMinTime: "06:00:00",
                slotMaxTime: "20:00:00",
                expandRows: true,
                slotEventOverlap: false,
                handleWindowResize: true,

                dateClick: function (info) {
                    document.querySelectorAll(".selected-day").forEach((el) => el.classList.remove("selected-day"));
                    info.dayEl.classList.add("selected-day");
                    updateScheduleDetail(info.dateStr, calendar);
                },
                eventClick: function (info) {
                    alert("일정: " + info.event.title + "\n내용: " + (info.event.extendedProps.description || "내용 없음"));
                },
            });
            calendar.render();
        } catch (e) { console.error("메인 캘린더 에러:", e); }
    }

    /**
     * 사이드바 등에 표시되는 미니 캘린더(FullCalendar) 인스턴스를 초기화합니다.
     * 일별 최대 3개의 일정을 점(dot) 형태로 간략히 표시하며, 날짜 클릭 시 상세 일정을 갱신합니다.
     */
    const miniEl = document.getElementById("mini-calendar");
    if (miniEl && typeof FullCalendar !== "undefined") {
        const miniCalendar = new FullCalendar.Calendar(miniEl, {
            initialView: "dayGridMonth",
            locale: currentLang,
            headerToolbar: {
                left: 'prev',
                center: 'title',
                right: 'next'
            },
            height: "auto",
            events: "/api/calendar/events",

            eventDisplay: "list-item",
            dayMaxEvents: 3,
            dayMaxEventRows: 3,

            eventDataTransform: function (eventData) {
                return eventData;
            },

            eventsSet: function () {
                const now = new Date();
                const todayStr = getLocalDateStr(now);
                const dateToSelect = localStorage.getItem("selectedDate") || todayStr;

                setTimeout(() => {
                    document.querySelectorAll(".fc-daygrid-day").forEach(el => {
                        const dateAttr = el.getAttribute("data-date");
                        if (dateAttr === dateToSelect) {
                            el.classList.add("selected-day");
                        }
                    });
                }, 0);

                updateScheduleDetail(dateToSelect, miniCalendar);
            },

            dayCellContent: (arg) => ({ html: arg.date.getDate() }),

            moreLinkContent: () => ({ html: "" }),
            moreLinkDidMount: (info) => {
                info.el.style.display = "none";
            },

            eventDidMount: function (info) {
                info.el.style.background = "none";
                info.el.style.border = "none";
                info.el.style.boxShadow = "none";
                info.el.style.padding = "0";
                info.el.style.margin = "0";

                const dayEl = info.el.closest(".fc-daygrid-day");
                if (dayEl) {
                    const allDots = dayEl.querySelectorAll(".fc-daygrid-event-harness");
                    const index = Array.from(allDots).indexOf(info.el.closest(".fc-daygrid-event-harness"));
                    if (index >= 3) {
                        info.el.closest(".fc-daygrid-event-harness").style.display = "none";
                        return;
                    }
                }
                if (dayEl && dayEl.classList.contains("fc-day-other")) {
                    info.el.closest(".fc-daygrid-event-harness").style.display = "none";
                    return;
                }

                const dot = info.el.querySelector(".fc-daygrid-event-dot");
                if (dot) {
                    const color = info.event.backgroundColor || info.event.color || "#ff6b6b";
                    dot.style.setProperty("background-color", color, "important");
                    dot.style.setProperty("border-color", color, "important");
                    dot.style.setProperty("width", "6px", "important");
                    dot.style.setProperty("height", "6px", "important");
                    dot.style.setProperty("border-radius", "50%", "important");
                    dot.style.setProperty("border", "none", "important");
                    dot.style.setProperty("display", "block", "important");
                    dot.style.setProperty("visibility", "visible", "important");
                    dot.style.setProperty("flex-shrink", "0", "important");
                }
            },

            dateClick: function (info) {
                localStorage.setItem("selectedDate", info.dateStr);
                document
                    .querySelectorAll(".selected-day")
                    .forEach((el) => el.classList.remove("selected-day"));
                info.dayEl.classList.add("selected-day");
                updateScheduleDetail(info.dateStr, miniCalendar);
            },
        });
        miniCalendar.render();
    }

    /**
     * Date 객체를 YYYY-MM-DD 형식의 문자열로 변환하여 반환합니다.
     *
     * @param {Date} date 변환할 기준 날짜 객체
     * @returns {string} 로컬 기준의 포맷팅된 날짜 문자열
     */
    function getLocalDateStr(date) {
        const y = date.getFullYear();
        const m = String(date.getMonth() + 1).padStart(2, "0");
        const d = String(date.getDate()).padStart(2, "0");
        return `${y}-${m}-${d}`;
    }

    /**
     * 특정 날짜를 클릭하거나 이벤트가 갱신될 때,
     * 대상 캘린더에서 해당 일자의 이벤트를 필터링하여 상세 목록 UI를 업데이트합니다.
     *
     * @param {string} dateStr 선택된 날짜 문자열 (YYYY-MM-DD)
     * @param {Object} calendarApi 데이터를 조회할 대상 FullCalendar 인스턴스
     */
    function updateScheduleDetail(dateStr, calendarApi) {
        const container = document.getElementById("event-list-container");
        if (!container) return;

        const titleEl = document.getElementById("selected-date-title");
        const titleSuffix =
            typeof kumoMsgs !== "undefined" ? kumoMsgs.scheduleTitle : " 일정";
        if (titleEl) titleEl.innerText = dateStr + " " + titleSuffix;

        function getLocalDateStr(date) {
            const y = date.getFullYear();
            const m = String(date.getMonth() + 1).padStart(2, "0");
            const d = String(date.getDate()).padStart(2, "0");
            return `${y}-${m}-${d}`;
        }

        const events = calendarApi.getEvents().filter((e) => {
            return getLocalDateStr(e.start) === dateStr;
        });

        container.innerHTML = "";
        if (events.length === 0) {
            const emptyMsg =
                typeof kumoMsgs !== "undefined"
                    ? kumoMsgs.noSchedule
                    : "일정이 없습니다.";
            container.innerHTML = `<div class="empty-state text-center p-3">${emptyMsg}</div>`;
        } else {
            events.forEach((e) => {
                const card = document.createElement("div");
                card.className = "sidebar-card";

                const eventColor = e.backgroundColor || e.color || "#7db4e6";
                card.style.setProperty("border-left-color", eventColor, "important");

                const timeStr = e.start.toLocaleTimeString([], {
                    hour: "2-digit",
                    minute: "2-digit",
                    hour12: false,
                });

                card.innerHTML = `
          <div style="display: flex; align-items: center; gap: 12px; width: 100%; padding: 2px 0;">
            <div class="event-item-title" style="margin: 0; font-size: 0.9rem; font-weight: 700;">${e.title}</div>
            <div class="event-item-time" style="font-size: 0.8rem; color: #8b95a1; white-space: nowrap;">
              <i class="bi bi-clock me-1"></i>${timeStr}
            </div>
          </div>`;
                container.appendChild(card);
            });
        }
    }
});