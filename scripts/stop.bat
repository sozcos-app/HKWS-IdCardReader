@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

set PORT=8864

echo 正在查找占用端口 %PORT% 的进程...
echo 当前目录: "%~dp0"

REM 使用netstat查找占用指定端口的PID
set PID=
for /f "tokens=5 delims= " %%a in ('netstat -ano ^| findstr ":%PORT%" ^| findstr "LISTENING"') do (
    set PID=%%a
)

if defined PID (
    echo 找到占用端口 %PORT% 的进程，PID: !PID!

    REM 获取进程名称
    for /f "tokens=1 delims=," %%b in ('tasklist /fi "PID eq !PID!" /fo csv ^| findstr /v "信息"') do (
        set PROCESS_NAME=%%~b
    )

    echo 进程名称: !PROCESS_NAME!

    REM 终止进程
    taskkill /f /pid !PID!
    echo 已终止进程 PID: !PID!
) else (
    echo 没有找到占用端口 %PORT% 的进程
)