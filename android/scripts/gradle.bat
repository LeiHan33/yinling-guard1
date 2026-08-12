@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
set "ANDROID_HOME=D:\test\Cursor\android-sdk"
set "GRADLE_USER_HOME=D:\test\Cursor\.gradle"
set "GRADLE_OPTS=-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d D:\test\Cursor\android
call gradlew.bat %*
exit /b %ERRORLEVEL%
