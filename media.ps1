Add-Type -AssemblyName Windows.winmd

if ([Windows.Media.MediaControl]::IsPlaying) {
    $artist = [Windows.Media.MediaControl]::Artist
    $title = [Windows.Media.MediaControl]::TrackName

    Write-Host "Artist: $artist"
    Write-Host "Title: $title"
} else {
    Write-Host "No media is currently playing."
}
