@echo off
setlocal
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
if not defined ANDROID_HOME set "ANDROID_HOME=C:\Users\anujb\AppData\Local\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "C:\Users\anujb\.gradle\wrapper\dists\gradle-9.1.0-all\7wzd0jkjit61aq2p43wpjgij9\gradle-9.1.0\bin\gradle.bat" %*
endlocal

