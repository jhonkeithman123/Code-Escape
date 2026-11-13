package codeescape.desktop.adapter;

import codeescape.core.port.AudioPort;
import javax.sound.sampled.*;
import java.io.*;

public class DesktopAudioAdapter implements AudioPort {
    private Clip backgroundMusic;
    private boolean musicEnabled = true;
    private float musicVolume = 0.45f;
    private int currentMusicLevel = 1;

    @Override public void setEnabled(boolean enabled) { this.musicEnabled = enabled; if (!enabled) stopMusic(); }
    @Override public boolean isEnabled() { return musicEnabled; }
    @Override public void setVolume(float volume01) { this.musicVolume = Math.max(0f, Math.min(1f, volume01)); applyVolume(); }
    @Override public float getVolume() { return musicVolume; }

    @Override
    public void playMusic(String trackId, boolean loop) {
        if (trackId == null) return;
        playMusicFile(trackId, "Music");
    }

    @Override
    public void stopMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
        }
    }

    @Override
    public void playFeedback(boolean correct) {
        if (!musicEnabled) return;
        String[] names = correct
                ? new String[]{"correct.wav", "success.wav", "correct.mp3", "sfx/correct.wav", "music/correct.wav", "audio/correct.wav"}
                : new String[]{"wrong.wav", "error.wav", "wrong.mp3", "sfx/wrong.wav", "music/wrong.wav", "audio/wrong.wav"};
        for (String name : names) {
            if (playSfx(name)) return;
        }
        try { java.awt.Toolkit.getDefaultToolkit().beep(); } catch (Exception ignored) {}
    }

    private boolean playSfx(String fileName) {
        try {
            AudioInputStream ais = openMusic(fileName);
            if (ais == null) return false;
            try (AudioInputStream stream = ais) {
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                applyVolumeToClip(clip);
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP || event.getType() == LineEvent.Type.CLOSE) {
                        try { clip.close(); } catch (Exception ignored) {}
                    }
                });
                clip.setFramePosition(0);
                clip.start();
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void applyVolumeToClip(Clip clip) {
        if (clip == null) return;
        try {
            FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            if (musicVolume <= 0f) control.setValue(control.getMinimum());
            else {
                float db = (float) (20.0 * Math.log10(musicVolume));
                control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), db)));
            }
        } catch (Exception ignored) {}
    }

    public void playLevelMusic(int levelNumber) {
        currentMusicLevel = levelNumber;
        playMusicFile(codeescape.core.content.AudioCatalog.levelTrack(levelNumber), "Level music");
    }

    public void playSpecialMusic(String fileName) { playMusicFile(fileName, "Special music"); }
    public void playMainMenuMusic() { playMusicFile(codeescape.core.content.AudioCatalog.TRACK_MAIN_MENU, "Main menu music"); }
    public void startBackgroundMusic() { if (musicEnabled) playMainMenuMusic(); }
    public Clip getClip() { return backgroundMusic; }

    private AudioInputStream openMusic(String fileName) throws Exception {
        AudioInputStream source = null;
        InputStream resource = DesktopAudioAdapter.class.getResourceAsStream("/music/" + fileName);
        if (resource != null) {
            source = AudioSystem.getAudioInputStream(new BufferedInputStream(resource));
        } else {
            File file = new File("music", fileName);
            if (file.isFile()) source = AudioSystem.getAudioInputStream(file);
        }
        if (source == null) return null;
        AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0f, 16, 2, 4, 44100.0f, false);
        AudioFormat sourceFormat = source.getFormat();
        if (AudioSystem.isConversionSupported(target, sourceFormat)) {
            return AudioSystem.getAudioInputStream(target, source);
        }
        return source;
    }

    private void playMusicFile(String fileName, String errorLabel) {
        if (!musicEnabled) return;
        try {
            if (backgroundMusic != null) {
                backgroundMusic.stop();
                backgroundMusic.close();
                backgroundMusic = null;
            }
            AudioInputStream ais = openMusic(fileName);
            if (ais == null) {
                System.out.println(errorLabel + ": music track not found: " + fileName);
                return;
            }
            try (AudioInputStream stream = ais) {
                backgroundMusic = AudioSystem.getClip();
                backgroundMusic.open(stream);
            }
            applyVolume();
            backgroundMusic.setFramePosition(0);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusic.start();
        } catch (Exception e) {
            System.out.println(errorLabel + " could not be started: " + e.getMessage());
        }
    }

    private void applyVolume() {
        if (backgroundMusic == null) return;
        try {
            FloatControl control = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
            if (musicVolume <= 0f) control.setValue(control.getMinimum());
            else {
                float db = (float) (20.0 * Math.log10(musicVolume));
                control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), db)));
            }
        } catch (Exception ignored) {}
    }
}
