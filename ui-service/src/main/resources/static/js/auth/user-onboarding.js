// user-onboarding.js
// (File user-guard.js đã được chèn vào <head> để bảo vệ)

document.addEventListener('DOMContentLoaded', function() {

    const form = document.getElementById('onboardingForm');
    const statusMessage = document.getElementById('status-message');

    const API_BASE_URL = typeof window.getApiBaseUrl === 'function'
        ? window.getApiBaseUrl()
        : 'http://localhost:8084';
    const PROFILE_API_URL = `${API_BASE_URL}/api/auth/users/profile`;
    const UPLOAD_API_URL = `${API_BASE_URL}/api/auth/users/profile/upload`;

    // --- (Lệnh gọi checkAuthAndLoadUser() đã được XÓA khỏi đây) ---
    // (File auth-utils.js sẽ tự động chạy)

    /**
     * TẢI DỮ LIỆU: Gọi API GET /profile để điền vào form.
     */
    async function loadUserProfile() {
        try {
            const response = await authenticatedFetch(PROFILE_API_URL, {
                method: 'GET'
            });

            if (response.ok) {
                const user = await response.json();
                if (typeof window.updateStoredProfileStatus === 'function') {
                    window.updateStoredProfileStatus(user.profileStatus || 'PENDING');
                }

                // 1. Điền dữ liệu text
                document.getElementById('fullName').value = user.fullName || '';
                document.getElementById('email').value = user.email || '';
                document.getElementById('phoneNumber').value = user.phoneNumber || '';
                document.getElementById('dateOfBirth').value = user.dateOfBirth || '';
                document.getElementById('idCardNumber').value = user.idCardNumber || '';
                document.getElementById('idCardIssueDate').value = user.idCardIssueDate || '';
                document.getElementById('idCardIssuePlace').value = user.idCardIssuePlace || '';
                document.getElementById('licenseNumber').value = user.licenseNumber || '';
                if(user.licenseClass) document.getElementById('licenseClass').value = user.licenseClass;
                document.getElementById('licenseIssueDate').value = user.licenseIssueDate || '';
                document.getElementById('licenseExpiryDate').value = user.licenseExpiryDate || '';

                // 2. Cập nhật tên User trên Header (Nếu auth-utils chưa kịp chạy)
                const userNameDisplay = document.getElementById('userNameDisplay');
                if(userNameDisplay) userNameDisplay.textContent = user.fullName || user.email;

                // 3. Hiển thị ảnh đã tải lên
                displayUploadedImage('file-cmnd-front', user.idCardFrontUrl);
                displayUploadedImage('file-cmnd-back', user.idCardBackUrl);
                displayUploadedImage('file-license', user.licenseImageUrl);
                displayUploadedImage('file-portrait', user.portraitImageUrl);

            } else {
                // Xử lý các lỗi khác nhau
                // Đọc response body một lần duy nhất
                const responseText = await response.text();
                let errorData = {};
                
                try {
                    errorData = JSON.parse(responseText);
                } catch (e) {
                    // Nếu không parse được JSON, dùng text thuần
                    errorData = { message: responseText || 'Lỗi không xác định' };
                }

                console.error("Lỗi tải hồ sơ:", response.status, errorData);

                if (response.status === 401) {
                    // Lỗi xác thực - yêu cầu đăng nhập lại
                    if (typeof logout === 'function') {
                        statusMessage.classList.add('error');
                        statusMessage.textContent = errorData.message || 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
                        statusMessage.style.display = 'block';
                        setTimeout(() => {
                            logout({ skipRemote: true });
                        }, 2000);
                    } else {
                        window.location.href = '/auth/login';
                    }
                } else if (response.status === 400) {
                    // Lỗi yêu cầu không hợp lệ
                    statusMessage.classList.add('error');
                    statusMessage.textContent = errorData.message || 'Yêu cầu không hợp lệ. Vui lòng thử lại.';
                    statusMessage.style.display = 'block';
                } else {
                    // Lỗi khác
                    statusMessage.classList.add('error');
                    statusMessage.textContent = errorData.message || 'Không thể tải hồ sơ. Vui lòng thử lại.';
                    statusMessage.style.display = 'block';
                }
            }
        } catch (error) {
            console.error("Lỗi tải hồ sơ:", error);
            statusMessage.classList.add('error');
            statusMessage.textContent = 'Lỗi kết nối: ' + (error.message || 'Không thể tải hồ sơ. Vui lòng thử lại.');
            statusMessage.style.display = 'block';
        }
    }

    // Hàm hiển thị ảnh đã tải lên (từ server)
    function displayUploadedImage(elementId, url) {
        const fileInput = document.getElementById(elementId);
        if (!fileInput) return;
        const dropZone = fileInput.closest('.drop-zone');
        if (!dropZone) return;

        // Không xóa input, chỉ ẩn text và thêm/thay đổi thẻ img preview
        const textEl = dropZone.querySelector('p');
        if (textEl) {
            textEl.style.display = 'none';
        }

        // Tạo (hoặc lấy) thẻ img để hiển thị preview
        let img = dropZone.querySelector('img.preview-image');
        if (!img) {
            img = document.createElement('img');
            img.className = 'preview-image';
            img.style.maxWidth = '100%';
            img.style.maxHeight = '100%';
            img.style.objectFit = 'contain';
            img.style.borderRadius = '6px';
            dropZone.appendChild(img);
        }

        // Tạo URL đầy đủ nếu là relative path
        if (url) {
            const fullUrl = url.startsWith('http') ? url : `${API_BASE_URL}${url}`;
            img.src = fullUrl;
            dropZone.classList.add('has-image');
        } else {
            // Không có URL thì ẩn ảnh
            img.remove();
            dropZone.classList.remove('has-image');
            if (textEl) {
                textEl.style.display = '';
            }
        }
    }

    // Hàm preview ảnh khi user chọn file
    function previewImage(fileInputId) {
        const fileInput = document.getElementById(fileInputId);
        if (!fileInput || !fileInput.files || fileInput.files.length === 0) return;

        const file = fileInput.files[0];
        const dropZone = fileInput.closest('.drop-zone');
        if (!dropZone) return;

        // Kiểm tra xem có phải là file ảnh không
        if (!file.type.startsWith('image/')) {
            alert('Vui lòng chọn file ảnh!');
            fileInput.value = '';
            return;
        }

        // Kiểm tra kích thước file (tối đa 5MB)
        if (file.size > 5 * 1024 * 1024) {
            alert('File ảnh quá lớn! Vui lòng chọn file nhỏ hơn 5MB.');
            fileInput.value = '';
            return;
        }

        // Ẩn text hướng dẫn
        const textEl = dropZone.querySelector('p');
        if (textEl) {
            textEl.style.display = 'none';
        }

        // Tạo (hoặc lấy) thẻ img để hiển thị preview
        let img = dropZone.querySelector('img.preview-image');
        if (!img) {
            img = document.createElement('img');
            img.className = 'preview-image';
            img.style.maxWidth = '100%';
            img.style.maxHeight = '100%';
            img.style.objectFit = 'contain';
            img.style.borderRadius = '6px';
            dropZone.appendChild(img);
        }

        // Tạo URL preview từ file local
        const reader = new FileReader();
        reader.onload = function(e) {
            img.src = e.target.result;
            dropZone.classList.add('has-image');
        };
        reader.readAsDataURL(file);
    }

    // Hàm xóa ảnh preview
    window.removeImage = function(fileInputId) {
        const fileInput = document.getElementById(fileInputId);
        if (!fileInput) return;
        
        const dropZone = fileInput.closest('.drop-zone');
        if (!dropZone) return;

        // Reset file input
        fileInput.value = '';
        
        // Xóa ảnh preview nhưng giữ lại input và text
        const img = dropZone.querySelector('img.preview-image');
        if (img) {
            img.remove();
        }

        const textEl = dropZone.querySelector('p');
        if (textEl) {
            textEl.style.display = '';
        } else {
            // Nếu vì lý do nào đó không có <p>, thêm lại
            const p = document.createElement('p');
            p.textContent = 'Kéo thả hoặc click để tải ảnh';
            dropZone.appendChild(p);
        }

        dropZone.classList.remove('has-image');
    };

    // Thêm event listeners cho tất cả các file input để preview ảnh
    const fileInputs = ['file-cmnd-front', 'file-cmnd-back', 'file-license', 'file-portrait'];
    fileInputs.forEach(fileInputId => {
        const fileInput = document.getElementById(fileInputId);
        if (fileInput) {
            fileInput.addEventListener('change', function() {
                previewImage(fileInputId);
            });

            // Hỗ trợ drag and drop
            const dropZone = fileInput.closest('.drop-zone');
            if (dropZone) {
                // Ngăn chặn hành vi mặc định khi kéo thả
                dropZone.addEventListener('dragover', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    dropZone.style.backgroundColor = '#EFF6FF';
                });

                dropZone.addEventListener('dragleave', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    dropZone.style.backgroundColor = '';
                });

                dropZone.addEventListener('drop', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    dropZone.style.backgroundColor = '';

                    const files = e.dataTransfer.files;
                    if (files.length > 0) {
                        fileInput.files = files;
                        previewImage(fileInputId);
                    }
                });
            }
        }
    });

    // Gọi hàm tải dữ liệu
    loadUserProfile();

    /**
     * HÀM UPLOAD THỰC TẾ
     */
    async function uploadImage(fileId) {
        const fileInput = document.getElementById(fileId);
        
        // Kiểm tra xem fileInput có tồn tại không
        if (!fileInput) {
            console.warn(`File input với id "${fileId}" không tồn tại trong DOM`);
            return null;
        }
        
        // Kiểm tra xem có file nào được chọn không
        if (!fileInput.files || fileInput.files.length === 0) {
            return null;
        }

        const file = fileInput.files[0];
        const formData = new FormData();
        formData.append("file", file);

        try {
            const response = await authenticatedFetch(UPLOAD_API_URL, {
                method: 'POST',
                body: formData
            });

            // Đọc response body một lần duy nhất
            const responseText = await response.text();
            
            if (response.ok) {
                try {
                    const result = JSON.parse(responseText);
                    return result.fileUrl;
                } catch (parseError) {
                    console.error(`Lỗi parse JSON từ upload response:`, parseError);
                    throw new Error('Phản hồi từ server không hợp lệ');
                }
            } else {
                // Thử parse error message từ JSON, nếu không được thì dùng text thuần
                let errorMessage = 'Lỗi khi tải file lên server';
                try {
                    const errorData = JSON.parse(responseText);
                    errorMessage = errorData.message || errorData.error || errorMessage;
                } catch (e) {
                    // Nếu không parse được JSON, dùng text thuần
                    errorMessage = responseText || errorMessage;
                }
                throw new Error(errorMessage);
            }
        } catch (error) {
            console.error(`Lỗi tải file ${fileId}:`, error);
            // Không throw error để cho phép các file khác tiếp tục upload
            // Chỉ log và trả về null
            return null;
        }
    }

    /**
     * HÀM XỬ LÝ FORM SUBMIT (CẬP NHẬT)
     */
    form.addEventListener('submit', async function(e) {
        e.preventDefault();
        statusMessage.className = 'status-message';
        statusMessage.style.display = 'none';

        // Validation Số điện thoại
        const phoneNumber = document.getElementById('phoneNumber').value;
        const phoneRegex = /^0[0-9]{9}$/;
        if (!phoneRegex.test(phoneNumber)) {
            statusMessage.classList.add('error');
            statusMessage.textContent = 'Số điện thoại không hợp lệ. Vui lòng nhập 10 chữ số bắt đầu bằng 0.';
            statusMessage.style.display = 'block';
            document.getElementById('phoneNumber').focus();
            return;
        }

        try {
            statusMessage.textContent = 'Đang tải lên ảnh và cập nhật hồ sơ...';
            statusMessage.style.display = 'block';

            // Upload các file ảnh (nếu có)
            // Sử dụng Promise.allSettled để không bị dừng nếu một file upload thất bại
            const uploadResults = await Promise.allSettled([
                uploadImage('file-cmnd-front'),
                uploadImage('file-cmnd-back'),
                uploadImage('file-license'),
                uploadImage('file-portrait')
            ]);
            
            // Lấy kết quả từ các promise đã settled
            const idCardFrontUrl = uploadResults[0].status === 'fulfilled' ? uploadResults[0].value : null;
            const idCardBackUrl = uploadResults[1].status === 'fulfilled' ? uploadResults[1].value : null;
            const licenseImageUrl = uploadResults[2].status === 'fulfilled' ? uploadResults[2].value : null;
            const portraitImageUrl = uploadResults[3].status === 'fulfilled' ? uploadResults[3].value : null;

            const updateData = {
                fullName: document.getElementById('fullName').value,
                phoneNumber: phoneNumber,
                dateOfBirth: document.getElementById('dateOfBirth').value,
                idCardNumber: document.getElementById('idCardNumber').value,
                idCardIssueDate: document.getElementById('idCardIssueDate').value,
                idCardIssuePlace: document.getElementById('idCardIssuePlace').value,
                licenseNumber: document.getElementById('licenseNumber').value,
                licenseClass: document.getElementById('licenseClass').value,
                licenseIssueDate: document.getElementById('licenseIssueDate').value,
                licenseExpiryDate: document.getElementById('licenseExpiryDate').value,
                idCardFrontUrl: idCardFrontUrl,
                idCardBackUrl: idCardBackUrl,
                licenseImageUrl: licenseImageUrl,
                portraitImageUrl: portraitImageUrl,
            };

            const filteredUpdateData = Object.fromEntries(
                Object.entries(updateData).filter(([_, v]) => v != null)
            );

            const response = await authenticatedFetch(PROFILE_API_URL, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(filteredUpdateData)
            });

            // Đọc response body một lần duy nhất
            const responseText = await response.text();
            let result = {};
            
            try {
                result = JSON.parse(responseText);
            } catch (e) {
                // Nếu không parse được JSON, dùng text thuần
                result = { message: responseText || 'Lỗi không xác định' };
            }

            if (response.ok) {
                statusMessage.classList.add('success');
                statusMessage.textContent = 'Đăng ký thông tin thành công! Đang chuyển hướng...';
                statusMessage.style.display = 'block';
                localStorage.setItem('userName', updateData.fullName);
                if (typeof window.updateStoredProfileStatus === 'function') {
                    window.updateStoredProfileStatus('PENDING');
                }

                setTimeout(() => {
                    window.location.href = '/user/auth-profile-status';
                }, 2000);
            } else {
                statusMessage.classList.add('error');
                
                // Xử lý các loại lỗi khác nhau
                let errorMessage = 'Đăng ký thất bại';
                if (response.status === 401) {
                    errorMessage = result.message || 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
                    if (typeof logout === 'function') {
                        setTimeout(() => {
                            logout({ skipRemote: true });
                        }, 3000);
                    }
                } else if (response.status === 400) {
                    errorMessage = result.message || 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại thông tin.';
                } else {
                    errorMessage = result.message || result.error || JSON.stringify(result);
                }
                
                statusMessage.textContent = errorMessage;
                statusMessage.style.display = 'block';
            }
        } catch (error) {
            statusMessage.classList.add('error');
            statusMessage.textContent = 'Lỗi kết nối hoặc xử lý: ' + error.message;
            statusMessage.style.display = 'block';
        }
    });
});