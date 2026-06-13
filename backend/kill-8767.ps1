Get-NetTCPConnection -LocalPort 8767 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
Get-Process java -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.Id -Force }
Start-Sleep 3
Get-NetTCPConnection -LocalPort 8767 -State Listen -ErrorAction SilentlyContinue | Format-Table -AutoSize
Get-Process java -ErrorAction SilentlyContinue | Format-Table Id,ProcessName,StartTime -AutoSize
