package codeescape;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import codeescape.core.animation.AnimationTiming;
import codeescape.core.content.GameConfig;
import codeescape.core.content.LevelMetadata;
import codeescape.core.content.QuestionBank;
import codeescape.core.content.StoryContent;
import codeescape.core.model.PlayerProfile;
import codeescape.core.model.Question;
import codeescape.core.port.NarrationCallback;
import codeescape.core.port.ProfileRepository;
import codeescape.desktop.adapter.DesktopAudioAdapter;
import codeescape.desktop.adapter.WindowsNarrationAdapter;
import codeescape.desktop.persistence.FileProfileRepository;
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

    private JLabel[] hintEndingTextHolder = new JLabel[1];
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

    private javax.swing.Timer questionTimer;
    private final codeescape.desktop.scheduler.SwingScheduler uiScheduler = new codeescape.desktop.scheduler.SwingScheduler();
    private codeescape.desktop.ui.question.QuestionHudController questionHudController;
    // Audio/music and narration ownership lives in the port adapters (DesktopAudioAdapter /
    // WindowsNarrationAdapter); story-screen karaoke label state is owned by StoryScreenBuilder.
    private boolean storyMusicWasPlaying = false;
    private final DesktopAudioAdapter audioAdapter = new DesktopAudioAdapter();
    private final WindowsNarrationAdapter narrationAdapter = new WindowsNarrationAdapter();
    // Narration is routed through the core NarrationPort contract (karaoke callback included).
    private final codeescape.core.port.NarrationPort narrationPort = narrationAdapter;
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

    // A shuffled copy is created for every new lesson so the question order
    // changes each time while keeping the same session().level structure.
    private Question[][] sessionQuestions = new Question[5][];
    private final codeescape.core.engine.LessonEngine lessonEngine = new codeescape.core.engine.LessonEngine();
    private codeescape.core.model.GameSession session() { return lessonEngine.getSession(); }
    private final codeescape.core.flow.Navigator flowNavigator = new codeescape.core.flow.Navigator() {
        @Override public void navigate(codeescape.core.flow.FlowState state) { navigate(state, session().level); }
        @Override public void navigate(codeescape.core.flow.FlowState state, int level) {
            if (state == codeescape.core.flow.FlowState.QUESTION) {
                startQuestion();
            } else {
                showScreen(codeescape.core.flow.FlowTransitions.screenName(state));
            }
        }
    };

    /** Routes app-level flow events through the core transition contract when a target is defined. */
    private void dispatch(codeescape.core.flow.FlowEvent event) {
        codeescape.core.flow.FlowState target = codeescape.core.flow.FlowTransitions.targetFor(event);
        if (target != null) flowNavigator.navigate(target);
    }

    /** Music playing state is owned by the audio adapter (AudioPort); no mirrored clip reference. */
    private boolean isBackgroundMusicPlaying() {
        Clip clip = audioAdapter.getClip();
        return clip != null && clip.isRunning();
    }

    private final Question[][] questions = QuestionBank.canonical();

    private static Question q(String text, String a, String b, String c, String d, int correct) {
        return new Question(text, new String[]{a, b, c, d}, correct, false, null);
    }
    private static Question qs(String text, String expected) { return new Question(text, new String[0], -1, true, expected); }
    private Color BLACK;
    private Color WHITE;

    public CodeEscapeApp() {
        setTitle("Code Escape - Gamified Programming Learning System");
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
                    session().xp = 0;
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
                } else {
                    saveUserDatabase();
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

    private JPanel styleSection(String title, String[] choices, java.util.function.IntConsumer action) {
        JPanel outer = new JPanel(new BorderLayout(3,2)); outer.setOpaque(false);
        JLabel t = label(title, 13, CYAN); t.setAlignmentX(Component.CENTER_ALIGNMENT); outer.add(t, BorderLayout.NORTH);
        JPanel row = new JPanel(new GridLayout(1,3,3,3)); row.setOpaque(false);
        for (int i=0;i<3;i++) { final int v=i+1; JButton b=smallBlue(choices[i]); b.addActionListener(e->action.accept(v)); row.add(b); }
        outer.add(row, BorderLayout.CENTER); return outer;
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

    private String customAssetBase(String character) {
        return characterVariantFile(character, selectedOutfit).replace(".png", "");
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

        JLabel musicStatus = label(audioAdapter.isEnabled() ? "♫ ON" : "♫ OFF", 12, audioAdapter.isEnabled() ? MINT : PEACH);
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

        JButton options = menuGameButton("⚙  OPTIONS", BLUE);
        options.addActionListener(e -> showOptionsDialog(musicStatus));
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
        if (mainMenuXpLabel != null) mainMenuXpLabel.setText("★  XP: " + (session().showZeroXpAfterCancelledRun ? 0 : savedXp));
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

    private void showOptionsDialog(JLabel musicStatus) {
        JDialog dialog = new JDialog(this, "OPTIONS", true);
        dialog.setSize(350, 330);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel box = new JPanel();
        box.setBackground(NAVY);
        box.setBorder(new EmptyBorder(24, 30, 24, 30));
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel title = label("OPTIONS", 30, GOLD);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(title);
        box.add(Box.createVerticalStrut(18));

        JCheckBox music = new JCheckBox("Background Music");
        music.setSelected(audioAdapter.isEnabled());
        music.setForeground(WHITE);
        music.setBackground(NAVY);
        music.setFont(new Font("SansSerif", Font.BOLD, 17));
        music.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSlider volume = new JSlider(0, 100, Math.round(audioAdapter.getVolume() * 100));
        volume.setOpaque(false);
        volume.setAlignmentX(Component.CENTER_ALIGNMENT);
        volume.setMaximumSize(new Dimension(280, 45));

        JLabel volumeText = label("Volume: " + volume.getValue() + "%", 15, CREAM);
        volumeText.setAlignmentX(Component.CENTER_ALIGNMENT);

        music.addActionListener(e -> {
            // Audio enable/disable state is owned by the AudioPort adapter.
            audioAdapter.setEnabled(music.isSelected());
            if (music.isSelected()) startBackgroundMusic();
            else stopBackgroundMusic();
            if (musicStatus != null) {
                musicStatus.setText(audioAdapter.isEnabled() ? "♫ MUSIC ON" : "♫ MUSIC OFF");
                musicStatus.setForeground(audioAdapter.isEnabled() ? MINT : PEACH);
            }
        });

        volume.addChangeListener(e -> {
            setMusicVolume(volume.getValue() / 100f);
            volumeText.setText("Volume: " + volume.getValue() + "%");
        });

        box.add(music);
        box.add(Box.createVerticalStrut(12));
        box.add(volumeText);
        box.add(volume);
        box.add(Box.createVerticalStrut(18));

        JButton done = gold("DONE");
        done.setAlignmentX(Component.CENTER_ALIGNMENT);
        done.addActionListener(e -> dialog.dispose());
        box.add(done);

        dialog.setContentPane(box);
        dialog.setVisible(true);
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
        session().xp = lessonEngine.getXp();
        session().hearts = lessonEngine.getHearts();
        session().level = lessonEngine.getLevel();
        session().questionIndex = lessonEngine.getQuestionIndex();
        session().correctAnswers = lessonEngine.getCorrectAnswers();
        session().showZeroXpAfterCancelledRun = false;
        session().sessionCommitted = false;
        sessionQuestions = new Question[questions.length][]; // legacy field retained for compatibility
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
            @Override public int getHeightSafe() { return getHeight() > 0 ? getHeight() : 760; }
            @Override public java.util.List<int[]> buildStoryRanges(String text) { return CodeEscapeApp.this.buildStoryRanges(text); }
            @Override public String wrapStoryHtml(String text, int maxChars) { return CodeEscapeApp.this.wrapStoryHtml(text, maxChars); }
            @Override public void speakStoryKaraoke(String text, JLabel label) { CodeEscapeApp.this.speakStoryKaraoke(text, label); }
            @Override public void stopStorySpeech() { CodeEscapeApp.this.stopStorySpeech(); }
            @Override public void setActiveStory(JLabel story, java.util.List<int[]> ranges) { /* karaoke state owned by the story screen builder */ }
        }, titleText, storyText, buttonText, background, nextAction);
    }
    private String htmlEscape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("\n", "<br>");
    }

    public java.util.List<int[]> buildStoryRanges(String text) {
        java.util.List<int[]> ranges = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[^.!?]+[.!?]+|[^.!?]+$").matcher(text);
        while (m.find()) {
            int a=m.start(), b=m.end();
            while(a<b && Character.isWhitespace(text.charAt(a))) a++;
            while(b>a && Character.isWhitespace(text.charAt(b-1))) b--;
            if(b>a) ranges.add(new int[]{a,b});
        }
        return ranges;
    }

    public void showStoryLyric(JLabel label, String text, int index, java.util.List<int[]> ranges) {
        if (label == null || index < 0 || index >= ranges.size()) return;
        String line = text.substring(ranges.get(index)[0], ranges.get(index)[1]).trim();
        int maxChars = label.getWidth() > 500 ? 36 : 23;
        String html = wrapStoryHtml(line, maxChars);
        SwingUtilities.invokeLater(() -> {
            label.setText(html);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            fadeStoryLine(label);
        });
    }

    // Wrap every story sentence into several short lines. Keeping the line
    // length controlled is more reliable than relying on Swing HTML width,
    // especially on smaller laptop screens where the old text was clipped.
    public String wrapStoryHtml(String text, int maxChars) {
        if (text == null) return "<html><div style='text-align:center;'></div></html>";
        String[] words = text.trim().split("\\s+");
        java.util.List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= maxChars) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0) lines.add(current.toString());

        // Avoid a lonely one-line sentence when there is enough text to make
        // the story card visually balanced.
        if (lines.size() == 1 && lines.get(0).length() > 18) {
            String only = lines.get(0);
            String[] w = only.split("\\s+");
            int mid = Math.max(1, w.length / 2);
            StringBuilder a = new StringBuilder();
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < w.length; i++) {
                if (i < mid) { if (a.length() > 0) a.append(' '); a.append(w[i]); }
                else { if (b.length() > 0) b.append(' '); b.append(w[i]); }
            }
            if (b.length() > 0) { lines.clear(); lines.add(a.toString()); lines.add(b.toString()); }
        }

        StringBuilder html = new StringBuilder("<html><div style='text-align:center;'>");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) html.append("<br>");
            html.append("<span style='color:#FFD54A;'>")
                .append(htmlEscape(lines.get(i)))
                .append("</span>");
        }
        html.append("</div></html>");
        return html.toString();
    }

    private void fadeStoryLine(JLabel label) {
        if (label == null) return;
        final Color target = Color.WHITE;
        final Color start = new Color(110, 125, 150);
        final long started = System.currentTimeMillis();
        // Non-question UI timing goes through the desktop scheduler abstraction.
        final javax.swing.Timer[] fade = new javax.swing.Timer[1];
        fade[0] = uiScheduler.repeating(25, () -> {
            double t = Math.min(1.0, (System.currentTimeMillis() - started) / 360.0);
            int r = (int)Math.round(start.getRed() + (target.getRed() - start.getRed()) * t);
            int g = (int)Math.round(start.getGreen() + (target.getGreen() - start.getGreen()) * t);
            int b = (int)Math.round(start.getBlue() + (target.getBlue() - start.getBlue()) * t);
            label.setForeground(new Color(r,g,b));
            if (t >= 1.0 && fade[0] != null) fade[0].stop();
        });
    }

    // ==================== STORY TEXT-TO-SPEECH / KARAOKE ====================
    // Wired through the NarrationPort contract (WindowsNarrationAdapter) with UI callbacks for parity.
    // Narration process/thread/generation ownership lives in the adapter.
    public void speakStoryKaraoke(String text, JLabel label) {
        stopStorySpeech();
        final java.util.List<int[]> ranges = buildStoryRanges(text);

        if (!narrationAdapter.isSupported()) {
            if (label != null) label.setText(wrapStoryHtml(text, label.getWidth() > 500 ? 36 : 23));
            return;
        }

        storyMusicWasPlaying = isBackgroundMusicPlaying();
        if (storyMusicWasPlaying) audioAdapter.stopMusic();
        narrationPort.speakStory(text, new NarrationCallback() {
            @Override public void onLine(int index, String t, java.util.List<int[]> r) {
                showStoryLyric(label, t, index, r);
            }
            @Override public void onComplete() {
                SwingUtilities.invokeLater(() -> {
                    if (label != null) label.setText("<html><div style='text-align:center;'>Story complete!</div></html>");
                });
                if (storyMusicWasPlaying && audioAdapter.isEnabled()) {
                    audioAdapter.startBackgroundMusic();
                }
                storyMusicWasPlaying = false;
            }
        });
    }

    public void stopStorySpeech(){
        narrationPort.stop();
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
            GOLD);
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
        questionHudController = new codeescape.desktop.ui.question.QuestionHudController(questionHudRefs, this::handleTimeout);
        questionHudController.setAcceptingAnswer(acceptingAnswer);
        questionHudController.setSecondsLeft(secondsLeft);
        return questionBackground;
    }

    private void stopQuestionTimer() {
        if (questionHudController != null) questionHudController.stop();
        if (questionTimer != null) questionTimer.stop();
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
            questionTimer = null;
            return;
        }
        startLegacyQuestionTimer();
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
        String counter = "LEVEL " + session().level + " • QUESTION " + (session().questionIndex + 1) + " / " + GameConfig.QUESTIONS_PER_LEVEL;
        String questionHtml = "<html><center>" + q.text + "</center></html>";
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
            questionTimer = null;
            return;
        }
        startLegacyQuestionTimer();
    }

    /** Legacy fallback timer loop retained for parity when the HUD controller is unavailable. */
    private void startLegacyQuestionTimer() {
        if (timerLabel == null) return;
        timerLabel.setText(timerLabelText());
        timerLabel.setForeground(secondsLeft <= 5 ? RED : GOLD);
        questionTimer = uiScheduler.repeating(GameConfig.QUESTION_TICK_MS, () -> {
            if (!acceptingAnswer) return;
            secondsLeft--;
            if (questionHudController != null) questionHudController.setSecondsLeft(secondsLeft);
            timerLabel.setText(timerLabelText());
            timerLabel.setForeground(secondsLeft <= 5 ? RED : GOLD);
            if (secondsLeft <= 0) { stopQuestionTimer(); handleTimeout(); }
        });
        questionTimer.start();
    }

    private void syncQuestionTimerUiFromController() {
        if (questionHudController != null) secondsLeft = questionHudController.getSecondsLeft();
        if (timerLabel != null) {
            timerLabel.setText(timerLabelText());
            timerLabel.setForeground(secondsLeft <= 5 ? RED : GOLD);
        }
    }

    /** Formats the countdown label; prefix is the original legacy string preserved byte-for-byte. */
    private String timerLabelText() { return "⏱ " + secondsLeft + "s"; }

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
        String timerText = timerLabelText();
        if (questionHudController != null && questionHudRefs != null) {
            questionHudController.applyHud(hearts, xpText, timerText, timerLabel != null ? timerLabel.getForeground() : null);
        } else {
            if (heartsLabel != null) heartsLabel.setText(hearts);
            if (xpLabel != null) xpLabel.setText(xpText);
            if (timerLabel != null) timerLabel.setText(timerText);
        }
        if (dashboardXpLabel != null) dashboardXpLabel.setText("★  XP: " + (session().showZeroXpAfterCancelledRun ? 0 : savedXp));
    }


    // ==================== RUNNING ====================
    private void runToNextLevel() {
        stopQuestionTimer();

        String runnerFile = runnerOutfitFile(selectedCharacter, selectedOutfit);
        playLevelMusic(session().level);
        codeescape.desktop.ui.animation.MovingRunner runner = new codeescape.desktop.ui.animation.MovingRunner(this::load, runnerFile);
        codeescape.desktop.ui.screens.RunningTransitionScreen.Result tr =
                codeescape.desktop.ui.screens.RunningTransitionScreen.create(
                        session().level, LevelMetadata.of(session().level).backgroundImage, this::load, runner, CYAN, CREAM, GOLD);
        runner.progressBar = tr.progressBar;

        root.add(tr.panel, "RUNNING");
        showScreen("RUNNING");
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

    private JButton bigMenuButton(String s) {
        JButton b = gold(s);
        b.setPreferredSize(new Dimension(310, 58));
        b.setMaximumSize(new Dimension(320, 60));
        b.setFont(new Font("SansSerif", Font.BOLD, 19));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        return b;
    }

    private JButton answerButton(String s) {
        JButton b = base(s);
        b.setPreferredSize(new Dimension(310, 58));
        b.setMaximumSize(new Dimension(330, 62));
        b.setFont(new Font("SansSerif", Font.BOLD, 16));
        b.setBackground(new Color(40, 91, 139));
        b.setForeground(new Color(246, 251, 255));
        b.setBorder(new RoundedBorder(new Color(116, 204, 247), 1, 16));
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

    private void drawCharacter(Graphics2D g, BufferedImage image, int x, int y, int width, int height) {
        if (image == null) return;
        g.drawImage(image, x, y, Math.max(1, width), Math.max(1, height), null);
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

    // ==================== LOCAL USER DATABASE ====================
    // Delegated to ProfileRepository (FileProfileRepository) for users.properties/scores.properties compatibility.
    private void saveUserDatabase() {
        if (username != null && !username.isEmpty() && accounts.containsKey(username)) {
            PlayerProfile p = snapshotCurrentProfile();
            profileRepository.saveProfile(p);
        }
        loadPersistenceIntoCache();
    }

    // ==================== MUSIC ====================
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

    private int detectFrameCount(BufferedImage image) {
        return codeescape.desktop.ui.animation.SpriteFrames.detectFrameCount(image);
    }

    private int[] frameSequenceFor(int count) {
        return codeescape.desktop.ui.animation.SpriteFrames.frameSequenceFor(count);
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











