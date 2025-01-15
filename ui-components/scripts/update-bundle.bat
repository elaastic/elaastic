@echo off
setlocal enabledelayedexpansion

:: Check if version parameter is provided
if "%1"=="" (
    echo Error: Version parameter is required
    echo Usage: %0 ^<version^>
    exit /b 1
)

set VERSION=%1
echo Using version: %VERSION%

:: Check if we are in the correct directory (scripts folder of ui-components)
for %%I in (.) do set CURRENT_DIR=%%~nxI
for %%I in (..) do set PARENT_DIR=%%~nxI

if not "%CURRENT_DIR%"=="scripts" (
    echo Error: This script must be executed from the 'scripts' folder of ui-components
    echo Current directory: %CD%
    exit /b 1
)

if not "%PARENT_DIR%"=="ui-components" (
    echo Error: Parent directory must be 'ui-components'
    echo Parent directory: %PARENT_DIR%
    exit /b 1
)

if not exist "..\package.json" (
    echo Error: package.json not found in parent directory
    exit /b 1
)

:: Go to parent directory (ui-components)
cd ..

:: Update version in package.json
call npm version %VERSION% --git-tag-version false --allow-same-version true
if errorlevel 1 (
    echo Error updating version in package.json
    exit /b 1
)

echo Successfully updated version to %VERSION% in package.json

:: Build the bundle
call npm run build
if errorlevel 1 (
    echo Error during build process
    exit /b 1
)

:: Check if generated files exist
if not exist "dist\elaastic-vue-components-v%VERSION%.umd.min.js" (
    echo JS file not found: elaastic-vue-components-v%VERSION%.umd.min.js
    exit /b 1
)

if not exist "dist\style-v%VERSION%.css" (
    echo CSS file not found: style-v%VERSION%.css
    exit /b 1
)

:: Check/Create destination directory and clean its content
cd ..
set "TARGET_DIR=server\src\main\resources\static\vue-components"

if not exist "%TARGET_DIR%" (
    mkdir "%TARGET_DIR%"
) else (
    del /Q "%TARGET_DIR%\*"
)

:: Copy the new files
copy "ui-components\dist\elaastic-vue-components-v%VERSION%.umd.min.js" "%TARGET_DIR%\"
copy "ui-components\dist\style-v%VERSION%.css" "%TARGET_DIR%\"

echo Update completed successfully!
echo New files copied to %TARGET_DIR%

endlocal