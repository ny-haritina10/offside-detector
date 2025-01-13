@echo off

:: Load the configuration file
for /F "delims=" %%a in (_config.conf) do set "%%a"

:: Build the derived paths after loading root from config
set "bin=%root%\bin"
set "lib=%root%\lib"
set "web=%root%\web"
set "temp=%root%\temp"
set "src=%root%\src\mg\itu"

:: Create temp and bin directories if they don't exist
if not exist "%temp%" mkdir "%temp%"
if not exist "%bin%" mkdir "%bin%"

:: Copy all java files to temp directory
for /r "%src%" %%f in (*.java) do (
    xcopy "%%f" "%temp%"
)

:: Move to temp to compile all java files
cd "%temp%"
javac -d "%bin%" -cp "%lib%\*" *.java

:: Move back to root
cd "%root%"

:: Copy lib and classes to web-inf
xcopy /s /e /i "%lib%\*" "%web%\WEB-INF\lib\"
xcopy /s /e /i "%bin%\*" "%web%\WEB-INF\classes\"

:: Archive web folder into WAR file
jar -cvf "%war_name%.war" -C "%web%" .

:: Deploy WAR to server 
copy "%war_name%.war" "%target_dir%"

:: Remove WAR and temp
del "%war_name%.war"
rmdir /s /q "%temp%"

echo Deployment complete.
pause