package codeescape.desktop.persistence;

import codeescape.core.model.PlayerProfile;
import codeescape.core.port.ProfileRepository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Desktop file-backed repository (users.properties / scores.properties).
 * Designed to preserve compatibility with existing save formats during migration.
 */
public class FileProfileRepository implements ProfileRepository {
    private final File usersFile;
    private final File scoresFile;
    private final Map<String, String> accounts = new HashMap<>();
    private final Map<String, String> emails = new HashMap<>();
    private final Map<String, Integer> scores = new HashMap<>();
    private final Map<String, PlayerProfile> profiles = new HashMap<>();

    public FileProfileRepository(File baseDir) {
        File dir = baseDir == null ? new File(".") : baseDir;
        this.usersFile = new File(dir, "users.properties");
        this.scoresFile = new File(dir, "scores.properties");
        load();
        ensureDemoAccount();
    }

    /**
     * Keep a local demo account available for parity with legacy desktop behavior.
     * Non-destructive: preserves existing demo credentials/profile data when present.
     */
    private synchronized void ensureDemoAccount() {
        boolean changed = false;
        if (!accounts.containsKey("demo")) {
            accounts.put("demo", "demo");
            changed = true;
        }
        if (!emails.containsKey("demo")) {
            emails.put("demo", "demo@codeescape.local");
            changed = true;
        }
        if (!scores.containsKey("demo")) {
            scores.put("demo", 0);
            changed = true;
        }
        PlayerProfile demo = profiles.get("demo");
        if (demo == null) {
            demo = new PlayerProfile("demo", emails.getOrDefault("demo", "demo@codeescape.local"));
            demo.character = "Boy";
            demo.outfitVariant = 0;
            demo.selectedTop = 1; demo.selectedBottom = 1; demo.selectedShoes = 1; demo.selectedAccessory = 1;
            demo.lifetimeXp = scores.getOrDefault("demo", 0);
            demo.savedXp = demo.lifetimeXp;
            profiles.put("demo", demo);
            changed = true;
        } else {
            if (demo.username == null || demo.username.isEmpty()) { demo.username = "demo"; changed = true; }
            if (demo.email == null || demo.email.isEmpty()) { demo.email = emails.getOrDefault("demo", "demo@codeescape.local"); changed = true; }
            if (demo.character == null || demo.character.isEmpty()) { demo.character = "Boy"; changed = true; }
            scores.putIfAbsent("demo", Math.max(0, demo.lifetimeXp));
        }
        if (changed) {
            saveUsers();
            saveScores();
        }
    }

    public synchronized void reload() {
        accounts.clear();
        emails.clear();
        scores.clear();
        profiles.clear();
        load();
        ensureDemoAccount();
    }

    public synchronized Map<String, String> getAccounts() {
        return new LinkedHashMap<>(accounts);
    }

    public synchronized Map<String, String> getEmails() {
        return new LinkedHashMap<>(emails);
    }

    public synchronized Map<String, Integer> getScores() {
        return new LinkedHashMap<>(scores);
    }

    public synchronized Map<String, String> getSavedCharacters() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, PlayerProfile> e : profiles.entrySet()) {
            if (e.getValue() != null && e.getValue().character != null) {
                out.put(e.getKey(), e.getValue().character);
            }
        }
        return out;
    }

    public synchronized Map<String, Integer> getSavedOutfits() {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, PlayerProfile> e : profiles.entrySet()) {
            if (e.getValue() != null) out.put(e.getKey(), e.getValue().outfitVariant);
        }
        return out;
    }

    public synchronized Map<String, int[]> getSavedStyles() {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, PlayerProfile> e : profiles.entrySet()) {
            if (e.getValue() != null) {
                out.put(e.getKey(), new int[]{e.getValue().selectedTop, e.getValue().selectedBottom, e.getValue().selectedShoes, e.getValue().selectedAccessory});
            }
        }
        return out;
    }

    private synchronized void load() {
        loadScores();
        loadUsers();
    }

    private void loadScores() {
        if (!scoresFile.isFile()) return;
        Properties data = new Properties();
        try (FileInputStream in = new FileInputStream(scoresFile)) {
            data.load(in);
            for (String name : data.stringPropertyNames()) {
                try { scores.put(name, Math.max(0, Integer.parseInt(data.getProperty(name, "0")))); }
                catch (NumberFormatException ignored) {}
            }
        } catch (IOException ignored) {}
    }

    private void loadUsers() {
        if (!usersFile.isFile()) return;
        Properties data = new Properties();
        try (FileInputStream in = new FileInputStream(usersFile)) {
            data.load(in);
            for (String property : data.stringPropertyNames()) {
                if (!property.startsWith("user.") || !property.endsWith(".password")) continue;
                String key = property.substring(5, property.length() - 9);
                try {
                    String name = new String(Base64.getUrlDecoder().decode(key), StandardCharsets.UTF_8);
                    accounts.put(name, data.getProperty(property, ""));
                    emails.put(name, data.getProperty("user." + key + ".email", ""));
                    PlayerProfile p = new PlayerProfile(name, emails.get(name));
                    p.character = data.getProperty("user." + key + ".character", "Boy");
                    try { p.outfitVariant = Math.max(0, Math.min(3, Integer.parseInt(data.getProperty("user." + key + ".outfit", "0")))); } catch (Exception ignored) { p.outfitVariant = 0; }
                    try { p.lifetimeXp = Math.max(0, Integer.parseInt(data.getProperty("user." + key + ".xp", String.valueOf(scores.getOrDefault(name, 0))))); } catch (Exception ignored) { p.lifetimeXp = scores.getOrDefault(name, 0); }
                    try { p.selectedTop = Integer.parseInt(data.getProperty("user." + key + ".style0", "1")); } catch (Exception ignored) { p.selectedTop = 1; }
                    try { p.selectedBottom = Integer.parseInt(data.getProperty("user." + key + ".style1", "1")); } catch (Exception ignored) { p.selectedBottom = 1; }
                    try { p.selectedShoes = Integer.parseInt(data.getProperty("user." + key + ".style2", "1")); } catch (Exception ignored) { p.selectedShoes = 1; }
                    try { p.selectedAccessory = Integer.parseInt(data.getProperty("user." + key + ".style3", "1")); } catch (Exception ignored) { p.selectedAccessory = 1; }
                    p.savedXp = p.lifetimeXp;
                    profiles.put(name, p);
                    scores.putIfAbsent(name, p.lifetimeXp);
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (IOException ignored) {}
    }

    @Override public synchronized Optional<PlayerProfile> findByUsername(String username) {
        return Optional.ofNullable(profiles.get(username));
    }

    @Override public synchronized boolean authenticate(String username, String password) {
        return accounts.containsKey(username) && Objects.equals(accounts.get(username), password);
    }

    @Override public synchronized void saveAccount(String username, String email, String password, PlayerProfile profile) {
        accounts.put(username, password);
        emails.put(username, email == null ? "" : email);
        if (profile == null) profile = new PlayerProfile(username, email);
        profile.username = username;
        profile.email = email == null ? "" : email;
        profiles.put(username, profile);
        scores.putIfAbsent(username, profile.lifetimeXp);
        saveUsers();
        saveScores();
    }

    @Override public synchronized void saveProfile(PlayerProfile profile) {
        if (profile == null || profile.username == null) return;
        profiles.put(profile.username, profile);
        emails.put(profile.username, profile.email == null ? "" : profile.email);
        scores.put(profile.username, Math.max(0, profile.lifetimeXp));
        saveUsers();
        saveScores();
    }

    @Override public synchronized int getLifetimeXp(String username) {
        return Math.max(0, scores.getOrDefault(username, profiles.get(username) != null ? profiles.get(username).lifetimeXp : 0));
    }

    @Override public synchronized void commitAttemptXp(String username, int earnedXp) {
        if (username == null) return;
        int add = Math.max(0, earnedXp);
        int total = Math.max(0, getLifetimeXp(username) + add);
        scores.put(username, total);
        PlayerProfile p = profiles.get(username);
        if (p != null) { p.lifetimeXp = total; p.savedXp = total; }
        saveScores();
        saveUsers();
    }

    private void saveScores() {
        Properties data = new Properties();
        for (Map.Entry<String, Integer> e : scores.entrySet()) data.setProperty(e.getKey(), String.valueOf(Math.max(0, e.getValue())));
        try (FileOutputStream out = new FileOutputStream(scoresFile)) {
            data.store(out, "Code Escape player XP scores");
        } catch (IOException ignored) {}
    }

    private void saveUsers() {
        Properties data = new Properties();
        for (String name : accounts.keySet()) {
            String key = Base64.getUrlEncoder().withoutPadding().encodeToString(name.getBytes(StandardCharsets.UTF_8));
            data.setProperty("user." + key + ".password", accounts.getOrDefault(name, ""));
            data.setProperty("user." + key + ".email", emails.getOrDefault(name, ""));
            PlayerProfile p = profiles.get(name);
            if (p == null) p = new PlayerProfile(name, emails.getOrDefault(name, ""));
            data.setProperty("user." + key + ".character", p.character == null ? "Boy" : p.character);
            data.setProperty("user." + key + ".outfit", String.valueOf(p.outfitVariant));
            data.setProperty("user." + key + ".xp", String.valueOf(scores.getOrDefault(name, p.lifetimeXp)));
            data.setProperty("user." + key + ".style0", String.valueOf(p.selectedTop));
            data.setProperty("user." + key + ".style1", String.valueOf(p.selectedBottom));
            data.setProperty("user." + key + ".style2", String.valueOf(p.selectedShoes));
            data.setProperty("user." + key + ".style3", String.valueOf(p.selectedAccessory));
        }
        try (FileOutputStream out = new FileOutputStream(usersFile)) {
            data.store(out, "Code Escape local user database");
        } catch (IOException ignored) {}
    }
}
