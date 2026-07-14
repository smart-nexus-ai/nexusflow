Add-Type -AssemblyName System.Drawing

$sourcePath = "d:\Android Apps\NexusFlow\.dev\assets\nexusflow-Icon-dark.png"
$resPath = "d:\Android Apps\NexusFlow\app\src\main\res"

if (!(Test-Path $sourcePath)) {
    Write-Error "Source image not found: $sourcePath"
    exit 1
}

# Load the source bitmap
$srcBitmap = New-Object System.Drawing.Bitmap($sourcePath)

$densities = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

try {
    foreach ($dir in $densities.Keys) {
        $size = $densities[$dir]
        $destDir = Join-Path $resPath $dir
        if (!(Test-Path $destDir)) {
            New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        }
        
        # Create destination bitmap
        $destBitmap = New-Object System.Drawing.Bitmap($size, $size)
        $graphics = [System.Drawing.Graphics]::FromImage($destBitmap)
        
        # High quality scaling settings
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        
        # Draw image resized
        $graphics.DrawImage($srcBitmap, 0, 0, $size, $size)
        
        # Save files
        $launcherPath = Join-Path $destDir "ic_launcher.png"
        $launcherRoundPath = Join-Path $destDir "ic_launcher_round.png"
        
        $destBitmap.Save($launcherPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $destBitmap.Save($launcherRoundPath, [System.Drawing.Imaging.ImageFormat]::Png)
        
        Write-Host "Generated $size x $size icons in $dir"
        
        # Clean up
        $graphics.Dispose()
        $destBitmap.Dispose()
    }
}
finally {
    $srcBitmap.Dispose()
}

Write-Host "App icons successfully updated!"
