function initializeFlatpickr() {
    const targetElement = document.querySelector("#reservationDatetime");
    if (targetElement) {
        flatpickr(targetElement, {
            enableTime: true,
            dateFormat: "Y-m-d H:i",
            minDate: "today",
        });
    } else {
        console.warn("予約日時入力フォームが見つかりません。");
    }
}

document.addEventListener("DOMContentLoaded", initializeFlatpickr);
