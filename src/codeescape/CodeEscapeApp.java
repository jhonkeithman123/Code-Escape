package codeescape;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import codeescape.core.content.GameConfig;
import codeescape.core.content.LevelMetadata;
import codeescape.core.content.QuestionBank;
import codeescape.core.content.StoryContent;
import codeescape.core.model.GameSettings;
import codeescape.core.model.PlayerProfile;
import codeescape.core.model.Question;
import codeescape.core.port.NarrationCallback;
import codeescape.core.port.ProfileRepository;
import codeescape.desktop.adapter.DesktopAudioAdapter;
import codeescape.desktop.adapter.WindowsNarrationAdapter;
import codeescape.desktop.persistence.FileProfileRepository;
import codeescape.desktop.persistence.FileSettingsStore;
import codeescape.desktop.ui.components.*;

public class CodeEscapeApp extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private JPanel endingCard;
    private final Map<String, String> accounts = new HashMap<>();
    private final Map<String, String> emails = new HashMap<>();
    private final Map<String, Integer> scores = new HashMap<>();
    private final Map<String, String> savedCharacters = new HashMap<>();
    private final Map<String, Integer> savedOutfits = new HashMap<>();
    private final Map<String, int[]> savedStyles = new HashMap<>();
    private final FileProfileRepository profileRepository = new FileProfileRepository(new File("."));

    private String username = "";
    private String email = "";
    private String selectedCharacter = "Boy";
    private int selectedOutfit = 0;
    // Individual clothing slots: 1 = Set 1, 2 = Set 2, 3 = Set 3.
    private int selectedTop = 1;
    private int selectedBottom = 1;
    private int selectedShoes = 1;
    private int selectedAccessory = 1;

    // XP saved from before the current login. Gameplay XP resets to 0 each login.
    private int savedXp = 0;
    private int secondsLeft = GameConfig.MCQ_SECONDS;

    private final codeescape.desktop.scheduler.SwingScheduler uiScheduler = new codeescape.desktop.scheduler.SwingScheduler();
    private codeescape.desktop.ui.question.QuestionHudController questionHudController;
    // Audio/music and narration ownership lives in the port adapters (DesktopAudioAdapter /
    // WindowsNarrationAdapter); story-screen karaoke label state is owned by StoryScreenBuilder.
    private boolean storyMusicWasPlaying = false;
    private final DesktopAudioAdapter audioAdapter = new DesktopAudioAdapter();
    private final WindowsNarrationAdapter narrationAdapter = new WindowsNarrationAdapter();
    // Narration is routed through the core NarrationPort contract (karaoke callback included).
    private final codeescape.core.port.NarrationPort narrationPort = narrationAdapter;
    // Shared audio/narration settings, persisted per-machine in settings.properties.
    private final FileSettingsStore settingsStore = new FileSettingsStore(new File("settings.properties"));
    private final GameSettings settings = settingsStore.load();
    private JLabel timerLabel;
    private JLabel xpLabel;
    private JLabel heartsLabel;
    private codeescape.desktop.ui.screens.QuestionScreen.HudRefs questionHudRefs;
    private codeescape.desktop.ui.screens.QuestionScreen.Refs questionScreenRefs;
    private JLabel questionLabel;
    private JLabel questionCounterLabel;
    private JButton[] answerButtons = new JButton[4];
    private JLabel dashboardAvatar;
    private JLabel dashboardCharacterLabel;
    private JLabel dashboardWelcome;
    private JLabel mainMenuXpLabel;
    private JLabel dashboardXpLabel;
    private JLabel profileAvatar;
    private JLabel profileCharacterLabel;
    private JLabel profilePlayerLabel;
    private JLabel profileXpLabel;
    private JLabel profileCorrectLabel;
    private JLabel profileLevelLabel;
    private JLabel profileHeartsLabel;
    private JLabel equippedTopLabel, equippedBottomLabel, equippedShoesLabel, equippedAccessoryLabel;
    private JLabel gameOverXpLabel;
    private JLabel completeXpLabel;
    private JTextField syntaxAnswerField;
    private JButton syntaxSubmitButton;
    private JPanel answerGrid;
    private JPanel syntaxPanel;
    private boolean acceptingAnswer = false;
    private codeescape.desktop.ui.components.BackgroundPanel questionBackground;

    private static final Color NAVY = new Color(18, 39, 68);
    private static final Color PANEL = new Color(20, 51, 86, 238);
    private static final Color BLUE = new Color(73, 166, 255);
    private static final Color GOLD = new Color(255, 209, 102);
    private static final Color CYAN = new Color(105, 229, 245);
    private static final Color PINK = new Color(255, 126, 190);
    private static final Color GREEN = new Color(82, 220, 150);
    private static final Color RED = new Color(240, 75, 82);
    private static final Color CREAM = new Color(255, 231, 171);
    private static final Color PEACH = new Color(255, 190, 135);
    private static final Color MINT = new Color(150, 247, 211);

    private final codeescape.core.engine.LessonEngine lessonEngine = new codeescape.core.engine.LessonEngine();
    private codeescape.core.model.GameSession session() { return lessonEngine.getSession(); }
    private final codeescape.core.flow.Navigator flowNavigator = new codeescape.core.flow.Navigator() {
        @Override public void navigate(codeescape.core.flow.FlowState state) { navigate(state, session().level); }
        @Override public void navigate(codeescape.core.flow.FlowState state, int level) {
            if (state == null) return;
            switch (state) {
                case QUESTION:
                    startQuestion();
                    return;
                case DASHBOARD:
                    refreshDashboard();
                    showScreen(codeescape.core.flow.FlowTransitions.screenName(state));
                    return;
                case PROFILE:
                    refreshProfile();
                    showScreen(codeescape.core.flow.FlowTransitions.screenName(state));
                    return;
                default:
                    showScreen(codeescape.core.flow.FlowTransitions.screenName(state));
            }
        }
    };

    /** Routes app-level flow events through the core transition contract when a target is resolved. */
    private void dispatch(codeescape.core.flow.FlowEvent event) {
        // Core resolves unambiguous and selected context-sensitive transitions (cross-frontend contract).
        // Desktop navigator owns screen-specific effects (e.g. QUESTION -> startQuestion) for parity.
        codeescape.core.flow.FlowTransitions.dispatch(flowNavigator, null, event);
    }

    /** Music playing state is owned by the audio adapter (AudioPort); no mirrored clip reference. */
    private boolean isBackgroundMusicPlaying() {
        Clip clip = audioAdapter.getClip();
        return clip != null && clip.isRunning();
    }

    private final Question[][] questions = QuestionBank.canonical();

    private void applyWindowIcon() {
        // Swing does not inherit the jpackage launcher icon, so set the window/taskbar icon
        // explicitly from the bundled logo at several sizes for crisp rendering.
        BufferedImage logo = load("logo.png");
        if (logo == null) return;
        java.util.List<Image> icons = new ArrayList<>();
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        for (int s : sizes) {
            icons.add(scaleToSquare(logo, s));
        }
        setIconImages(icons);
    }

    private static BufferedImage scaleToSquare(BufferedImage src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double ratio = Math.min((double) size / src.getWidth(), (double) size / src.getHeight());
        int w = Math.max(1, (int) Math.round(src.getWidth() * ratio));
        int h = Math.max(1, (int) Math.round(src.getHeight() * ratio));
        g.drawImage(src, (size - w) / 2, (size - h) / 2, w, h, null);
        g.dispose();
        return out;
    }

    public CodeEscapeApp() {
        setTitle("Code Escape - Gamified Programming Learning System");
        applyWindowIcon();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        // Start at a size that fits the current screen. The layouts can also
        // resize with the window instead of requiring a large PC monitor.
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int startWidth = Math.min(1280, Math.max(360, screenSize.width - 40));
        int startHeight = Math.min(800, Math.max(520, screenSize.height - 80));
        setMinimumSize(new Dimension(360, 520));
        setPreferredSize(new Dimension(startWidth, startHeight));
        setSize(startWidth, startHeight);
        if (screenSize.width >= 1100 && screenSize.height >= 700) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            setLocationRelativeTo(null);
        }
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                root.revalidate();
                root.repaint();
            }
        });

        loadPersistenceIntoCache();
        ensureDemoAccount();
        applySettings();

        buildScreens();
        showScreen("LOADING");
        startInitialLoading();
        startBackgroundMusic();
    }

    private void buildScreens() {
    root.add(welcomeScreen(), "WELCOME");
    root.add(loadingScreen(), "LOADING");
    root.add(loginScreen(), "LOGIN");
    root.add(registerScreen(), "REGISTER");
    root.add(characterScreen(), "CHARACTER");

    // ================= STORY SCREENS =================
    root.add(storyIntroScreen(), "STORY_INTRO");
    root.add(storyLevel2Screen(), "STORY_LEVEL2");
    root.add(storyLevel3Screen(), "STORY_LEVEL3");
    root.add(storyLevel4Screen(), "STORY_LEVEL4");
    root.add(storyLevel5Screen(), "STORY_LEVEL5");
    // Ending screen is built when entered, using the player's final selection.

    // ================= MAIN SCREENS =================
    root.add(mainMenuScreen(), "MAIN_MENU");
    root.add(dashboardScreen(), "DASHBOARD");
    root.add(questionScreen(), "QUESTION");
    root.add(profileScreen(), "PROFILE");
    root.add(completeScreen(), "COMPLETE");
    root.add(gameOverScreen(), "GAMEOVER");
    root.add(logoutScreen(), "LOGOUT");

    setContentPane(root);
    installDemoCheatHotkey();
}

    private void showScreen(String name) {
        if ("ENDING".equals(name)) {
            if (endingCard != null) root.remove(endingCard);
            endingCard = endingScreen();
            root.add(endingCard, "ENDING");
            root.revalidate();
            root.repaint();
        }
        // Keep a different soundtrack for the menu, each lesson area, and the final scene.
        if ("WELCOME".equals(name) || "REGISTER".equals(name)
                || "CHARACTER".equals(name) || "DASHBOARD".equals(name)) {
            playMainMenuMusic();
        } else if ("LOGIN".equals(name) || "PROFILE".equals(name)) {
            playLevelMusic(5);
        } else if ("STORY_INTRO".equals(name)) {
            playSpecialMusic(codeescape.core.content.AudioCatalog.TRACK_DUNGEON);
        } else if ("STORY_LEVEL2".equals(name)) {
            playLevelMusic(2);
        } else if ("STORY_LEVEL3".equals(name)) {
            playLevelMusic(3);
        } else if ("STORY_LEVEL4".equals(name)) {
            playLevelMusic(4);
        } else if ("STORY_LEVEL5".equals(name) || "ENDING".equals(name)) {
            playLevelMusic(5);
        } else if ("COMPLETE".equals(name)) {
            playSpecialMusic(codeescape.core.content.AudioCatalog.TRACK_VICTORY);
        }

        if ("GAMEOVER".equals(name)) {
            // The Game Over screen is created when the app starts, so its
            // original XP text would otherwise stay at 0. Refresh it with
            // the XP currently saved for the player.
            if (gameOverXpLabel != null) {
                gameOverXpLabel.setText("Final XP: " + session().xp);
            }
        }
        if ("COMPLETE".equals(name)) {
            if (completeXpLabel != null) completeXpLabel.setText("Final XP: " + session().xp);
            refreshProfile();
            refreshDashboard();
        }
        if ("MAIN_MENU".equals(name)) {
            refreshMainMenuXp();
        }
        cards.show(root, name);
    }

    // ==================== WELCOME ====================
    private JPanel welcomeScreen() {
        return codeescape.desktop.ui.screens.WelcomeScreen.create(new codeescape.desktop.ui.screens.WelcomeScreen.Ui() {
            @Override public JPanel background(String name) { return new BackgroundPanel(name); }
            @Override public JPanel panel(int w, int h) { return CodeEscapeApp.this.panel(w, h); }
            @Override public JLabel image(String name, int w, int h) { return CodeEscapeApp.this.image(name, w, h); }
            @Override public void addCenter(JPanel p, String text, int size, Color c) { CodeEscapeApp.this.addCenter(p, text, size, c); }
            @Override public JLabel label(String text, int size, Color c) { return CodeEscapeApp.this.label(text, size, c); }
            @Override public JButton gold(String text) { return CodeEscapeApp.this.gold(text); }
            @Override public JButton blue(String text) { return CodeEscapeApp.this.blue(text); }
            @Override public Color cream() { return CREAM; }
        }, this::showScreen);
    }

    // ==================== LOADING ====================
    private JPanel loadingScreen() {
        return codeescape.desktop.ui.screens.LoadingScreen.create(() -> {
            BackgroundPanel p = new BackgroundPanel("loading.png");
            p.setLayout(new BorderLayout());
            return p;
        });
    }

    private void startInitialLoading() {
        // Startup -> login transition decided by the core flow contract.
        uiScheduler.once(GameConfig.LOADING_DELAY_MS,
                () -> dispatch(codeescape.core.flow.FlowEvent.of(codeescape.core.flow.FlowEvent.Type.STARTUP_COMPLETE)));
    }

    private void showLoadingThenCharacter() {
        showScreen("LOADING");
        uiScheduler.once(GameConfig.LOADING_DELAY_MS, () -> {
            refreshDashboard();
            refreshProfile();
            flowNavigator.navigate(codeescape.core.flow.FlowState.STORY_INTRO);
        });
    }

    // ==================== LOGIN ====================
    private JPanel loginScreen() {
        return codeescape.desktop.ui.screens.LoginScreen.create(new codeescape.desktop.ui.screens.LoginScreen.Host() {
            @Override public JPanel background(String name) { return new BackgroundPanel(name); }
            @Override public JPanel panel(int w, int h) { return CodeEscapeApp.this.panel(w, h); }
            @Override public void title(JPanel box, String text) { CodeEscapeApp.this.title(box, text); }
            @Override public JLabel image(String name, int w, int h) { return CodeEscapeApp.this.image(name, w, h); }
            @Override public JTextField textField(String tip) { return CodeEscapeApp.this.textField(tip); }
            @Override public JPasswordField passwordField(String tip) { return CodeEscapeApp.this.passwordField(tip); }
            @Override public JButton gold(String text) { return CodeEscapeApp.this.gold(text); }
            @Override public JButton link(String text) { return CodeEscapeApp.this.link(text); }
            @Override public JButton smallBlue(String text) { return CodeEscapeApp.this.smallBlue(text); }
            @Override public JLabel label(String text, int size, Color c) { return CodeEscapeApp.this.label(text, size, c); }
            @Override public Color cream() { return CREAM; }
            @Override public void onLogin(String u, String pw, Runnable onSuccess, java.util.function.Consumer<String> onError) {
                loadPersistenceIntoCache();
                if (profileRepository.authenticate(u, pw)) {
                    username = u;
                    PlayerProfile p = profileRepository.findByUsername(u).orElse(new PlayerProfile(u, emails.getOrDefault(u, "")));
                    savedXp = profileRepository.getLifetimeXp(u);
                    email = p.email == null ? emails.getOrDefault(u, "") : p.email;
                    session().username = u;
                    lessonEngine.cancelRun(savedXp); // fresh run state, display 0 until committed/new lesson context
                    selectedCharacter = p.character == null ? savedCharacters.getOrDefault(u, "Boy") : p.character;
                    selectedOutfit = Math.max(0, Math.min(3, p.outfitVariant));
                    selectedTop = p.selectedTop; selectedBottom = p.selectedBottom; selectedShoes = p.selectedShoes; selectedAccessory = p.selectedAccessory;
                    scores.put(u, savedXp);
                    showLoadingThenCharacter();
                    onSuccess.run();
                } else {
                    onError.accept("Invalid username or password.");
                }
            }
            @Override public void showScreen(String name) { CodeEscapeApp.this.showScreen(name); }
        });
    }
    // ==================== REGISTER ====================
    private JPanel registerScreen() {
        return codeescape.desktop.ui.screens.RegisterScreen.create(new codeescape.desktop.ui.screens.RegisterScreen.Host() {
            @Override public JPanel background(String name) { return new BackgroundPanel(name); }
            @Override public JPanel panel(int w, int h) { return CodeEscapeApp.this.panel(w, h); }
            @Override public void title(JPanel box, String text) { CodeEscapeApp.this.title(box, text); }
            @Override public JLabel image(String name, int w, int h) { return CodeEscapeApp.this.image(name, w, h); }
            @Override public JTextField textField(String tip) { return CodeEscapeApp.this.textField(tip); }
            @Override public JPasswordField passwordField(String tip) { return CodeEscapeApp.this.passwordField(tip); }
            @Override public JButton gold(String text) { return CodeEscapeApp.this.gold(text); }
            @Override public JButton smallBlue(String text) { return CodeEscapeApp.this.smallBlue(text); }
            @Override public void onRegister(String u, String em, String pw, String cpw, Runnable onSuccess, java.util.function.Consumer<String> onError) {
                loadPersistenceIntoCache();
                if (u.isEmpty() || em.isEmpty() || pw.isEmpty()) { onError.accept("Please complete all fields."); return; }
                if (!pw.equals(cpw)) { onError.accept("Passwords do not match."); return; }
                if (accounts.containsKey(u) || profileRepository.findByUsername(u).isPresent()) { onError.accept("Username already exists."); return; }
                PlayerProfile p = new PlayerProfile(u, em);
                p.character = "Boy"; p.outfitVariant = 0; p.selectedTop = 1; p.selectedBottom = 1; p.selectedShoes = 1; p.selectedAccessory = 1;
                p.lifetimeXp = 0; p.savedXp = 0;
                profileRepository.saveAccount(u, em, pw, p);
                loadPersistenceIntoCache();
                username = u; email = em; savedXp = 0;
                session().username = u;
                lessonEngine.startNewLesson(savedXp);
                session().showZeroXpAfterCancelledRun = false; // new account/session baseline
                selectedCharacter = "Boy"; selectedOutfit = 0; selectedTop = 1; selectedBottom = 1; selectedShoes = 1; selectedAccessory = 1;
                showLoadingThenCharacter();
                onSuccess.run();
            }
            @Override public void showScreen(String name) { CodeEscapeApp.this.showScreen(name); }
        });
    }
    // ==================== CHARACTER ====================

    private JPanel characterScreen() {
        return codeescape.desktop.ui.screens.CharacterScreen.create(new codeescape.desktop.ui.screens.CharacterScreen.Host() {
            @Override public java.awt.image.BufferedImage load(String name) { return CodeEscapeApp.this.load(name); }
            @Override public java.awt.Image scale(java.awt.image.BufferedImage img, int w, int h) { return CodeEscapeApp.this.scale(img, w, h); }
            @Override public java.awt.Image scaleCharacter(java.awt.image.BufferedImage img, int w, int h) { return CodeEscapeApp.this.scaleCharacter(img, w, h); }
            @Override public JLabel label(String text, int size, Color c) { return CodeEscapeApp.this.label(text, size, c); }
            @Override public JButton smallBlue(String text) { return CodeEscapeApp.this.smallBlue(text); }
            @Override public JButton gold(String text) { return CodeEscapeApp.this.gold(text); }
            @Override public Color pink() { return PINK; }
            @Override public Color blue() { return BLUE; }
            @Override public Color goldColor() { return GOLD; }
            @Override public Color cyan() { return CYAN; }
            @Override public String getSelectedCharacter() { return selectedCharacter; }
            @Override public void setSelectedCharacter(String c) { selectedCharacter = c; }
            @Override public int getSelectedOutfit() { return selectedOutfit; }
            @Override public void setSelectedOutfit(int o) { selectedOutfit = o; }
            @Override public String frontFacingFile(String character) { return CodeEscapeApp.this.frontFacingFile(character); }
            @Override public String characterVariantFile(String character, int variant) { return CodeEscapeApp.this.characterVariantFile(character, variant); }
            @Override public void onSaveAndContinue() {
                // Character/style selection is persisted through the repository; the cache
                // is re-synced from it right after save (single source of ownership).
                if (username != null && !username.isEmpty()) {
                    profileRepository.saveProfile(snapshotCurrentProfile());
                    loadPersistenceIntoCache();
                }
                refreshDashboard();
                refreshProfile();
                flowNavigator.navigate(codeescape.core.flow.FlowState.DASHBOARD);
            }
            @Override public void refreshCharacterPreview(JLabel preview, JLabel topTitle) { CodeEscapeApp.this.refreshCharacterPreview(preview, topTitle); }
            @Override public void refreshVariantButtons(JButton[] buttons) { CodeEscapeApp.this.refreshVariantButtons(buttons); }
            @Override public void refreshVariantImages(JButton[] buttons) { CodeEscapeApp.this.refreshVariantImages(buttons); }
            @Override public JButton characterButton(String img, String txt) { return CodeEscapeApp.this.characterButton(img, txt); }
        });
    }

    private void refreshVariantImages(JButton[] buttons) {
        for (int i = 0; i < buttons.length; i++) {
            setCharacterButtonImage(buttons[i], characterVariantFile(selectedCharacter, i));
        }
    }

    private void refreshVariantButtons(JButton[] buttons) {
        refreshVariantImages(buttons);
        for (int i = 0; i < buttons.length; i++) {
            selectBorder(buttons[i], i == selectedOutfit, GOLD);
        }
    }

    private String characterVariantFile(String character, int variant) {
        // Selection cards must show the character facing the player, not a
        // side-facing running frame. The matching walk sheet uses the same
        // gender and variant, so the chosen look is preserved in the ending.
        String g = "Girl".equalsIgnoreCase(character) ? "girl" : "boy";
        int v = Math.max(1, Math.min(4, variant + 1));
        
        // Use the transparent variant artwork for selection cards. The old
        // *_standing_vN files have a baked-in white rectangle behind the art.
        String modern = "modern_" + g + "_variant" + v + ".png";
        if (load(modern) != null) return modern;
        String standing = "modern_" + g + "_standing_v" + v + ".png";
        return load(standing) != null ? standing : g + ".png";
    }

    private String frontFacingFile(String character) {
        // Prefer the front-facing pose for selection, menus and the final pause.
        // The running sheet remains tied to this exact selected variant.
        String gender = "Girl".equalsIgnoreCase(character) ? "girl" : "boy";
        int variant = Math.max(1, Math.min(4, selectedOutfit + 1));
        
        // The variant PNGs have transparent backgrounds. Do not prioritize the
        // legacy standing files because those contain a baked-in white canvas.
        String variantFile = "modern_" + gender + "_variant" + variant + ".png";
        if (load(variantFile) != null) return variantFile;
        String standingFile = "modern_" + gender + "_standing_v" + variant + ".png";
        return load(standingFile) != null ? standingFile : (gender + ".png");
    }

    private void refreshCharacterPreview(JLabel preview,JLabel topTitle){
        topTitle.setForeground(selectedCharacter.equals("Girl")?PINK:BLUE);
        BufferedImage img=load(frontFacingFile(selectedCharacter));
        if(img!=null){
            Container parent = preview.getParent();
            int pw = parent == null ? 190 : Math.max(80, parent.getWidth() - 24);
            int ph = parent == null ? 300 : Math.max(100, parent.getHeight() - 60);
            preview.setIcon(new ImageIcon(scaleCharacter(img, Math.min(pw, 175), Math.min(ph, 315))));
            preview.setText("");
        }
        if(equippedTopLabel!=null){
            equippedTopLabel.setIcon(new ImageIcon(scale(load(new String[]{"hoodie.png","tshirt.png","long_sleeve.png"}[selectedTop-1]),70,52)));
            equippedBottomLabel.setIcon(new ImageIcon(scale(load(new String[]{"shorts.png","jeans.png","baggy_pants.png"}[selectedBottom-1]),70,52)));
            equippedShoesLabel.setIcon(new ImageIcon(scale(load(new String[]{"shoes.png","boots.png","slippers.png"}[selectedShoes-1]),70,52)));
            equippedAccessoryLabel.setIcon(new ImageIcon(scale(load(new String[]{"cap.png","headband.png","glasses.png"}[selectedAccessory-1]),70,52)));
        }
        Container c=preview.getParent(); while(c!=null){for(Component child:c.getComponents()) if(child instanceof JLabel && "styleSummary".equals(child.getName())) ((JLabel)child).setText(styleName(0,selectedTop)+" + "+styleName(3,selectedAccessory)+" + "+styleName(1,selectedBottom)+" + "+styleName(2,selectedShoes)); c.revalidate();c.repaint();c=c.getParent();}
    }

    private String styleName(int slot,int v){
        if(slot==0)return new String[]{"Hoodie","T-Shirt","Long Sleeve"}[v-1];
        if(slot==1)return new String[]{"Shorts","Jeans","Baggy Pants"}[v-1];
        if(slot==2)return new String[]{"Shoes","Boots","Slippers"}[v-1];
        return new String[]{"Cap","Headband","Glasses"}[v-1];
    }

    private String runnerOutfitFile(String character, int variantIndex) {
        // Select the matching color variant's own running sheet. Each sheet has
        // four authored poses for exactly one character; frames are isolated
        // before drawing so neighboring poses can never leak into the runner.
        String gender = "Girl".equalsIgnoreCase(character) ? "girl" : "boy";
        int variant = Math.max(1, Math.min(4, variantIndex + 1));
        String runningSheet = "modern_" + gender + "_run_v" + variant + ".png";
        if (load(runningSheet) != null) return runningSheet;
        String generic = "modern_" + gender + "_walk.png";
        return load(generic) != null ? generic : characterVariantFile(character, variantIndex);
    }

    private String outfitFile(String character, int variantIndex) {
        return characterVariantFile(character, variantIndex);
    }

    private void setCharacterButtonImage(JButton b, String file) {
        if (b.getComponentCount() > 0 && b.getComponent(0) instanceof JLabel) {
            JLabel im = (JLabel)b.getComponent(0);
            BufferedImage img = load(file);
            if (img != null) {
                // Keep the PNG alpha channel intact; an opaque label can paint a
                // rectangular backing over transparent character pixels.
                im.setOpaque(false);
                im.setBackground(new Color(0, 0, 0, 0));
                im.setIcon(new ImageIcon(scaleCharacter(img, 105, 180)));
            }
            im.setText("");
        }
    }

    private JButton characterButton(String img, String txt) {
        JButton b = new JButton();
        b.setEnabled(true);
        b.setLayout(new BorderLayout());
        // Transparent card surface lets the scene artwork show through; the
        // character PNG itself must never sit on a white rectangle.
        b.setBackground(new Color(0, 0, 0, 0));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        // Restore the clean white card outline from the character-selection design.
        // The scenic background remains visible through the card interior.
        b.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        JLabel im = image(img, 150, 180);
        im.setHorizontalAlignment(SwingConstants.CENTER);
        // Character artwork is RGBA: let the card's white surface show through
        // instead of drawing a second background behind the PNG.
        im.setOpaque(false);
        im.setBackground(new Color(0, 0, 0, 0));
        b.add(im, BorderLayout.CENTER);
        JLabel t = label(txt, 16, CREAM);
        t.setOpaque(true);
        t.setBackground(new Color(4, 35, 79));
        t.setForeground(CREAM);
        t.setHorizontalAlignment(SwingConstants.CENTER);
        t.setBorder(new EmptyBorder(8, 0, 10, 0));
        b.add(t, BorderLayout.SOUTH);
        return b;
    }

    private void selectBorder(JButton b, boolean selected, Color color) {
        // Unselected cards retain the original white frame; the active card is gold.
        b.setBorder(BorderFactory.createLineBorder(selected ? color : Color.WHITE, selected ? 5 : 2));
    }

    // ==================== MAIN MENU ====================
    private JPanel mainMenuScreen() {
        BackgroundPanel p = new BackgroundPanel("castle.png");
        p.setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(23, 61, 98, 242));
        top.setBorder(new EmptyBorder(8, 18, 8, 18));

        JLabel logo = image("logo.png", 105, 58);
        top.add(logo, BorderLayout.WEST);

        JLabel welcome = label("WELCOME, " + (username.isEmpty() ? "PLAYER" : username).toUpperCase(), 16, GOLD);
        welcome.setHorizontalAlignment(SwingConstants.CENTER);
        top.add(welcome, BorderLayout.CENTER);

        JLabel musicStatus = label(musicStatusText(), 12, musicStatusColor());
        musicStatus.setHorizontalAlignment(SwingConstants.RIGHT);
        top.add(musicStatus, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        JPanel menu = panel(360, 610);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));

        JLabel title = label("MAIN MENU", 38, GOLD);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        menu.add(Box.createVerticalStrut(8));
        menu.add(title);
        menu.add(Box.createVerticalStrut(8));

        mainMenuXpLabel = label("★  XP: " + (session().showZeroXpAfterCancelledRun ? 0 : savedXp), 19, GOLD);
        mainMenuXpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        menu.add(mainMenuXpLabel);
        menu.add(Box.createVerticalStrut(8));

        JLabel subtitle = label("READY TO ESCAPE?", 17, CREAM);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        menu.add(subtitle);
        menu.add(Box.createVerticalStrut(18));

        // Keep the selected color variant, using its front-facing still image.
        JPanel avatarFrame = new JPanel(new BorderLayout());
        avatarFrame.setBackground(new Color(5, 27, 61, 220));
        avatarFrame.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(83, 190, 255), 2),
                new EmptyBorder(8, 12, 8, 12)));
        avatarFrame.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarFrame.add(image(frontFacingFile(selectedCharacter), 145, 205), BorderLayout.CENTER);
        menu.add(avatarFrame);
        menu.add(Box.createVerticalStrut(18));

        JButton play = menuGameButton("▶  PLAY", GOLD);
        play.addActionListener(e -> {
            refreshDashboard();
            startNewLesson();
        });
        menu.add(play);
        menu.add(Box.createVerticalStrut(12));

        JButton options = menuGameButton("⚙  SETTINGS", BLUE);
        options.addActionListener(e -> showSettingsDialog(musicStatus));
        menu.add(options);
        menu.add(Box.createVerticalStrut(12));

        JButton credits = menuGameButton("ℹ  CREDITS", CYAN);
        credits.addActionListener(e -> showCreditsDialog());
        menu.add(credits);
        menu.add(Box.createVerticalStrut(12));

        JButton exit = menuGameButton("✕  EXIT", RED);
        exit.addActionListener(e -> {
            stopBackgroundMusic();
            System.exit(0);
        });
        menu.add(exit);

        center.add(menu);
        p.add(center, BorderLayout.CENTER);

        return p;
    }

    private void refreshMainMenuXp() {
        syncSavedXpFromRepository();
        if (mainMenuXpLabel != null) mainMenuXpLabel.setText("★  XP: " + (session().showZeroXpAfterCancelledRun ? 0 : savedXp));
    }

    private void syncSavedXpFromRepository() {
        try {
            String u = session() != null ? session().username : username;
            if (u == null || u.trim().isEmpty()) return;
            int repoXp = profileRepository.getLifetimeXp(u);
            savedXp = Math.max(0, repoXp);
            scores.put(u, savedXp);
        } catch (Exception ignored) {}
    }

    private JButton menuGameButton(String text, Color accent) {
        JButton b = base(text);
        b.setPreferredSize(new Dimension(310, 52));
        b.setMaximumSize(new Dimension(310, 52));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setFont(new Font("SansSerif", Font.BOLD, 17));
        if (accent == GOLD) {
            b.setBackground(GOLD);
            b.setForeground(new Color(25, 42, 62));
        } else {
            b.setBackground(new Color(72, 150, 218));
            b.setForeground(Color.WHITE);
        }
        b.setBorder(new RoundedBorder(accent, 2, 17));
        return b;
    }

    private void showSettingsDialog(JLabel musicStatus) {
        JDialog dialog = new JDialog(this, "SETTINGS", true);
        dialog.setSize(380, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel box = new JPanel();
        box.setBackground(NAVY);
        box.setBorder(new EmptyBorder(22, 30, 22, 30));
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel title = label("SETTINGS", 30, GOLD);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(title);
        box.add(Box.createVerticalStrut(14));

        JCheckBox music = new JCheckBox("Background Music");
        music.setSelected(settings.isMusicEnabled());
        music.setForeground(Color.WHITE);
        music.setBackground(NAVY);
        music.setFont(new Font("SansSerif", Font.BOLD, 16));
        music.setAlignmentX(Component.CENTER_ALIGNMENT);

        JCheckBox mute = new JCheckBox("Mute All Sound");
        mute.setSelected(settings.isMuted());
        mute.setForeground(Color.WHITE);
        mute.setBackground(NAVY);
        mute.setFont(new Font("SansSerif", Font.BOLD, 16));
        mute.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSlider volume = new JSlider(0, 100, Math.round(settings.getVolume() * 100));
        volume.setOpaque(false);
        volume.setAlignmentX(Component.CENTER_ALIGNMENT);
        volume.setMaximumSize(new Dimension(280, 45));

        JLabel volumeText = label("Volume: " + volume.getValue() + "%", 15, CREAM);
        volumeText.setAlignmentX(Component.CENTER_ALIGNMENT);

        JCheckBox tts = new JCheckBox("Text-to-Speech Narration");
        tts.setSelected(settings.isTtsEnabled());
        tts.setForeground(Color.WHITE);
        tts.setBackground(NAVY);
        tts.setFont(new Font("SansSerif", Font.BOLD, 16));
        tts.setAlignmentX(Component.CENTER_ALIGNMENT);

        music.addActionListener(e -> {
            settings.setMusicEnabled(music.isSelected());
            applySettings();
            if (music.isSelected() && !settings.isMuted()) startBackgroundMusic();
            else stopBackgroundMusic();
            persistSettings();
            updateMusicStatus(musicStatus);
        });

        mute.addActionListener(e -> {
            settings.setMuted(mute.isSelected());
            applySettings();
            if (mute.isSelected()) stopBackgroundMusic();
            else if (settings.isMusicEnabled()) startBackgroundMusic();
            persistSettings();
            updateMusicStatus(musicStatus);
        });

        volume.addChangeListener(e -> {
            settings.setVolume(volume.getValue() / 100f);
            audioAdapter.setVolume(settings.getVolume());
            volumeText.setText("Volume: " + volume.getValue() + "%");
            persistSettings();
        });

        tts.addActionListener(e -> {
            settings.setTtsEnabled(tts.isSelected());
            narrationPort.setEnabled(tts.isSelected());
            if (!tts.isSelected()) narrationPort.stop();
            persistSettings();
        });

        box.add(music);
        box.add(Box.createVerticalStrut(8));
        box.add(mute);
        box.add(Box.createVerticalStrut(12));
        box.add(volumeText);
        box.add(volume);
        box.add(Box.createVerticalStrut(12));
        box.add(tts);
        box.add(Box.createVerticalStrut(16));

        JButton done = gold("DONE");
        done.setAlignmentX(Component.CENTER_ALIGNMENT);
        done.addActionListener(e -> dialog.dispose());
        box.add(done);

        dialog.setContentPane(box);
        dialog.setVisible(true);
    }

    private void updateMusicStatus(JLabel musicStatus) {
        if (musicStatus == null) return;
        musicStatus.setText(musicStatusText());
        musicStatus.setForeground(musicStatusColor());
    }

    private void showCreditsDialog() {
        JDialog dialog = new JDialog(this, "CREDITS", true);
        dialog.setSize(350, 390);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel box = new JPanel();
        box.setBackground(NAVY);
        box.setBorder(new EmptyBorder(25, 35, 25, 35));
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel title = label("CODE ESCAPE", 32, GOLD);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(title);
        box.add(Box.createVerticalStrut(8));

        JLabel sub = label("Gamified Programming Learning System", 16, CREAM);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(sub);
        box.add(Box.createVerticalStrut(22));

        JLabel credits = new JLabel("<html><div style='text-align:center;'>"
                + "<b>Developed by the Code Escape Team</b><br><br>"
                + "Programming • UI • Game Logic • Documentation<br>"
                + "HTML • CSS • JavaScript • Java • Programming Logic"
                + "</div></html>");
        credits.setFont(new Font("SansSerif", Font.PLAIN, 16));
        credits.setForeground(CREAM);
        credits.setAlignmentX(Component.CENTER_ALIGNMENT);
        credits.setHorizontalAlignment(SwingConstants.CENTER);
        box.add(credits);
        box.add(Box.createVerticalStrut(22));

        JButton done = gold("BACK");
        done.setAlignmentX(Component.CENTER_ALIGNMENT);
        done.addActionListener(e -> dialog.dispose());
        box.add(done);

        dialog.setContentPane(box);
        dialog.setVisible(true);
    }

    // ==================== DASHBOARD ====================
    private JPanel dashboardScreen() {
        codeescape.desktop.ui.screens.DashboardScreen.Refs refs = new codeescape.desktop.ui.screens.DashboardScreen.Refs();
        JPanel p = codeescape.desktop.ui.screens.DashboardScreen.create(
            new codeescape.desktop.ui.screens.DashboardScreen.Host() {
                @Override public JPanel background(String name) { return new BackgroundPanel(name); }
                @Override public JLabel image(String name, int w, int h) { return CodeEscapeApp.this.image(name, w, h); }
                @Override public JLabel label(String text, int size, Color c) { return CodeEscapeApp.this.label(text, size, c); }
                @Override public JButton dashboardActionButton(String text, Color accent) { return CodeEscapeApp.this.dashboardActionButton(text, accent); }
                @Override public Color gold() { return GOLD; }
                @Override public Color cream() { return CREAM; }
                @Override public Color cyan() { return CYAN; }
                @Override public Color pink() { return PINK; }
                @Override public Color blue() { return BLUE; }
                @Override public Color red() { return RED; }
            },
            refs,
            username,
            selectedCharacter,
            selectedOutfit,
            outfitFile(selectedCharacter, selectedOutfit),
            (session().xp > 0 ? session().xp : savedXp),
            () -> startNewLesson(),
            () -> { refreshProfile(); dispatch(codeescape.core.flow.FlowEvent.of(codeescape.core.flow.FlowEvent.Type.PROFILE_OPENED)); },
            () -> { session().xp = 0; stopBackgroundMusic(); System.exit(0); }
        );
        this.dashboardAvatar = refs.dashboardAvatar;
        this.dashboardCharacterLabel = refs.dashboardCharacterLabel;
        this.dashboardWelcome = refs.dashboardWelcome;
        this.dashboardXpLabel = refs.dashboardXpLabel;
        return p;
    }

    private void refreshDashboard() {
    if (dashboardAvatar != null) {
        String file = outfitFile(selectedCharacter, selectedOutfit);

        BufferedImage im = load(file);

        if (im != null) {
            dashboardAvatar.setIcon(new ImageIcon(scale(im, 105, 250)));
            dashboardAvatar.setText("");
        }
    }

    if (dashboardCharacterLabel != null) {
        dashboardCharacterLabel.setText(selectedCharacter.toUpperCase());
        dashboardCharacterLabel.setForeground(
            selectedCharacter.equals("Girl") ? PINK : CYAN
        );
    }

    if (dashboardWelcome != null) {
        dashboardWelcome.setText(
            "WELCOME, " +
            (username.isEmpty() ? "PLAYER" : username).toUpperCase()
        );
    }

    if (dashboardXpLabel != null) {
        syncSavedXpFromRepository();
        if (session() != null) session().savedXpBeforeRun = Math.max(0, savedXp);
        int displayedXp = session().showZeroXpAfterCancelledRun ? 0 : savedXp;
        dashboardXpLabel.setText("★  XP: " + displayedXp);
        dashboardXpLabel.setForeground(GOLD);
    }

    root.revalidate();
    root.repaint();
}
    
    private void startNewLesson() {
        // Every new lesson is a fresh attempt. Previously committed XP remains
        // in savedXp and is shown in Profile.
        lessonEngine.startNewLesson();
        playLevelMusic(1);
        startQuestion();
    }
    
    private JPanel createStoryScreen(
        String titleText, String storyText, String buttonText, String background, Runnable nextAction) {
        return codeescape.desktop.ui.screens.StoryScreenBuilder.create(new codeescape.desktop.ui.screens.StoryScreenBuilder.Host() {
            @Override public JPanel background(String name) { return new BackgroundPanel(name); }
            @Override public JLabel label(String text, int size, Color c) { return CodeEscapeApp.this.label(text, size, c); }
            @Override public JButton blue(String text) { return CodeEscapeApp.this.blue(text); }
            @Override public JButton gold(String text) { return CodeEscapeApp.this.gold(text); }
            @Override public Color goldColor() { return GOLD; }
            @Override public int getWidthSafe() { return getWidth() > 0 ? getWidth() : 430; }
            @Override public void speakStoryKaraoke(String text, JLabel label) { CodeEscapeApp.this.speakStoryKaraoke(text, label); }
            @Override public void stopStorySpeech() { CodeEscapeApp.this.stopStorySpeech(); }
            @Override public void setActiveStory(JLabel story) { activeStoryLabel = story; }
        }, titleText, storyText, buttonText, background, nextAction);
    }
    // ==================== STORY TEXT-TO-SPEECH / KARAOKE ====================
    // Wired through the NarrationPort contract (WindowsNarrationAdapter) with UI callbacks for parity.
    // Narration process/thread/generation ownership lives in the adapter.
    private JLabel activeStoryLabel;

    public void speakStoryKaraoke(String text, JLabel label) {
        stopStorySpeech();
        activeStoryLabel = label;
        codeescape.desktop.ui.screens.StoryScreenBuilder.clearHighlight(label);

        if (!settings.isTtsEnabled()) {
            // TTS is disabled in Settings: keep the paragraph visible, just silent.
            return;
        }

        if (!narrationAdapter.isSupported()) {
            // Keep the same wrapped paragraph layout instead of swapping in a
            // narrow fixed-character HTML block that no longer fits the card.
            codeescape.desktop.ui.screens.StoryScreenBuilder.clearHighlight(label);
            return;
        }

        storyMusicWasPlaying = isBackgroundMusicPlaying();
        if (storyMusicWasPlaying) audioAdapter.stopMusic();
        // TTS only: visual highlight sync disabled to keep full paragraph stable/readable.
        narrationPort.speakStory(text, new NarrationCallback() {
            @Override public void onLine(int index, String t, java.util.List<int[]> r) { }
            @Override public void onComplete() {
                SwingUtilities.invokeLater(() -> {
                    codeescape.desktop.ui.screens.StoryScreenBuilder.clearHighlight(label);
                });
                if (storyMusicWasPlaying && audioAdapter.isEnabled()) {
                    audioAdapter.startBackgroundMusic();
                }
                storyMusicWasPlaying = false;
                activeStoryLabel = null;
            }
        });
    }

    public void stopStorySpeech(){
        narrationPort.stop();
        if (activeStoryLabel != null) {
            codeescape.desktop.ui.screens.StoryScreenBuilder.clearHighlight(activeStoryLabel);
            activeStoryLabel = null;
        }
        if (storyMusicWasPlaying && audioAdapter.isEnabled()) {
            audioAdapter.startBackgroundMusic();
        }
        storyMusicWasPlaying=false;
    }

    // ==================== STORY SCREENS ====================

    private JPanel storyIntroScreen() {
        StoryContent.StoryPage sp = StoryContent.INTRO;
        return createStoryScreen(sp.title, sp.body, sp.buttonText, sp.backgroundImage, () -> {
            refreshDashboard();
            refreshProfile();
            flowNavigator.navigate(codeescape.core.flow.FlowState.CHARACTER);
        });
    }

    private JPanel storyLevel2Screen() {
        StoryContent.StoryPage sp = StoryContent.LEVEL2;
        return createStoryScreen(sp.title, sp.body, sp.buttonText, sp.backgroundImage, () -> runToNextLevel());
    }

    private JPanel storyLevel3Screen() {
        StoryContent.StoryPage sp = StoryContent.LEVEL3;
        return createStoryScreen(sp.title, sp.body, sp.buttonText, sp.backgroundImage, () -> runToNextLevel());
    }

    private JPanel storyLevel4Screen() {
        StoryContent.StoryPage sp = StoryContent.LEVEL4;
        return createStoryScreen(sp.title, sp.body, sp.buttonText, sp.backgroundImage, () -> runToNextLevel());
    }

    private JPanel storyLevel5Screen() {
        StoryContent.StoryPage sp = StoryContent.LEVEL5;
        return createStoryScreen(sp.title, sp.body, sp.buttonText, sp.backgroundImage, () -> runToNextLevel());
    }

// ==================== ENDING ====================

private JPanel endingScreen() {
    // Capture the player's exact selection once for this scene. The animation must
    // not resolve its runner and standing pose from mutable UI selection state.
    final String endingCharacter = selectedCharacter;
    final int endingVariant = Math.max(0, Math.min(3, selectedOutfit));
    final String endingRunner = runnerOutfitFile(endingCharacter, endingVariant);
    final String endingStanding = characterVariantFile(endingCharacter, endingVariant);
    final String endingVictory = victoryPoseFile(endingCharacter, endingVariant);
    codeescape.desktop.ui.screens.EndingScreen.Result shell =
            codeescape.desktop.ui.screens.EndingScreen.create(this::load, null, GOLD);
    codeescape.desktop.ui.animation.EndingScene scene = new codeescape.desktop.ui.animation.EndingScene(
            this::load, this::loadIsolatedWalkFrames,
            endingRunner, endingStanding, endingVictory, shell.instructions,
            this::openTreasureChest,
            "VICTORY POSE NOT FOUND: " + victoryPoseFile(endingCharacter, endingVariant),
            GOLD, uiScheduler);
    shell.panel.add(scene, BorderLayout.CENTER);
    SwingUtilities.invokeLater(scene::start);
    return shell.panel;
}

/** Maps the eight authored victory poses to the exact gender and color selected by the player. */
private String victoryPoseFile(String character, int variantIndex) {
    String gender = "Girl".equalsIgnoreCase(character) ? "girl" : "boy";
    String[] colors = {"green", "blue", "purple", "orange"};
    int index = Math.max(0, Math.min(3, variantIndex));
    return "victory_" + gender + "_" + colors[index] + ".png";
}

/** Animated final scene: run, stop, jump, reveal a clickable key, fly to a new vault, open it, and celebrate. */
// EndingScene animation view migrated to codeescape.desktop.ui.animation.EndingScene
private void openTreasureChest() {
    codeescape.desktop.ui.dialogs.GameDialogs.openTreasureChest(this, this::load, img -> scale(img, 350, 390),
            t -> label(t, 22, GOLD), this::gold, GOLD,
            () -> playSpecialMusic(codeescape.core.content.AudioCatalog.TRACK_VICTORY), this::showEndingXpDialog);
}

    private void showEndingXpDialog() {
        codeescape.desktop.ui.dialogs.GameDialogs.showEndingXpDialog(this, savedXp,
                t -> label(t, 24, GOLD), this::gold, this::smallBlue, GOLD, CREAM,
                () -> { refreshDashboard(); refreshMainMenuXp(); showScreen("DASHBOARD"); },
                this::startNewLesson);
    }

    // ==================== QUESTION ====================
    private JPanel questionScreen() {
        codeescape.desktop.ui.screens.QuestionScreen.Refs refs = codeescape.desktop.ui.screens.QuestionScreen.create(
                new codeescape.desktop.ui.screens.QuestionScreen.Ui() {
                    @Override public JPanel panel(int w, int h) { return CodeEscapeApp.this.panel(w, h); }
                    @Override public JLabel label(String text, int size, Color c) { return CodeEscapeApp.this.label(text, size, c); }
                    @Override public JButton answerButton(String text) { return CodeEscapeApp.this.answerButton(text); }
                    @Override public JTextField textField(String tip) { return CodeEscapeApp.this.textField(tip); }
                    @Override public JButton gold(String text) { return CodeEscapeApp.this.gold(text); }
                    @Override public JButton base(String text) { return CodeEscapeApp.this.base(text); }
                    @Override public Color gold() { return GOLD; }
                    @Override public Color cream() { return CREAM; }
                    @Override public Color cyan() { return CYAN; }
                    @Override public Color red() { return RED; }
                    @Override public java.util.function.Function<String, java.awt.image.BufferedImage> loader() { return CodeEscapeApp.this::load; }
                    @Override public String heartsText() { return CodeEscapeApp.this.heartsText(); }
                    @Override public int xp() { return CodeEscapeApp.this.session().xp; }
                    @Override public int secondsLeft() { return secondsLeft; }
                },
                this::answer, this::answerSyntax, this::showGameExitDialog);
        questionBackground = refs.questionBackground;
        questionCounterLabel = refs.questionCounterLabel;
        questionLabel = refs.questionLabel;
        answerGrid = refs.answerGrid;
        answerButtons = refs.answerButtons;
        syntaxPanel = refs.syntaxPanel;
        syntaxAnswerField = refs.syntaxAnswerField;
        syntaxSubmitButton = refs.syntaxSubmitButton;
        heartsLabel = refs.hud.heartsLabel;
        timerLabel = refs.hud.timerLabel;
        xpLabel = refs.hud.xpLabel;
        questionHudRefs = refs.hud;
        questionScreenRefs = refs;
        questionHudController = new codeescape.desktop.ui.question.QuestionHudController(questionHudRefs, this::handleTimeout, uiScheduler);
        questionHudController.setAcceptingAnswer(acceptingAnswer);
        questionHudController.setSecondsLeft(secondsLeft);
        return questionBackground;
    }

    private void stopQuestionTimer() {
        if (questionHudController != null) questionHudController.stop();
    }
    /** Central accepting-answer state: keeps the HUD controller and legacy field in sync. */
    private void setAcceptingAnswer(boolean value) {
        acceptingAnswer = value;
        if (questionHudController != null) questionHudController.setAcceptingAnswer(value);
    }

    /** Central countdown state: keeps the HUD controller and legacy field in sync. */
    private void setSecondsLeft(int value) {
        secondsLeft = value;
        if (questionHudController != null) questionHudController.setSecondsLeft(value);
    }

    /** Current countdown value, preferring the HUD controller when it owns the timer. */
    private int currentSecondsLeft() {
        return questionHudController != null ? questionHudController.getSecondsLeft() : secondsLeft;
    }

    /** Enables/disables all question inputs through the extracted screen helper. */
    private void setAnswerInputsEnabled(boolean enabled) {
        codeescape.desktop.ui.screens.QuestionScreen.setAnswerInputsEnabled(questionScreenRefs, enabled);
    }

    private void showGameExitDialog() {
        stopQuestionTimer();
        setAcceptingAnswer(false);
        codeescape.desktop.ui.dialogs.GameDialogs.showGameExitDialog(this, GOLD, CREAM, RED,
                t -> label(t, 28, GOLD),
                this::gold,
                this::base,
                () -> currentSecondsLeft(),
                () -> { setAcceptingAnswer(true); if (currentSecondsLeft() > 0) restartTimerWithRemainingTime(); },
                () -> { cancelUnfinishedAttempt(); goDashboard(); }
        );
    }

    private void cancelUnfinishedAttempt() {
        stopQuestionTimer();
        stopStorySpeech();
        setAcceptingAnswer(false);
        lessonEngine.cancelRun(savedXp);
        setSecondsLeft(GameConfig.MCQ_SECONDS);
        refreshDashboard();
    }

    private String heartsText() {
        return (session().hearts >= 1 ? "♥" : "♡") + " " + (session().hearts >= 2 ? "♥" : "♡") + " " + (session().hearts >= 3 ? "♥" : "♡");
    }

    private void restartTimerWithRemainingTime() {
        stopQuestionTimer();
        if (questionHudController != null) {
            questionHudController.setAcceptingAnswer(acceptingAnswer);
            questionHudController.restart(currentSecondsLeft(), GameConfig.QUESTION_TICK_MS,
                    this::syncQuestionTimerUiFromController,
                    () -> { syncQuestionTimerUiFromController(); handleTimeout(); });
            return;
        }
        // Defensive fallback if screen/controller not yet initialized.
        if (timerLabel != null) {
            timerLabel.setText(timerLabelText());
            timerLabel.setForeground(timerColor());
        }
    }

    private void startQuestion() {
        session().level = Math.max(1, Math.min(GameConfig.LEVEL_COUNT, session().level));
        session().questionIndex = Math.max(0, Math.min(GameConfig.QUESTIONS_PER_LEVEL - 1, session().questionIndex));
        questionBackground.setBackgroundImage(LevelMetadata.of(session().level).backgroundImage);
        questionBackground.revalidate(); questionBackground.repaint();
        showScreen("QUESTION");
        loadQuestion();
    }

    private void loadQuestion() {
        Question q = lessonEngine.currentQuestion();
        session().level = lessonEngine.getLevel();
        session().questionIndex = lessonEngine.getQuestionIndex();
        setAcceptingAnswer(true);
        setSecondsLeft(lessonEngine.questionTimeSeconds());
        boolean syntax = q.syntax;
        String counter = codeescape.desktop.ui.screens.QuestionScreen.formatCounter(
                session().level, session().questionIndex, GameConfig.QUESTIONS_PER_LEVEL);
        String questionHtml = codeescape.desktop.ui.screens.QuestionScreen.formatQuestionHtml(q.text);
        codeescape.desktop.ui.screens.QuestionScreen.Refs refs = questionScreenRefs;
        if (refs == null) {
            // Defensive fallback: rebuild refs from legacy fields (pre-extraction parity path).
            refs = new codeescape.desktop.ui.screens.QuestionScreen.Refs();
            refs.questionBackground = questionBackground;
            refs.questionCounterLabel = questionCounterLabel;
            refs.questionLabel = questionLabel;
            refs.answerGrid = answerGrid;
            refs.answerButtons = answerButtons;
            refs.syntaxPanel = syntaxPanel;
            refs.syntaxAnswerField = syntaxAnswerField;
            refs.syntaxSubmitButton = syntaxSubmitButton;
            refs.hud = questionHudRefs;
        }
        codeescape.desktop.ui.screens.QuestionScreen.renderQuestion(refs, counter, questionHtml, syntax, q.answers, null);

        updateHud();
        restartTimer();
    }

    private void restartTimer() {
        stopQuestionTimer();
        int startSeconds = lessonEngine.questionTimeSeconds();
        setSecondsLeft(startSeconds);
        if (questionHudController != null) {
            questionHudController.setAcceptingAnswer(acceptingAnswer);
            questionHudController.restart(startSeconds, GameConfig.QUESTION_TICK_MS,
                    this::syncQuestionTimerUiFromController,
                    () -> { syncQuestionTimerUiFromController(); handleTimeout(); });
            return;
        }
        // Defensive fallback if screen/controller not yet initialized.
        if (timerLabel != null) {
            timerLabel.setText(timerLabelText());
            timerLabel.setForeground(timerColor());
        }
    }

    private void syncQuestionTimerUiFromController() {
        if (questionHudController != null) {
            secondsLeft = questionHudController.getSecondsLeft();
            questionHudController.syncTimerHud(timerLabelText(), timerColor());
        } else if (timerLabel != null) {
            timerLabel.setText(timerLabelText());
            timerLabel.setForeground(timerColor());
        }
    }

    /** Formats the countdown label; prefix is the original legacy string preserved byte-for-byte. */
    private String timerLabelText() {
        return codeescape.desktop.ui.question.QuestionHudController.formatTimerLabel(secondsLeft);
    }

    private Color timerColor() {
        return codeescape.desktop.ui.question.QuestionHudController.timerColor(secondsLeft, GOLD, RED);
    }

    private void handleAnswerResult(codeescape.core.engine.LessonEngine.AnswerResult result, String heading, String big, String detail, Color accent, boolean feedbackCorrect) {
        updateHud();
        refreshDashboard();
        playFeedbackSound(feedbackCorrect);
        showGamePopup(heading, big, detail, accent);
        if (codeescape.core.flow.FlowTransitions.shouldCommitSession(result)) {
            commitSessionXp();
        }
        codeescape.core.flow.FlowState next = codeescape.core.flow.FlowTransitions.nextAfterAnswer(result, session().level);
        if (next == codeescape.core.flow.FlowState.QUESTION) {
            loadQuestion();
        } else {
            flowNavigator.navigate(next, session().level);
        }
    }

    private boolean isDemoAccountActive() {
        return username != null && "demo".equalsIgnoreCase(username.trim());
    }

    private void installDemoCheatHotkey() {
        javax.swing.JRootPane rp = getRootPane();
        if (rp == null) return;
        javax.swing.InputMap im = rp.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap am = rp.getActionMap();
        im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0), "demoCheatCompleteStage");
        am.put("demoCheatCompleteStage", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                triggerDemoCheatCompleteStage();
            }
        });
    }

    /** Demo-only testing cheat (F3): complete current stage perfectly and jump to narration/ending flow. */
    private void triggerDemoCheatCompleteStage() {
        if (!isDemoAccountActive()) return;
        if (session() == null || session().sessionCommitted) return;
        // Only meaningful during an active question stage.
        if (questionBackground == null || !questionBackground.isShowing()) return;

        stopQuestionTimer();
        setAcceptingAnswer(false);
        setAnswerInputsEnabled(false);
        if (questionHudController != null) questionHudController.setAcceptingAnswer(false);

        codeescape.core.engine.LessonEngine.AnswerResult result = lessonEngine.cheatCompleteCurrentStagePerfectly();
        updateHud();
        refreshDashboard();

        if (codeescape.core.flow.FlowTransitions.shouldCommitSession(result)) {
            commitSessionXp();
        }
        codeescape.core.flow.FlowState next = codeescape.core.flow.FlowTransitions.nextAfterAnswer(result, session().level);
        if (next == codeescape.core.flow.FlowState.QUESTION) {
            loadQuestion();
        } else {
            flowNavigator.navigate(next, session().level);
        }
    }

    private void answer(int choice) {
        if (!acceptingAnswer) return;
        setAcceptingAnswer(false);
        stopQuestionTimer();
        setAnswerInputsEnabled(false);
        codeescape.core.engine.LessonEngine.AnswerResult result = lessonEngine.submitChoice(choice);
        if (result == codeescape.core.engine.LessonEngine.AnswerResult.CORRECT || result == codeescape.core.engine.LessonEngine.AnswerResult.LEVEL_COMPLETE || result == codeescape.core.engine.LessonEngine.AnswerResult.GAME_COMPLETE) {
            handleAnswerResult(result, "CORRECT!", "+10 XP", "Great Job! You got the Correct Answer", GREEN, true);
        } else {
            handleAnswerResult(result, "NOT QUITE!", "-1 HEART", "Try again! You must perfect this question before moving to the next one.", RED, false);
        }
    }

    private void answerSyntax() {
        if (!acceptingAnswer) return;
        setAcceptingAnswer(false);
        stopQuestionTimer();
        setAnswerInputsEnabled(false);
        String typed = syntaxAnswerField == null ? "" : syntaxAnswerField.getText().trim().replaceAll("\\s+", " ");
        codeescape.core.engine.LessonEngine.AnswerResult result = lessonEngine.submitSyntax(typed);
        if (result == codeescape.core.engine.LessonEngine.AnswerResult.CORRECT || result == codeescape.core.engine.LessonEngine.AnswerResult.LEVEL_COMPLETE || result == codeescape.core.engine.LessonEngine.AnswerResult.GAME_COMPLETE) {
            handleAnswerResult(result, "CORRECT!", "+10 XP", "Great Job! You completed the syntax!", GREEN, true);
        } else {
            handleAnswerResult(result, "NOT QUITE!", "-1 HEART", "Check the syntax and try again.", RED, false);
        }
    }

    private void handleTimeout() {
        if (!acceptingAnswer) return;
        setAcceptingAnswer(false);
        setAnswerInputsEnabled(false);
        codeescape.core.engine.LessonEngine.AnswerResult result = lessonEngine.timeout();
        handleAnswerResult(result, "TIME'S UP!", "-1 HEART", "Time Ran Out. Try the same question again.", RED, false);
    }

    private void updateHud() {
        String hearts = heartsText();
        String xpText = "⭐ XP: " + session().xp;
        if (questionHudController != null && questionHudRefs != null) {
            questionHudController.setSecondsLeft(currentSecondsLeft());
            questionHudController.renderHud(hearts, xpText, GOLD, RED);
        } else {
            if (heartsLabel != null) heartsLabel.setText(hearts);
            if (xpLabel != null) xpLabel.setText(xpText);
            if (timerLabel != null) {
                timerLabel.setText(timerLabelText());
                timerLabel.setForeground(timerColor());
            }
        }
        if (dashboardXpLabel != null) dashboardXpLabel.setText("★  XP: " + (session().showZeroXpAfterCancelledRun ? 0 : savedXp));
    }


    // ==================== RUNNING ====================
    private void runToNextLevel() {
        stopQuestionTimer();

        String runnerFile = runnerOutfitFile(selectedCharacter, selectedOutfit);
        playLevelMusic(session().level);
        codeescape.desktop.ui.animation.MovingRunner runner = new codeescape.desktop.ui.animation.MovingRunner(this::load, runnerFile, uiScheduler);
        codeescape.desktop.ui.screens.RunningTransitionScreen.Result tr =
                codeescape.desktop.ui.screens.RunningTransitionScreen.create(
                        session().level, LevelMetadata.of(session().level).backgroundImage, this::load, runner, CYAN, CREAM, GOLD);
        runner.progressBar = tr.progressBar;

        root.add(tr.panel, codeescape.core.flow.FlowTransitions.screenName(codeescape.core.flow.FlowState.RUNNING_TRANSITION));
        flowNavigator.navigate(codeescape.core.flow.FlowState.RUNNING_TRANSITION, session().level);
        // Question lifecycle entry decided by the core flow contract.
        runner.onFinished = () -> dispatch(codeescape.core.flow.FlowEvent.of(codeescape.core.flow.FlowEvent.Type.LESSON_STARTED));
    }

    // ==================== PROFILE ====================
    private JPanel profileScreen() {
        codeescape.desktop.ui.screens.ProfileScreen.Result res = codeescape.desktop.ui.screens.ProfileScreen.create(
                new codeescape.desktop.ui.screens.ProfileScreen.Host() {
                    @Override public JPanel background(String name) { return new BackgroundPanel(name); }
                    @Override public JPanel panel(int w, int h) { return CodeEscapeApp.this.panel(w, h); }
                    @Override public JButton navButton(String text) { return CodeEscapeApp.this.navButton(text); }
                    @Override public void title(JPanel box, String text) { CodeEscapeApp.this.title(box, text); }
                    @Override public JLabel image(String name, int w, int h) { return CodeEscapeApp.this.image(name, w, h); }
                    @Override public JLabel label(String text, int size, Color c) { return CodeEscapeApp.this.label(text, size, c); }
                    @Override public Color gold() { return GOLD; }
                    @Override public Color cream() { return CREAM; }
                    @Override public Color cyan() { return CYAN; }
                    @Override public Color pink() { return PINK; }
                    @Override public Color peach() { return PEACH; }
                    @Override public void stopQuestionTimer() { CodeEscapeApp.this.stopQuestionTimer(); }
                    @Override public void setAcceptingAnswer(boolean accepting) { CodeEscapeApp.this.setAcceptingAnswer(accepting); }
                    @Override public void refreshDashboard() { CodeEscapeApp.this.refreshDashboard(); }
                    @Override public void showScreen(String name) { CodeEscapeApp.this.showScreen(name); }
                    @Override public void startNewLesson() { CodeEscapeApp.this.startNewLesson(); }
                },
                username, selectedCharacter, selectedOutfit,
                outfitFile(selectedCharacter, selectedOutfit),
                getLifetimeXp(username), session().correctAnswers, session().level, session().hearts
        );
        profileAvatar = res.refs.avatar;
        profilePlayerLabel = res.refs.playerLabel;
        profileCharacterLabel = res.refs.characterLabel;
        profileXpLabel = res.refs.xpLabel;
        profileCorrectLabel = res.refs.correctLabel;
        profileLevelLabel = res.refs.levelLabel;
        profileHeartsLabel = res.refs.heartsLabel;
        return res.panel;
    }
   
   private void refreshProfile() {
    if (profileAvatar != null) {
        String file = outfitFile(selectedCharacter, selectedOutfit);

        BufferedImage im = load(file);

        if (im != null) {
            profileAvatar.setIcon(
                new ImageIcon(scale(im, 145, 210))
            );
            profileAvatar.setText("");
        }
    }

    if (profilePlayerLabel != null) {
        profilePlayerLabel.setText(
            "Player: " + (username.isEmpty() ? "Guest" : username)
        );
    }

    if (profileCharacterLabel != null) {
        profileCharacterLabel.setText(
            "Character: " + selectedCharacter
        );

        profileCharacterLabel.setForeground(
            selectedCharacter.equalsIgnoreCase("Girl") ? PINK : CYAN
        );
    }

    if (profileXpLabel != null) {
        profileXpLabel.setText("Total XP: " + scores.getOrDefault(username, 0));
    }

    if (profileCorrectLabel != null) {
        profileCorrectLabel.setText(
            "Correct Answers: " + session().correctAnswers
        );
    }

    if (profileLevelLabel != null) {
        profileLevelLabel.setText(
            "Current Level: " + session().level
        );
    }

    if (profileHeartsLabel != null) {
        profileHeartsLabel.setText(
            "Hearts: " + session().hearts
        );
    }

    root.revalidate();
    root.repaint();
}

    // ==================== COMPLETE / GAME OVER / LOGOUT ====================
    private void goDashboard() {
        dispatch(codeescape.core.flow.FlowEvent.of(codeescape.core.flow.FlowEvent.Type.DASHBOARD_REQUESTED));
    }

    private JPanel completeScreen() {
        codeescape.desktop.ui.screens.MessageScreen.Result r = codeescape.desktop.ui.screens.MessageScreen.create(
            msgHost(), "LESSON COMPLETE!", "You finished all 5 levels!", "Final XP: " + session().xp,
            "TAKE ANOTHER LESSON", e -> { startNewLesson(); },
            "HOME", e -> { lessonEngine.cancelRun(savedXp); refreshDashboard(); refreshMainMenuXp(); goDashboard(); },
            "castle.png", session().xp);
        completeXpLabel = r.xpLabel;
        return r.panel;
    }

    private JPanel gameOverScreen() {
        codeescape.desktop.ui.screens.MessageScreen.Result r = codeescape.desktop.ui.screens.MessageScreen.create(
            msgHost(), "GAME OVER", "You ran out of hearts.", "Final XP: " + session().xp,
            "TRY AGAIN", e -> { startNewLesson(); },
            "HOME", e -> { lessonEngine.cancelRun(savedXp); refreshDashboard(); goDashboard(); },
            "level5.png", session().xp);
        gameOverXpLabel = r.xpLabel;
        return r.panel;
    }

    private JPanel logoutScreen() {
        codeescape.desktop.ui.screens.MessageScreen.Result r = codeescape.desktop.ui.screens.MessageScreen.create(
            msgHost(), "LOGOUT", "Are you sure you want to logout?", "Your XP will remain saved for this session.",
            "YES", e -> {
                saveScore();
                username = ""; email = "";
                selectedCharacter = "Boy"; selectedOutfit = 0;
                lessonEngine.cancelRun(0); session().username = "";
                // Logout confirmation routed through the core flow contract.
                dispatch(codeescape.core.flow.FlowEvent.of(codeescape.core.flow.FlowEvent.Type.LOGOUT_CONFIRMED));
            },
            "NO", e -> goDashboard(),
            "level5.png", session().xp);
        return r.panel;
    }

    private codeescape.desktop.ui.screens.MessageScreen.Host msgHost() {
        return new codeescape.desktop.ui.screens.MessageScreen.Host() {
            @Override public JPanel background(String name) { return new BackgroundPanel(name); }
            @Override public JPanel panel(int w, int h) { return CodeEscapeApp.this.panel(w, h); }
            @Override public void title(JPanel box, String text) { CodeEscapeApp.this.title(box, text); }
            @Override public void addCenter(JPanel box, String text, int size, Color c) { CodeEscapeApp.this.addCenter(box, text, size, c); }
            @Override public JLabel label(String text, int size, Color c) { return CodeEscapeApp.this.label(text, size, c); }
            @Override public JButton gold(String text) { return CodeEscapeApp.this.gold(text); }
            @Override public JButton blue(String text) { return CodeEscapeApp.this.blue(text); }
            @Override public Color cream() { return CREAM; }
            @Override public Color goldColor() { return GOLD; }
        };
    }
    // ==================== UI HELPERS ====================
    public JPanel panel(int w, int h) {
        JPanel p = new ModernCardPanel();
        int rw = Math.min(w, Math.max(280, getWidth() > 0 ? getWidth() - 24 : 360));
        int rh = Math.min(h, Math.max(220, getHeight() > 0 ? getHeight() - 100 : h));
        p.setPreferredSize(new Dimension(rw, rh));
        p.setMaximumSize(new Dimension(rw, rh));
        p.setBackground(PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(new Color(111, 196, 255, 210), 2, 22),
            new EmptyBorder(15, 25, 15, 25)
        ));
        return p;
    }

    public void title(JPanel p, String s) {
        int size = getWidth() > 0 && getWidth() <= 500 ? 24 : 30;
        JLabel l = label(s, size, GOLD);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(Box.createVerticalStrut(12));
        p.add(l);
        p.add(Box.createVerticalStrut(20));
    }

    public void addCenter(JPanel p, String s, int size, Color c) {
        JLabel l = label(s, size, c);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(Box.createVerticalStrut(6));
        p.add(l);
    }

    public JLabel label(String s, int size, Color c) {
        JLabel l = new JLabel(s);
        int responsiveSize = (getWidth() > 0 && getWidth() <= 500) ? Math.min(size, 22) : size;
        l.setFont(new Font("SansSerif", Font.BOLD, responsiveSize));
        l.setForeground(c);
        return l;
    }

    public JTextField textField(String tip) {
        JTextField f = new PlaceholderTextField(tip);
        f.setPreferredSize(new Dimension(310, 46));
        f.setMaximumSize(new Dimension(310, 46));
        f.setForeground(new Color(239, 247, 255));
        f.setCaretColor(CYAN);
        f.setBackground(new Color(18, 43, 73));
        f.setFont(new Font("SansSerif", Font.PLAIN, 15));
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(new Color(105, 185, 245), 1, 14),
            new EmptyBorder(8, 12, 8, 12)
        ));
        f.setToolTipText(tip);
        return f;
    }

    public JPasswordField passwordField(String tip) {
        JPasswordField f = new PlaceholderPasswordField(tip);
        f.setPreferredSize(new Dimension(310, 46));
        f.setMaximumSize(new Dimension(310, 46));
        f.setForeground(new Color(239, 247, 255));
        f.setCaretColor(CYAN);
        f.setBackground(new Color(18, 43, 73));
        f.setFont(new Font("SansSerif", Font.PLAIN, 15));
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(new Color(105, 185, 245), 1, 14),
            new EmptyBorder(8, 12, 8, 12)
        ));
        f.setToolTipText(tip);
        return f;
    }

    private JButton base(String s) {
        JButton b = new ModernButton(s);
        b.setPreferredSize(new Dimension(260, 48));
        b.setMaximumSize(new Dimension(390, 52));
        b.setFont(new Font("SansSerif", Font.BOLD, 16));
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setRolloverEnabled(true);
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setVerticalAlignment(SwingConstants.CENTER);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setVerticalTextPosition(SwingConstants.CENTER);
        b.setForeground(new Color(15, 35, 59));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8, 18, 8, 18));
        return b;
    }

    public JButton gold(String s) {
        JButton b = base(s);
        b.setBackground(GOLD);
        b.setForeground(new Color(25, 42, 62));
        b.setBorder(new RoundedBorder(new Color(255, 235, 170), 1, 16));
        return b;
    }

    public JButton blue(String s) {
        JButton b = base(s);
        b.setBackground(new Color(92, 177, 255));
        b.setForeground(new Color(13, 38, 67));
        b.setBorder(new RoundedBorder(new Color(173, 223, 255), 1, 16));
        return b;
    }

    public JButton smallBlue(String s) {
        JButton b = blue(s);
        b.setPreferredSize(new Dimension(220, 42));
        b.setMaximumSize(new Dimension(220, 42));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        return b;
    }

    public JButton link(String s) {
        JButton b = base(s);
        b.setForeground(new Color(220, 242, 255));
        b.setBackground(new Color(36, 82, 125));
        b.setBorder(new RoundedBorder(new Color(105, 218, 245), 1, 14));
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        return b;
    }

    private JButton navButton(String s) {
        JButton b = base(s);
        b.setPreferredSize(new Dimension(98, 42));
        b.setMaximumSize(new Dimension(105, 44));
        b.setBackground(new Color(68, 143, 209));
        b.setForeground(Color.WHITE);
        b.setBorder(new RoundedBorder(new Color(145, 218, 255), 1, 14));
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        return b;
    }

    public JButton dashboardActionButton(String s, Color accent) {
        JButton b = base(s);
        b.setPreferredSize(new Dimension(330, 58));
        b.setMaximumSize(new Dimension(330, 58));
        b.setMinimumSize(new Dimension(280, 54));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setFont(new Font("SansSerif", Font.BOLD, 18));
        if (accent == GOLD) {
            b.setBackground(GOLD);
            b.setForeground(new Color(25, 42, 62));
        } else if (accent == RED) {
            b.setBackground(new Color(238, 105, 119));
            b.setForeground(new Color(54, 24, 38));
        } else {
            b.setBackground(new Color(72, 150, 218));
            b.setForeground(Color.WHITE);
        }
        b.setBorder(new RoundedBorder(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220), 2, 18));
        return b;
    }

    private JButton answerButton(String s) {
        JButton b = base(s == null ? "" : s);
        b.setPreferredSize(new Dimension(160, 72));
        b.setMinimumSize(new Dimension(120, 52));
        b.setMaximumSize(new Dimension(340, 120));
        b.setFont(new Font("SansSerif", Font.BOLD, 15));
        b.setBackground(new Color(40, 91, 139));
        b.setForeground(new Color(246, 251, 255));
        b.setBorder(new RoundedBorder(new Color(116, 204, 247), 1, 16));
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setVerticalAlignment(SwingConstants.CENTER);
        b.setMargin(new Insets(6, 8, 6, 8));
        return b;
    }

    public JLabel image(String n, int w, int h) {
        JLabel l = new JLabel();
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setVerticalAlignment(SwingConstants.CENTER);
        BufferedImage im = load(n);
        if (im != null) {
            l.setIcon(new ImageIcon(scale(im, w, h)));
        } else {
            l.setText("[" + n + "]");
            l.setForeground(CREAM);
        }
        return l;
    }

    private BufferedImage load(String n) {
    try {
        // Try project images folder
        File f = new File("images", n);
        if (f.exists()) {
            return ImageIO.read(f);
        }

        // Try src/images folder
        File f2 = new File("src/images", n);
        if (f2.exists()) {
            return ImageIO.read(f2);
        }

        // Try classpath / resources
        java.net.URL url = getClass().getResource("/images/" + n);
        if (url != null) {
            return ImageIO.read(url);
        }

        // Try classpath root
        url = getClass().getResource("/" + n);
        if (url != null) {
            return ImageIO.read(url);
        }

    } catch (Exception e) {
        System.out.println("Could not load image: " + n);
        e.printStackTrace();
    }

    System.out.println("IMAGE NOT FOUND: " + n);
    return null;
}

    // Character art files have different canvas proportions (especially Boy/Girl).
    // Normalize their display box so both characters occupy the same visual area.
    private Image scaleCharacter(BufferedImage im, int width, int height) {
        if (im == null) return null;
        int w = Math.max(1, width), h = Math.max(1, height);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        // Fit instead of stretching: boy and girl artwork has different source
        // proportions but should occupy a consistent, natural-looking area.
        double factor = Math.min(w / (double) im.getWidth(), h / (double) im.getHeight());
        int dw = Math.max(1, (int) Math.round(im.getWidth() * factor));
        int dh = Math.max(1, (int) Math.round(im.getHeight() * factor));
        g.drawImage(im, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
        g.dispose();
        return out;
    }

    private Image scale(BufferedImage im, int mw, int mh) {
        double r = Math.min((double) mw / im.getWidth(), (double) mh / im.getHeight());
        int w = Math.max(1, (int) Math.round(im.getWidth() * r));
        int h = Math.max(1, (int) Math.round(im.getHeight() * r));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.drawImage(im, 0, 0, w, h, null);
        g2.dispose();
        return out;
    }

    public void error(String s) {
        JOptionPane.showMessageDialog(this, s, "Code Escape", JOptionPane.ERROR_MESSAGE);
    }

    private void showGamePopup(String heading, String bigText, String detail, Color accent) {
        final JDialog dialog = new JDialog(this, heading, true);
        dialog.setUndecorated(true);
        dialog.setSize(420, 430);
        dialog.setMinimumSize(new Dimension(390, 410));
        dialog.setLocationRelativeTo(this);

        JPanel outer = new JPanel(new BorderLayout(8, 8));
        outer.setBackground(new Color(5, 24, 58));
        outer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent, 4),
            new EmptyBorder(20, 24, 18, 24)
        ));

        JLabel h = label(heading, 30, accent);
        h.setHorizontalAlignment(SwingConstants.CENTER);
        outer.add(h, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel points = label(bigText, 34, GOLD);
        points.setAlignmentX(Component.CENTER_ALIGNMENT);
        points.setHorizontalAlignment(SwingConstants.CENTER);
        center.add(points);
        center.add(Box.createVerticalStrut(16));

        JTextArea message = new JTextArea(detail);
        message.setEditable(false);
        message.setFocusable(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setOpaque(false);
        message.setForeground(Color.WHITE);
        message.setFont(new Font("SansSerif", Font.BOLD, 18));
        message.setAlignmentX(Component.CENTER_ALIGNMENT);
        message.setRows(3);
        message.setColumns(28);
        message.setMaximumSize(new Dimension(330, 105));
        message.setPreferredSize(new Dimension(330, 90));
        center.add(message);

        outer.add(center, BorderLayout.CENTER);

        JButton ok = gold("CONTINUE");
        ok.setPreferredSize(new Dimension(220, 50));
        ok.setFont(new Font("SansSerif", Font.BOLD, 18));
        ok.addActionListener(e -> dialog.dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottom.setOpaque(false);
        bottom.add(ok);
        outer.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(outer);
        dialog.setVisible(true);
    }

    private void loadPersistenceIntoCache() {
        accounts.clear();
        emails.clear();
        scores.clear();
        savedCharacters.clear();
        savedOutfits.clear();
        savedStyles.clear();
        accounts.putAll(profileRepository.getAccounts());
        emails.putAll(profileRepository.getEmails());
        scores.putAll(profileRepository.getScores());
        savedCharacters.putAll(profileRepository.getSavedCharacters());
        savedOutfits.putAll(profileRepository.getSavedOutfits());
        savedStyles.putAll(profileRepository.getSavedStyles());
    }

    private void ensureDemoAccount() {
        // Called from constructor path; also keep a safe local fallback in cache.
        if (!accounts.containsKey("demo")) {
            PlayerProfile demo = new PlayerProfile("demo", "demo@codeescape.local");
            demo.character = "Boy";
            demo.outfitVariant = 0;
            demo.selectedTop = 1; demo.selectedBottom = 1; demo.selectedShoes = 1; demo.selectedAccessory = 1;
            demo.lifetimeXp = 0; demo.savedXp = 0;
            profileRepository.saveAccount("demo", "demo@codeescape.local", "demo", demo);
            loadPersistenceIntoCache();
        }
        accounts.putIfAbsent("demo", "demo");
        emails.putIfAbsent("demo", "demo@codeescape.local");
        scores.putIfAbsent("demo", 0);
        savedCharacters.putIfAbsent("demo", "Boy");
        savedOutfits.putIfAbsent("demo", 0);
        savedStyles.putIfAbsent("demo", new int[]{1, 1, 1, 1});
    }

    private PlayerProfile snapshotCurrentProfile() {
        PlayerProfile p = (username == null || username.isEmpty())
                ? new PlayerProfile(username, email)
                : profileRepository.findByUsername(username).orElse(new PlayerProfile(username, email));
        p.username = username == null ? "" : username;
        p.email = email == null ? emails.getOrDefault(p.username, "") : email;
        p.character = selectedCharacter == null ? "Boy" : selectedCharacter;
        p.outfitVariant = Math.max(0, Math.min(3, selectedOutfit));
        p.selectedTop = selectedTop;
        p.selectedBottom = selectedBottom;
        p.selectedShoes = selectedShoes;
        p.selectedAccessory = selectedAccessory;
        p.lifetimeXp = Math.max(0, scores.getOrDefault(p.username, savedXp));
        p.savedXp = Math.max(0, savedXp);
        return p;
    }

    private int getLifetimeXp(String name) {
        if (name == null || name.isEmpty()) return 0;
        int repoXp = profileRepository.getLifetimeXp(name);
        scores.put(name, Math.max(0, repoXp));
        return Math.max(0, repoXp);
    }

    private void commitSessionXp() {
        if (username.isEmpty() || session().sessionCommitted) return;
        profileRepository.commitAttemptXp(username, Math.max(0, session().xp));
        savedXp = profileRepository.getLifetimeXp(username);
        scores.put(username, savedXp);
        lessonEngine.markSessionCommitted();
        // Persist current character/style/profile fields alongside committed XP for compatibility.
        profileRepository.saveProfile(snapshotCurrentProfile());
        loadPersistenceIntoCache();
        savedXp = scores.getOrDefault(username, savedXp);
    }

    // XP is committed only by commitSessionXp() after the entire game is completed.
    private void saveScore() {
        // Intentionally does not save temporary XP from an unfinished run.
        if (username != null && !username.isEmpty()) {
            profileRepository.saveProfile(snapshotCurrentProfile());
            loadPersistenceIntoCache();
        }
    }

    // ==================== MUSIC ====================
    // Shared audio/narration settings are applied to the adapters and persisted on change.
    private void applySettings() {
        audioAdapter.setEnabled(settings.isMusicEnabled());
        audioAdapter.setMuted(settings.isMuted());
        audioAdapter.setVolume(settings.getVolume());
        narrationPort.setEnabled(settings.isTtsEnabled());
    }

    private void persistSettings() {
        settingsStore.save(settings);
    }

    private String musicStatusText() {
        if (settings.isMuted()) return "♫ MUTED";
        return settings.isMusicEnabled() ? "♫ ON" : "♫ OFF";
    }

    private Color musicStatusColor() {
        return (settings.isMuted() || !settings.isMusicEnabled()) ? PEACH : MINT;
    }
    // Delegated to DesktopAudioAdapter (AudioPort wiring); enabled/volume state is adapter-owned.
    private void playFeedbackSound(boolean correct) {
        audioAdapter.playFeedback(correct);
    }

    private void playLevelMusic(int levelNumber) {
        audioAdapter.playLevelMusic(levelNumber);
    }

    private void playSpecialMusic(String fileName) {
        audioAdapter.playSpecialMusic(fileName);
    }

    private void playMainMenuMusic() {
        audioAdapter.playMainMenuMusic();
    }

    private void startBackgroundMusic() {
        audioAdapter.startBackgroundMusic();
    }

    private void stopBackgroundMusic() {
        audioAdapter.stopMusic();
    }

    private void setMusicVolume(float volume) {
        audioAdapter.setVolume(volume);
    }

    // ==================== RUNNING ANIMATION ====================

    private BufferedImage[] loadIsolatedWalkFrames(String file) {
        return codeescape.desktop.ui.animation.SpriteFrames.loadIsolatedWalkFrames(this::load, file);
    }

    // MiniRunner and MovingRunner animation views migrated to codeescape.desktop.ui.animation.*


    // Migrated to desktop UI component; thin adapter keeps legacy constructor/API.
    class BackgroundPanel extends codeescape.desktop.ui.components.BackgroundPanel {
        BackgroundPanel(String n) { super(n, CodeEscapeApp.this::load); }
    }

    // UI component classes migrated to codeescape.desktop.ui.components (imported above).

    // Question model moved to core (codeescape.core.model.Question) for reuse across frontends.

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            CodeEscapeApp app = new CodeEscapeApp();
            app.setExtendedState(JFrame.MAXIMIZED_BOTH);
            app.setVisible(true);
        });
    }
}











