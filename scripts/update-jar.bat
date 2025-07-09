@echo off
setlocal enabledelayedexpansion

:: 初始化路径
set "SCRIPTS_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPTS_DIR%..\"
cd /d "%PROJECT_ROOT%"

echo ========== JAR 更新工具 ==========
echo [1/4] 正在验证Maven环境...
call mvn --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到Maven，请先安装并配置环境变量
    pause
    exit /b 1
)

echo [2/4] 正在执行Maven打包 (clean package)...
call mvn clean package
if %errorlevel% neq 0 (
    echo [错误] Maven打包失败！
    pause
    exit /b 1
)

echo [3/4] 定位最新JAR文件...
set "TARGET_JAR="
for /f "delims=" %%f in ('dir /b /o-d "target\*.jar" ^| findstr /v "sources.jar$"') do (
    if not defined TARGET_JAR (
        set "TARGET_JAR=target\%%f"
        set "JAR_NAME=%%f"
    )
)

if not defined TARGET_JAR (
    echo [错误] 在target目录未找到JAR文件
    pause
    exit /b 1
)

echo [4/4] 复制到scripts目录...
if not exist "%SCRIPTS_DIR%" mkdir "%SCRIPTS_DIR%"
copy /y "%TARGET_JAR%" "%SCRIPTS_DIR%%JAR_NAME%"

echo ========== 操作结果 ==========
echo 源文件: %TARGET_JAR%
echo 目标位置: %SCRIPTS_DIR%%JAR_NAME%
echo 状态: 更新成功!
echo =============================
timeout /t 3 >nul