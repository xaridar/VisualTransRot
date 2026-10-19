@echo off

REM If an argument is passed, it will be used as the version number
set suffix=
if "%1%" NEQ "" (
    set suffix=_%1%
)
if "%2%" NEQ "" (
    echo "Only one argument is supported"
    exit
)

REM Copy static files
xcopy /I src\*.otf bin\vtr /y
xcopy /I saved_dbase.txt bin /y
xcopy /I saved_config.txt bin /y
xcopy /I TransRot.jar bin /y
xcopy /I README.md bin /y

REM Compile source
cd src
javac -d ..\bin\vtr Main.java
javac -d ..\bin\wrapper Wrapper.java

REM Create jar files
cd ..\bin
jar cfe vtr.jar Main -C vtr/ .
jar cfe wrapper.jar Wrapper -C wrapper/ .

REM Package binaries
@REM powershell -Command "Compress-Archive -Path 'saved_dbase.txt', 'saved_config.txt', 'README.md', 'TransRot.jar', 'vtr.jar', 'wrapper.jar' -DestinationPath '.\vtr%suffix%-win.zip' -Force"
@REM powershell -Command ^
@REM     "Add-Type -AssemblyName System.IO.Compression.FileSystem;" ^
@REM     "if (Test-Path '.\vtr%suffix%-mac.zip') {" ^
@REM         "Remove-Item '.\vtr%suffix%-mac.zip' -Force;" ^
@REM     "}" ^
@REM     "$zip = [System.IO.Compression.ZipFile]::Open('.\vtr%suffix%-mac.zip', 'Create');" ^
@REM     "function Add-File($f, $perm) {" ^
@REM         "if (Test-Path .\$f) {" ^
@REM             "$entry = $script:zip.CreateEntry($f);" ^
@REM             "$entry.ExternalAttributes = $perm;" ^
@REM             "$stream = $entry.Open();" ^
@REM             "$fs = [System.IO.File]::OpenRead((Get-Item .\$f).FullName);" ^
@REM             "$fs.CopyTo($stream);" ^
@REM             "$fs.Close();" ^
@REM             "$stream.Close();" ^
@REM             "try {" ^
@REM                 "$lhf = $entry.GetType().GetField('_localHeaderOffset', [System.Reflection.BindingFlags]'NonPublic|Instance');" ^
@REM                 "if ($lhf) {" ^
@REM                     "$offset = $lhf.GetValue($entry);" ^
@REM                     "$archiveStream = $script:zip.GetType().GetField('_archiveStream', [System.Reflection.BindingFlags]'NonPublic|Instance').GetValue($script:zip);" ^
@REM                     "if ($archiveStream -and $archiveStream.CanWrite) {" ^
@REM                         "$pos = $archiveStream.Position;" ^
@REM                         "$archiveStream.Seek($offset + 5, [System.IO.SeekOrigin]::Begin) > $null;" ^
@REM                         "$archiveStream.WriteByte(3);" ^
@REM                         "$archiveStream.Seek($pos, [System.IO.SeekOrigin]::Begin) > $null;" ^
@REM                     "}" ^
@REM                 "}" ^
@REM             "} catch {}" ^
@REM         "}" ^
@REM     "}" ^
@REM     "Add-File 'vtr.jar' -2115174400;" ^
@REM     "Add-File 'TransRot.jar' -2115174400;" ^
@REM     "Add-File 'README.md' -2119043072;" ^
@REM     "Add-File 'saved_dbase.txt' -2119043072;" ^
@REM     "Add-File 'saved_config.txt' -2119043072;" ^
@REM     "$zip.Dispose();"
@REM echo f | xcopy .\vtr%suffix%-mac.zip .\vtr%suffix%-linux.zip /y

cd ..