$baseDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarPath = Join-Path $baseDir "IdCardReader.jar"
$logPath = "$env:USERPROFILE\idcard-reader-error.log"

try {
    Start-Process `
        -FilePath "java" `
        -ArgumentList "-jar `"$jarPath`"" `
        -WindowStyle Hidden `
        -RedirectStandardError $logPath `
        -RedirectStandardOutput "$env:USERPROFILE\idcard-reader-output.log"
}
catch {
    $msg = "身份证读取服务启动失败:`n$($_.Exception.Message)"
    [System.Windows.MessageBox]::Show($msg, "启动错误", "OK", "Error")
}
