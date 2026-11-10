$ErrorActionPreference = 'Continue'
$jars = (Get-ChildItem "$env:USERPROFILE/.m2/repository" -Recurse -Filter *.jar | ForEach-Object { $_.FullName -replace '\\','/' }) -join ';'
New-Item -ItemType Directory -Force -Path 'E:/workspace/QQSX/target/classes' | Out-Null
$args = @(
  '-proc:full',
  '-encoding UTF-8',
  ('-cp E:/workspace/QQSX/target/classes;' + $jars),
  '-d E:/workspace/QQSX/target/classes',
  'E:/workspace/QQSX/src/main/java/com/sy/model/game/BattleManager.java'
)
[IO.File]::WriteAllText('E:/workspace/QQSX/javac_args.txt', ($args -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
$out = & 'C:/Program Files/Java/jdk-17.0.18/bin/javac.exe' '@E:/workspace/QQSX/javac_args.txt' 2>&1
$exit = $LASTEXITCODE
$out | Select-Object -First 40
"EXIT=$exit"
