const form = document.getElementById("loginForm");
const accountInput = document.getElementById("account");
const passwordInput = document.getElementById("password");
const errorText = document.getElementById("errorText");

async function login(e) {
    e.preventDefault();
    errorText.textContent = "";

    const account = accountInput.value.trim();
    const password = passwordInput.value;

    if (!account || !password) {
        errorText.textContent = "请输入账号和密码";
        return;
    }

    try {
        const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ account, password })
        });

        const data = await response.json();
        if (data.code !== 200) {
            errorText.textContent = data.msg || "登录失败";
            return;
        }

        window.location.href = "/";
    } catch (err) {
        errorText.textContent = "登录请求失败，请稍后重试";
    }
}

form.addEventListener("submit", login);
