package codeescape.desktop.adapter;

import codeescape.core.port.NarrationPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WindowsNarrationAdapter implements NarrationPort {
    private Process storySpeechProcess;
    private Thread storySpeechThread;
    private volatile int storySpeechGeneration = 0;

    @Override public boolean isSupported() { return System.getProperty("os.name", "").toLowerCase().contains("win"); }
    @Override public void speakStory(String text) { speakStory(text, (NarrationCallback) null); }
    @Override public void stop() { stop(null, null); }

    public void speakStory(String text, NarrationCallback callback) { speakStory(text, callback, null); }

    /** Core NarrationPort karaoke contract (NarrationCallback extends the core callback). */
    @Override
    public void speakStory(String text, codeescape.core.port.NarrationCallback callback) {
        if (callback == null) {
            speakStory(text, (NarrationCallback) null, null);
        } else if (callback instanceof NarrationCallback) {
            speakStory(text, (NarrationCallback) callback, null);
        } else {
            speakStory(text, new NarrationCallback() {
                @Override public void onLine(int index, String t, List<int[]> r) { callback.onLine(index, t, r); }
                @Override public void onComplete() { callback.onComplete(); }
            }, null);
        }
    }

    public void speakStory(String text, NarrationCallback callback, Runnable onMusicPauseResume) {
        stop(null, null);
        if (!isSupported()) { if (callback != null) callback.onComplete(); return; }
        final int generation = ++storySpeechGeneration;
        final List<int[]> ranges = buildStoryRanges(text);
        storySpeechThread = new Thread(() -> {
            try {
                StringBuilder speechText = new StringBuilder();
                for (int i = 0; i < ranges.size(); i++) {
                    if (i > 0) speechText.append((char)30);
                    speechText.append(text.substring(ranges.get(i)[0], ranges.get(i)[1]).trim());
                }
                String encoded = Base64.getEncoder().encodeToString(speechText.toString().getBytes(StandardCharsets.UTF_8));
                String command = "$b=[Convert]::FromBase64String('" + encoded + "');"
                        + "$t=[Text.Encoding]::UTF8.GetString($b);"
                        + "$parts=$t.Split([char]30);"
                        + "Add-Type -AssemblyName System.Speech;"
                        + "$s=New-Object System.Speech.Synthesis.SpeechSynthesizer;"
                        + "$s.Rate=1;$s.Volume=100;"
                        + "for($i=0;$i -lt $parts.Length;$i++){"
                        + "[Console]::Out.WriteLine('LINE:'+$i);"
                        + "$s.Speak($parts[$i]);"
                        + "};$s.Dispose();";
                Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", command)
                        .redirectErrorStream(true).start();
                storySpeechProcess = process;
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                String outputLine;
                while ((outputLine = reader.readLine()) != null) {
                    if (generation != storySpeechGeneration || Thread.currentThread().isInterrupted()) { process.destroy(); break; }
                    if (outputLine.startsWith("LINE:") && callback != null) {
                        try {
                            int idx = Integer.parseInt(outputLine.substring(5).trim());
                            callback.onLine(idx, text, ranges);
                            if (idx >= 0 && idx < ranges.size()) {
                                int[] r = ranges.get(idx);
                                callback.onRange(r[0], r[1]);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
                process.waitFor();
            } catch (Exception ignored) {
            } finally {
                if (generation == storySpeechGeneration) {
                    storySpeechThread = null; storySpeechProcess = null;
                    if (callback != null) {
                        try { callback.onComplete(); } catch (Exception ignored2) {}
                    }
                }
            }
        }, "CodeEscape-Story-Karaoke");
        storySpeechThread.setDaemon(true);
        storySpeechThread.start();
    }

    public void stop(Runnable onStopped, Runnable resumeMusic) {
        storySpeechGeneration++;
        Process p = storySpeechProcess; storySpeechProcess = null;
        if (p != null && p.isAlive()) {
            p.destroy();
            try { p.waitFor(250, TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        Thread t = storySpeechThread; storySpeechThread = null;
        if (t != null && t != Thread.currentThread()) t.interrupt();
        if (onStopped != null) onStopped.run();
    }

    public static List<int[]> buildStoryRanges(String text) {
        List<int[]> ranges = new ArrayList<>();
        if (text == null) return ranges;
        Matcher m = Pattern.compile("[^.!?]+[.!?]+|[^.!?]+$").matcher(text);
        while (m.find()) {
            int a = m.start(), b = m.end();
            while (a < b && Character.isWhitespace(text.charAt(a))) a++;
            while (b > a && Character.isWhitespace(text.charAt(b - 1))) b--;
            if (b > a) ranges.add(new int[]{a, b});
        }
        return ranges;
    }
}
