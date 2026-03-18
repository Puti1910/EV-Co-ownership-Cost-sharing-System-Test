// User Profile JavaScript

document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('profile-form');
    const userId = document.querySelector('.user-container')?.dataset?.userId;
    
    if (!form) return;
    
    // Load existing profile data
    loadProfileData();
    
    // Handle form submission
    form.addEventListener('submit', async function(e) {
        e.preventDefault();
        await saveProfile();
    });
});

async function loadProfileData() {
    const userId = document.querySelector('.user-container')?.dataset?.userId;
    if (!userId) return;
    
    try {
        const token = localStorage.getItem('jwtToken') || getCookie('jwtToken');
        const headers = {
            'Content-Type': 'application/json'
        };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        
        // Try to load user profile from API
        const response = await fetch(`/api/users/${userId}/profile`, {
            credentials: 'include',
            headers: headers
        });
        
        if (response.ok) {
            const profile = await response.json();
            populateForm(profile);
        } else if (response.status === 404) {
            // Profile doesn't exist yet, that's okay
            console.log('Profile not found, user can create new one');
        }
    } catch (error) {
        console.error('Error loading profile:', error);
    }
}

function populateForm(profile) {
    if (profile.fullName) {
        document.getElementById('fullName').value = profile.fullName;
    }
    if (profile.phone) {
        document.getElementById('phone').value = profile.phone;
    }
    if (profile.dateOfBirth) {
        // Convert date format if needed
        const date = new Date(profile.dateOfBirth);
        document.getElementById('dateOfBirth').value = date.toISOString().split('T')[0];
    }
    if (profile.idNumber) {
        document.getElementById('idNumber').value = profile.idNumber;
    }
    if (profile.idIssueDate) {
        const date = new Date(profile.idIssueDate);
        document.getElementById('idIssueDate').value = date.toISOString().split('T')[0];
    }
    if (profile.idIssuePlace) {
        document.getElementById('idIssuePlace').value = profile.idIssuePlace;
    }
    if (profile.licenseNumber) {
        document.getElementById('licenseNumber').value = profile.licenseNumber;
    }
    if (profile.licenseClass) {
        document.getElementById('licenseClass').value = profile.licenseClass;
    }
}

async function saveProfile() {
    const userId = document.querySelector('.user-container')?.dataset?.userId;
    if (!userId) {
        showToast('Không tìm thấy thông tin người dùng', 'error');
        return;
    }
    
    const form = document.getElementById('profile-form');
    const formData = new FormData(form);
    
    const profileData = {
        fullName: formData.get('fullName'),
        phone: formData.get('phone'),
        dateOfBirth: formData.get('dateOfBirth'),
        idNumber: formData.get('idNumber'),
        idIssueDate: formData.get('idIssueDate'),
        idIssuePlace: formData.get('idIssuePlace'),
        licenseNumber: formData.get('licenseNumber'),
        licenseClass: formData.get('licenseClass')
    };
    
    try {
        const token = localStorage.getItem('jwtToken') || getCookie('jwtToken');
        const headers = {
            'Content-Type': 'application/json'
        };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        
        const response = await fetch(`/api/users/${userId}/profile`, {
            method: 'PUT',
            credentials: 'include',
            headers: headers,
            body: JSON.stringify(profileData)
        });
        
        if (response.ok) {
            showToast('Lưu thông tin thành công!', 'success');
        } else {
            const error = await response.text();
            showToast('Lỗi khi lưu thông tin: ' + error, 'error');
        }
    } catch (error) {
        console.error('Error saving profile:', error);
        showToast('Lỗi khi lưu thông tin', 'error');
    }
}

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

function showToast(message, type = 'info') {
    // Use existing toast function from user-dashboard.js if available
    if (typeof window.showToast === 'function') {
        window.showToast(message, type);
    } else {
        alert(message);
    }
}

