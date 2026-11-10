param(
    [switch]$DryRun,
    [switch]$CleanOld
)

# =====================================================================
# Equipment image remap / placeholder script (v2, ShanHaiJing simplified)
# - Builds front-end image keys {PREFIX}{001..030}.png for 12 subtypes.
# - Old images use 4-digit numbers (J1000.png); new use 3-digit (J001.png).
# - Reuses old images by subtype semantics, round-robin when short (REUSE).
# - Copies ONLY .png (never .meta); let Cocos Creator re-import to regen meta.
# Usage:
#   powershell -ExecutionPolicy Bypass -File equip_image_remap.ps1 -DryRun
#   powershell -ExecutionPolicy Bypass -File equip_image_remap.ps1
#   powershell -ExecutionPolicy Bypass -File equip_image_remap.ps1 -CleanOld
# =====================================================================

$empDir = 'd:\workspace\mnxy-main\assets\resources\game\texture\frames\emp'
if (-not (Test-Path $empDir)) {
    Write-Error "image dir not found: $empDir"; exit 1
}

# new subtype -> source old prefixes (nearest semantic; round-robin if short)
# new: J-sword G-bow F-staff | C-armor T-helmet X-boots | Z-ring N-necklace P-cape | Y-seal H-gourd B-banner
$SOURCE_MAP = [ordered]@{
    'J' = @('J')
    'G' = @('G')
    'F' = @('Q', 'F')
    'C' = @('S')
    'T' = @('M')
    'X' = @('Y')
    'Z' = @('D')
    'N' = @('X')
    'P' = @('P')
    'Y' = @('K')
    'H' = @('K')
    'B' = @('K')
}

# collect old pngs grouped by prefix letter, sorted by trailing number
$pngs = Get-ChildItem $empDir -File -Filter '*.png'
$byPrefix = @{}
foreach ($f in $pngs) {
    $m = [regex]::Match($f.BaseName, '^([A-Za-z]+)(\d+)$')
    if (-not $m.Success) { continue }
    $pfx = $m.Groups[1].Value.ToUpper()
    $num = [int]$m.Groups[2].Value
    if (-not $byPrefix.ContainsKey($pfx)) { $byPrefix[$pfx] = New-Object System.Collections.ArrayList }
    [void]$byPrefix[$pfx].Add([pscustomobject]@{ Num = $num; File = $f })
}
foreach ($k in @($byPrefix.Keys)) { $byPrefix[$k] = @($byPrefix[$k] | Sort-Object Num) }

$report  = New-Object System.Collections.ArrayList
$missing = New-Object System.Collections.ArrayList
$created = 0

foreach ($newPfx in $SOURCE_MAP.Keys) {
    $sources = @()
    foreach ($op in $SOURCE_MAP[$newPfx]) {
        if ($byPrefix.ContainsKey($op)) { $sources += $byPrefix[$op] }
    }
    if ($sources.Count -eq 0) {
        [void]$missing.Add(('{0}001~{0}030 (no source, need new art)' -f $newPfx))
        continue
    }
    for ($i = 1; $i -le 30; $i++) {
        $newKey = '{0}{1:D3}' -f $newPfx, $i
        $src    = $sources[($i - 1) % $sources.Count]
        $isReuse = (($i - 1) -ge $sources.Count)
        $dst    = Join-Path $empDir ($newKey + '.png')
        if ($i -ge 28) { $tag = 'RESERVED' } elseif ($isReuse) { $tag = 'REUSE' } else { $tag = 'OK' }
        [void]$report.Add([pscustomobject]@{
            NewKey = $newKey; SourceOldKey = $src.File.BaseName; Status = $tag
        })
        if (-not $DryRun) {
            Copy-Item -LiteralPath $src.File.FullName -Destination $dst -Force
            $created++
        }
    }
}

$reportPath = Join-Path $empDir 'equip_image_remap_report.csv'
if (-not $DryRun) {
    $report | Export-Csv -LiteralPath $reportPath -NoTypeInformation -Encoding UTF8
}

# optional: archive old 4-digit pngs (+meta) to _old_backup
if ((-not $DryRun) -and $CleanOld) {
    $bak = Join-Path $empDir '_old_backup'
    if (-not (Test-Path $bak)) { New-Item -ItemType Directory -Path $bak | Out-Null }
    foreach ($f in (Get-ChildItem $empDir -File -Filter '*.png')) {
        $m = [regex]::Match($f.BaseName, '^([A-Za-z]+)(\d+)$')
        if ($m.Success -and $m.Groups[2].Value.Length -ge 4) {
            Move-Item -LiteralPath $f.FullName -Destination $bak -Force
            $meta = $f.FullName + '.meta'
            if (Test-Path $meta) { Move-Item -LiteralPath $meta -Destination $bak -Force }
        }
    }
}

$ok      = @($report | Where-Object { $_.Status -eq 'OK' }).Count
$reuse   = @($report | Where-Object { $_.Status -eq 'REUSE' }).Count
$reserve = @($report | Where-Object { $_.Status -eq 'RESERVED' }).Count
Write-Output '==================================================================='
Write-Output ('mode        : ' + $(if ($DryRun) { 'DRY-RUN (no write)' } else { 'EXECUTED' }))
Write-Output ('png created : ' + $created + ' / planned ' + $report.Count)
Write-Output ('first-use OK: ' + $ok)
Write-Output ('reuse  REUSE: ' + $reuse + '  (source short, placeholder, artist to replace)')
Write-Output ('reserved    : ' + $reserve + '  (028~030 activity/exclusive slots)')
Write-Output ('missing     : ' + $missing.Count)
foreach ($mm in $missing) { Write-Output ('  - ' + $mm) }
if (-not $DryRun) { Write-Output ('report csv  : ' + $reportPath) }
Write-Output '==================================================================='
Write-Output 'note: new pngs have NO .meta; re-import the resource folder in Cocos Creator to generate meta.'
