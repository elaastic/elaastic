@echo off
setlocal enabledelayedexpansion

:: Check that you are in the scripts folder of ui-components
for %%I in ("%CD%") do set CURRENT_DIR=%%~nxI
for %%I in ("%CD%\..") do set PARENT_DIR=%%~nxI

if not "%CURRENT_DIR%"=="scripts" (
    echo Error: This script must be executed from the 'scripts' folder of ui-components
    echo Current folder: %CD%
    exit /b 1
)

if not "%PARENT_DIR%"=="ui-components" (
    echo Error: Parent folder must be 'ui-components'
    echo Parent folder: %PARENT_DIR%
    exit /b 1
)

if not exist "..\package.json" (
    echo Error: package.json not found in parent folder
    exit /b 1
)

:: Go to parent folder (ui-components)
cd ..

:: 1. Get version from package.json
for /f "tokens=*" %%a in ('powershell -Command "(Get-Content package.json | ConvertFrom-Json).version"') do (
    set VERSION=%%a
)
echo Version detected: %VERSION%

:: 2. Build the bundle
call npm run build
if errorlevel 1 (
    echo Error during build
    exit /b 1
)

:: 3. Check the existence of generated files
if not exist "dist\elaastic-vue-components-v%VERSION%.umd.min.js" (
    echo JS file not found: elaastic-vue-components-v%VERSION%.umd.min.js
    exit /b 1
)
if not exist "dist\style-v%VERSION%.css" (
    echo CSS file not found: style-v%VERSION%.css
    exit /b 1
)

:: 4. Delete old bundle
cd ..
if exist "server\src\main\resources\static\vue-components" (
    echo Removing content from old bundle...
    del /q "server\src\main\resources\static\vue-components\*.*"
) else (
    :: Create the folder if it does not exist
    mkdir "server\src\main\resources\static\vue-components"
)

:: 5. Copy new files
copy "ui-components\dist\elaastic-vue-components-v%VERSION%.umd.min.js" "server\src\main\resources\static\vue-components\"
copy "ui-components\dist\style-v%VERSION%.css" "server\src\main\resources\static\vue-components\"

echo Update completed successfully!
echo New files copied to server\src\main\resources\static\vue-components