document.addEventListener('DOMContentLoaded', function () {
    const toastElement = document.getElementById('toast-message');
    if (toastElement) {
        const toast = new bootstrap.Toast(toastElement);
        toast.show();
    }
});
