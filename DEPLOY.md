# 项目部署说明（Windows）

本项目为 Spring Boot 应用，默认端口 `8081`。

## 1. 环境要求

- JDK 11+
- Maven 3.8+
- SQL Server 可访问

## 2. 推荐的生产配置

使用 `prod` 配置文件：`src/main/resources/application-prod.yml`。

请在部署机器上设置环境变量（PowerShell 示例）：

```powershell
$env:DB_URL="jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=ershoupingtai;encrypt=false;trustServerCertificate=true"
$env:DB_USERNAME="sa"
$env:DB_PASSWORD="your-db-password"
$env:JWT_SECRET="your-strong-jwt-secret"
$env:ADMIN_USERNAME="admin"
$env:ADMIN_PASSWORD="your-admin-password"
$env:SERVER_PORT="8081"
```

## 3. 构建

```powershell
./scripts/deploy/build.ps1
```

构建产物：`target/campus-trade-hub-1.0.0.jar`

## 4. 启动

```powershell
./scripts/deploy/start.ps1
```

脚本会：

- 使用 `prod` 配置启动应用
- 记录 PID 到 `run/campus-trade-hub.pid`
- 输出日志到 `logs/campus-trade-hub.out.log`

## 5. 状态检查

```powershell
./scripts/deploy/status.ps1
```

## 6. 查看日志

```powershell
./scripts/deploy/tail-log.ps1
```

## 7. 停止

```powershell
./scripts/deploy/stop.ps1
```

## 8. 快速验证

- 打开：`http://localhost:8081/user/login`
- 后台入口：`http://localhost:8081/admin/login`
