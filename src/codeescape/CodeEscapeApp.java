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

    private int xp = 0;
    // XP saved from before the current login. Gameplay XP resets to 0 each login.
    private int savedXp = 0;
    // When an unfinished lesson is explicitly exited, the menu/dashboard must
    // show 0 for the current run instead of displaying the previously saved XP.
    // The saved XP itself is never deleted.
    private boolean showZeroXpAfterCancelledRun = false;
    private int hearts = 3;
    private int level = 1;
    private int questionIndex = 0;
    private int secondsLeft = 15;
    private int correctAnswers = 0;
    private boolean sessionCommitted = false;

    private javax.swing.Timer questionTimer;
    private javax.swing.Timer runningTimer;
    private Clip backgroundMusic;
    private Process storySpeechProcess;
    private Thread storySpeechThread;
    private volatile int storySpeechGeneration = 0;
    private JLabel activeStoryLabel;
    private java.util.List<int[]> activeStoryRanges = new ArrayList<>();
    private int activeStoryCurrent = -1;
    private boolean storyMusicWasPlaying = false;
    private boolean musicEnabled = true;
    private float musicVolume = 0.45f;
    private int currentMusicLevel = 1;
    private MiniRunner menuRunner;
    private JLabel timerLabel;
    private JLabel xpLabel;
    private JLabel heartsLabel;
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
    private BackgroundPanel questionBackground;

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
    // changes each time while keeping the same level structure.
    private Question[][] sessionQuestions = new Question[5][];

    private final Question[][] questions = {
    {
        q("Which HTML tag creates a paragraph?",
                "<p>", "<h1>", "<br>", "<div>", 0), // ANSWER: <p>

        q("Which HTML tag creates the largest heading?",
                "<h6>", "<head>", "<h1>", "<title>", 2), // ANSWER: <h1>

        q("Which HTML tag displays an image?",
                "<image>", "<pic>", "<img>", "<src>", 2), // ANSWER: <img>

        qs("Complete the HTML syntax to create a paragraph containing Hello World:",
                "<p>Hello World</p>"),

        qs("Complete the HTML syntax to display an image named photo.jpg:",
                "<img src=\"photo.jpg\">"),
        q("Which attribute provides alternate text for an HTML image?", "href", "alt", "srcset", "class", 1),
        q("Which tag creates an ordered list?", "<ul>", "<ol>", "<li>", "<dl>", 1),
        q("Which HTML attribute links a label to an input id?", "for", "name", "target", "rel", 0),
        qs("Complete the HTML syntax for a line break:", "<br>"),
        qs("Complete the HTML syntax for a link to index.html: <a ______>Home</a>", "href=\"index.html\""),
    },

    {
        q("Which CSS property changes text color?",
                "font-color", "text-color", "color", "foreground", 2), // ANSWER: color

        q("Which selector targets an element by id?",
                ".id", "#id", "*id", "@id", 1), // ANSWER: #id

        q("Which CSS property changes the background color?",
                "background-color", "bg-color", "color-background", "background", 0), // ANSWER: background-color

        qs("Complete the CSS syntax to make paragraph text blue: p { ______ }",
                "color: blue;"),

        qs("Complete the CSS syntax to add a 10px margin: .box { ______: 10px; }",
                "margin"),
        q("Which CSS property rounds an element's corners?", "border-style", "corner-radius", "border-radius", "round", 2),
        q("Which selector targets every paragraph?", "#p", ".p", "p", "@p", 2),
        q("Which CSS property controls space inside an element's border?", "margin", "padding", "gap", "outline", 1),
        qs("Complete the CSS syntax to select an element with class card: ______ { color: white; }", ".card"),
        qs("Complete the CSS declaration for a 2px solid border: .box { ______ }", "border: 2px solid;"),
    },

    {
        q("Which keyword declares a variable that can be reassigned in JavaScript?",
                "const", "let", "static", "fixed", 1), // ANSWER: let

        q("Which symbol is used for strict equality in JavaScript?",
                "=", "==", "===", "!=", 2), // ANSWER: ===

        q("Which method prints a message in the browser console?",
                "console.log()", "print()", "writeLine()", "echo()", 0), // ANSWER: console.log()

        qs("Complete the JavaScript syntax to declare score with the value 10:",
                "let score = 10;"),

        qs("Complete the JavaScript condition: if (score ______ 10) { console.log(\"Good\"); }",
                ">="),
        q("Which JavaScript structure repeats code while a condition is true?", "if", "while", "switch", "try", 1),
        q("Which operator adds one to a number?", "--", "**", "++", "==", 2),
        q("Which keyword returns a value from a function?", "give", "send", "return", "yielded", 2),
        qs("Complete the JavaScript statement to show an alert: ______(\"Hello\");", "alert"),
        qs("Complete the function call to print name: console.______(name);", "log"),
    },

    {
        q("What should you check first when a webpage does not display correctly?",
                "Only the volume", "The HTML structure and tags", "The keyboard", "The wallpaper", 1), // ANSWER: HTML structure and tags

        q("Which character commonly ends a CSS declaration?",
                ";", "?", "#", "@", 0), // ANSWER: ;

        q("If JavaScript says a function is not defined, what should you check?",
                "The screen size", "The function name and spelling", "The mouse", "The image size", 1), // ANSWER: function name and spelling

        qs("Fix the missing closing brace: function greet() { console.log(\"Hello\"); ______",
                "}"),

        qs("Fix the missing HTML closing tag: <p>Hello World______",
                "</p>"),
        q("What does a syntax error usually mean?", "The code breaks language rules", "The monitor is off", "The file is too colorful", "The mouse is disconnected", 0),
        q("Which browser tool helps inspect console errors?", "Calculator", "Developer Tools", "File Explorer", "Paint", 1),
        q("What is a common cause of a missing-variable error?", "Variable name is misspelled", "Screen brightness", "Image format", "Speaker volume", 0),
        qs("Complete the JavaScript comment for one line: ______ this line", "//"),
        qs("Complete the HTML closing tag for a div: ______", "</div>"),
    },

    {
        q("Which HTML element contains the main visible webpage content?",
                "<head>", "<body>", "<meta>", "<title>", 1), // ANSWER: <body>

        q("Which CSS property changes the size of text?",
                "text-style", "font-size", "text-size", "font-height", 1), // ANSWER: font-size

        q("Which JavaScript method prints to the console?",
                "alert()", "console.log()", "printLine()", "display()", 1), // ANSWER: console.log()

        qs("Complete the HTML button syntax: <button ______>Click</button>",
                "type=\"button\""),

        qs("Complete the CSS syntax to add 10px padding: .card { ______: 10px; }",
                "padding"),
        q("Which HTML tag connects an external CSS file?", "<style src>", "<css>", "<link>", "<script>", 2),
        q("Which CSS layout system is useful for arranging items in rows or columns?", "Flexbox", "Console", "HTML", "Prompt", 0),
        q("Which JavaScript keyword checks a condition?", "when", "if", "check", "caseif", 1),
        qs("Complete the HTML script tag opening for app.js: <script ______>", "src=\"app.js\""),
        qs("Complete the CSS syntax to center text: .title { text-align: ______; }", "center"),
    }
};

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

        accounts.put("demo", "demo");
        emails.put("demo", "demo@codeescape.local");
        loadScores();
        loadUserDatabase();
        scores.putIfAbsent("demo", 0);

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
            playSpecialMusic("dungeon.wav");
        } else if ("STORY_LEVEL2".equals(name)) {
            playLevelMusic(2);
        } else if ("STORY_LEVEL3".equals(name)) {
            playLevelMusic(3);
        } else if ("STORY_LEVEL4".equals(name)) {
            playLevelMusic(4);
        } else if ("STORY_LEVEL5".equals(name) || "ENDING".equals(name)) {
            playLevelMusic(5);
        } else if ("COMPLETE".equals(name)) {
            playSpecialMusic("victory.wav");
        }

        if ("GAMEOVER".equals(name)) {
            // The Game Over screen is created when the app starts, so its
            // original XP text would otherwise stay at 0. Refresh it with
            // the XP currently saved for the player.
            if (gameOverXpLabel != null) {
                gameOverXpLabel.setText("Final XP: " + xp);
            }
        }
        if ("COMPLETE".equals(name)) {
            if (completeXpLabel != null) completeXpLabel.setText("Final XP: " + xp);
            refreshProfile();
            refreshDashboard();
        }
        if ("MAIN_MENU".equals(name)) {
            refreshMainMenuXp();
            refreshMenuRunner();
        }
        cards.show(root, name);
    }

    // ==================== WELCOME ====================
    private JPanel welcomeScreen() {
        BackgroundPanel p = new BackgroundPanel("castle.png");
        p.setLayout(new GridBagLayout());

        JPanel box = panel(360, 640);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(new Color(21, 54, 88, 238));
        box.add(Box.createVerticalStrut(12));

        JLabel logo = image("logo.png", 250, 155);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(logo);
        addCenter(box, "Gamified Programming Learning System", 17, CREAM);
        box.add(Box.createVerticalStrut(26));

        JButton login = gold("LOGIN");
        login.setPreferredSize(new Dimension(330, 54));
        login.setMaximumSize(new Dimension(330, 54));
        login.setAlignmentX(Component.CENTER_ALIGNMENT);
        login.addActionListener(e -> showScreen("LOGIN"));
        box.add(login);
        box.add(Box.createVerticalStrut(14));

        JButton signup = blue("SIGN UP / REGISTER");
        signup.setPreferredSize(new Dimension(330, 54));
        signup.setMaximumSize(new Dimension(330, 54));
        signup.setAlignmentX(Component.CENTER_ALIGNMENT);
        signup.addActionListener(e -> showScreen("REGISTER"));
        box.add(signup);

        p.add(box);
        return p;
    }

    // ==================== LOADING ====================
    private JPanel loadingScreen() {
        BackgroundPanel p = new BackgroundPanel("loading.png");
        p.setLayout(new BorderLayout());
        return p;
    }

    private void startInitialLoading() {
        javax.swing.Timer loadingTimer = new javax.swing.Timer(1800, e -> showScreen("LOGIN"));
        loadingTimer.setRepeats(false);
        loadingTimer.start();
    }

    private void showLoadingThenCharacter() {
    showScreen("LOADING");

    javax.swing.Timer loadingTimer = new javax.swing.Timer(1800, e -> {
        refreshDashboard();
        refreshProfile();
        showScreen("STORY_INTRO");
    });

    loadingTimer.setRepeats(false);
    loadingTimer.start();
}

    // ==================== LOGIN ====================
    private JPanel loginScreen() {
        BackgroundPanel p = new BackgroundPanel("level5.png");
        p.setLayout(new GridBagLayout());
        JPanel box = panel(350, 520);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        title(box, "LOGIN");

        JTextField user = textField("Username / Email");
        JPasswordField pass = passwordField("Password");
        user.addActionListener(e -> pass.requestFocusInWindow());
        box.add(user);
        box.add(Box.createVerticalStrut(14));
        box.add(pass);
        box.add(Box.createVerticalStrut(24));

        JButton b = gold("LOGIN");
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.addActionListener(e -> {
            String u = user.getText().trim();
            String pw = new String(pass.getPassword());
            if (accounts.containsKey(u) && accounts.get(u).equals(pw)) {
                username = u;
                savedXp = 0;
                email = emails.getOrDefault(u, "");
                // A login always starts a fresh gameplay session. Previous
                // completed attempts remain stored in savedXp/Profile, but
                // the main menu/dashboard for this new login starts at 0 XP.
                xp = 0;
                showZeroXpAfterCancelledRun = true;
                sessionCommitted = false;
                hearts = 3;
                level = 1;
                questionIndex = 0;
                correctAnswers = 0;
                selectedCharacter = savedCharacters.getOrDefault(u, "Boy");
                selectedOutfit = Math.max(0, Math.min(3, savedOutfits.getOrDefault(u, 0)));
                int[] style = savedStyles.getOrDefault(u, new int[]{1,1,1,1});
                selectedTop = style[0]; selectedBottom = style[1]; selectedShoes = style[2]; selectedAccessory = style[3];
                showLoadingThenCharacter();
            } else {
                error("Invalid username or password.");
            }
        });
        pass.addActionListener(e -> b.doClick());
        box.add(b);
        box.add(Box.createVerticalStrut(18));

        JLabel signupPrompt = label("Don't have an account?", 12, CREAM);
        signupPrompt.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(signupPrompt);
        JButton signup = link("SIGN UP / REGISTER");
        signup.setAlignmentX(Component.CENTER_ALIGNMENT);
        signup.addActionListener(e -> showScreen("REGISTER"));
        box.add(signup);

        JButton back = smallBlue("← BACK");
        back.addActionListener(e -> showScreen("WELCOME"));
        box.add(Box.createVerticalStrut(12));
        box.add(back);

        p.add(box);
        return p;
    }

    // ==================== REGISTER ====================
    private JPanel registerScreen() {
        BackgroundPanel p = new BackgroundPanel("castle.png");
        p.setLayout(new GridBagLayout());
        JPanel box = panel(360, 590);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        title(box, "REGISTER");

        JTextField user = textField("Username");
        JTextField mail = textField("Email");
        JPasswordField pass = passwordField("Password");
        JPasswordField confirm = passwordField("Confirm Password");
        user.addActionListener(e -> mail.requestFocusInWindow());
        mail.addActionListener(e -> pass.requestFocusInWindow());
        pass.addActionListener(e -> confirm.requestFocusInWindow());
        box.add(user); box.add(Box.createVerticalStrut(10));
        box.add(mail); box.add(Box.createVerticalStrut(10));
        box.add(pass); box.add(Box.createVerticalStrut(10));
        box.add(confirm); box.add(Box.createVerticalStrut(20));

        JButton b = gold("REGISTER");
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.addActionListener(e -> {
            String u = user.getText().trim();
            String em = mail.getText().trim();
            String pw = new String(pass.getPassword());
            String cpw = new String(confirm.getPassword());
            if (u.isEmpty() || em.isEmpty() || pw.isEmpty()) { error("Please complete all fields."); return; }
            if (!pw.equals(cpw)) { error("Passwords do not match."); return; }
            if (accounts.containsKey(u)) { error("Username already exists."); return; }
            accounts.put(u, pw); emails.put(u, em); scores.put(u, 0);
            savedCharacters.put(u, "Boy");
            savedOutfits.put(u, 0);
            savedStyles.put(u, new int[]{1,1,1,1});
            username = u; email = em; savedXp = 0; xp = 0; showZeroXpAfterCancelledRun = false; sessionCommitted = false; hearts = 3; level = 1; questionIndex = 0; correctAnswers = 0;
            selectedCharacter = "Boy";
            selectedOutfit = 0;
            selectedTop = 1; selectedBottom = 1; selectedShoes = 1; selectedAccessory = 1;
            saveUserDatabase();
            showLoadingThenCharacter();
        });
        confirm.addActionListener(e -> b.doClick());
        box.add(b);
        JButton back = smallBlue("← BACK TO LOGIN");
        back.addActionListener(e -> showScreen("LOGIN"));
        box.add(Box.createVerticalStrut(12)); box.add(back);
        p.add(box);
        return p;
    }

    // ==================== CHARACTER ====================
    private JPanel characterScreen() {
        BackgroundPanel p = new BackgroundPanel("castle.png");
        p.setLayout(new BorderLayout(8, 8));

        JLabel topTitle = label("CHOOSE YOUR CHARACTER", 28,
                selectedCharacter.equals("Girl") ? PINK : BLUE);
        topTitle.setHorizontalAlignment(SwingConstants.CENTER);
        topTitle.setBorder(new EmptyBorder(12, 0, 4, 0));
        p.add(topTitle, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(12, 8));
        center.setOpaque(false);

        JPanel previewPanel = new JPanel(new BorderLayout(4, 8));
        // Transparent preview: do not draw a white rectangle behind the character.
        previewPanel.setOpaque(false);
        previewPanel.setBackground(new Color(0, 0, 0, 0));
        previewPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(83, 190, 255), 2),
                new EmptyBorder(8, 8, 8, 8)));
        previewPanel.setPreferredSize(new Dimension(190, 390));
        previewPanel.setMinimumSize(new Dimension(125, 190));
        JLabel characterPreview = new JLabel();
        characterPreview.setHorizontalAlignment(SwingConstants.CENTER);
        characterPreview.setVerticalAlignment(SwingConstants.CENTER);
        previewPanel.add(characterPreview, BorderLayout.CENTER);

        JLabel selectedLabel = label("", 14, new Color(20, 45, 80));
        selectedLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewPanel.add(selectedLabel, BorderLayout.SOUTH);
        center.add(previewPanel, BorderLayout.WEST);
        center.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                int availableW = center.getWidth();
                int availableH = center.getHeight();
                if (availableW < 760) {
                    previewPanel.setPreferredSize(new Dimension(Math.max(125, availableW - 20), Math.max(190, Math.min(250, availableH / 3))));
                } else {
                    previewPanel.setPreferredSize(new Dimension(Math.max(190, Math.min(250, availableW / 4)), Math.max(300, availableH - 10)));
                }
                BufferedImage selectedImage = load(frontFacingFile(selectedCharacter));
                if (selectedImage != null) {
                    int pw = Math.max(80, previewPanel.getWidth() - 24);
                    int ph = Math.max(100, previewPanel.getHeight() - 60);
                    characterPreview.setIcon(new ImageIcon(scaleCharacter(selectedImage, Math.min(pw, 175), Math.min(ph, 315))));
                }
                previewPanel.revalidate();
            }
        });

        JPanel options = new JPanel();
        options.setOpaque(false);
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.setBorder(new EmptyBorder(4, 4, 4, 8));

        JLabel hint = label("Choose a character, then choose 1 of 4 variants.", 16, GOLD);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        options.add(hint);
        options.add(Box.createVerticalStrut(8));

        JPanel genders = new JPanel(new GridLayout(1, 2, 8, 0));
        genders.setOpaque(false);
        JButton boy = smallBlue("BOY");
        JButton girl = smallBlue("GIRL");
        genders.add(boy);
        genders.add(girl);
        options.add(genders);
        options.add(Box.createVerticalStrut(10));

        JLabel variantTitle = label("CHARACTER VARIANTS", 14, CYAN);
        variantTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        options.add(variantTitle);
        options.add(Box.createVerticalStrut(5));

        JPanel variants = new JPanel(new GridLayout(2, 2, 8, 8));
        variants.setOpaque(false);
        JButton[] variantButtons = new JButton[4];
        for (int i = 0; i < 4; i++) {
            final int variant = i;
            variantButtons[i] = characterButton(frontFacingFile(selectedCharacter), "VARIANT " + (i + 1));
            variantButtons[i].setPreferredSize(new Dimension(150, 220));
            variantButtons[i].addActionListener(e -> {
                // A variant changes only the outfit. It must never change the
                // selected gender (Boy/Girl). This keeps Boy variants as Boy
                // even after the variant images are refreshed.
                String chosenCharacter = selectedCharacter;
                selectedOutfit = variant;
                selectedCharacter = chosenCharacter;
                refreshVariantButtons(variantButtons);
                refreshCharacterPreview(characterPreview, topTitle);
                selectedLabel.setText("SELECTED: " + selectedCharacter.toUpperCase() + " • VARIANT " + (selectedOutfit + 1));
            });
            variants.add(variantButtons[i]);
        }
        options.add(variants);
        center.add(options, BorderLayout.CENTER);

        boy.addActionListener(e -> {
            selectedCharacter = "Boy";
            refreshVariantImages(variantButtons);
            refreshCharacterPreview(characterPreview, topTitle);
            selectedLabel.setText("SELECTED: BOY • VARIANT " + (selectedOutfit + 1));
        });
        girl.addActionListener(e -> {
            selectedCharacter = "Girl";
            refreshVariantImages(variantButtons);
            refreshCharacterPreview(characterPreview, topTitle);
            selectedLabel.setText("SELECTED: GIRL • VARIANT " + (selectedOutfit + 1));
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        JButton cont = gold("SAVE CHARACTER & CONTINUE");
        cont.addActionListener(e -> {
            savedCharacters.put(username, selectedCharacter);
            savedOutfits.put(username, selectedOutfit);
            // Keep the old style map for compatibility with existing saved data,
            // but the new character system uses only the four variants.
            savedStyles.put(username, new int[]{selectedTop, selectedBottom, selectedShoes, selectedAccessory});
            saveUserDatabase();
            refreshDashboard();
            refreshProfile();
            refreshMenuRunner();
            showScreen("DASHBOARD");
        });
        bottom.add(cont);
        p.add(center, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);

        refreshVariantButtons(variantButtons);
        refreshCharacterPreview(characterPreview, topTitle);
        selectedLabel.setText("SELECTED: " + selectedCharacter.toUpperCase() + " • VARIANT " + (selectedOutfit + 1));
        return p;
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

        JLabel musicStatus = label(musicEnabled ? "♫ ON" : "♫ OFF", 12, musicEnabled ? MINT : PEACH);
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

        mainMenuXpLabel = label("★  XP: " + (showZeroXpAfterCancelledRun ? 0 : savedXp), 19, GOLD);
        mainMenuXpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        menu.add(mainMenuXpLabel);
        menu.add(Box.createVerticalStrut(8));

        JLabel subtitle = label("READY TO ESCAPE?", 17, CREAM);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        menu.add(subtitle);
        menu.add(Box.createVerticalStrut(18));

        // Keep the selected color variant, using its front-facing still image.
        menuRunner = null;
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
            stopMiniRunners();
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

    private void stopMiniRunners() {
        if (menuRunner != null) menuRunner.stop();
    }

    private void refreshMainMenuXp() {
        if (mainMenuXpLabel != null) mainMenuXpLabel.setText("★  XP: " + (showZeroXpAfterCancelledRun ? 0 : savedXp));
    }

    private void refreshMenuRunner() {
        if (menuRunner != null) {
            menuRunner.setCharacter(
                runnerOutfitFile(selectedCharacter, selectedOutfit)
            );
        }
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
        music.setSelected(musicEnabled);
        music.setForeground(WHITE);
        music.setBackground(NAVY);
        music.setFont(new Font("SansSerif", Font.BOLD, 17));
        music.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSlider volume = new JSlider(0, 100, Math.round(musicVolume * 100));
        volume.setOpaque(false);
        volume.setAlignmentX(Component.CENTER_ALIGNMENT);
        volume.setMaximumSize(new Dimension(280, 45));

        JLabel volumeText = label("Volume: " + volume.getValue() + "%", 15, CREAM);
        volumeText.setAlignmentX(Component.CENTER_ALIGNMENT);

        music.addActionListener(e -> {
            musicEnabled = music.isSelected();
            if (musicEnabled) startBackgroundMusic();
            else stopBackgroundMusic();
            if (musicStatus != null) {
                musicStatus.setText(musicEnabled ? "♫ MUSIC ON" : "♫ MUSIC OFF");
                musicStatus.setForeground(musicEnabled ? MINT : PEACH);
            }
        });

        volume.addChangeListener(e -> {
            musicVolume = volume.getValue() / 100f;
            setMusicVolume(musicVolume);
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
        BackgroundPanel p = new BackgroundPanel("castle.png");
        p.setLayout(new BorderLayout());

        // Logo stays in the upper-left while the dashboard content is split
        // into two clear sections: character/XP on the LEFT and buttons on
        // the RIGHT, matching the requested main-menu layout.
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(10, 16, 0, 18));

        JLabel logo = image("logo.png", 105, 62);
        top.add(logo, BorderLayout.WEST);
        p.add(top, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(20, 28, 28, 28));

        GridBagConstraints leftGbc = new GridBagConstraints();
        leftGbc.gridx = 0;
        leftGbc.gridy = 0;
        leftGbc.weightx = 0.52;
        leftGbc.weighty = 1.0;
        leftGbc.fill = GridBagConstraints.BOTH;
        leftGbc.anchor = GridBagConstraints.CENTER;
        leftGbc.insets = new Insets(0, 0, 0, 22);

        GridBagConstraints rightGbc = new GridBagConstraints();
        rightGbc.gridx = 1;
        rightGbc.gridy = 0;
        rightGbc.weightx = 0.48;
        rightGbc.weighty = 1.0;
        rightGbc.fill = GridBagConstraints.BOTH;
        rightGbc.anchor = GridBagConstraints.CENTER;
        rightGbc.insets = new Insets(0, 22, 0, 0);

        // ================= LEFT: CHARACTER + XP =================
        JPanel characterPanel = new JPanel();
        characterPanel.setOpaque(false);
        characterPanel.setLayout(new BoxLayout(characterPanel, BoxLayout.Y_AXIS));

        JLabel welcomeTitle = label("CODE ESCAPE", 32, GOLD);
        welcomeTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcomeTitle.setHorizontalAlignment(SwingConstants.CENTER);
        characterPanel.add(welcomeTitle);

        dashboardWelcome = label(
            "WELCOME, " + (username.isEmpty() ? "PLAYER" : username).toUpperCase(),
            17,
            CREAM
        );
        dashboardWelcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        characterPanel.add(Box.createVerticalStrut(5));
        characterPanel.add(dashboardWelcome);
        characterPanel.add(Box.createVerticalStrut(12));

        JPanel characterCard = new JPanel();
        characterCard.setOpaque(true);
        characterCard.setBackground(new Color(5, 28, 67, 225));
        characterCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(88, 218, 255), 2),
            new EmptyBorder(12, 18, 14, 18)
        ));
        characterCard.setLayout(new BoxLayout(characterCard, BoxLayout.Y_AXIS));
        characterCard.setAlignmentX(Component.CENTER_ALIGNMENT);

        dashboardAvatar = image(
            outfitFile(selectedCharacter, selectedOutfit),
            210,
            300
        );
        dashboardAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        characterCard.add(dashboardAvatar);

        dashboardCharacterLabel = label(
            selectedCharacter.toUpperCase(),
            25,
            selectedCharacter.equals("Girl") ? PINK : CYAN
        );
        dashboardCharacterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        characterCard.add(Box.createVerticalStrut(4));
        characterCard.add(dashboardCharacterLabel);

        dashboardXpLabel = label("★  XP: " + (xp > 0 ? xp : savedXp), 28, GOLD);
        dashboardXpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        characterCard.add(Box.createVerticalStrut(4));
        characterCard.add(dashboardXpLabel);

        characterPanel.add(characterCard);

        // ================= RIGHT: MENU BUTTONS =================
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        JLabel menuTitle = label("MAIN MENU", 30, GOLD);
        menuTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(menuTitle);
        buttonPanel.add(Box.createVerticalStrut(18));

        JButton play = dashboardActionButton("▶  PLAY", GOLD);
play.addActionListener(e -> startNewLesson());
buttonPanel.add(play);
buttonPanel.add(Box.createVerticalStrut(12));

JButton profile = dashboardActionButton("PROFILE", BLUE);
profile.addActionListener(e -> {
    refreshProfile();
    showScreen("PROFILE");
});
buttonPanel.add(profile);
buttonPanel.add(Box.createVerticalStrut(12));

JButton exit = dashboardActionButton("EXIT", RED);
exit.addActionListener(e -> {
    // Do not save a still-active lesson attempt when leaving the game.
    xp = 0;
    stopBackgroundMusic();
    System.exit(0);
});
buttonPanel.add(exit);

content.add(characterPanel, leftGbc);
content.add(buttonPanel, rightGbc);
p.add(content, BorderLayout.CENTER);

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
        int displayedXp = showZeroXpAfterCancelledRun ? 0 : savedXp;
        dashboardXpLabel.setText("★  XP: " + displayedXp);
        dashboardXpLabel.setForeground(GOLD);
    }

    root.revalidate();
    root.repaint();
}
    
    private void startNewLesson() {
        // Every new lesson is a fresh attempt. Previously committed XP remains
        // in savedXp and is shown in Profile.
        xp = 0;
        showZeroXpAfterCancelledRun = false;
        sessionCommitted = false;
        hearts = 3;
        level = 1;
        questionIndex = 0;
        correctAnswers = 0;
        playLevelMusic(1);

        // Shuffle the questions for every new attempt so the player cannot
        // memorize the fixed question order. Each level still contains its
        // own five questions.
        sessionQuestions = new Question[questions.length][];
        for (int i = 0; i < questions.length; i++) {
            java.util.List<Question> attemptQuestions = new ArrayList<>(Arrays.asList(questions[i]));
            // Shuffle all ten together: syntax and multiple-choice questions can
            // appear in any position on every attempt.
            Collections.shuffle(attemptQuestions);
            // Clone each question and shuffle its answer choices independently.
            // Move the correct-answer index with the correct text so grading
            // remains accurate regardless of the displayed button position.
            for (int qi = 0; qi < attemptQuestions.size(); qi++) {
                Question original = attemptQuestions.get(qi);
                if (!original.syntax && original.answers != null && original.answers.length > 1) {
                    java.util.List<Integer> order = new ArrayList<>();
                    for (int a = 0; a < original.answers.length; a++) order.add(a);
                    Collections.shuffle(order);
                    String[] shuffledAnswers = new String[original.answers.length];
                    int shuffledCorrect = -1;
                    for (int a = 0; a < order.size(); a++) {
                        int oldIndex = order.get(a);
                        shuffledAnswers[a] = original.answers[oldIndex];
                        if (oldIndex == original.correct) shuffledCorrect = a;
                    }
                    attemptQuestions.set(qi, new Question(original.text, shuffledAnswers,
                            shuffledCorrect, false, original.expected));
                } else {
                    attemptQuestions.set(qi, new Question(original.text,
                            original.answers == null ? new String[0] : original.answers.clone(),
                            original.correct, original.syntax, original.expected));
                }
            }
            sessionQuestions[i] = attemptQuestions.toArray(new Question[0]);
        }

        startQuestion();
    }
    
    private JPanel createStoryScreen(
        String titleText, String storyText, String buttonText, String background, Runnable nextAction) {

        BackgroundPanel p = new BackgroundPanel(background);
        p.setLayout(new GridBagLayout());

        JPanel content = new JPanel(new BorderLayout(10, 16));
        content.setOpaque(false);
        int screenW = getWidth() > 0 ? getWidth() : 430;
        int screenH = getHeight() > 0 ? getHeight() : 760;
        int cw = Math.min(980, Math.max(320, screenW - 34));
        int ch = Math.min(680, Math.max(470, screenH - 55));
        content.setPreferredSize(new Dimension(cw, ch));
        content.setMaximumSize(new Dimension(980, 680));
        content.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel title = label(titleText, screenW <= 500 ? 25 : 30, GOLD);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title, BorderLayout.NORTH);

        JPanel storyHolder = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 43, 72, 158));
                g2.fillRoundRect(12,12,getWidth()-24,getHeight()-24,28,28);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        storyHolder.setOpaque(false);
        storyHolder.setBorder(new EmptyBorder(14, 18, 14, 18));
        // Keep the story card compact so it does not dominate the screen.
        // The text itself is wrapped inside a fixed-width area below.
        int storyCardWidth = Math.min(cw - 36, screenW <= 500 ? 360 : 620);
        storyHolder.setPreferredSize(new Dimension(Math.max(300, storyCardWidth), 205));
        storyHolder.setMaximumSize(new Dimension(Math.max(300, storyCardWidth), 220));

        JLabel story = new JLabel();
        story.setHorizontalAlignment(SwingConstants.CENTER);
        story.setVerticalAlignment(SwingConstants.CENTER);
        story.setForeground(Color.WHITE);
        story.setFont(new Font("SansSerif", Font.BOLD, screenW <= 500 ? 19 : 23));
        story.setOpaque(false);
        int storyWidth = Math.max(260, Math.min(storyCardWidth - 28, screenW <= 500 ? 315 : 560));
        int storyHeight = 165;
        story.setPreferredSize(new Dimension(storyWidth, storyHeight));
        story.setMaximumSize(new Dimension(storyWidth, storyHeight));
        java.util.List<int[]> initialRanges = buildStoryRanges(storyText);
        String initialLine = initialRanges.isEmpty() ? storyText : storyText.substring(initialRanges.get(0)[0], initialRanges.get(0)[1]).trim();
        story.setText(wrapStoryHtml(initialLine, screenW <= 500 ? 23 : 36));
        activeStoryLabel = story;
        activeStoryRanges = buildStoryRanges(storyText);
        activeStoryCurrent = -1;
        storyHolder.add(story, new GridBagConstraints());
        content.add(storyHolder, BorderLayout.CENTER);

        JButton read=blue("🔊 READ STORY"); read.setPreferredSize(new Dimension(165,48)); read.addActionListener(e->speakStoryKaraoke(storyText,story));
        JButton skip=blue("SKIP ▶"); skip.setPreferredSize(new Dimension(135,48)); skip.addActionListener(e->{stopStorySpeech();nextAction.run();});
        JButton next=gold(buttonText); next.setPreferredSize(new Dimension(190,48)); next.addActionListener(e->{stopStorySpeech();nextAction.run();});
        JPanel bottom=new JPanel(new FlowLayout(FlowLayout.CENTER,10,5)); bottom.setOpaque(false); bottom.add(read); bottom.add(skip); bottom.add(next);
        content.add(bottom,BorderLayout.SOUTH);

        p.addComponentListener(new java.awt.event.ComponentAdapter(){
            @Override public void componentShown(java.awt.event.ComponentEvent e){
                speakStoryKaraoke(storyText,story);
            }
            @Override public void componentHidden(java.awt.event.ComponentEvent e){stopStorySpeech();}
        });
        p.add(content);
        return p;
    }

    private String htmlEscape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("\n", "<br>");
    }

    private java.util.List<int[]> buildStoryRanges(String text) {
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

    private void showStoryLyric(JLabel label, String text, int index, java.util.List<int[]> ranges) {
        if (label == null || index < 0 || index >= ranges.size()) return;
        String line = text.substring(ranges.get(index)[0], ranges.get(index)[1]).trim();
        int maxChars = label.getWidth() > 500 ? 36 : 23;
        String html = wrapStoryHtml(line, maxChars);
        SwingUtilities.invokeLater(() -> {
            label.setText(html);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            activeStoryCurrent = index;
            fadeStoryLine(label);
        });
    }

    // Wrap every story sentence into several short lines. Keeping the line
    // length controlled is more reliable than relying on Swing HTML width,
    // especially on smaller laptop screens where the old text was clipped.
    private String wrapStoryHtml(String text, int maxChars) {
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
        javax.swing.Timer fade = new javax.swing.Timer(25, null);
        fade.addActionListener(e -> {
            double t = Math.min(1.0, (System.currentTimeMillis() - started) / 360.0);
            int r = (int)Math.round(start.getRed() + (target.getRed() - start.getRed()) * t);
            int g = (int)Math.round(start.getGreen() + (target.getGreen() - start.getGreen()) * t);
            int b = (int)Math.round(start.getBlue() + (target.getBlue() - start.getBlue()) * t);
            label.setForeground(new Color(r,g,b));
            if (t >= 1.0) ((javax.swing.Timer)e.getSource()).stop();
        });
        fade.setRepeats(true);
        fade.start();
    }

    // ==================== STORY TEXT-TO-SPEECH / KARAOKE ====================
    private void speakStoryKaraoke(String text, JLabel label) {
        stopStorySpeech();
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            // Keep the on-screen story usable even when Windows text-to-speech
            // is unavailable.
            if (label != null) label.setText(wrapStoryHtml(text, label.getWidth() > 500 ? 36 : 23));
            return;
        }

        final int generation = ++storySpeechGeneration;
        final java.util.List<int[]> ranges = buildStoryRanges(text);
        activeStoryLabel = label;
        activeStoryRanges = ranges;
        activeStoryCurrent = -1;

        // Pause background music while narration plays, then resume it afterward.
        storyMusicWasPlaying = backgroundMusic != null && backgroundMusic.isRunning();
        if (storyMusicWasPlaying) backgroundMusic.stop();

        // Use one PowerShell process for the whole story. PowerShell reports the
        // sentence index immediately before speaking it, so karaoke text follows
        // the actual speech instead of relying on guessed sentence durations.
        storySpeechThread = new Thread(() -> {
            try {
                StringBuilder speechText = new StringBuilder();
                for (int i = 0; i < ranges.size(); i++) {
                    if (i > 0) speechText.append((char)30);
                    speechText.append(text.substring(ranges.get(i)[0], ranges.get(i)[1]).trim());
                }
                String encoded = Base64.getEncoder().encodeToString(
                        speechText.toString().getBytes(StandardCharsets.UTF_8));
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
                Process process = new ProcessBuilder("powershell.exe", "-NoProfile",
                        "-ExecutionPolicy", "Bypass", "-Command", command)
                        .redirectErrorStream(true).start();
                storySpeechProcess = process;
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                String outputLine;
                while ((outputLine = reader.readLine()) != null) {
                    if (generation != storySpeechGeneration || Thread.currentThread().isInterrupted()) {
                        process.destroy();
                        break;
                    }
                    if (outputLine.startsWith("LINE:")) {
                        try {
                            int sentenceIndex = Integer.parseInt(outputLine.substring(5).trim());
                            showStoryLyric(label, text, sentenceIndex, ranges);
                        } catch (NumberFormatException ignored) {
                            // Ignore non-index output from PowerShell.
                        }
                    }
                }
                process.waitFor();
            } catch (Exception ignored) {
                // If narration fails, keep the story screen and navigation usable.
            } finally {
                if (generation == storySpeechGeneration) {
                    activeStoryCurrent = -1;
                    SwingUtilities.invokeLater(() -> {
                        if (label != null && generation == storySpeechGeneration) {
                            label.setText("<html><div style='text-align:center;'>Story complete!</div></html>");
                        }
                    });
                    storySpeechThread = null;
                    storySpeechProcess = null;
                    if (storyMusicWasPlaying && musicEnabled && backgroundMusic != null) {
                        backgroundMusic.start();
                    }
                    storyMusicWasPlaying = false;
                }
            }
        }, "CodeEscape-Story-Karaoke");
        storySpeechThread.setDaemon(true);
        storySpeechThread.start();
    }

    private void stopStorySpeech(){
        storySpeechGeneration++;
        Process p=storySpeechProcess;
        storySpeechProcess=null;
        if(p!=null && p.isAlive()){
            p.destroy();
            try{p.waitFor(250,java.util.concurrent.TimeUnit.MILLISECONDS);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}
        }
        Thread t=storySpeechThread;
        storySpeechThread=null;
        if(t!=null && t!=Thread.currentThread()) t.interrupt();

        if (storyMusicWasPlaying && musicEnabled && backgroundMusic != null && !backgroundMusic.isRunning()) {
            backgroundMusic.start();
        }
        storyMusicWasPlaying=false;
        activeStoryCurrent=-1;
    }

// ==================== INTRO STORY ====================

private JPanel storyIntroScreen() {
    String story =
        "A quake has sealed the ancient Code Dungeon, trapping you beneath a ruined digital kingdom. " +
        "Your chosen explorer enters the first chamber with only three hearts and a flickering map. " +
        "Decode the HTML ruins to open the stone gate, cross the CSS shadow maze, and reactivate the JavaScript relics. " +
        "Then survive the Debugger Bridge, where corrupted traps guard the path to the final vault. " +
        "The last chamber combines every skill. Solve its challenges, claim the key, and escape before the dungeon resets.";

    return createStoryScreen(
        "CODE ESCAPE — SYSTEM FAILURE",
        story,
        "BEGIN JOURNEY",
        "castle.png",
        () -> {
            refreshDashboard();
            refreshProfile();
            showScreen("CHARACTER");
        }
    );
}

// ==================== LEVEL 2 STORY ====================

private JPanel storyLevel2Screen() {
    String story =
        "The stone gate slams shut behind you as you enter the Shadow Maze. Its walls shift whenever the styling runes are wrong. " +
        "Use CSS selectors, colors, spacing, and layout properties to light the rune torches and reveal the hidden passage. " +
        "A wrong move wakes the dungeon traps, so study each clue before advancing.";

    return createStoryScreen("LEVEL 2 — THE STYLED MAZE", story, "ENTER LEVEL 2", "level2.png", () -> runToNextLevel());
}

// ==================== LEVEL 3 STORY ====================

private JPanel storyLevel3Screen() {
    String story =
        "Beyond the maze lies the Relic Chamber, where ancient machines guard a bridge over a glowing abyss. " +
        "The mechanisms are frozen until JavaScript variables, operators, conditions, and functions restore their logic. " +
        "Activate the relics in time and the bridge will rise from the darkness.";

    return createStoryScreen("LEVEL 3 — THE LOGIC CHAMBER", story, "ENTER LEVEL 3", "level3.png", () -> runToNextLevel());
}

// ==================== LEVEL 4 STORY ====================

private JPanel storyLevel4Screen() {
    String story =
        "The bridge leads into the Bugkeeper's Gauntlet. Broken code has become living traps, and every warning is a clue. " +
        "Find missing tags, misspelled names, and broken syntax to disarm the traps and reach the dungeon's inner door.";

    return createStoryScreen("LEVEL 4 — THE CODE BRIDGE", story, "ENTER LEVEL 4", "level4.png", () -> runToNextLevel());
}

// ==================== LEVEL 5 STORY ====================

private JPanel storyLevel5Screen() {
    String story =
        "At last, you reach the Dragon Vault beneath the dungeon. Its lock is powered by one master program, and every skill must work together. " +
        "Build the structure with HTML, style it with CSS, activate it with JavaScript, and debug every hidden fault. " +
        "Complete the challenge, claim the ancient key, and escape with the dungeon treasure.";

    return createStoryScreen("LEVEL 5 — THE FINAL CODE", story, "FACE THE FINAL CODE", "level5.png", () -> runToNextLevel());
}

// ==================== ENDING ====================

private JPanel endingScreen() {
    BackgroundPanel background = new BackgroundPanel("castle.png");
    background.setLayout(new BorderLayout());
    JLabel title = label("THE FINAL TREASURE", 30, GOLD);
    title.setHorizontalAlignment(SwingConstants.CENTER);
    title.setBorder(new EmptyBorder(14, 8, 4, 8));
    background.add(title, BorderLayout.NORTH);

    JLabel instructions = label("YOUR CHARACTER IS RUNNING TOWARD THE ANCIENT VAULT...", 15, GOLD);
    instructions.setHorizontalAlignment(SwingConstants.CENTER);
    instructions.setBorder(new EmptyBorder(4, 8, 12, 8));
    background.add(instructions, BorderLayout.SOUTH);

    // Capture the player's exact selection once for this scene. The animation must
    // not resolve its runner and standing pose from mutable UI selection state.
    final String endingCharacter = selectedCharacter;
    final int endingVariant = Math.max(0, Math.min(3, selectedOutfit));
    final String endingRunner = runnerOutfitFile(endingCharacter, endingVariant);
    final String endingStanding = characterVariantFile(endingCharacter, endingVariant);
    final String endingVictory = victoryPoseFile(endingCharacter, endingVariant);
    EndingScene scene = new EndingScene(endingRunner, endingStanding, endingVictory, instructions);
    background.add(scene, BorderLayout.CENTER);
    SwingUtilities.invokeLater(scene::start);
    return background;
}

/** Maps the eight authored victory poses to the exact gender and color selected by the player. */
private String victoryPoseFile(String character, int variantIndex) {
    String gender = "Girl".equalsIgnoreCase(character) ? "girl" : "boy";
    String[] colors = {"green", "blue", "purple", "orange"};
    int index = Math.max(0, Math.min(3, variantIndex));
    return "victory_" + gender + "_" + colors[index] + ".png";
}

/** Animated final scene: run, stop, jump, reveal a clickable key, fly to a new vault, open it, and celebrate. */
private class EndingScene extends JPanel {
    private static final int KEY_WAITING = 0, KEY_FLYING = 1, OPENING = 2, OPENED = 3;
    private final BufferedImage runSheet;
    private final BufferedImage[] endingRunFrames;
    private final BufferedImage standing;
    private final BufferedImage victoryPose;
    private final BufferedImage keyImage;
    private final JLabel instructions;
    private final javax.swing.Timer timer;
    // The selected victory pose is visible immediately when the ending loads.
    // The key interaction is independent and only controls the vault opening.
    private int stage = KEY_WAITING;
    private long stageStarted;
    private double runnerX = -120;
    private double keyProgress = 0;
    private int keyFlightStartX = 0;
    private int keyFlightStartY = 0;
    private double lidAngle = 0;
    private boolean completionShown = false;

    EndingScene(String runFile, String standingFile, String victoryFile, JLabel instructions) {
        this.runSheet = load(runFile);
        this.endingRunFrames = loadIsolatedWalkFrames(runFile);
        this.standing = load(standingFile);
        this.victoryPose = load(victoryFile);
        this.keyImage = load("golden_key.png");
        this.instructions = instructions;
        setOpaque(false);
        setPreferredSize(new Dimension(850, 520));
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (stage == KEY_WAITING && keyBounds().contains(e.getPoint())) {
                    Rectangle key = keyBounds();
                    keyFlightStartX = key.x;
                    keyFlightStartY = key.y;
                    keyProgress = 0;
                    stage = KEY_FLYING;
                    stageStarted = System.currentTimeMillis();
                    instructions.setText("THE KEY IS FLYING TO THE ANCIENT VAULT!");
                    repaint();
                }
            }
        });
        timer = new javax.swing.Timer(30, e -> tick());
    }

    void start() {
        stageStarted = System.currentTimeMillis();
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        int characterHeight = Math.min((int)(h * .78), 405);
        int characterWidth = victoryPose == null ? Math.max(58, (int)(characterHeight * .48))
                : Math.max(90, (int)(characterHeight * victoryPose.getWidth() / (double)victoryPose.getHeight()));
        runnerX = w / 2.0 - characterWidth / 2.0;
        instructions.setText("YOUR VICTORY POSE IS READY! CLICK THE KEY TO UNLOCK THE VAULT.");
        timer.start();
    }

    private void tick() {
        long elapsed = System.currentTimeMillis() - stageStarted;
        int w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        double characterWidth = Math.max(58, Math.min((int)(h * .48), 245) * .48);
        double targetX = w * .50 - characterWidth / 2.0;
        if (stage == KEY_FLYING) {
            keyProgress = Math.min(1.0, elapsed / 720.0);
            if (keyProgress >= 1.0) { stage = OPENING; stageStarted = System.currentTimeMillis(); lidAngle = 0; instructions.setText("THE ANCIENT VAULT IS OPENING..."); }
        } else if (stage == OPENING) {
            double openT = Math.min(1.0, elapsed / 760.0);
            lidAngle = openT * openT * (3.0 - 2.0 * openT);
            if (lidAngle >= 1.0) {
                stage = OPENED; timer.stop(); instructions.setText("THE VAULT IS OPEN! TREASURE UNLOCKED.");
                repaint();
                if (!completionShown) { completionShown = true; javax.swing.Timer popup = new javax.swing.Timer(550, ev -> openTreasureChest()); popup.setRepeats(false); popup.start(); }
            }
        }
        repaint();
    }

    private Rectangle keyBounds() {
        int w = getWidth(), h = getHeight();
        int kw = Math.max(48, (int)(w * 0.09)), kh = Math.max(30, (int)(h * 0.09));
        int characterHeight = Math.min((int)(h * .78), 405);
        int characterWidth = victoryPose == null ? Math.max(58, (int)(characterHeight * .48))
                : Math.max(90, (int)(characterHeight * victoryPose.getWidth() / (double)victoryPose.getHeight()));
        int x = w / 2 - kw / 2;
        int popLift = stage == KEY_WAITING
                ? (int)(Math.sin(Math.min(1.0, (System.currentTimeMillis() - stageStarted) / 420.0) * Math.PI / 2.0) * Math.min(30, h * 0.07)) : 0;
        int y = h - characterHeight - 14 - 52 - popLift;
        return new Rectangle(x, y, kw, kh);
    }

    private void drawImageFit(Graphics2D g, BufferedImage im, int x, int y, int maxW, int maxH) {
        if (im == null) return;
        double scale = Math.min(maxW / (double)im.getWidth(), maxH / (double)im.getHeight());
        int dw = Math.max(1, (int)(im.getWidth() * scale)), dh = Math.max(1, (int)(im.getHeight() * scale));
        g.drawImage(im, x, y, dw, dh, null);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int w = getWidth(), h = getHeight();
        // Draw a detailed illustrated wooden chest, perfectly centered above the character.
        BufferedImage closedChest = load("treasure_chest_real.png");
        BufferedImage chestBody = load("treasure_chest_body.png");
        BufferedImage chestLid = load("treasure_chest_lid.png");
        int vw = Math.min(Math.max(145, (int)(w * .25)), Math.max(145, w - 56));
        int vh = closedChest == null ? Math.max(125, (int)(h * .30))
                : Math.max(125, (int)(vw * closedChest.getHeight() / (double)closedChest.getWidth()));
        int vx = (w - vw) / 2;
        int vy = Math.max(8, (int)(h * .035));
        int lockX = vx + vw / 2;
        int lockY = vy + (int)(vh * .49);
        if (stage == OPENING || stage == OPENED) {
            // Keep the chest compact. The lid opens upward by moving vertically,
            // rather than spinning around the lock.
            if (chestBody != null) {
                g.drawImage(chestBody, vx, vy + (int)(vh * .12), vw, (int)(vh * .88), null);
            } else if (closedChest != null) {
                // Draw the lower portion only; do not leave a second closed lid under the animated lid.
                g.drawImage(closedChest, vx, vy + (int)(vh * .25), vw, (int)(vh * .75), 0,
                        (int)(closedChest.getHeight() * .25), closedChest.getWidth(), closedChest.getHeight(), null);
            }
            int openingY = vy + (int)(vh * .17);
            int openingH = Math.max(16, (int)(vh * .22));
            g.setColor(new Color(45, 21, 13, 235));
            g.fillRoundRect(vx + (int)(vw * .13), openingY, (int)(vw * .74), openingH, 12, 12);
            if (chestLid != null) {
                int lidH = Math.max(35, (int)(vh * .42));
                int lift = (int)(lidAngle * lidH * 0.82);
                Graphics2D lidGraphics = (Graphics2D) g.create();
                lidGraphics.rotate(-Math.toRadians(9.0 * lidAngle), vx + vw / 2.0, vy + lidH * 0.78);
                lidGraphics.drawImage(chestLid, vx, vy - lift, vw, lidH, null);
                lidGraphics.dispose();
            }
            g.setColor(new Color(255, 211, 65, 35 + (int)(90 * lidAngle)));
            g.fillOval(vx - 12, vy - 8, vw + 24, vh + 36);
            g.setColor(new Color(255, 231, 125));
            for (int i = 0; i < 14; i++) {
                double a = i * Math.PI / 7.0;
                int sx = lockX + (int)(Math.cos(a) * (25 + lidAngle * 55));
                int sy = vy + vh / 3 + (int)(Math.sin(a) * (18 + lidAngle * 36));
                g.fillOval(sx, sy, 5, 5);
            }
        } else if (closedChest != null) {
            g.drawImage(closedChest, vx, vy, vw, vh, null);
        } else {
            // Safe fallback if the chest image is missing from a local build.
            g.setColor(new Color(110, 57, 24));
            g.fillRoundRect(vx, vy, vw, vh, 18, 18);
            g.setColor(new Color(238, 178, 51));
            g.setStroke(new BasicStroke(Math.max(5, vw / 45f)));
            g.drawRoundRect(vx, vy, vw, vh, 18, 18);
            g.fillRoundRect(lockX - 15, lockY - 12, 30, 36, 7, 7);
            g.setColor(new Color(50, 30, 15));
            g.fillOval(lockX - 4, lockY - 2, 8, 8);
        }
        // Display the exact selected victory pose as soon as the ending screen loads.
        // Do not wait for the key click or chest animation to reveal the pose.
        int maxVictoryW = Math.max(110, (int)(w * 0.34));
        int maxVictoryH = Math.max(180, (int)(h * 0.78));
        // Never show the old standing sprite: only the matching victory pose is valid here.
        BufferedImage finalCharacter = victoryPose;
        if (finalCharacter != null) {
            double victoryScale = Math.min(maxVictoryW / (double)finalCharacter.getWidth(),
                                           maxVictoryH / (double)finalCharacter.getHeight());
            int victoryW = Math.max(1, (int)(finalCharacter.getWidth() * victoryScale));
            int victoryH = Math.max(1, (int)(finalCharacter.getHeight() * victoryScale));
            int victoryX = (w - victoryW) / 2;
            int victoryY = h - victoryH - 5;
            g.drawImage(finalCharacter, victoryX, victoryY, victoryW, victoryH, null);
            // Gentle celebratory sparkles are present from the moment the pose appears.
            g.setColor(new Color(255, 226, 125, 225));
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8.0;
                int sx = w / 2 + (int)(Math.cos(a) * victoryW * 0.62);
                int sy = victoryY + victoryH / 3 + (int)(Math.sin(a) * victoryH * 0.24);
                g.fillOval(sx, sy, 5, 5);
            }
        } else {
            g.setColor(new Color(255, 225, 150));
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            String missing = "VICTORY POSE NOT FOUND: " + victoryPoseFile(selectedCharacter, selectedOutfit);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(missing, Math.max(12, (w - fm.stringWidth(missing)) / 2), h / 2);
        }
        // The key appears above the character and is clickable; then it flies to the vault lock.
        if (stage == KEY_WAITING || stage == KEY_FLYING) {
            Rectangle b = keyBounds();
            int kx=b.x, ky=b.y;
            if (stage == KEY_FLYING) {
                double t=keyProgress;
                double eased=t*t*(3.0-2.0*t);
                int tx=lockX-18, ty=lockY-12;
                kx=(int)(keyFlightStartX+(tx-keyFlightStartX)*eased);
                ky=(int)(keyFlightStartY+(ty-keyFlightStartY)*eased);
            }
            if (keyImage != null) drawImageFit(g,keyImage,kx,ky,Math.max(50,(int)(w*.10)),Math.max(34,(int)(h*.10)));
            else { g.setColor(GOLD); g.setFont(new Font("SansSerif",Font.BOLD,40)); g.drawString("⚿",kx,ky+35); }
            if (stage == KEY_WAITING) { g.setColor(new Color(255, 220, 90)); g.setStroke(new BasicStroke(2)); g.drawRoundRect(b.x-4,b.y-4,b.width+8,b.height+8,12,12); }
        }
        g.dispose();
    }
}

private void openTreasureChest() {
    playSpecialMusic("victory.wav");
    JDialog dialog = new JDialog(this, "Final Treasure", true);
    dialog.setSize(430, 620);
    dialog.setLocationRelativeTo(this);
    dialog.setLayout(new BorderLayout(10, 10));
    dialog.getContentPane().setBackground(new Color(5, 18, 43));

    JLabel title = label("YOU FOUND THE FINAL TREASURE!", 22, GOLD);
    title.setHorizontalAlignment(SwingConstants.CENTER);
    dialog.add(title, BorderLayout.NORTH);

    JPanel notePanel = new JPanel(new BorderLayout());
    notePanel.setOpaque(false);
    BufferedImage note = load("treasure_note.png");
    if (note != null) {
        JLabel noteImage = new JLabel(new ImageIcon(scale(note, 350, 390)));
        noteImage.setHorizontalAlignment(SwingConstants.CENTER);
        notePanel.add(noteImage, BorderLayout.CENTER);
    }
    dialog.add(notePanel, BorderLayout.CENTER);

    JButton finish = gold("CONTINUE");
    finish.setPreferredSize(new Dimension(180, 48));
    finish.addActionListener(e -> {
        dialog.dispose();
        showEndingXpDialog();
    });
    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bottom.setOpaque(false);
    bottom.add(finish);
    dialog.add(bottom, BorderLayout.SOUTH);
    dialog.setVisible(true);
}

    private void showEndingXpDialog() {
        JDialog result = new JDialog(this, "ATTEMPT RESULTS", true);
        result.setSize(390, 270);
        result.setLocationRelativeTo(this);
        result.setResizable(false);
        JPanel box = new JPanel();
        box.setBackground(new Color(5, 18, 43));
        box.setBorder(new EmptyBorder(22, 24, 22, 24));
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel heading = label("LEVELS COMPLETED!", 24, GOLD);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel earned = label("XP collected this login: " + savedXp, 18, CREAM);
        earned.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(heading);
        box.add(Box.createVerticalStrut(14));
        box.add(earned);
        box.add(Box.createVerticalStrut(24));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actions.setOpaque(false);
        JButton menu = gold("MENU");
        menu.addActionListener(e -> {
            result.dispose();
            refreshDashboard();
            refreshMainMenuXp();
            showScreen("DASHBOARD");
        });
        JButton retry = smallBlue("TRY AGAIN");
        retry.addActionListener(e -> {
            result.dispose();
            startNewLesson();
        });
        actions.add(menu);
        actions.add(retry);
        box.add(actions);
        result.setContentPane(box);
        result.setVisible(true);
    }

    // ==================== QUESTION ====================
   private JPanel questionScreen(){
    questionBackground=new BackgroundPanel("level1.png"); questionBackground.setLayout(new BorderLayout()); questionBackground.add(hud(),BorderLayout.NORTH); JPanel center=new JPanel(new GridBagLayout()); center.setOpaque(false);
    JPanel card=panel(350,540); card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS)); card.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(new Color(111,196,255,220),2,22),new EmptyBorder(14,12,18,12)));
    questionCounterLabel=label("LEVEL 1 • QUESTION 1 / 10",21,GOLD); questionCounterLabel.setAlignmentX(Component.CENTER_ALIGNMENT); questionCounterLabel.setHorizontalAlignment(SwingConstants.CENTER);
    questionLabel=new JLabel(); questionLabel.setFont(new Font("SansSerif",Font.BOLD,20)); questionLabel.setForeground(CREAM); questionLabel.setHorizontalAlignment(SwingConstants.CENTER); questionLabel.setVerticalAlignment(SwingConstants.CENTER);
    JPanel questionHolder=new JPanel(new BorderLayout()); questionHolder.setOpaque(false); questionHolder.setPreferredSize(new Dimension(320,150)); questionHolder.setMinimumSize(new Dimension(280,120)); questionHolder.setMaximumSize(new Dimension(340,180)); questionHolder.add(questionLabel,BorderLayout.CENTER);
    answerGrid=new JPanel(new GridLayout(2,2,8,8)); answerGrid.setOpaque(false); answerGrid.setBorder(new EmptyBorder(12,6,8,6)); answerGrid.setPreferredSize(new Dimension(320,190)); answerGrid.setMaximumSize(new Dimension(340,210));
    for(int i=0;i<4;i++){final int choice=i; answerButtons[i]=answerButton(""); answerButtons[i].addActionListener(e->answer(choice)); answerGrid.add(answerButtons[i]);}
    syntaxPanel=new JPanel(); syntaxPanel.setOpaque(false); syntaxPanel.setLayout(new BoxLayout(syntaxPanel,BoxLayout.Y_AXIS)); JLabel sh=label("Type the missing syntax below:",14,CYAN); sh.setAlignmentX(Component.CENTER_ALIGNMENT); syntaxAnswerField=textField("Complete the syntax here"); syntaxAnswerField.setMaximumSize(new Dimension(320,48)); syntaxSubmitButton=gold("SUBMIT ANSWER"); syntaxSubmitButton.setAlignmentX(Component.CENTER_ALIGNMENT); syntaxSubmitButton.addActionListener(e->answerSyntax()); syntaxAnswerField.addActionListener(e->answerSyntax()); syntaxPanel.add(sh); syntaxPanel.add(Box.createVerticalStrut(10)); syntaxPanel.add(syntaxAnswerField); syntaxPanel.add(Box.createVerticalStrut(12)); syntaxPanel.add(syntaxSubmitButton);
    card.add(Box.createVerticalStrut(12)); card.add(questionCounterLabel); card.add(Box.createVerticalStrut(20)); card.add(questionHolder); card.add(Box.createVerticalStrut(10)); card.add(answerGrid); card.add(syntaxPanel); card.add(Box.createVerticalGlue()); center.add(card); questionBackground.add(center,BorderLayout.CENTER); return questionBackground;
}

    private JPanel hud() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(new Color(19, 53, 88, 245));
        h.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel stats = new JPanel(new GridLayout(1, 3));
        stats.setOpaque(false);
        heartsLabel = label(heartsText(), 24, RED);
        heartsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timerLabel = label("⏱ 15s", 24, GOLD);
        timerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        xpLabel = label("⭐ XP: " + xp, 24, GOLD);
        xpLabel.setHorizontalAlignment(SwingConstants.CENTER);
        stats.add(heartsLabel);
        stats.add(timerLabel);
        stats.add(xpLabel);

        JButton exit = circularExitButton();
        exit.addActionListener(e -> showGameExitDialog());

        h.add(stats, BorderLayout.CENTER);
        h.add(exit, BorderLayout.EAST);
        return h;
    }

    private JButton circularExitButton() {
        JButton b = base("EXIT");
        b.setPreferredSize(new Dimension(105, 42));
        b.setMinimumSize(new Dimension(105, 42));
        b.setMaximumSize(new Dimension(105, 42));
        b.setFont(new Font("SansSerif", Font.BOLD, 17));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(120, 28, 45));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RED, 2),
            new EmptyBorder(6, 18, 6, 18)
        ));
        b.setToolTipText("Exit");
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void showGameExitDialog() {
        if (questionTimer != null) questionTimer.stop();
        acceptingAnswer = false;

        final JDialog dialog = new JDialog(this, "EXIT GAME", true);
        dialog.setUndecorated(true);
        dialog.setSize(360, 230);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel box = new JPanel();
        box.setBackground(new Color(5, 28, 67, 250));
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GOLD, 3),
            new EmptyBorder(22, 25, 22, 25)
        ));
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel title = label("EXIT GAME?", 28, GOLD);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(title);
        box.add(Box.createVerticalStrut(10));

        JLabel msg = label("Your unfinished run will not be saved.", 15, CREAM);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(msg);
        box.add(Box.createVerticalStrut(20));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setOpaque(false);

        JButton continueButton = gold("CONTINUE GAME");
        continueButton.setPreferredSize(new Dimension(150, 45));
        continueButton.addActionListener(e -> {
            dialog.dispose();
            acceptingAnswer = true;
            if (secondsLeft > 0) restartTimerWithRemainingTime();
        });

        JButton exitButton = base("EXIT GAME");
        exitButton.setPreferredSize(new Dimension(130, 45));
        exitButton.setBackground(new Color(120, 28, 45));
        exitButton.setForeground(Color.BLACK);
        exitButton.setBorder(BorderFactory.createLineBorder(RED, 2));
        exitButton.addActionListener(e -> {
            dialog.dispose();
            cancelUnfinishedAttempt();
            showScreen("DASHBOARD");
        });

        buttons.add(continueButton);
        buttons.add(exitButton);
        box.add(buttons);

        dialog.setContentPane(box);
        dialog.setVisible(true);
    }

    private void cancelUnfinishedAttempt() {
        if (questionTimer != null) questionTimer.stop();
        if (runningTimer != null) runningTimer.stop();
        stopStorySpeech();
        acceptingAnswer = false;
        xp = 0;
        showZeroXpAfterCancelledRun = true;
        correctAnswers = 0;
        hearts = 3;
        level = 1;
        questionIndex = 0;
        secondsLeft = 15;
        refreshDashboard();
    }

    private String heartsText() {
        return (hearts >= 1 ? "♥" : "♡") + " " + (hearts >= 2 ? "♥" : "♡") + " " + (hearts >= 3 ? "♥" : "♡");
    }

    private void restartTimerWithRemainingTime() {
        if (questionTimer != null) questionTimer.stop();
        timerLabel.setText("⏱ " + secondsLeft + "s");
        timerLabel.setForeground(secondsLeft <= 5 ? RED : GOLD);
        questionTimer = new javax.swing.Timer(1000, e -> {
            if (!acceptingAnswer) return;
            secondsLeft--;
            timerLabel.setText("⏱ " + secondsLeft + "s");
            timerLabel.setForeground(secondsLeft <= 5 ? RED : GOLD);
            if (secondsLeft <= 0) { questionTimer.stop(); handleTimeout(); }
        });
        questionTimer.start();
    }

    private void startQuestion() {
        level = Math.max(1, Math.min(5, level));
        questionIndex = Math.max(0, Math.min(9, questionIndex));
        questionBackground.setBackgroundImage("level" + level + ".png");
        questionBackground.revalidate(); questionBackground.repaint();
        showScreen("QUESTION");
        loadQuestion();
    }

    private void loadQuestion() {
        Question[][] activeQuestions = (sessionQuestions != null && sessionQuestions.length == questions.length)
                ? sessionQuestions : questions;
        Question q = activeQuestions[level - 1][questionIndex];
        acceptingAnswer = true;
        secondsLeft = q.syntax ? 30 : 15;
        questionCounterLabel.setText("LEVEL " + level + " • QUESTION " + (questionIndex + 1) + " / 10");
        questionLabel.setText(
            "<html><center>" + q.text + "</center></html>"
         );
         questionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
         questionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        questionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        boolean syntax = q.syntax;
        answerGrid.setVisible(!syntax); syntaxPanel.setVisible(syntax);
        if(syntax){syntaxAnswerField.setText("");syntaxAnswerField.setEnabled(true);syntaxSubmitButton.setEnabled(true);} else {for(int i=0;i<4;i++){answerButtons[i].setText(q.answers[i]);answerButtons[i].setEnabled(true);}}
        questionBackground.revalidate(); questionBackground.repaint();
        updateHud();
        restartTimer();
    }

    private void restartTimer() {
        if (questionTimer != null) questionTimer.stop();
        Question[][] activeQuestions = (sessionQuestions != null && sessionQuestions.length == questions.length)
                ? sessionQuestions : questions;
        Question current = activeQuestions[level - 1][questionIndex];
        secondsLeft = current.syntax ? 30 : 15;
        timerLabel.setText("⏱ " + secondsLeft + "s");
        timerLabel.setForeground(GOLD);
        questionTimer = new javax.swing.Timer(1000, e -> {
            if (!acceptingAnswer) return;
            secondsLeft--;
            timerLabel.setText("⏱ " + secondsLeft + "s");
            timerLabel.setForeground(secondsLeft <= 5 ? RED : GOLD);
            if (secondsLeft <= 0) { questionTimer.stop(); handleTimeout(); }
        });
        questionTimer.start();
    }

    private void answer(int choice) {
        if (!acceptingAnswer) return;
        acceptingAnswer = false;
        if (questionTimer != null) questionTimer.stop();
        for (JButton b : answerButtons) b.setEnabled(false);
        Question[][] activeQuestions = (sessionQuestions != null && sessionQuestions.length == questions.length)
                ? sessionQuestions : questions;
        Question q = activeQuestions[level - 1][questionIndex];
        if (choice == q.correct) {
            xp += 10;
            correctAnswers++;
            updateHud();
            refreshDashboard();
            showGamePopup("CORRECT!", "+10 XP", "Great Job! You got the Correct Answer", GREEN);
            afterCorrect();
        } else {
            hearts--;
            updateHud();
            refreshDashboard();
            showGamePopup("NOT QUITE!", "-1 HEART", "Try again! You must perfect this question before moving to the next one.", RED);
            afterWrong();
        }
    }

    private void answerSyntax(){
        if(!acceptingAnswer)return; acceptingAnswer=false; if(questionTimer!=null)questionTimer.stop(); syntaxSubmitButton.setEnabled(false); syntaxAnswerField.setEnabled(false);
        Question[][] active=(sessionQuestions!=null&&sessionQuestions.length==questions.length)?sessionQuestions:questions; Question q=active[level-1][questionIndex]; String typed=syntaxAnswerField.getText().trim().replaceAll("\\s+"," "); String expected=q.expected.trim().replaceAll("\\s+"," ");
        if(typed.equalsIgnoreCase(expected)){xp+=10;correctAnswers++;updateHud();refreshDashboard();showGamePopup("CORRECT!","+10 XP","Great Job! You completed the syntax!",GREEN);afterCorrect();}
        else{hearts--;updateHud();refreshDashboard();showGamePopup("NOT QUITE!","-1 HEART","Check the syntax and try again.",RED);afterWrong();}
    }

    private void handleTimeout() {
        if (!acceptingAnswer) return;
        acceptingAnswer = false;
        for (JButton b : answerButtons) b.setEnabled(false);
        hearts--;
        updateHud();
        refreshDashboard();
        showGamePopup("TIME'S UP!", "-1 HEART", "Time Ran Out. Try the same question again.", RED);
        afterWrong();
    }

    private void afterCorrect() {
        playFeedbackSound(true);
    questionIndex++;

    if (questionIndex >= 10) {

        // Finished Level 5
        if (level >= 5) {
            commitSessionXp();
            showScreen("ENDING");
            return;
        }

        // Move to the next level
        level++;
        questionIndex = 0;

        // Show the story before entering the next level
        switch (level) {
            case 2:
                showScreen("STORY_LEVEL2");
                break;

            case 3:
                showScreen("STORY_LEVEL3");
                break;

            case 4:
                showScreen("STORY_LEVEL4");
                break;

            case 5:
                showScreen("STORY_LEVEL5");
                break;
        }

    } else {
        loadQuestion();
    }
}

    private void afterWrong() {
        playFeedbackSound(false);
        if (hearts <= 0) {
            // Running out of hearts is a completed attempt and DOES save the XP
            // earned during that attempt. Only the explicit Exit action discards it.
            commitSessionXp();
            updateHud();
            refreshDashboard();
            showScreen("GAMEOVER");
            return;
        }
        loadQuestion();
    }

    private void updateHud() {
        if (heartsLabel != null) heartsLabel.setText(heartsText());
        if (xpLabel != null) xpLabel.setText("⭐ XP: " + xp);
        if (dashboardXpLabel != null) dashboardXpLabel.setText("★  XP: " + (showZeroXpAfterCancelledRun ? 0 : savedXp));
        if (timerLabel != null) timerLabel.setText("⏱ " + secondsLeft + "s");
    }

    // ==================== RUNNING ====================
    private void runToNextLevel() {
        if (questionTimer != null) questionTimer.stop();
        if (runningTimer != null) runningTimer.stop();

        BackgroundPanel p = new BackgroundPanel("level" + level + ".png");
        p.setLayout(new BorderLayout(0, 12));
        p.setBorder(new EmptyBorder(16, 20, 18, 20));

        GlassPanel header = new GlassPanel(new Color(10, 25, 48, 210));
        header.setLayout(new BorderLayout(14, 0));
        header.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel title = label("CODE ESCAPE  /  LEVEL " + level, 23, CYAN);
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        JLabel subtitle = label("NEXT AREA UNLOCKING", 12, new Color(210, 225, 245));
        subtitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.add(title);
        titleStack.add(Box.createVerticalStrut(4));
        titleStack.add(subtitle);
        header.add(titleStack, BorderLayout.WEST);
        JLabel levelBadge = label(String.format("%02d  /  05", level), 18, GOLD);
        levelBadge.setFont(new Font("SansSerif", Font.BOLD, 18));
        levelBadge.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(levelBadge, BorderLayout.EAST);
        p.add(header, BorderLayout.NORTH);

        String runnerFile = runnerOutfitFile(selectedCharacter, selectedOutfit);
        playLevelMusic(level);
        MovingRunner runner = new MovingRunner(runnerFile);
        p.add(runner, BorderLayout.CENTER);

        GlassPanel footer = new GlassPanel(new Color(10, 25, 48, 218));
        footer.setLayout(new BorderLayout(14, 8));
        footer.setBorder(new EmptyBorder(13, 20, 13, 20));
        JLabel destination = label("RUNNING TO THE NEXT LEVEL", 15, CREAM);
        destination.setFont(new Font("SansSerif", Font.BOLD, 15));
        footer.add(destination, BorderLayout.WEST);
        JLabel hint = label("KEEP GOING  •  YOU'RE DOING GREAT", 11, CYAN);
        hint.setFont(new Font("SansSerif", Font.BOLD, 11));
        hint.setHorizontalAlignment(SwingConstants.RIGHT);
        footer.add(hint, BorderLayout.EAST);
        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(0);
        progress.setBorderPainted(false);
        progress.setStringPainted(false);
        progress.setOpaque(false);
        progress.setForeground(CYAN);
        progress.setBackground(new Color(255, 255, 255, 45));
        progress.setPreferredSize(new Dimension(200, 8));
        progress.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = c.getWidth(), h = c.getHeight();
                g2.setColor(new Color(255, 255, 255, 48));
                g2.fillRoundRect(0, 0, w, h, h, h);
                int fill = (int) Math.round(w * progress.getPercentComplete());
                g2.setPaint(new GradientPaint(0, 0, new Color(90, 245, 230), w, h, new Color(63, 151, 255)));
                if (fill > 0) g2.fillRoundRect(0, 0, fill, h, h, h);
                g2.dispose();
            }
        });
        footer.add(progress, BorderLayout.SOUTH);
        p.add(footer, BorderLayout.SOUTH);
        runner.progressBar = progress;

        root.add(p, "RUNNING");
        showScreen("RUNNING");
        runner.onFinished = () -> {
            if (runningTimer != null) runningTimer.stop();
            startQuestion();
        };
    }

    // ==================== PROFILE ====================
   private JPanel profileScreen() {
    BackgroundPanel p = new BackgroundPanel("level5.png");
    p.setLayout(new BorderLayout());

    JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 8));
    top.setBackground(new Color(19, 53, 88, 245));

    JButton home = navButton("HOME");
    JButton play = navButton("PLAY");
    JButton profile = navButton("PROFILE");

    home.addActionListener(e -> {
        if (questionTimer != null) questionTimer.stop();
        acceptingAnswer = false;
        refreshDashboard();
        showScreen("DASHBOARD");
    });
    play.addActionListener(e -> startNewLesson());
    profile.setEnabled(false);

    top.add(home);
    top.add(play);
    top.add(profile);

    p.add(top, BorderLayout.NORTH);

    JPanel center = new JPanel(new GridBagLayout());
    center.setOpaque(false);

    JPanel box = panel(350, 560);
    box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

    title(box, "PROFILE");

    // Character
    profileAvatar = image(
    outfitFile(selectedCharacter, selectedOutfit),
    145,
    210
    );
    profileAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);
    box.add(profileAvatar);

    // Player name
    profilePlayerLabel = label(
        "Player: " + (username.isEmpty() ? "Guest" : username),
        20,
        CREAM
    );
    profilePlayerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    box.add(Box.createVerticalStrut(6));
    box.add(profilePlayerLabel);

    // Character name
    profileCharacterLabel = label(
        "Character: " + selectedCharacter,
        18,
        selectedCharacter.equals("Girl") ? PINK : CYAN
    );
    profileCharacterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    box.add(Box.createVerticalStrut(6));
    box.add(profileCharacterLabel);

    // XP
    profileXpLabel = label("Total XP: " + scores.getOrDefault(username, 0), 24, GOLD);
    profileXpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    box.add(Box.createVerticalStrut(6));
    box.add(profileXpLabel);

    // Correct answers
    profileCorrectLabel = label(
        "Correct Answers: " + correctAnswers,
        17,
        CREAM
    );
    profileCorrectLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    box.add(profileCorrectLabel);

    // Level
    profileLevelLabel = label(
        "Current Level: " + level,
        17,
        CREAM
    );
    profileLevelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    box.add(profileLevelLabel);

    // Hearts
    profileHeartsLabel = label(
        "Hearts: " + hearts,
        17,
        PEACH
    );
    profileHeartsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    box.add(profileHeartsLabel);

    center.add(box);
    p.add(center, BorderLayout.CENTER);

    return p;
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
            "Correct Answers: " + correctAnswers
        );
    }

    if (profileLevelLabel != null) {
        profileLevelLabel.setText(
            "Current Level: " + level
        );
    }

    if (profileHeartsLabel != null) {
        profileHeartsLabel.setText(
            "Hearts: " + hearts
        );
    }

    root.revalidate();
    root.repaint();
}

    // ==================== COMPLETE / GAME OVER / LOGOUT ====================
    private JPanel completeScreen() {
        return messageScreen(
            "LESSON COMPLETE!",
            "You finished all 5 levels!",
            "Final XP: " + xp,
            "TAKE ANOTHER LESSON",
            e -> { startNewLesson(); },
            "HOME",
            e -> { xp = savedXp; correctAnswers = 0; refreshDashboard(); refreshMainMenuXp(); showScreen("DASHBOARD"); },
            "castle.png"
        );
    }

    private JPanel gameOverScreen() {
        return messageScreen(
            "GAME OVER",
            "You ran out of hearts.",
            "Final XP: " + xp,
            "TRY AGAIN",
            e -> { startNewLesson(); },
            "HOME",
            e -> { correctAnswers = 0; refreshDashboard(); showScreen("DASHBOARD"); },
            "level5.png"
        );
    }

    private JPanel logoutScreen() {
        return messageScreen(
            "LOGOUT",
            "Are you sure you want to logout?",
            "Your XP will remain saved for this session.",
            "YES",
            e -> {
                saveScore();
                username = "";
                email = "";
                selectedCharacter = "Boy";
                selectedOutfit = 0;
                xp = 0;
                hearts = 3;
                level = 1;
                questionIndex = 0;
                showScreen("WELCOME");
            },
            "NO",
            e -> showScreen("DASHBOARD"),
            "level5.png"
        );
    }

    private JPanel messageScreen(String titleText, String msg, String detail,
                                 String a, java.awt.event.ActionListener aa,
                                 String b, java.awt.event.ActionListener bb,
                                 String bg) {
        BackgroundPanel p = new BackgroundPanel(bg);
        p.setLayout(new GridBagLayout());
        JPanel box = panel(350, 390);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        title(box, titleText);
        addCenter(box, msg, 19, CREAM);
        if ("GAME OVER".equals(titleText)) {
            gameOverXpLabel = label("Final XP: " + xp, 25, GOLD);
            gameOverXpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(Box.createVerticalStrut(6));
            box.add(gameOverXpLabel);
        } else if ("LESSON COMPLETE!".equals(titleText)) {
            completeXpLabel = label("Final XP: " + xp, 25, GOLD);
            completeXpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(Box.createVerticalStrut(6));
            box.add(completeXpLabel);
        } else {
            addCenter(box, detail, 25, GOLD);
        }
        box.add(Box.createVerticalStrut(25));
        JButton x = gold(a);
        x.setAlignmentX(Component.CENTER_ALIGNMENT);
        x.addActionListener(aa);
        box.add(x);
        box.add(Box.createVerticalStrut(12));
        JButton y = blue(b);
        y.setAlignmentX(Component.CENTER_ALIGNMENT);
        y.addActionListener(bb);
        box.add(y);
        p.add(box);
        return p;
    }

    // ==================== UI HELPERS ====================
    private JPanel panel(int w, int h) {
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

    private void title(JPanel p, String s) {
        int size = getWidth() > 0 && getWidth() <= 500 ? 24 : 30;
        JLabel l = label(s, size, GOLD);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(Box.createVerticalStrut(12));
        p.add(l);
        p.add(Box.createVerticalStrut(20));
    }

    private void addCenter(JPanel p, String s, int size, Color c) {
        JLabel l = label(s, size, c);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(Box.createVerticalStrut(6));
        p.add(l);
    }

    private JLabel label(String s, int size, Color c) {
        JLabel l = new JLabel(s);
        int responsiveSize = (getWidth() > 0 && getWidth() <= 500) ? Math.min(size, 22) : size;
        l.setFont(new Font("SansSerif", Font.BOLD, responsiveSize));
        l.setForeground(c);
        return l;
    }

    private JTextField textField(String tip) {
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

    private JPasswordField passwordField(String tip) {
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

    private JButton gold(String s) {
        JButton b = base(s);
        b.setBackground(GOLD);
        b.setForeground(new Color(25, 42, 62));
        b.setBorder(new RoundedBorder(new Color(255, 235, 170), 1, 16));
        return b;
    }

    private JButton blue(String s) {
        JButton b = base(s);
        b.setBackground(new Color(92, 177, 255));
        b.setForeground(new Color(13, 38, 67));
        b.setBorder(new RoundedBorder(new Color(173, 223, 255), 1, 16));
        return b;
    }

    private JButton smallBlue(String s) {
        JButton b = blue(s);
        b.setPreferredSize(new Dimension(220, 42));
        b.setMaximumSize(new Dimension(220, 42));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        return b;
    }

    private JButton link(String s) {
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

    private JButton dashboardActionButton(String s, Color accent) {
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

    private JLabel image(String n, int w, int h) {
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

    private void error(String s) {
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

    private void commitSessionXp() {
        if (username.isEmpty() || sessionCommitted) return;
        savedXp = Math.max(0, savedXp + xp);
        showZeroXpAfterCancelledRun = false;
        int lifetimeXp = Math.max(0, scores.getOrDefault(username, 0) + xp);
        scores.put(username, lifetimeXp);
        sessionCommitted = true;
        Properties data = new Properties();
        data.putAll(scoresToStringMap());
        try (FileOutputStream out = new FileOutputStream("scores.properties")) {
            data.store(out, "Code Escape player XP scores");
        } catch (IOException ex) {
            System.err.println("Could not save XP scores: " + ex.getMessage());
        }
        saveUserDatabase();
    }

    // XP is committed only by commitSessionXp() after the entire game is completed.
    private void saveScore() {
        // Intentionally does not save temporary XP from an unfinished run.
        saveUserDatabase();
    }

    private Map<String, String> scoresToStringMap() {
        Map<String, String> values = new HashMap<>();
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            values.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return values;
    }

    // ==================== LOCAL USER DATABASE ====================
    // Accounts are stored outside the running session so registration survives
    // closing and reopening the program. Character and XP data are stored too.
    private void saveUserDatabase() {
        Properties data = new Properties();
        for (String name : accounts.keySet()) {
            String key = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(name.getBytes(StandardCharsets.UTF_8));
            data.setProperty("user." + key + ".password", accounts.getOrDefault(name, ""));
            data.setProperty("user." + key + ".email", emails.getOrDefault(name, ""));
            data.setProperty("user." + key + ".character", savedCharacters.getOrDefault(name, "Boy"));
            data.setProperty("user." + key + ".outfit", String.valueOf(savedOutfits.getOrDefault(name, 0)));
            data.setProperty("user." + key + ".xp", String.valueOf(scores.getOrDefault(name, 0)));
            int[] style = savedStyles.getOrDefault(name, new int[]{1,1,1,1});
            for (int i = 0; i < 4; i++) data.setProperty("user." + key + ".style" + i, String.valueOf(style[i]));
        }
        try (FileOutputStream out = new FileOutputStream("users.properties")) {
            data.store(out, "Code Escape local user database");
        } catch (IOException ex) {
            System.err.println("Could not save user database: " + ex.getMessage());
        }
    }

    private void loadUserDatabase() {
        File file = new File("users.properties");
        if (!file.isFile()) return;
        Properties data = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            data.load(in);
            for (String property : data.stringPropertyNames()) {
                if (!property.startsWith("user.") || !property.endsWith(".password")) continue;
                String key = property.substring(5, property.length() - 9);
                try {
                    String name = new String(Base64.getUrlDecoder().decode(key), StandardCharsets.UTF_8);
                    accounts.put(name, data.getProperty(property, ""));
                    emails.put(name, data.getProperty("user." + key + ".email", ""));
                    savedCharacters.put(name, data.getProperty("user." + key + ".character", "Boy"));
                    if (data.containsKey("user." + key + ".xp")) {
                        try {
                            scores.put(name, Math.max(0, Integer.parseInt(data.getProperty("user." + key + ".xp", "0"))));
                        } catch (NumberFormatException ignored) {
                            scores.putIfAbsent(name, 0);
                        }
                    }
                    try {
                        savedOutfits.put(name, Math.max(0, Math.min(3, Integer.parseInt(data.getProperty("user." + key + ".outfit", "0")))));
                    } catch (NumberFormatException ignored) {
                        savedOutfits.put(name, 0);
                    }
                    int[] style = new int[4];
                    for (int i = 0; i < 4; i++) {
                        try { style[i] = Integer.parseInt(data.getProperty("user." + key + ".style" + i, "1")); }
                        catch (NumberFormatException ignored) { style[i] = 1; }
                    }
                    savedStyles.put(name, style);
                } catch (IllegalArgumentException ignored) {
                    // Ignore a damaged user record instead of breaking startup.
                }
            }
        } catch (IOException ex) {
            System.err.println("Could not load user database: " + ex.getMessage());
        }
    }

    private void loadScores() {
        File file = new File("scores.properties");
        if (!file.isFile()) return;
        Properties data = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            data.load(in);
            for (String name : data.stringPropertyNames()) {
                try {
                    scores.put(name, Math.max(0, Integer.parseInt(data.getProperty(name))));
                } catch (NumberFormatException ignored) {
                    // Ignore an invalid saved score and keep the game running.
                }
            }
        } catch (IOException ex) {
            System.err.println("Could not load XP scores: " + ex.getMessage());
        }
    }

    // ==================== MUSIC ====================
    /**
     * Opens a music track from the classpath first. This makes music work
     * when the project is run from NetBeans as well as when the JAR is run.
     * The external music/ folder is kept as a fallback for older copies.
     */
    private AudioInputStream openMusic(String fileName) throws Exception {
        AudioInputStream source = null;
        String resourcePath = "/music/" + fileName;
        InputStream resource = CodeEscapeApp.class.getResourceAsStream(resourcePath);
        if (resource != null) {
            source = AudioSystem.getAudioInputStream(new BufferedInputStream(resource));
        } else {
            File file = new File("music", fileName);
            if (file.isFile()) {
                source = AudioSystem.getAudioInputStream(file);
            }
        }
        if (source == null) return null;

        // Force a standard little-endian PCM format so Java Sound can play the
        // music consistently from NetBeans and from the packaged JAR.
        AudioFormat target = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, 44100.0f, 16, 2, 4, 44100.0f, false
        );
        AudioFormat sourceFormat = source.getFormat();
        if (AudioSystem.isConversionSupported(target, sourceFormat)) {
            return AudioSystem.getAudioInputStream(target, source);
        }
        return source;
    }

    // Small built-in feedback sounds work offline and need no extra downloads.
    private void playFeedbackSound(boolean correct) {
        try {
            java.awt.Toolkit.getDefaultToolkit().beep();
            // Keep the sound effect lightweight and reliable across Java systems.
        } catch (Exception ignored) {
            // Audio feedback is optional; gameplay must continue if unavailable.
        }
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
            setMusicVolume(musicVolume);
            backgroundMusic.setFramePosition(0);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusic.start();
        } catch (Exception e) {
            System.out.println(errorLabel + " could not be started: " + e.getMessage());
        }
    }

    private void playLevelMusic(int levelNumber) {
        currentMusicLevel = levelNumber;
        String track;
        switch (levelNumber) {
            case 1:
                track = "adventure.wav";
                break;
            case 2:
                track = "blizzard.wav";
                break;
            case 3:
                track = "magma.wav";
                break;
            case 4:
                track = "sky.wav";
                break;
            case 5:
                track = "dark_castle.wav";
                break;
            default:
                track = "code_escape_theme.wav";
                break;
        }
        playMusicFile(track, "Level music");
    }

    private void playSpecialMusic(String fileName) {
        playMusicFile(fileName, "Special music");
    }

    private void playMainMenuMusic() {
        // Original upbeat chiptune adventure theme, generated for offline playback.
        playMusicFile("code_escape_theme.wav", "Main menu music");
    }

    private void startBackgroundMusic() {
        if (!musicEnabled) return;
        playMainMenuMusic();
    }

    private void stopBackgroundMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
        }
    }

    private void setMusicVolume(float volume) {
        if (backgroundMusic == null) return;
        try {
            FloatControl control = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
            if (volume <= 0f) {
                control.setValue(control.getMinimum());
            } else {
                float db = (float) (20.0 * Math.log10(volume));
                control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), db)));
            }
        } catch (Exception ignored) {}
    }

    // ==================== RUNNING ANIMATION ====================
    class MiniRunner extends JPanel {
        BufferedImage sheet;
        int frame = 0;
        int frameCount = 4;
        int runnerX = -1;
        boolean jumping = false;
        int jumpTicks = 0;
        javax.swing.Timer timer;

        MiniRunner(String file) {
            setOpaque(false);
            setCharacter(file);
            timer = new javax.swing.Timer(85, e -> {
                frame = (frame + 1) % Math.max(1, frameCount);
                if (runnerX >= 0) {
                    runnerX += 5;
                    if (runnerX > getWidth()) runnerX = -Math.max(1, getWidth() / 8);
                }
                if (jumpTicks > 0) { jumpTicks--; jumping = true; }
                else jumping = false;
                repaint();
            });
            addHierarchyListener(e -> {
                if (isShowing() && !timer.isRunning()) timer.start();
                else if (!isShowing() && timer.isRunning()) timer.stop();
            });
        }

        void setCharacter(String file) {
            sheet = load(file);
            frame = 0;
            frameCount = detectFrameCount(sheet);
            repaint();
        }

        void jump() {
            jumpTicks = 12;
            if (timer != null && !timer.isRunning()) timer.start();
        }

        void stop() {
            if (timer != null) timer.stop();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (sheet == null) return;
            int fw = Math.max(1, sheet.getWidth() / Math.max(1, frameCount));
            int fh = sheet.getHeight();
            // Fixed height prevents the old 'growing' effect when the panel
            // is laid out or resized. The character simply cycles through
            // the four walking frames at the same size.
            int dh = Math.min(86, Math.max(64, getHeight() - 6));
            int dw = (int)Math.round(fw * (dh / (double)fh));
            if (runnerX < 0) runnerX = 0;
            int x = runnerX;
            int y = getHeight() - dh - (jumping ? (int)(28 * Math.sin((12 - jumpTicks) * Math.PI / 12.0)) : 0);
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            // Every character variant is rendered as ONE complete authored frame.
            // Never splice the torso from one pose onto legs from another pose:
            // that caused visible seams and flicker. All eight walk sheets use
            // four equal full-body frames, so the same stable crop works for Boy
            // and Girl variants 1-4.
            int frameIndex = Math.floorMod(frame, Math.max(1, frameCount));
            int sx1 = frameIndex * fw;
            int sx2 = Math.min(sheet.getWidth(), sx1 + fw);
            g2.drawImage(sheet, x, y, x + dw, y + dh,
                    sx1, 0, sx2, fh, null);
            g2.dispose();
        }
    }

    private BufferedImage[] loadIsolatedWalkFrames(String file) {
        // Trim each authored pose to its own alpha bounds, then place it on a
        // shared bottom-aligned canvas. Supports the eight-pose generic sheets
        // and the four-pose color-specific variant sheets.
        if (file == null || !file.matches("modern_(boy|girl)_(walk(_v[1-4])?|run_v[1-4])\\.png")) return null;
        BufferedImage source = load(file);
        if (source == null) return null;
        final int count;
        if (source.getWidth() == 2240 && source.getHeight() == 380) count = 8;
        else if (source.getWidth() == 384 && (source.getHeight() == 365 || source.getHeight() == 390)) count = 4;
        else return null;
        final int cellWidth = source.getWidth() / count;
        Rectangle[] bounds = new Rectangle[count];
        int canvasWidth = 1;
        int canvasHeight = 1;

        for (int i = 0; i < count; i++) {
            int minX = cellWidth, minY = source.getHeight();
            int maxX = -1, maxY = -1;
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = i * cellWidth; x < (i + 1) * cellWidth; x++) {
                    if ((source.getRGB(x, y) >>> 24) > 8) {
                        minX = Math.min(minX, x - i * cellWidth);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x - i * cellWidth);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            if (maxX < minX || maxY < minY) return null;
            bounds[i] = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
            canvasWidth = Math.max(canvasWidth, bounds[i].width);
            canvasHeight = Math.max(canvasHeight, bounds[i].height);
        }

        BufferedImage[] frames = new BufferedImage[count];
        for (int i = 0; i < count; i++) {
            Rectangle b = bounds[i];
            BufferedImage frameImage = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D fg = frameImage.createGraphics();
            fg.setComposite(AlphaComposite.Src);
            fg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            BufferedImage crop = source.getSubimage(i * cellWidth + b.x, b.y, b.width, b.height);
            int dx = (canvasWidth - b.width) / 2;
            int dy = canvasHeight - b.height;
            fg.drawImage(crop, dx, dy, null);
            fg.dispose();
            frames[i] = frameImage;
        }
        return frames;
    }

    private int detectFrameCount(BufferedImage image) {
        if (image == null) return 1;
        // The clean side-running sheets contain eight separated poses.
        if (image.getWidth() == 2240) return 8;
        // Legacy overlapping modern_boy/girl_walk_v1..v4 sheets.
        // Explicitly recognize them so every variant uses identical frame
        // boundaries and cannot fall back to a single flickering image.
        if (image.getWidth() == 384 && (image.getHeight() == 365 || image.getHeight() == 390)) return 4;
        // Compatibility with earlier bundled four-frame sheets.
        if (image.getWidth() == 640 && image.getHeight() == 400) return 4;
        if (image.getWidth() == 173 || image.getWidth() == 171 || image.getWidth() == 229 || image.getWidth() == 740) {
            return image.getWidth() >= 220 ? 4 : 3;
        }
        if (image.getWidth() % 4 == 0) return 4;
        if (image.getWidth() % 3 == 0) return 3;
        return 1;
    }

    private int[] frameSequenceFor(int count) {
        // A running sprite sheet is a forward-moving cycle. Never play the
        // frames backward: reversing the stride makes the legs look as though
        // they are swinging left and right instead of running forward.
        if (count <= 1) return new int[]{0};
        int[] sequence = new int[count];
        for (int i = 0; i < count; i++) sequence[i] = i;
        return sequence;
    }

    class MovingRunner extends JPanel {
        BufferedImage sheet;
        BufferedImage[] isolatedFrames;
        double x;
        int frame = 0;
        long startNanos;
        long lastNanos;
        javax.swing.Timer timer;
        Runnable onFinished;
        JProgressBar progressBar;
        private int frameCount = 1;
        private int[] runFrames = {0};
        private boolean spriteSheet;
        private boolean staticCharacter;
        private static final long RUN_DURATION_NANOS = 6_000_000_000L;
        private static final long FRAME_DURATION_NANOS = 120_000_000L;

        MovingRunner(String file) {
            setOpaque(false);
            setDoubleBuffered(true);
            sheet = load(file);
            isolatedFrames = loadIsolatedWalkFrames(file);
            // Use the generated eight-pose, full-body run sheet for the selected
            // character variant. Each pose is separated by a full cell, so
            // no neighboring character fragments can enter the rendered frame.
            spriteSheet = isolatedFrames != null && isolatedFrames.length > 1;
            staticCharacter = !spriteSheet;
            frameCount = spriteSheet ? isolatedFrames.length : 1;
            runFrames = spriteSheet ? frameSequenceFor(frameCount) : new int[]{0};
            x = -120;
            startNanos = System.nanoTime();
            lastNanos = startNanos;
            timer = new javax.swing.Timer(15, e -> {
                long now = System.nanoTime();
                long elapsed = now - startNanos;
                long delta = Math.max(0L, now - lastNanos);
                lastNanos = now;

                int target = Math.max(10, getWidth() - getDisplayWidth() - 20);
                double startX = -getDisplayWidth() - 4.0;
                double progress = Math.min(1.0, elapsed / (double) RUN_DURATION_NANOS);
                // Smooth horizontal movement so the transition run feels less mechanical.
                double eased = progress * progress * (3.0 - 2.0 * progress);
                x = startX + (target - startX) * eased;
                if (progressBar != null) progressBar.setValue((int)Math.round(eased * 100));

                // Derive the pose directly from elapsed time. This prevents timer
                // jitter or delayed Swing events from repeatedly toggling poses.
                // Static fallback art stays completely still instead of flickering
                // through artificial rotations/scales.
                if (!staticCharacter) {
                    frame = (int) ((elapsed / FRAME_DURATION_NANOS) % runFrames.length);
                } else {
                    frame = 0;
                }

                if (progress >= 1.0) {
                    x = target;
                    timer.stop();
                    repaint();
                    if (onFinished != null) SwingUtilities.invokeLater(onFinished);
                    return;
                }
                repaint();
            });
            addHierarchyListener(e -> {
                if (isShowing() && !timer.isRunning()) {
                    startNanos = System.nanoTime();
                    lastNanos = startNanos;
                    timer.start();
                } else if (!isShowing() && timer.isRunning()) {
                    timer.stop();
                }
            });
        }

        private int getFrameWidth() {
            if (spriteSheet && isolatedFrames != null) return isolatedFrames[0].getWidth();
            return sheet == null ? 1 : sheet.getWidth();
        }

        private int getFrameHeight() {
            if (spriteSheet && isolatedFrames != null) return isolatedFrames[0].getHeight();
            return sheet == null ? 1 : sheet.getHeight();
        }

        private int getDisplayHeight() {
            // Keep the transition runner balanced: larger than the compact size, but smaller than the original.
            int available = getHeight() > 0 ? getHeight() : 480;
            return Math.min(190, Math.max(160, available / 3));
        }

        private int getDisplayWidth() {
            // Keep the original proportions of a single 96px pose cell.
            // The previous fixed wide value stretched the character and made
            // neighboring-pose artifacts more noticeable.
            double sourceRatio = getFrameWidth() / (double)Math.max(1, getFrameHeight());
            return Math.max(68, (int)Math.round(getDisplayHeight() * sourceRatio));
        }

        void stop() {
            if (timer != null) timer.stop();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (sheet == null) return;
            int fw = getFrameWidth();
            int fh = getFrameHeight();
            int dh = getDisplayHeight();
            int dw = getDisplayWidth();

            // Stable ground line and stable sprite geometry: the only changing
            // pixels are the authored poses in the sprite sheet. Avoid rotating,
            // stretching, or swapping a full character image, which creates a
            // flicker/ghosting effect on transition screens.
            int y = Math.max(0, getHeight() - dh - 8);

            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int shadowW = Math.max(32, (int)(dw * 0.66));
            int shadowH = 7;
            g2.setColor(new Color(3, 13, 28, 90));
            g2.fillOval((int)Math.round(x) + (dw-shadowW)/2, getHeight()-15, shadowW, shadowH);
            int sx = runFrames[frame % runFrames.length];
            if (spriteSheet && isolatedFrames != null) {
                // Each frame is a complete isolated character on a normalized
                // transparent canvas. Draw exactly one frame per repaint.
                BufferedImage pose = isolatedFrames[sx];
                g2.drawImage(pose, (int)Math.round(x), y,
                        (int)Math.round(x) + dw, y + dh, 0, 0, pose.getWidth(), pose.getHeight(), null);
            } else {
                // Static fallback remains one selected character, never a sheet crop.
                g2.drawImage(sheet, (int)Math.round(x), y,
                        (int)Math.round(x) + dw, y + dh, 0, 0, fw, fh, null);
            }
            g2.dispose();
        }
    }

    class BackgroundPanel extends JPanel {
        BufferedImage bg;

        BackgroundPanel(String n) {
            bg = load(n);
            setOpaque(true);
            setBackground(new Color(19, 43, 72));
        }

        void setBackgroundImage(String n) {
            bg = load(n);
            repaint();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bg == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(19, 43, 72));
            g2.fillRect(0, 0, getWidth(), getHeight());
            // Fill the entire window without black bars. Anchor the image at the top
            // so the important upper part of the artwork is never cropped off.
            double s = Math.max(getWidth() / (double) bg.getWidth(), getHeight() / (double) bg.getHeight());
            int w = Math.max(1, (int)Math.round(bg.getWidth() * s));
            int h = Math.max(1, (int)Math.round(bg.getHeight() * s));
            int x = (getWidth() - w) / 2;
            int y = 0;
            // Bilinear scaling keeps the background crisp and avoids the extra
            // softness caused by bicubic interpolation on these game images.
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(bg, x, y, w, h, null);
            // Keep the artwork bright; panels provide contrast for text.
            g2.dispose();
        }
    }

    // Soft translucent cards provide a modern HUD without obscuring the level art.
    static class GlassPanel extends JPanel {
        private final Color fill;
        GlassPanel(Color fill) { this.fill = fill; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            g2.setColor(new Color(255, 255, 255, 38));
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 22, 22);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Modern rounded controls keep the original game flow and logic intact.
    static class RoundedBorder implements javax.swing.border.Border {
        private final Color color;
        private final int thickness;
        private final int arc;
        RoundedBorder(Color color, int thickness, int arc) {
            this.color = color;
            this.thickness = thickness;
            this.arc = arc;
        }
        @Override public Insets getBorderInsets(Component c) {
            int pad = Math.max(5, thickness + 4);
            return new Insets(pad, pad + 2, pad, pad + 2);
        }
        @Override public boolean isBorderOpaque() { return false; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            int off = thickness;
            g2.drawRoundRect(x + off, y + off, Math.max(0, w - off * 2 - 1),
                    Math.max(0, h - off * 2 - 1), arc, arc);
            g2.dispose();
        }
    }

    static class ModernButton extends JButton {
        ModernButton(String text) {
            super(text);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(true);
            setFocusPainted(false);
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    putClientProperty("modernHover", Boolean.TRUE);
                    repaint();
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    putClientProperty("modernHover", Boolean.FALSE);
                    repaint();
                }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            Color base = getBackground();
            if (base == null) base = new Color(72, 150, 218);
            boolean hover = Boolean.TRUE.equals(getClientProperty("modernHover")) && isEnabled();
            Color top = hover ? base.brighter() : new Color(
                    Math.min(255, base.getRed() + 14),
                    Math.min(255, base.getGreen() + 14),
                    Math.min(255, base.getBlue() + 14));
            Color bottom = isEnabled() ? base : new Color(125, 137, 151);
            if (getModel().isPressed()) {
                top = base.darker();
                bottom = base.darker();
            }
            g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 18, 18);
            if (hover) {
                g2.setColor(new Color(255, 255, 255, 34));
                g2.fillRoundRect(2, 2, getWidth() - 4, Math.max(8, getHeight() / 2), 16, 16);
            }
            g2.dispose();
            setOpaque(false);
            setContentAreaFilled(false);
            super.paintComponent(g);
        }
    }

    static class ModernCardPanel extends JPanel {
        ModernCardPanel() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = getBackground() == null ? new Color(20, 51, 86, 238) : getBackground();
            Color top = new Color(Math.min(255, c.getRed() + 10),
                    Math.min(255, c.getGreen() + 14), Math.min(255, c.getBlue() + 18), c.getAlpha());
            g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), c));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
            // A subtle inner highlight gives the card a polished game HUD feel.
            g2.setColor(new Color(255, 255, 255, 18));
            g2.drawRoundRect(2, 2, Math.max(0, getWidth() - 5), Math.max(0, getHeight() - 5), 22, 22);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class PlaceholderTextField extends JTextField {
        private final String placeholder;
        PlaceholderTextField(String placeholder) { this.placeholder = placeholder; }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(190, 205, 225));
                g2.setFont(getFont());
                Insets in = getInsets();
                g2.drawString(placeholder, in.left, getHeight() / 2 + getFont().getSize() / 2 - 3);
                g2.dispose();
            }
        }
    }

    static class PlaceholderPasswordField extends JPasswordField {
        private final String placeholder;
        PlaceholderPasswordField(String placeholder) { this.placeholder = placeholder; }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getPassword().length == 0 && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(190, 205, 225));
                g2.setFont(getFont());
                Insets in = getInsets();
                g2.drawString(placeholder, in.left, getHeight() / 2 + getFont().getSize() / 2 - 3);
                g2.dispose();
            }
        }
    }

    static class Question { String text; String[] answers; int correct; boolean syntax; String expected; Question(String text,String[] answers,int correct,boolean syntax,String expected){this.text=text;this.answers=answers;this.correct=correct;this.syntax=syntax;this.expected=expected;} }

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
