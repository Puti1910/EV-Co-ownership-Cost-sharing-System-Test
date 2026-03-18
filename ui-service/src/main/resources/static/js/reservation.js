document.addEventListener('DOMContentLoaded', function () {
    const pickupDate = document.querySelector('input[type="date"]:nth-of-type(1)');
    const returnDate = document.querySelector('input[type="date"]:nth-of-type(2)');
    const daysEl = document.querySelector('.days');
    const totalPriceEl = document.querySelector('.total-value .price');
    const bookButton = document.querySelector('#bookButton'); // Nút để gửi yêu cầu đặt xe

    function updateSummary() {
        if (pickupDate.value && returnDate.value) {
            const start = new Date(pickupDate.value);
            const end = new Date(returnDate.value);
            const days = Math.ceil((end - start) / (1000 * 60 * 60 * 24));

            daysEl.textContent = days;

            // Giả sử giá trung bình 800,000 VNĐ/ngày
            const total = days * 800000;
            totalPriceEl.textContent = total.toLocaleString();
            totalPriceEl.parentElement.innerHTML = `<span class="price">${total.toLocaleString()}</span> VNĐ`;
        }
    }

    pickupDate.addEventListener('change', updateSummary);
    returnDate.addEventListener('change', updateSummary);

    // Select car functionality
    document.querySelectorAll('.status-available').forEach(btn => {
        btn.addEventListener('click', function () {
            document.querySelectorAll('.car-card').forEach(card => card.classList.remove('selected'));
            this.closest('.car-card').classList.add('selected');

            const price = this.parentElement.querySelector('.price').textContent.replace(/,/g, '');
            const days = parseInt(daysEl.textContent) || 1;
            const total = parseInt(price) * days;
            totalPriceEl.textContent = total.toLocaleString();
            totalPriceEl.parentElement.innerHTML = `<span class="price">${total.toLocaleString()}</span> VNĐ`;
        });
    });

    // Gửi yêu cầu đặt xe
    bookButton.addEventListener('click', async function () {
        const selectedCar = document.querySelector('.car-card.selected');
        if (!selectedCar) {
            alert('Vui lòng chọn một xe!');
            return;
        }
        if (!pickupDate.value || !returnDate.value) {
            alert('Vui lòng chọn ngày nhận và ngày trả xe!');
            return;
        }

        const vehicleId = selectedCar.dataset.vehicleId; // Giả sử bạn lưu vehicle_id trong data attribute
        const totalPrice = parseInt(totalPriceEl.textContent.replace(/,/g, ''));
        const payload = {
            start_date: pickupDate.value + 'T00:00:00', // Định dạng ISO 8601
            end_date: returnDate.value + 'T00:00:00',
            note: document.querySelector('#note')?.value || '', // Lấy ghi chú từ input
            status: 'Đã đặt', // Giá trị hợp lệ
            total_price: totalPrice,
            vehicle_id: parseInt(vehicleId)
        };

        try {
            const response = await fetch('http://localhost:8084/api/reservations', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json; charset=UTF-8'
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Lỗi khi đặt xe');
            }

            alert('Đặt xe thành công!');
        } catch (error) {
            console.error('Lỗi:', error);
            alert('Đã xảy ra lỗi khi đặt xe: ' + error.message);
        }
    });
});