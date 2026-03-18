document.getElementById("loginForm").addEventListener("submit", async (e) => {
    e.preventDefault();

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value.trim();
    const errorEl = document.getElementById("error");
    errorEl.textContent = "";

    try {
        const response = await fetch("http://localhost:8084/api/admin/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password })
        });

        // Nếu đăng nhập sai (ví dụ 401)
        if (!response.ok) {
            let msg = "Đăng nhập thất bại";
            try {
                // ✅ lấy message nếu server trả JSON
                const data = await response.json();
                if (data.message) msg = data.message;
            } catch {
                // Nếu response không phải JSON thì bỏ qua
            }
            throw new Error(msg);
        }

        // ✅ Nếu đăng nhập thành công
        const data = await response.json();
        localStorage.setItem("token", data.token);
        localStorage.setItem("username", data.username);
        window.location.href = "/admin-dashboard.html";
    } catch (error) {
        errorEl.textContent = error.message;
    }
});
