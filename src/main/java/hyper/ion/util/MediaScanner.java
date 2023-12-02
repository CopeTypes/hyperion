package hyper.ion.util;


import hyper.ion.Hyperion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

// Scan for known media apps (Spotify etc.) and attempt to retrieve song info from the title
// Todo script implementation for linux/macOS
public class MediaScanner {

    private boolean hasMedia, active;
    private String currentTrack, currentArtist;

    public MediaScanner() {
        Hyperion.log("Initializing Media Scanner.");
        active = true;
        hasMedia = false;
        currentTrack = null;
        currentArtist = null;
        Hyperion.THREAD.execute(this::scannerThread);
    }

    public void shutdown() {
        active = false;
    }

    public boolean hasMedia() {
        return hasMedia;
    }

    public String getMedia() {
        if (hasMedia & currentTrack != null) return "Playing " + currentTrack + " - " + currentArtist;
        return "No media is currently playing";
    }

    private void scannerThread() {
        try {Thread.sleep(2500);} catch (InterruptedException ignored) {}
        while (active) {
            try {Thread.sleep(1000);} catch (InterruptedException ignored) {}
            if (hasProcess("Spotify")) {
                SongData sd = getSpotify();
                if (sd != null && !sd.artist.equals("Unknown") && !sd.name.equals("Unknown")) {
                    hasMedia = true;
                    currentTrack = sd.name;
                    currentArtist = sd.artist;
                }
            } else reset();
            // todo checks for other media apps
        }
    }

    private void reset() {
        hasMedia = false;
        currentTrack = null;
        currentArtist = null;
    }

    private SongData getSpotify() {
        ArrayList<String> results = new ArrayList<>();
        try {
            ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "for /f \"tokens=* skip=9 delims= \" %g in ('tasklist /v /fo list /fi \"imagename eq spotify*\"') do @echo %g");
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                if (line == null) { break; }
                if (line.contains("Window Title:")) results.add(line);
            }
        } catch (IOException e) {return null;}
        if (results.isEmpty()) return null;
        String data = "";
        for (String line: results) {
            if (line.contains("-")) {
                data = line;
                break;
            }
        }
        if (data.equals("") || data.isBlank()) return null;
        data = data.replace("Window Title: ", "");
        String[] sd = data.split("-", 0);
        if (sd.length > 1 ) return new SongData(sd[1].trim(), sd[0].trim());
        return new SongData("Unknown", "Unknown");
    }

    private boolean hasProcess(String name) {
        return ProcessHandle.allProcesses()
            .filter(ProcessHandle::isAlive)
            .map(ph -> ph.info().command().orElse(""))
            .anyMatch(n -> n.contains(name) || n.contains(name.toLowerCase()));
    }

    private record SongData(String name, String artist) {}

}
