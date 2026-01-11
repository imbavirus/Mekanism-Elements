<#
.SYNOPSIS
  Build + publish Mekanism-Elements mod to GitHub, with SemVer bump, git commit + tag + push.

.DESCRIPTION
  - Requires a clean git working tree before starting.
  - Bumps version (default: patch, i.e. +0.0.1).
  - Updates:
      - gradle.properties (mod_version)
  - Commits, tags vX.Y.Z, pushes branch + tag to origin.
  - Builds the mod using Gradle.
  - Creates a GitHub release and uploads JAR artifacts.

.PARAMETER Bump
  patch | minor | major
  Default: patch

.PARAMETER OnlyPublish
  If set, skip version bump + commit/tag/push and only build+publish.

.PARAMETER Version
  Optional explicit version to publish (X.Y.Z). When provided, no bump is performed.

.PARAMETER Note
  Optional release note string for the GitHub release.

.PARAMETER GitHubToken
  GitHub personal access token with repo scope.
  If omitted, uses $env:GITHUB_TOKEN.

.PARAMETER GitHubRepo
  GitHub repository in format "owner/repo" (e.g., "username/mekanism-elements").
  If omitted, tries to detect from git remote.

.EXAMPLE
  pwsh ./publish.ps1

.EXAMPLE
  pwsh ./publish.ps1 -Bump minor -Note "New features added"

.EXAMPLE
  pwsh ./publish.ps1 -OnlyPublish -Version "2.4.0"
#>

[CmdletBinding()]
param(
  [ValidateSet("patch", "minor", "major")]
  [string]$Bump = "patch",

  # If set, skip version bump + commit/tag/push and only build+publish.
  [switch]$OnlyPublish,

  # Optional explicit version to publish (X.Y.Z). When provided, no bump is performed.
  [string]$Version,

  [string]$Note = "",

  # GitHub options
  [string]$GitHubToken = $env:GITHUB_TOKEN,
  [string]$GitHubRepo
)

$ErrorActionPreference = "Stop"

function Import-DotEnvIfPresent([string]$path) {
  if (-not (Test-Path $path)) { return }
  $lines = Get-Content -Path $path -ErrorAction Stop
  foreach ($line in $lines) {
    $trim = $line.Trim()
    if (-not $trim) { continue }
    if ($trim.StartsWith("#")) { continue }
    $idx = $trim.IndexOf("=")
    if ($idx -lt 1) { continue }
    $key = $trim.Substring(0, $idx).Trim()
    $value = $trim.Substring($idx + 1).Trim()
    # Strip surrounding quotes if present
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
      $value = $value.Substring(1, $value.Length - 2)
    }
    if ($key) {
      # Don't overwrite variables already set in the environment
      $existing = $null
      try {
        $existing = (Get-Item -Path ("Env:{0}" -f $key) -ErrorAction Stop).Value
      } catch {
        $existing = $null
      }
      if (-not [string]::IsNullOrWhiteSpace($existing)) { continue }
      Set-Item -Path "Env:$key" -Value $value
    }
  }
}

function Require-Command([string]$Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Missing required command: $Name"
  }
}

function Assert-GitClean() {
  $status = git status --porcelain
  if ($status) {
    throw "Git working tree is not clean. Commit/stash changes before publishing.`n`n$status"
  }
}

function Parse-SemVer([string]$v) {
  # Handle both "2.3" and "2.3.0" formats
  if ($v -match '^v?(\d+)\.(\d+)(?:\.(\d+))?$') {
    return @{
      Major = [int]$Matches[1]
      Minor = [int]$Matches[2]
      Patch = if ($Matches[3]) { [int]$Matches[3] } else { 0 }
    }
  }
  throw "Invalid SemVer version: '$v' (expected X.Y or X.Y.Z)"
}

function Format-SemVer([hashtable]$p) {
  return "$($p.Major).$($p.Minor).$($p.Patch)"
}

function Bump-SemVer([string]$v, [string]$kind) {
  $p = Parse-SemVer $v
  switch ($kind) {
    "major" { $p.Major++; $p.Minor = 0; $p.Patch = 0 }
    "minor" { $p.Minor++; $p.Patch = 0 }
    "patch" { $p.Patch++ }
    default { throw "Unknown bump kind: $kind" }
  }
  return Format-SemVer $p
}

function Read-GradleVersion([string]$gradlePropsPath) {
  $content = Get-Content -Path $gradlePropsPath
  foreach ($line in $content) {
    if ($line -match '^mod_version=(.+)$') {
      return $Matches[1].Trim()
    }
  }
  throw "Could not find mod_version in $gradlePropsPath"
}

function Update-GradleVersion([string]$gradlePropsPath, [string]$newVersion) {
  $content = Get-Content -Path $gradlePropsPath
  $updated = $false
  $newContent = @()
  
  foreach ($line in $content) {
    if ($line -match '^mod_version=(.+)$') {
      $newContent += "mod_version=$newVersion"
      $updated = $true
    } else {
      $newContent += $line
    }
  }
  
  if (-not $updated) {
    throw "Could not find mod_version in $gradlePropsPath"
  }
  
  Set-Content -Path $gradlePropsPath -Value $newContent
}

function Git-CommitTagPush([string]$newVersion) {
  $tag = "v$newVersion"
  git add gradle.properties

  git commit -m "chore(release): $tag"

  # Ensure tag doesn't already exist
  $existing = git tag -l $tag
  if ($existing) {
    throw "Tag already exists: $tag"
  }
  git tag $tag

  # Fetch latest from remote to see if we're behind
  Write-Host "Fetching latest from remote..."
  git fetch origin
  if ($LASTEXITCODE -ne 0) {
    Write-Warning "Failed to fetch from origin, but continuing..."
  }

  # Get current branch name
  $currentBranch = git rev-parse --abbrev-ref HEAD
  if (-not $currentBranch) {
    throw "Could not determine current branch name"
  }

  # Check if remote branch exists and if we're behind
  $remoteBranch = "origin/$currentBranch"
  $remoteExists = git rev-parse --verify "$remoteBranch" 2>$null
  $isBehind = $false
  
  if ($remoteExists) {
    # Check if we're behind the remote
    git fetch origin $currentBranch 2>$null | Out-Null
    $localCommit = git rev-parse HEAD
    $remoteCommit = git rev-parse $remoteBranch 2>$null
    if ($remoteCommit -and $localCommit) {
      $mergeBase = git merge-base HEAD $remoteBranch 2>$null
      if ($mergeBase -and $remoteCommit -ne $mergeBase) {
        $isBehind = $true
        Write-Host "Remote branch has new commits. Attempting to rebase..."
        # Try to rebase on top of remote
        git rebase $remoteBranch
        if ($LASTEXITCODE -ne 0) {
          Write-Warning "Rebase failed. You may have conflicts. Continuing with tag push only."
          Write-Warning "You'll need to manually resolve: git rebase --continue or git rebase --abort"
        } else {
          Write-Host "Rebase successful"
        }
      }
    }
  }

  # Push branch with all local commits
  Write-Host "Pushing branch $currentBranch to origin..."
  git push origin HEAD
  if ($LASTEXITCODE -ne 0) {
    Write-Warning "Failed to push branch to origin."
    Write-Warning "Continuing with tag push. You may need to manually sync the branch later."
  } else {
    Write-Host "Pushed branch to origin"
  }

  # Push tag (required for releases)
  Write-Host "Pushing tag $tag to origin..."
  git push origin $tag
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to push tag $tag to origin. This is required for releases."
  }
  Write-Host "Pushed tag $tag to origin"

  return $tag
}

function Build-Mod() {
  Write-Host "Building mod with Gradle..."
  
  # Use gradlew if available, otherwise try gradle
  $gradleCmd = if (Test-Path "gradlew.bat") { ".\gradlew.bat" } elseif (Test-Path "gradlew") { ".\gradlew" } else { "gradle" }
  
  & $gradleCmd clean build
  if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed"
  }
  
  Write-Host "Build completed successfully"
}

function Find-BuildArtifacts() {
  # Find JAR files in build/libs directory
  $libsDir = "build/libs"
  if (-not (Test-Path $libsDir)) {
    throw "Build directory not found: $libsDir (build may have failed)"
  }
  
  # Find all JAR files, but exclude sources and javadoc JARs
  $artifacts = Get-ChildItem -Path $libsDir -Filter "*.jar" | Where-Object {
    $name = $_.Name
    -not $name.Contains("-sources") -and -not $name.Contains("-javadoc")
  }
  
  if ($artifacts.Count -eq 0) {
    throw "No JAR artifacts found in $libsDir"
  }
  
  return $artifacts
}

function Upload-ToGitHubRelease([string]$version, [array]$artifacts) {
  # Load token from .env if not provided
  if (-not $script:GitHubToken) {
    Import-DotEnvIfPresent ".env"
    if (-not $script:GitHubToken) { $script:GitHubToken = $env:GITHUB_TOKEN }
  }
  
  if (-not $script:GitHubToken) {
    throw "Missing GitHubToken. Pass -GitHubToken or set `$env:GITHUB_TOKEN (or put it in .env)."
  }
  
  # Auto-detect GitHub repo from git remote if not provided
  if (-not $script:GitHubRepo) {
    $remote = git remote get-url origin
    Write-Host "Detecting GitHub repository from remote: $remote"
    
    # Match various GitHub URL formats:
    # HTTPS: https://github.com/username/repo.git
    # SSH: git@github.com:username/repo.git
    # SSH with protocol: ssh://git@github.com/username/repo.git
    $detectedRepo = $null
    
    # Try SSH with protocol: ssh://git@host/path
    if ($remote -match '^ssh://[^@]+@([^/:]+)(?::\d+)?/(.+?)(?:\.git)?/?$') {
      $remoteHost = $Matches[1]
      $path = $Matches[2]
      if ($remoteHost -eq "github.com") {
        $detectedRepo = $path -replace '\.git/?$', ''
      }
    }
    # Try HTTPS: https://host/path
    elseif ($remote -match '^https?://([^/:]+)(?::\d+)?/(.+?)(?:\.git)?/?$') {
      $remoteHost = $Matches[1]
      $path = $Matches[2]
      if ($remoteHost -eq "github.com" -or $remoteHost -match '^github\.') {
        $detectedRepo = $path -replace '\.git/?$', ''
      }
    }
    # Try SSH without protocol: git@host:path
    elseif ($remote -match '^[^@]+@([^/:]+)(?::\d+)?[:/](.+?)(?:\.git)?/?$') {
      $remoteHost = $Matches[1]
      $path = $Matches[2]
      if ($remoteHost -eq "github.com" -or $remoteHost -match '^github\.') {
        $detectedRepo = $path -replace '\.git/?$', ''
      }
    }
    
    if ($detectedRepo) {
      $script:GitHubRepo = $detectedRepo
      Write-Host "Detected GitHub repository: $script:GitHubRepo"
    }
  }
  
  if (-not $script:GitHubRepo) {
    throw "Could not detect GitHub repository from git remote. Set -GitHubRepo or ensure origin remote points to GitHub."
  }
  
  $tag = "v$version"
  $releaseName = "Release $tag"
  $releaseDescription = if ($script:Note) { 
    $script:Note 
  } else { 
    "Release $tag"
  }
  
  # GitHub API base URL
  $apiBase = "https://api.github.com"
  $repoPath = $script:GitHubRepo -replace '/', '/'
  $releaseUrl = "$apiBase/repos/$repoPath/releases"
  
  $headers = @{
    "Authorization" = "token $script:GitHubToken"
    "Accept" = "application/vnd.github.v3+json"
  }
  
  # Check if release already exists
  Write-Host "Checking if release $tag already exists..."
  try {
    $existingRelease = Invoke-RestMethod -Uri "$releaseUrl/tags/$tag" -Headers $headers -ErrorAction Stop
    Write-Host "Release already exists, updating..."
    
    $releaseBody = @{
      name = $releaseName
      body = $releaseDescription
      draft = $false
      prerelease = $false
    } | ConvertTo-Json -Compress
    
    $releaseResponse = Invoke-RestMethod -Uri "$releaseUrl/$($existingRelease.id)" -Method Patch -Headers $headers -Body $releaseBody -ErrorAction Stop
    Write-Host "Updated existing release: $tag"
    $releaseId = $existingRelease.id
    # Store release response for potential upload_url usage
    $currentRelease = $releaseResponse
  } catch {
    # Release doesn't exist, create it
    Write-Host "Creating new release: $tag"
    
    $releaseBody = @{
      tag_name = $tag
      name = $releaseName
      body = $releaseDescription
      draft = $false
      prerelease = $false
    } | ConvertTo-Json -Compress
    
    try {
      $releaseResponse = Invoke-RestMethod -Uri $releaseUrl -Method Post -Headers $headers -Body $releaseBody -ErrorAction Stop
      Write-Host "Created GitHub release: $tag"
      $releaseId = $releaseResponse.id
    } catch {
      $statusCode = $_.Exception.Response.StatusCode.value__
      $errorContent = $_.ErrorDetails.Message
      throw "Failed to create GitHub release: HTTP $statusCode - $errorContent"
    }
  }
  
  # Upload artifacts
  # GitHub requires using uploads.github.com for asset uploads, not api.github.com
  # Construct upload URL manually for reliability
  Write-Host "Debug: repoPath = '$repoPath'"
  Write-Host "Debug: releaseId = '$releaseId'"
  
  if ([string]::IsNullOrWhiteSpace($repoPath)) {
    throw "repoPath is empty or null. Cannot construct upload URL."
  }
  if ([string]::IsNullOrWhiteSpace($releaseId)) {
    throw "releaseId is empty or null. Cannot construct upload URL."
  }
  
  $uploadsBase = "https://uploads.github.com"
  $uploadUrlBase = "$uploadsBase/repos/$repoPath/releases/$releaseId/assets"
  
  Write-Host "Debug: uploadsBase = '$uploadsBase'"
  Write-Host "Debug: uploadUrlBase = '$uploadUrlBase'"
  
  if ([string]::IsNullOrWhiteSpace($uploadUrlBase)) {
    throw "uploadUrlBase is empty or null. Cannot upload artifacts."
  }
  
  Write-Host "Uploading $($artifacts.Count) artifact(s) to GitHub release..."
  Write-Host "Upload base URL: $uploadUrlBase"
  
  foreach ($artifact in $artifacts) {
    $fileName = $artifact.Name
    $filePath = $artifact.FullName
    $fileSize = (Get-Item $filePath).Length
    
    Write-Host "Uploading $fileName ($([math]::Round($fileSize / 1MB, 2)) MB)..."
    
    # GitHub release asset upload API expects raw file content
    # The upload URL should include ?name= parameter
    # Use PowerShell's built-in URI encoding
    $encodedFileName = [Uri]::EscapeDataString($fileName)
    # Use explicit string concatenation to avoid interpolation issues
    $uploadUrlWithName = $uploadUrlBase + "?name=" + $encodedFileName
    
    Write-Host "Debug: uploadUrlBase in loop = '$uploadUrlBase'"
    Write-Host "Debug: encodedFileName = '$encodedFileName'"
    Write-Host "Full upload URL: $uploadUrlWithName"
    
    # Validate URL before attempting upload
    try {
      $uri = [Uri]::new($uploadUrlWithName)
      if (-not $uri.IsAbsoluteUri) {
        throw "Upload URL is not absolute: $uploadUrlWithName"
      }
    } catch {
      Write-Warning "Invalid upload URL: $uploadUrlWithName - $_"
      throw
    }
      
    $uploadHeaders = @{
      "Authorization" = "token $script:GitHubToken"
      "Accept" = "application/vnd.github.v3+json"
      "Content-Type" = "application/java-archive"
    }
      
    try {
      # Use HttpClient for reliable file uploads
      Add-Type -AssemblyName System.Net.Http
      
      $httpClient = New-Object System.Net.Http.HttpClient
      $httpClient.DefaultRequestHeaders.Add("Authorization", "token $script:GitHubToken")
      $httpClient.DefaultRequestHeaders.Add("Accept", "application/vnd.github.v3+json")
      
      $fileStream = [System.IO.File]::OpenRead($filePath)
      $streamContent = New-Object System.Net.Http.StreamContent($fileStream)
      $streamContent.Headers.ContentType = New-Object System.Net.Http.Headers.MediaTypeHeaderValue("application/java-archive")
      
      $response = $httpClient.PostAsync($uri, $streamContent).Result
      
      $fileStream.Close()
      $httpClient.Dispose()
      
      if ($response.IsSuccessStatusCode) {
        Write-Host "  [OK] Uploaded $fileName"
      } else {
        $errorContent = $response.Content.ReadAsStringAsync().Result
        throw "Upload failed: $($response.StatusCode) - $errorContent"
      }
    } catch {
      # Fallback to Invoke-WebRequest if HttpClient fails
      try {
        $fileBytes = [System.IO.File]::ReadAllBytes($filePath)
        $response = Invoke-WebRequest -Uri $uri -Method Post -Headers $uploadHeaders -Body $fileBytes -ContentType "application/java-archive"
        
        if ($response.StatusCode -eq 201) {
          Write-Host "  [OK] Uploaded ${fileName}"
        } else {
          Write-Warning "  [FAIL] Failed to upload ${fileName}: HTTP $($response.StatusCode)"
        }
      } catch {
        Write-Warning "  [FAIL] Failed to upload ${fileName}: $_"
      }
    }
  }
  
  Write-Host "GitHub release: https://github.com/$script:GitHubRepo/releases/tag/$tag"
}

# Main execution
Require-Command git

# Load .env if present
Import-DotEnvIfPresent ".env"

# Read current version
$currentVersion = Read-GradleVersion "gradle.properties"
Write-Host "Current version: $currentVersion"

if (-not $OnlyPublish) {
  Assert-GitClean
}

if ($OnlyPublish) {
  if ($Version) {
    Parse-SemVer $Version | Out-Null
    $newVersion = $Version
    # Update gradle.properties with the specified version so the build uses it
    Write-Host "Updating gradle.properties to version: $newVersion"
    Update-GradleVersion "gradle.properties" $newVersion
  } else {
    $newVersion = $currentVersion
  }
  Write-Host "OnlyPublish enabled. Publishing version: $newVersion"
} else {
  if ($Version) {
    throw "Do not pass -Version unless using -OnlyPublish. Normal publish flow bumps versions automatically."
  }

  $newVersion = Bump-SemVer $currentVersion $Bump
  Write-Host "Bumping version: $currentVersion -> $newVersion (bump: $Bump)"

  Update-GradleVersion "gradle.properties" $newVersion

  # After version edit we must commit+tag
  $tag = Git-CommitTagPush $newVersion
  Write-Host "Created and pushed tag: $tag"
}

# Build the mod
Build-Mod

# Find build artifacts
$artifacts = Find-BuildArtifacts
Write-Host "Found $($artifacts.Count) artifact(s):"
foreach ($artifact in $artifacts) {
  Write-Host "  - $($artifact.Name)"
}

# Upload to GitHub
Upload-ToGitHubRelease $newVersion $artifacts

Write-Host ""
Write-Host "Publish complete!"
if ($GitHubRepo) {
  Write-Host "Release: https://github.com/$GitHubRepo/releases/tag/v$newVersion"
}
