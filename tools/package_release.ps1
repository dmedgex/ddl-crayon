param(
    [switch]$SkipTests,
    [switch]$SkipClean,
    [switch]$SkipBuild,
    [string]$UpdateApiUrl = "",
    [string]$UpdatePageUrl = "",
    [string]$OutputDir = ""
)
$ErrorActionPreference = "Stop"

function Resolve-RepoRoot {
    $scriptDir = Split-Path -Parent $PSCommandPath
    return (Resolve-Path (Join-Path $scriptDir "..")).Path
}

function Read-VersionName {
    param([string]$BuildFile)
    $content = Get-Content -LiteralPath $BuildFile -Raw -Encoding UTF8
    $match = [regex]::Match($content, 'versionName\s*=\s*"([^"]+)"')
    if (-not $match.Success) {
        throw "Unable to read versionName from $BuildFile"
    }
    return $match.Groups[1].Value
}

function Invoke-Gradle {
    param(
        [string]$RepoRoot,
        [string[]]$GradleArgs
    )
    $gradle = Join-Path $RepoRoot "gradlew.bat"
    if (-not (Test-Path -LiteralPath $gradle)) {
        throw "gradlew.bat was not found at $gradle"
    }
    Write-Host ""
    Write-Host "Running: $gradle $($GradleArgs -join ' ')"
    
    # 保存当前目录并切换到项目根目录
    $originalLocation = Get-Location
    try {
        Set-Location -LiteralPath $RepoRoot
        & $gradle @GradleArgs
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle command failed with exit code $LASTEXITCODE"
        }
    } finally {
        # 恢复原工作目录
        Set-Location -LiteralPath $originalLocation
    }
}

function Copy-ReleaseApk {
    param(
        [string]$RepoRoot,
        [string]$VersionName,
        [string]$OutputDir
    )
    $apkPath = Join-Path $RepoRoot "app\build\outputs\apk\release\app-release.apk"
    if (-not (Test-Path -LiteralPath $apkPath)) {
        throw "Release APK was not found at $apkPath"
    }
    if ([string]::IsNullOrWhiteSpace($OutputDir)) {
        $OutputDir = Join-Path $RepoRoot "dist\release"
    } elseif (-not [System.IO.Path]::IsPathRooted($OutputDir)) {
        $OutputDir = Join-Path $RepoRoot $OutputDir
    }
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $outputFile = Join-Path $OutputDir "trickcal-crayon-v$VersionName-$timestamp-release.apk"
    Copy-Item -LiteralPath $apkPath -Destination $outputFile -Force
    return $outputFile
}

$repoRoot = Resolve-RepoRoot
$buildFile = Join-Path $repoRoot "app\build.gradle.kts"
$keystoreProperties = Join-Path $repoRoot "keystore.properties"
$versionName = Read-VersionName -BuildFile $buildFile

Write-Host "Repo root: $repoRoot"
Write-Host "Version: $versionName"

if (-not (Test-Path -LiteralPath $keystoreProperties)) {
    throw "keystore.properties is required for a signed release APK."
}

if (-not $SkipBuild) {
    $gradleArgs = New-Object System.Collections.Generic.List[string]
    if (-not $SkipClean) {
        $gradleArgs.Add("clean")
    }
    if (-not $SkipTests) {
        $gradleArgs.Add("testDebugUnitTest")
    }
    $gradleArgs.Add("assembleRelease")
    
    if (-not [string]::IsNullOrWhiteSpace($UpdateApiUrl)) {
        $gradleArgs.Add("-PupdateApiUrl=$UpdateApiUrl")
    }
    if (-not [string]::IsNullOrWhiteSpace($UpdatePageUrl)) {
        $gradleArgs.Add("-PupdatePageUrl=$UpdatePageUrl")
    }
    
    Invoke-Gradle -RepoRoot $repoRoot -GradleArgs $gradleArgs.ToArray()
}

$copiedApk = Copy-ReleaseApk -RepoRoot $repoRoot -VersionName $versionName -OutputDir $OutputDir
Write-Host ""
Write-Host "Release APK is ready:"
Write-Host $copiedApk