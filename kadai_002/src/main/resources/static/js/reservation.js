document.addEventListener('DOMContentLoaded', function () {
    var toastElement = document.getElementById('reservationToast');
    if (toastElement) {
        var toast = new bootstrap.Toast(toastElement);
        toast.show();
    }
});
