@echo off
chcp 65001 >nul
title Controle Obras — Build + Instalador

set PROJETO=C:\Users\Estagio\Projetos\ControleObras
set ADB="C:\Users\Estagio\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set APK="%PROJETO%\app\build\outputs\apk\debug\app-debug.apk"
set MODELO_NOME=gemma3-1b-it-int4.task
set MODELO_LOCAL=%~dp0%MODELO_NOME%
set DESTINO_TABLET=/sdcard/Android/data/pt.controleobras.app/files/llm/

echo.
echo ╔══════════════════════════════════════════╗
echo ║   CONTROLE OBRAS — BUILD + INSTALADOR   ║
echo ╚══════════════════════════════════════════╝
echo.

:: ── 1. Verificar tablet ligado ───────────────────────────────────────────────
echo [1/5] A verificar tablet...
%ADB% get-state >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo  ERRO: Nenhum tablet encontrado.
    echo  Certifica-te de que:
    echo    - O cabo USB esta ligado
    echo    - Depuracao USB esta ativa no tablet
    echo    - Aceitaste a autorizacao de depuracao no ecra do tablet
    echo.
    pause
    exit /b 1
)
echo  OK — Tablet encontrado.

:: ── 2. Build APK ─────────────────────────────────────────────────────────────
echo.
echo [2/5] A compilar APK (pode demorar alguns minutos)...
cd /d "%PROJETO%"
call gradlew.bat assembleDebug --quiet
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo  ERRO: Falha na compilacao.
    echo  Abre o Android Studio para ver os erros detalhados.
    echo.
    pause
    exit /b 1
)
echo  OK — APK compilado com sucesso!

:: ── 3. Verificar APK ─────────────────────────────────────────────────────────
echo.
echo [3/5] A verificar APK...
if not exist %APK% (
    echo.
    echo  ERRO: APK nao encontrado apos compilacao.
    echo.
    pause
    exit /b 1
)
echo  OK — APK encontrado.

:: ── 4. Instalar app ──────────────────────────────────────────────────────────
echo.
echo [4/5] A instalar app no tablet...
%ADB% install -r %APK%
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo  ERRO: Falha ao instalar a app.
    echo  Tenta desinstalar a versao anterior no tablet e corre este script de novo.
    echo.
    pause
    exit /b 1
)
echo  OK — App instalada com sucesso!

:: ── 5. Instalar modelo IA ────────────────────────────────────────────────────
echo.
echo [5/5] A verificar modelo de IA...

if not exist "%MODELO_LOCAL%" (
    echo.
    echo  AVISO: Modelo de IA nao encontrado nesta pasta.
    echo.
    echo  Para ativar a IA, coloca o ficheiro:
    echo    %MODELO_NOME%
    echo  na mesma pasta que este script:
    echo    %~dp0
    echo.
    echo  Onde descarregar (conta HuggingFace gratuita necessaria^):
    echo    https://huggingface.co/litert-community/Gemma3-1B-IT
    echo    Ficheiro: gemma3-1b-it-int4.task  (~529 MB^)
    echo.
    echo  Depois corre este script de novo para instalar a IA tambem.
    echo.
    echo  A app funciona normalmente sem o modelo (usa parser automatico).
    goto :fim
)

echo  Modelo encontrado. A copiar para o tablet (~529 MB, aguarda)...
%ADB% shell mkdir -p %DESTINO_TABLET% >nul 2>&1
%ADB% push "%MODELO_LOCAL%" %DESTINO_TABLET%%MODELO_NOME%
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo  ERRO: Falha ao copiar o modelo para o tablet.
    echo.
    pause
    exit /b 1
)
echo  OK — Modelo de IA instalado!

:fim
echo.
echo ╔══════════════════════════════════════════╗
echo ║          INSTALACAO CONCLUIDA!           ║
echo ╚══════════════════════════════════════════╝
echo.
pause
