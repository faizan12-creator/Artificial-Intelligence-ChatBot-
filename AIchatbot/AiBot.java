package AIchatbot;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.util.Duration;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AIChatBot — Main JavaFX Application.
 * Uses: AppTheme, ChatSession, APIService, BubbleBuilder
 *
 * Launch from Main.java:
 *   Application.launch(AIChatBot.class, args);
 */
public class AiBot extends Application {

    // ── Config ────────────────────────────────────────────────
    private String API_KEY = "YOUR_GROQ_API_KEY_HERE";

    private static final File SAVE_FILE =
            new File(System.getProperty("user.home"), "JavaAI_sessions.json");

    // ── State ─────────────────────────────────────────────────
    private VBox        chatBox;
    private TextArea    inputArea;
    private ScrollPane  scrollPane;
    private Label       statusLabel;
    private Label       tokenLabel;
    private Label       topTokenLabel;
    private Label       charCountLabel;
    private VBox        historyPanel;
    private HBox        searchBarBox;
    private TextField   searchField;
    private Button      sendBtn;
    private Button      themeToggleBtn;
    private BorderPane  root;
    private Scene       scene;
    private Stage       mainStage;

    private boolean searchVisible = false;
    private boolean isTyping      = false;
    private int     totalTokens   = 0;
    private double  fontSize      = 13.0;
    private double  temperature   = 0.7;
    private String  currentModel  = "llama-3.1-8b-instant";
    private String  systemPrompt  =
            "You are JavaAI, a smart friendly expert in Java, JavaFX, " +
                    "algorithms, and AI/ML. " +
                    "Format using markdown: # headers, - bullets, numbered lists, " +
                    "```java code blocks. Be concise, clear, and helpful.";

    private final List<Map<String, String>> history  = new ArrayList<>();
    private final List<ChatSession>         sessions = new ArrayList<>();
    private ChatSession                     activeSession;
    private final APIService                api      = new APIService();

    // ══════════════════════════════════════════════════════════
    //  APPLICATION START
    // ══════════════════════════════════════════════════════════
    @Override
    public void start(Stage stage) {
        this.mainStage = stage;

        loadSavedSessions();
        if (sessions.isEmpty()) {
            activeSession = new ChatSession("New Chat");
            sessions.add(activeSession);
        } else {
            activeSession = sessions.get(sessions.size() - 1);
        }

        root  = new BorderPane();
        scene = new Scene(root, 1280, 820);
        buildUI();

        // Global keyboard shortcuts
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown()) {
                switch (e.getCode()) {
                    case ENTER -> { handleSend();          e.consume(); }
                    case N     -> { createNewSession();    e.consume(); }
                    case F     -> { toggleSearch();        e.consume(); }
                    case L     -> { confirmClearChat();    e.consume(); }
                    case SLASH -> { showShortcutsDialog(); e.consume(); }
                    default    -> { /* ignore other Ctrl combos */ }
                }
            } else if (e.getCode() == KeyCode.ESCAPE && searchVisible) {
                toggleSearch();
                e.consume();
            }
        });

        stage.setTitle("JavaAI — Powered by Llama 3.1");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setMaximized(true);
        stage.show();

        // App entrance animation
        root.setOpacity(0);
        root.setScaleX(0.97);
        root.setScaleY(0.97);
        FadeTransition ft = new FadeTransition(Duration.millis(420), root);
        ft.setFromValue(0); ft.setToValue(1);
        ScaleTransition st = new ScaleTransition(Duration.millis(420), root);
        st.setFromX(0.97); st.setFromY(0.97);
        st.setToX(1.0);    st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, st).play();

        showWelcomeCard();

        // Restore last session messages if any
        if (!activeSession.messages.isEmpty()) {
            history.addAll(activeSession.messages);
            int idx = 0;
            for (Map<String, String> m : activeSession.messages) {
                if ("user".equals(m.get("role"))) {
                    final int fi = idx;
                    chatBox.getChildren().add(
                            BubbleBuilder.buildUserBubble(
                                    m.get("content"), fi, fontSize, this::editAndRetry));
                } else if ("assistant".equals(m.get("role"))) {
                    chatBox.getChildren().add(
                            BubbleBuilder.buildBotBubble(m.get("content"), fontSize));
                }
                idx++;
            }
        }
    }

    // ── Build / Rebuild full UI (called on startup + theme toggle)
    private void buildUI() {
        applyCSS();
        root.setStyle("-fx-background-color:" + AppTheme.C_BG + ";");
        root.setLeft(buildSidebar());
        root.setCenter(buildCenterPane());
    }

    private void applyCSS() {
        scene.getStylesheets().clear();
        scene.getStylesheets().add("data:text/css," + AppTheme.buildCSS());
    }

    // ══════════════════════════════════════════════════════════
    //  SIDEBAR
    // ══════════════════════════════════════════════════════════
    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(270);
        sidebar.setStyle(
                "-fx-background-color:" + AppTheme.C_SURFACE + ";" +
                        "-fx-border-color:" + AppTheme.C_BORDER + ";" +
                        "-fx-border-width:0 1 0 0;");

        // Brand logo row
        HBox logo = new HBox(12);
        logo.setPadding(new Insets(20, 18, 16, 18));
        logo.setAlignment(Pos.CENTER_LEFT);
        VBox brandInfo = new VBox(2);
        brandInfo.getChildren().addAll(
                BubbleBuilder.mkLabel("JavaAI", AppTheme.C_TEXT, 15, true),
                BubbleBuilder.mkLabel("Llama 3.1 Powered", AppTheme.C_TEXT_MUTED, 10, false));
        Circle onlineDot = new Circle(4, Color.web(AppTheme.C_GREEN));
        animatePulse(onlineDot);
        Region logoSp = new Region();
        HBox.setHgrow(logoSp, Priority.ALWAYS);
        logo.getChildren().addAll(
                BubbleBuilder.buildAvatar("AI", 38), brandInfo, logoSp, onlineDot);

        // New Conversation button
        Button newChatBtn = new Button("+ New Conversation");
        newChatBtn.setMaxWidth(Double.MAX_VALUE);
        newChatBtn.setPrefHeight(42);
        newChatBtn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        newChatBtn.setStyle(btnStyle(AppTheme.C_ACCENT2, "white"));
        newChatBtn.setOnMouseEntered(e -> {
            newChatBtn.setStyle(btnStyle("#6d28d9", "white"));
            BubbleBuilder.scaleTo(newChatBtn, 1.02);
        });
        newChatBtn.setOnMouseExited(e -> {
            newChatBtn.setStyle(btnStyle(AppTheme.C_ACCENT2, "white"));
            BubbleBuilder.scaleTo(newChatBtn, 1.0);
        });
        newChatBtn.setOnAction(e -> {
            BubbleBuilder.animatePress(newChatBtn);
            createNewSession();
        });
        VBox.setMargin(newChatBtn, new Insets(14, 14, 8, 14));

        // History section
        Label histTitle = BubbleBuilder.mkLabel(
                "RECENT CHATS", AppTheme.C_TEXT_MUTED, 10, true);
        histTitle.setPadding(new Insets(8, 18, 6, 18));

        historyPanel = new VBox(2);
        historyPanel.setPadding(new Insets(0, 8, 8, 8));

        ScrollPane histScroll = new ScrollPane(historyPanel);
        histScroll.setFitToWidth(true);
        histScroll.setStyle(
                "-fx-background:transparent;" +
                        "-fx-background-color:transparent;" +
                        "-fx-border-color:transparent;");
        VBox.setVgrow(histScroll, Priority.ALWAYS);
        refreshHistory();

        // Separator
        Region sepR = new Region();
        sepR.setPrefHeight(1);
        sepR.setStyle("-fx-background-color:" + AppTheme.C_BORDER + ";");

        // Footer
        VBox footer = new VBox(3);
        footer.setPadding(new Insets(10, 14, 14, 14));
        footer.setStyle(
                "-fx-border-color:" + AppTheme.C_BORDER + ";" +
                        "-fx-border-width:1 0 0 0;");
        tokenLabel = BubbleBuilder.mkLabel(
                "Tokens used: " + String.format("%,d", totalTokens),
                AppTheme.C_TEXT_MUTED, 10, false);
        footer.getChildren().addAll(
                tokenLabel,
                BubbleBuilder.mkLabel(
                        "JavaAI v3.0  •  Auto-Save ON",
                        AppTheme.C_TEXT_MUTED, 9, false));

        sidebar.getChildren().addAll(
                logo, mkSep(), newChatBtn,
                histTitle, histScroll,
                sepR, buildSettingsPanel(), footer);
        return sidebar;
    }

    // ── Settings panel inside sidebar ─────────────────────────
    private VBox buildSettingsPanel() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12, 14, 10, 14));

        Label title = BubbleBuilder.mkLabel(
                "SETTINGS", AppTheme.C_TEXT_MUTED, 10, true);
        title.setPadding(new Insets(0, 0, 2, 0));

        // Model selector
        ComboBox<String> modelBox = new ComboBox<>();
        modelBox.getItems().addAll(
                "llama-3.1-8b-instant",
                "llama-3.3-70b-versatile",
                "gemma2-9b-it");
        modelBox.setValue(currentModel);
        modelBox.setMaxWidth(Double.MAX_VALUE);
        modelBox.setStyle(
                "-fx-background-color:" + AppTheme.C_SURFACE2 + ";" +
                        "-fx-border-color:" + AppTheme.C_BORDER + ";" +
                        "-fx-border-radius:7;-fx-background-radius:7;");
        modelBox.setOnAction(e -> currentModel = modelBox.getValue());

        // Font size slider
        Label fontLbl = BubbleBuilder.mkLabel(
                "Font Size:  " + (int)fontSize + "px",
                AppTheme.C_TEXT_SEC, 11, false);
        Slider fontSlider = new Slider(10, 18, fontSize);
        fontSlider.setMaxWidth(Double.MAX_VALUE);
        fontSlider.setBlockIncrement(1);
        fontSlider.valueProperty().addListener((obs, o, n) -> {
            fontSize = Math.round(n.doubleValue());
            fontLbl.setText("Font Size:  " + (int)fontSize + "px");
        });

        // Temperature slider
        Label tempLbl = BubbleBuilder.mkLabel(
                "Temperature:  " + temperature,
                AppTheme.C_TEXT_SEC, 11, false);
        Slider tempSlider = new Slider(0.0, 1.0, temperature);
        tempSlider.setMaxWidth(Double.MAX_VALUE);
        tempSlider.valueProperty().addListener((obs, o, n) -> {
            temperature = Math.round(n.doubleValue() * 10) / 10.0;
            tempLbl.setText("Temperature:  " + temperature);
        });

        Button sysBtn   = sideBtn("[P] System Prompt");
        Button keyBtn   = sideBtn("[K] Set API Key");
        Button shortBtn = sideBtn("[?] Shortcuts");

        sysBtn.setOnAction(e   -> openSystemPromptDialog());
        keyBtn.setOnAction(e   -> openApiKeyDialog());
        shortBtn.setOnAction(e -> showShortcutsDialog());

        box.getChildren().addAll(
                title,
                BubbleBuilder.mkLabel("Model", AppTheme.C_TEXT_SEC, 11, false),
                modelBox,
                fontLbl, fontSlider,
                tempLbl, tempSlider,
                sysBtn, keyBtn, shortBtn);
        return box;
    }

    // ══════════════════════════════════════════════════════════
    //  CENTER PANE
    // ══════════════════════════════════════════════════════════
    private BorderPane buildCenterPane() {
        BorderPane center = new BorderPane();
        center.setStyle("-fx-background-color:" + AppTheme.C_BG + ";");

        VBox topSection = new VBox(0);
        topSection.getChildren().add(buildTopBar());
        searchBarBox = buildSearchBar();
        searchBarBox.setVisible(false);
        searchBarBox.setManaged(false);
        topSection.getChildren().add(searchBarBox);

        center.setTop(topSection);
        center.setCenter(buildChatArea());
        center.setBottom(buildInputArea());
        return center;
    }

    // ── Top bar ───────────────────────────────────────────────
    private HBox buildTopBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(10, 20, 10, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(
                "-fx-background-color:" + AppTheme.C_SURFACE + ";" +
                        "-fx-border-color:" + AppTheme.C_BORDER + ";" +
                        "-fx-border-width:0 0 1 0;");

        // Left: avatar + status
        VBox info = new VBox(2);
        info.getChildren().add(
                BubbleBuilder.mkLabel("JavaAI Assistant", AppTheme.C_TEXT, 14, true));
        statusLabel = BubbleBuilder.mkLabel("Ready", AppTheme.C_GREEN, 11, false);
        statusLabel.setStyle(
                "-fx-padding:2 8 2 8;" +
                        "-fx-background-color:#052e16;" +
                        "-fx-background-radius:20;");
        info.getChildren().add(statusLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Token counter
        topTokenLabel = BubbleBuilder.mkLabel(
                String.format("%,d", totalTokens) + " tokens",
                AppTheme.C_TEXT_MUTED, 10, false);
        topTokenLabel.setPadding(new Insets(0, 6, 0, 0));

        // Model badge
        Label modelBadge = new Label(
                currentModel.replace("-instant", "").replace("-versatile", ""));
        modelBadge.setFont(Font.font("Segoe UI", 10));
        modelBadge.setTextFill(Color.web(AppTheme.C_ACCENT2));
        modelBadge.setStyle(
                "-fx-background-color:#1e1528;" +
                        "-fx-border-color:#3d1f6e;" +
                        "-fx-border-radius:5;-fx-background-radius:5;" +
                        "-fx-padding:3 10 3 10;");

        // Theme toggle button
        themeToggleBtn = topIconBtn(
                AppTheme.isDark ? "Light" : "Dark", "Toggle Theme");
        themeToggleBtn.setOnAction(e -> toggleTheme());

        Button searchBtn = topIconBtn("[S]", "Search  Ctrl+F");
        Button exportBtn = topIconBtn("[v]", "Export Chat");
        Button clearBtn  = topIconBtn("[x]", "Clear  Ctrl+L");
        Button closeBtn  = topIconBtn(" X ", "Close");

        searchBtn.setOnAction(e -> toggleSearch());
        exportBtn.setOnAction(e -> exportChat());
        clearBtn.setOnAction(e  -> confirmClearChat());

        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color:" + AppTheme.C_RED_DIM + ";" +
                        "-fx-text-fill:" + AppTheme.C_RED + ";" +
                        "-fx-background-radius:7;-fx-cursor:hand;-fx-padding:6 10 6 10;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(topIconNormal()));
        closeBtn.setOnAction(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), root);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> mainStage.close());
            fadeOut.play();
        });

        bar.getChildren().addAll(
                BubbleBuilder.buildAvatar("AI", 36), info, spacer,
                topTokenLabel, modelBadge,
                themeToggleBtn, searchBtn, exportBtn, clearBtn, closeBtn);
        return bar;
    }

    // ── Theme toggle ──────────────────────────────────────────
    private void toggleTheme() {
        AppTheme.toggle();
        if (themeToggleBtn != null)
            themeToggleBtn.setText(AppTheme.isDark ? "Light" : "Dark");

        buildUI();   // rebuild sidebar + center with new colors

        // Re-render existing messages
        chatBox.getChildren().clear();
        showWelcomeCard();
        int idx = 0;
        for (Map<String, String> m : history) {
            if ("user".equals(m.get("role"))) {
                final int fi = idx;
                chatBox.getChildren().add(
                        BubbleBuilder.buildUserBubble(
                                m.get("content"), fi, fontSize, this::editAndRetry));
            } else if ("assistant".equals(m.get("role"))) {
                chatBox.getChildren().add(
                        BubbleBuilder.buildBotBubble(m.get("content"), fontSize));
            }
            idx++;
        }
        scrollToBottom();
    }

    // ── Search bar ────────────────────────────────────────────
    private HBox buildSearchBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(9, 20, 9, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(
                "-fx-background-color:" + AppTheme.C_SURFACE2 + ";" +
                        "-fx-border-color:" + AppTheme.C_BORDER + ";" +
                        "-fx-border-width:0 0 1 0;");

        searchField = new TextField();
        searchField.setPromptText("Search messages...");
        searchField.setFont(Font.font("Segoe UI", 12));
        searchField.setStyle(
                "-fx-background-color:" + AppTheme.C_SURFACE + ";" +
                        "-fx-text-fill:" + AppTheme.C_TEXT + ";" +
                        "-fx-border-color:" + AppTheme.C_BORDER2 + ";" +
                        "-fx-border-radius:7;-fx-background-radius:7;" +
                        "-fx-padding:7 12 7 12;" +
                        "-fx-prompt-text-fill:" + AppTheme.C_TEXT_MUTED + ";");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Label resultLbl = BubbleBuilder.mkLabel("", AppTheme.C_TEXT_MUTED, 11, false);
        resultLbl.setMinWidth(70);

        searchField.textProperty().addListener((obs, o, n) -> {
            if (n.trim().isEmpty()) {
                clearHighlights();
                resultLbl.setText("");
                return;
            }
            int count = searchMessages(n.trim());
            resultLbl.setText(count > 0 ? count + " found" : "Not found");
            resultLbl.setTextFill(
                    count > 0 ? Color.web(AppTheme.C_GREEN) : Color.web(AppTheme.C_RED));
        });

        Button closeSearch = new Button("Close  Esc");
        closeSearch.setFont(Font.font("Segoe UI", 10));
        closeSearch.setStyle(
                "-fx-background-color:" + AppTheme.C_SURFACE + ";" +
                        "-fx-text-fill:" + AppTheme.C_TEXT_MUTED + ";" +
                        "-fx-cursor:hand;-fx-background-radius:6;-fx-padding:5 10 5 10;");
        closeSearch.setOnAction(e -> toggleSearch());

        bar.getChildren().addAll(
                BubbleBuilder.mkLabel("Search:", AppTheme.C_TEXT_SEC, 11, false),
                searchField, resultLbl, closeSearch);
        return bar;
    }

    private void toggleSearch() {
        searchVisible = !searchVisible;
        if (searchVisible) {
            searchBarBox.setVisible(true);
            searchBarBox.setManaged(true);
            searchBarBox.setOpacity(0);
            searchBarBox.setTranslateY(-10);
            FadeTransition ft = new FadeTransition(Duration.millis(200), searchBarBox);
            ft.setFromValue(0); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), searchBarBox);
            tt.setFromY(-10); tt.setToY(0);
            tt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft, tt).play();
            searchField.clear();
            searchField.requestFocus();
        } else {
            FadeTransition ft = new FadeTransition(Duration.millis(150), searchBarBox);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> {
                searchBarBox.setVisible(false);
                searchBarBox.setManaged(false);
            });
            ft.play();
            clearHighlights();
        }
    }

    private int searchMessages(String term) {
        clearHighlights();
        String lower = term.toLowerCase();
        int    count = 0;
        HBox   first = null;

        for (javafx.scene.Node node : chatBox.getChildren()) {
            if (node instanceof HBox row) {
                String txt = extractRowText(row);
                if (txt != null && txt.toLowerCase().contains(lower)) {
                    String ex = row.getStyle() == null ? "" : row.getStyle();
                    row.setStyle(ex +
                            "-fx-border-color:#7c3aed;" +
                            "-fx-border-width:2;" +
                            "-fx-border-radius:10;" +
                            "-fx-padding:2;");
                    if (first == null) first = row;
                    count++;
                }
            }
        }
        if (first != null) {
            final HBox fm = first;
            Platform.runLater(() -> {
                double pos   = fm.getBoundsInParent().getMinY();
                double total = chatBox.getBoundsInLocal().getHeight();
                if (total > 0) scrollPane.setVvalue(pos / total);
            });
        }
        return count;
    }

    private String extractRowText(HBox row) {
        for (javafx.scene.Node n : row.getChildren()) {
            if (n instanceof VBox vb) {
                for (javafx.scene.Node c : vb.getChildren()) {
                    if (c instanceof Label l
                            && l.getText() != null
                            && !l.getText().isEmpty())
                        return l.getText();
                    if (c instanceof VBox iv) {
                        for (javafx.scene.Node ic : iv.getChildren())
                            if (ic instanceof Label l2
                                    && l2.getText() != null
                                    && !l2.getText().isEmpty())
                                return l2.getText();
                    }
                }
            }
        }
        return null;
    }

    private void clearHighlights() {
        String tag = "-fx-border-color:#7c3aed;" +
                "-fx-border-width:2;" +
                "-fx-border-radius:10;" +
                "-fx-padding:2;";
        for (javafx.scene.Node node : chatBox.getChildren())
            if (node instanceof HBox row
                    && row.getStyle() != null
                    && row.getStyle().contains("#7c3aed"))
                row.setStyle(row.getStyle().replace(tag, ""));
    }

    // ── Chat scroll area ──────────────────────────────────────
    private ScrollPane buildChatArea() {
        chatBox = new VBox(20);
        chatBox.setPadding(new Insets(28, 100, 28, 100));
        chatBox.setFillWidth(true);
        chatBox.setStyle("-fx-background-color:" + AppTheme.C_BG + ";");

        scrollPane = new ScrollPane(chatBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle(
                "-fx-background:" + AppTheme.C_BG + ";" +
                        "-fx-background-color:" + AppTheme.C_BG + ";" +
                        "-fx-border-color:transparent;");
        return scrollPane;
    }

    // ── Input area ────────────────────────────────────────────
    private VBox buildInputArea() {
        inputArea = new TextArea();
        inputArea.setPromptText("Message JavaAI...");
        inputArea.setPrefRowCount(2);
        inputArea.setMaxHeight(130);
        inputArea.setWrapText(true);
        inputArea.setFont(Font.font("Segoe UI", 13));
        inputArea.setStyle(
                "-fx-background-color:" + AppTheme.C_SURFACE2 + ";" +
                        "-fx-control-inner-background:" + AppTheme.C_SURFACE2 + ";" +
                        "-fx-text-fill:" + AppTheme.C_TEXT + ";" +
                        "-fx-prompt-text-fill:" + AppTheme.C_TEXT_MUTED + ";" +
                        "-fx-highlight-fill:#4c1d95;" +
                        "-fx-highlight-text-fill:" + AppTheme.C_TEXT + ";" +
                        "-fx-border-color:transparent;" +
                        "-fx-background-insets:0;" +
                        "-fx-background-radius:12;" +
                        "-fx-padding:12 14 12 14;");
        HBox.setHgrow(inputArea, Priority.ALWAYS);

        charCountLabel = BubbleBuilder.mkLabel("", AppTheme.C_TEXT_MUTED, 10, false);
        inputArea.textProperty().addListener((obs, o, n) ->
                charCountLabel.setText(n.length() > 0 ? n.length() + " chars" : ""));
        inputArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && e.isControlDown()) {
                handleSend();
                e.consume();
            }
        });

        sendBtn = new Button(">");
        sendBtn.setPrefSize(42, 42);
        sendBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        sendBtn.setStyle(sendStyle(AppTheme.C_SEND));
        sendBtn.setOnMouseEntered(e -> {
            sendBtn.setStyle(sendStyle("#6d28d9"));
            BubbleBuilder.scaleTo(sendBtn, 1.12);
        });
        sendBtn.setOnMouseExited(e -> {
            sendBtn.setStyle(sendStyle(AppTheme.C_SEND));
            BubbleBuilder.scaleTo(sendBtn, 1.0);
        });
        sendBtn.setOnAction(e -> handleSend());

        // Input wrapper with focus glow
        HBox inputInner = new HBox(8);
        inputInner.setAlignment(Pos.BOTTOM_CENTER);
        inputInner.setPadding(new Insets(0, 8, 0, 0));
        inputInner.setStyle(inputInnerStyle(false));
        inputArea.focusedProperty().addListener((obs, old, focused) ->
                inputInner.setStyle(inputInnerStyle(focused)));
        inputInner.getChildren().addAll(inputArea, sendBtn);

        Label hint = BubbleBuilder.mkLabel(
                "Ctrl+Enter send  •  Ctrl+N new chat  •  Ctrl+F search  •  Ctrl+/ shortcuts",
                AppTheme.C_TEXT_MUTED, 10, false);
        Region msp = new Region();
        HBox.setHgrow(msp, Priority.ALWAYS);
        HBox meta = new HBox(8, hint, msp, charCountLabel);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.setPadding(new Insets(5, 6, 0, 4));

        VBox wrapper = new VBox(6, inputInner, meta);
        wrapper.setPadding(new Insets(14, 100, 16, 100));
        wrapper.setStyle("-fx-background-color:" + AppTheme.C_BG + ";");
        return new VBox(0, mkSep(), wrapper);
    }

    private String inputInnerStyle(boolean focused) {
        return "-fx-background-color:" + AppTheme.C_SURFACE2 + ";" +
                "-fx-border-color:" + (focused ? AppTheme.C_ACCENT2 : AppTheme.C_BORDER2) + ";" +
                "-fx-border-radius:14;-fx-background-radius:14;-fx-border-width:1.5;";
    }

    // ══════════════════════════════════════════════════════════
    //  SEND MESSAGE
    // ══════════════════════════════════════════════════════════
    private void handleSend() {
        if (isTyping) return;
        String text = inputArea.getText().trim();
        if (text.isEmpty()) {
            BubbleBuilder.shakeNode(inputArea);
            return;
        }

        // Send button squish animation
        ScaleTransition sq = new ScaleTransition(Duration.millis(80), sendBtn);
        sq.setToX(0.82); sq.setToY(0.82);
        sq.setInterpolator(Interpolator.EASE_IN);
        ScaleTransition bk = new ScaleTransition(Duration.millis(200), sendBtn);
        bk.setToX(1.0); bk.setToY(1.0);
        bk.setInterpolator(new BubbleBuilder.ElasticInterp());
        new SequentialTransition(sq, bk).play();

        // Auto-name session from first message
        if (activeSession.messages.isEmpty())
            activeSession.title = text.length() > 36
                    ? text.substring(0, 36) + "..." : text;

        int msgIndex = history.size();
        chatBox.getChildren().add(
                BubbleBuilder.buildUserBubble(text, msgIndex, fontSize, this::editAndRetry));
        inputArea.clear();
        charCountLabel.setText("");
        isTyping = true;
        updateStatus("Thinking...", AppTheme.C_YELLOW);

        history.add(Map.of("role", "user", "content", text));
        activeSession.messages.add(Map.of("role", "user", "content", text));

        HBox typingRow = BubbleBuilder.buildTypingDots();
        chatBox.getChildren().add(typingRow);
        scrollToBottom();

        new Thread(() -> {
            try {
                String reply = api.call(
                        API_KEY, currentModel, temperature, systemPrompt, history);
                int newTokens = api.getLastTokenCount();

                Platform.runLater(() -> {
                    chatBox.getChildren().remove(typingRow);
                    chatBox.getChildren().add(
                            BubbleBuilder.buildBotBubble(reply, fontSize));
                    history.add(Map.of("role", "assistant", "content", reply));
                    activeSession.messages.add(
                            Map.of("role", "assistant", "content", reply));

                    totalTokens += newTokens;
                    tokenLabel.setText(
                            "Tokens used: " + String.format("%,d", totalTokens));
                    topTokenLabel.setText(
                            String.format("%,d", totalTokens) + " tokens");

                    isTyping = false;
                    updateStatus("Ready", AppTheme.C_GREEN);
                    scrollToBottom();
                    refreshHistory();
                    autoSave();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    chatBox.getChildren().remove(typingRow);
                    chatBox.getChildren().add(BubbleBuilder.buildBotBubble(
                            "Error: " + ex.getMessage() +
                                    "\n\nCheck API key under Settings -> Set API Key.",
                            fontSize));
                    isTyping = false;
                    updateStatus("Error", AppTheme.C_RED);
                });
            }
        }).start();
    }

    // ── Edit & Retry ──────────────────────────────────────────
    private void editAndRetry(HBox originalRow, int msgIndex) {
        if (isTyping) return;

        // Extract text from the bubble label
        String originalText = "";
        outer:
        for (javafx.scene.Node n : originalRow.getChildren()) {
            if (n instanceof VBox vb) {
                for (javafx.scene.Node c : vb.getChildren()) {
                    if (c instanceof Label l && !l.getText().isEmpty()) {
                        originalText = l.getText();
                        break outer;
                    }
                }
            }
        }

        int rowIdx = chatBox.getChildren().indexOf(originalRow);
        if (rowIdx >= 0) {
            final int ri = rowIdx;
            FadeTransition ft = new FadeTransition(Duration.millis(160), originalRow);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e ->
                    chatBox.getChildren().remove(ri, chatBox.getChildren().size()));
            ft.play();
        }
        if (msgIndex >= 0 && msgIndex <= history.size())
            history.subList(msgIndex, history.size()).clear();
        if (msgIndex >= 0 && msgIndex <= activeSession.messages.size())
            activeSession.messages.subList(msgIndex, activeSession.messages.size()).clear();

        inputArea.setText(originalText);
        inputArea.requestFocus();
        inputArea.positionCaret(originalText.length());
    }

    // ══════════════════════════════════════════════════════════
    //  WELCOME CARD  (staggered entrance animation)
    // ══════════════════════════════════════════════════════════
    private void showWelcomeCard() {
        VBox card = new VBox(22);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(780);
        card.setPadding(new Insets(44, 44, 40, 44));
        card.setStyle(
                "-fx-background-color:" + AppTheme.C_SURFACE + ";" +
                        "-fx-background-radius:24;" +
                        "-fx-border-color:" + AppTheme.C_BORDER2 + ";" +
                        "-fx-border-radius:24;" +
                        "-fx-border-width:1;");

        // Avatar (spins in)
        StackPane avatar = BubbleBuilder.buildAvatar("AI", 68);
        avatar.setOpacity(0);
        avatar.setScaleX(0.3); avatar.setScaleY(0.3);
        avatar.setRotate(-180);

        // Title (slides up)
        Label titleLbl = BubbleBuilder.mkLabel(
                "JavaAI  --  Llama 3.1 Powered", AppTheme.C_TEXT, 24, true);
        titleLbl.setAlignment(Pos.CENTER);
        titleLbl.setOpacity(0);
        titleLbl.setTranslateY(20);

        // Pills
        String mode = AppTheme.isDark ? "Dark Mode" : "Light Mode";
        Label[] pills = {
                makePill("Real AI",   AppTheme.C_ACCENT),
                makePill("Memory",    AppTheme.C_GREEN),
                makePill("No Maven",  AppTheme.C_ACCENT3),
                makePill("Auto-Save", AppTheme.C_ACCENT2),
                makePill(mode,        AppTheme.C_TEXT_MUTED)
        };
        HBox pillRow = new HBox(10);
        pillRow.setAlignment(Pos.CENTER);
        for (Label p : pills) { p.setOpacity(0); pillRow.getChildren().add(p); }

        // Suggestion grid
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);
        grid.setOpacity(0); grid.setTranslateY(15);

        String[][] prompts = {
                {"Explain OOP in Java",
                        "Explain OOP in Java with clear examples."},
                {"What is Deep Learning?",
                        "What is deep learning? Explain with real-world examples."},
                {"How to debug Java code?",
                        "Best strategies to debug Java code effectively?"},
                {"Teach me JavaFX basics",
                        "Beginner intro to JavaFX with a simple example."},
                {"Best sorting algorithm?",
                        "Compare sorting algorithms -- which to use and when?"},
                {"Tell me a Java joke",
                        "Tell me a funny programming joke!"}
        };

        for (int i = 0; i < prompts.length; i++) {
            Button btn = new Button(prompts[i][0]);
            btn.setPrefWidth(330);
            btn.setPrefHeight(48);
            btn.setFont(Font.font("Segoe UI", 12));
            btn.setMaxWidth(Double.MAX_VALUE);
            String ns = sugStyle(false), hs = sugStyle(true);
            btn.setStyle(ns);
            btn.setOnMouseEntered(ev -> {
                btn.setStyle(hs);
                BubbleBuilder.scaleTo(btn, 1.02);
            });
            btn.setOnMouseExited(ev -> {
                btn.setStyle(ns);
                BubbleBuilder.scaleTo(btn, 1.0);
            });
            final String p = prompts[i][1];
            btn.setOnAction(e -> {
                BubbleBuilder.animatePress(btn);
                inputArea.setText(p);
                handleSend();
            });
            grid.add(btn, i % 2, i / 2);
        }

        card.getChildren().addAll(avatar, titleLbl, pillRow, grid);
        HBox wrapper = new HBox(card);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(20, 0, 20, 0));
        chatBox.getChildren().add(wrapper);

        // ── Staggered entrance ──
        // 1. Avatar spins in
        PauseTransition a0 = new PauseTransition(Duration.millis(80));
        a0.setOnFinished(e -> {
            FadeTransition  ft2 = new FadeTransition(Duration.millis(380), avatar);
            ft2.setFromValue(0); ft2.setToValue(1);
            ScaleTransition st2 = new ScaleTransition(Duration.millis(480), avatar);
            st2.setFromX(0.3); st2.setFromY(0.3);
            st2.setToX(1.0);   st2.setToY(1.0);
            st2.setInterpolator(new BubbleBuilder.ElasticInterp());
            RotateTransition rt2 = new RotateTransition(Duration.millis(480), avatar);
            rt2.setFromAngle(-180); rt2.setToAngle(0);
            rt2.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft2, st2, rt2).play();
        });

        // 2. Title slides up
        PauseTransition a1 = new PauseTransition(Duration.millis(340));
        a1.setOnFinished(e -> {
            FadeTransition    ft2 = new FadeTransition(Duration.millis(300), titleLbl);
            ft2.setFromValue(0); ft2.setToValue(1);
            TranslateTransition tt2 = new TranslateTransition(Duration.millis(300), titleLbl);
            tt2.setFromY(20); tt2.setToY(0);
            tt2.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft2, tt2).play();
        });

        // 3. Pills appear one by one
        for (int i = 0; i < pills.length; i++) {
            final Label pill  = pills[i];
            final int   delay = 500 + i * 70;
            PauseTransition ap = new PauseTransition(Duration.millis(delay));
            ap.setOnFinished(e -> {
                FadeTransition  ft2 = new FadeTransition(Duration.millis(210), pill);
                ft2.setFromValue(0); ft2.setToValue(1);
                ScaleTransition st2 = new ScaleTransition(Duration.millis(210), pill);
                st2.setFromX(0.5); st2.setFromY(0.5);
                st2.setToX(1.0);   st2.setToY(1.0);
                st2.setInterpolator(new BubbleBuilder.ElasticInterp());
                new ParallelTransition(ft2, st2).play();
            });
            ap.play();
        }

        // 4. Grid fades in
        PauseTransition a2 = new PauseTransition(Duration.millis(860));
        a2.setOnFinished(e -> {
            FadeTransition    ft2 = new FadeTransition(Duration.millis(360), grid);
            ft2.setFromValue(0); ft2.setToValue(1);
            TranslateTransition tt2 = new TranslateTransition(Duration.millis(360), grid);
            tt2.setFromY(15); tt2.setToY(0);
            tt2.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft2, tt2).play();
        });

        a0.play(); a1.play(); a2.play();
    }

    // ══════════════════════════════════════════════════════════
    //  SESSION MANAGEMENT
    // ══════════════════════════════════════════════════════════
    private void createNewSession() {
        activeSession = new ChatSession("New Chat");
        sessions.add(activeSession);
        history.clear();
        clearChat();
    }

    private void switchSession(ChatSession s) {
        activeSession = s;
        history.clear();
        history.addAll(s.messages);

        FadeTransition fo = new FadeTransition(Duration.millis(140), chatBox);
        fo.setFromValue(1); fo.setToValue(0);
        fo.setOnFinished(e -> {
            chatBox.getChildren().clear();
            showWelcomeCard();
            int[] idx = {0};
            for (Map<String, String> m : s.messages) {
                if ("user".equals(m.get("role"))) {
                    final int fi = idx[0];
                    chatBox.getChildren().add(BubbleBuilder.buildUserBubble(
                            m.get("content"), fi, fontSize, this::editAndRetry));
                } else if ("assistant".equals(m.get("role"))) {
                    chatBox.getChildren().add(BubbleBuilder.buildBotBubble(
                            m.get("content"), fontSize));
                }
                idx[0]++;
            }
            FadeTransition fi2 = new FadeTransition(Duration.millis(180), chatBox);
            fi2.setFromValue(0); fi2.setToValue(1);
            fi2.play();
        });
        fo.play();
        refreshHistory();
    }

    private void clearChat() {
        FadeTransition fo = new FadeTransition(Duration.millis(160), chatBox);
        fo.setFromValue(1); fo.setToValue(0);
        fo.setOnFinished(e -> {
            chatBox.getChildren().clear();
            history.clear();
            activeSession.messages.clear();
            showWelcomeCard();
            FadeTransition fi = new FadeTransition(Duration.millis(180), chatBox);
            fi.setFromValue(0); fi.setToValue(1);
            fi.play();
        });
        fo.play();
        refreshHistory();
        autoSave();
    }

    private void confirmClearChat() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Clear Chat");
        a.setHeaderText("Clear this conversation?");
        a.setContentText("All messages will be removed.");
        a.initOwner(mainStage);
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) clearChat();
        });
    }

    private void renameSession(ChatSession s) {
        TextInputDialog dlg = new TextInputDialog(s.title);
        dlg.setTitle("Rename Chat");
        dlg.setHeaderText(null);
        dlg.setContentText("New name:");
        dlg.initOwner(mainStage);
        dlg.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                s.title = name.trim();
                refreshHistory();
                autoSave();
            }
        });
    }

    private void refreshHistory() {
        Platform.runLater(() -> {
            historyPanel.getChildren().clear();
            for (int i = sessions.size() - 1; i >= 0; i--) {
                ChatSession s      = sessions.get(i);
                boolean     active = (s == activeSession);

                HBox item = new HBox(8);
                item.setAlignment(Pos.CENTER_LEFT);
                item.setPadding(new Insets(8, 10, 8, 10));
                item.setStyle(histStyle(active));
                item.setOpacity(0);

                Label icon = BubbleBuilder.mkLabel(
                        ">", AppTheme.C_TEXT_MUTED, 11, false);
                icon.setMinWidth(16);

                Label lbl = BubbleBuilder.mkLabel(
                        s.title,
                        active ? AppTheme.C_TEXT : AppTheme.C_TEXT_MUTED,
                        12, active);
                lbl.setMaxWidth(130);

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                Label cnt = BubbleBuilder.mkLabel(
                        String.valueOf(s.messageCount()),
                        AppTheme.C_TEXT_MUTED, 10, false);
                cnt.setStyle(
                        "-fx-background-color:" + AppTheme.C_SURFACE3 + ";" +
                                "-fx-background-radius:10;-fx-padding:1 6 1 6;");

                item.getChildren().addAll(icon, lbl, sp, cnt);

                final ChatSession fs = s;
                item.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) renameSession(fs);
                    else switchSession(fs);
                });
                item.setOnMouseEntered(e -> {
                    if (fs != activeSession) item.setStyle(histStyle(true));
                    BubbleBuilder.scaleTo(item, 1.02);
                });
                item.setOnMouseExited(e -> {
                    if (fs != activeSession) item.setStyle(histStyle(false));
                    BubbleBuilder.scaleTo(item, 1.0);
                });

                historyPanel.getChildren().add(item);

                // Staggered fade-in
                final int delay = (sessions.size() - 1 - i) * 45;
                PauseTransition pt = new PauseTransition(Duration.millis(delay));
                pt.setOnFinished(e -> {
                    FadeTransition    ft = new FadeTransition(Duration.millis(180), item);
                    ft.setFromValue(0); ft.setToValue(1);
                    TranslateTransition tt = new TranslateTransition(Duration.millis(180), item);
                    tt.setFromX(-10); tt.setToX(0);
                    tt.setInterpolator(Interpolator.EASE_OUT);
                    new ParallelTransition(ft, tt).play();
                });
                pt.play();
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  AUTO-SAVE  (JSON  ->  ~/JavaAI_sessions.json)
    // ══════════════════════════════════════════════════════════
    private void autoSave() {
        new Thread(() -> {
            try {
                StringBuilder json = new StringBuilder();
                json.append("{\"totalTokens\":").append(totalTokens)
                        .append(",\"sessions\":[");

                for (int si = 0; si < sessions.size(); si++) {
                    ChatSession s = sessions.get(si);
                    if (si > 0) json.append(",");
                    json.append("{\"title\":")
                            .append(APIService.jsonString(s.title))
                            .append(",\"messages\":[");
                    for (int mi = 0; mi < s.messages.size(); mi++) {
                        Map<String, String> m = s.messages.get(mi);
                        if (mi > 0) json.append(",");
                        json.append("{\"role\":")
                                .append(APIService.jsonString(m.get("role")))
                                .append(",\"content\":")
                                .append(APIService.jsonString(m.get("content")))
                                .append("}");
                    }
                    json.append("]}");
                }
                json.append("]}");

                try (BufferedWriter bw = new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(SAVE_FILE),
                                StandardCharsets.UTF_8))) {
                    bw.write(json.toString());
                }
            } catch (Exception ex) {
                System.err.println("Auto-save failed: " + ex.getMessage());
            }
        }).start();
    }

    // ══════════════════════════════════════════════════════════
    //  LOAD SESSIONS  (on startup)
    // ══════════════════════════════════════════════════════════
    private void loadSavedSessions() {
        if (!SAVE_FILE.exists()) return;
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(SAVE_FILE),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            String json = sb.toString();

            // Parse totalTokens
            int ti = json.indexOf("\"totalTokens\":");
            if (ti >= 0) {
                String rest = json.substring(ti + 14).trim();
                int end = 0;
                while (end < rest.length()
                        && Character.isDigit(rest.charAt(end))) end++;
                if (end > 0)
                    totalTokens = Integer.parseInt(rest.substring(0, end));
            }

            // Parse sessions array
            int sessIdx = json.indexOf("\"sessions\":[");
            if (sessIdx < 0) return;
            String arr   = json.substring(sessIdx + 12);
            int    depth = 0, start = 0;
            for (int i = 0; i < arr.length(); i++) {
                char c = arr.charAt(i);
                if (c == '{') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        ChatSession s = parseSession(arr.substring(start, i + 1));
                        if (s != null) sessions.add(s);
                    }
                } else if (c == ']' && depth == 0) break;
            }
        } catch (Exception ex) {
            System.err.println("Load sessions failed: " + ex.getMessage());
        }
    }

    private ChatSession parseSession(String json) {
        String title = APIService.extractJsonValue(json, "title");
        if (title.isEmpty()) return null;
        ChatSession s = new ChatSession(title);

        int msgIdx = json.indexOf("\"messages\":[");
        if (msgIdx < 0) return s;
        String arr   = json.substring(msgIdx + 12);
        int    depth = 0, start = 0;

        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    String mj      = arr.substring(start, i + 1);
                    String role    = APIService.extractJsonValue(mj, "role");
                    String content = APIService.extractJsonValue(mj, "content");
                    if (!role.isEmpty() && !content.isEmpty())
                        s.messages.add(Map.of("role", role, "content", content));
                }
            } else if (c == ']' && depth == 0) break;
        }
        return s;
    }

    // ══════════════════════════════════════════════════════════
    //  EXPORT CHAT
    // ══════════════════════════════════════════════════════════
    private void exportChat() {
        if (history.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Start a conversation first!", ButtonType.OK).showAndWait();
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Chat Export");
        fc.setInitialFileName("JavaAI_" +
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".txt");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fc.showSaveDialog(mainStage);
        if (file == null) return;

        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
            fw.write("JavaAI Chat Export\n");
            fw.write("Date: " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\n");
            fw.write("Model: " + currentModel + "\n");
            fw.write("=".repeat(50) + "\n\n");
            for (Map<String, String> m : history)
                fw.write("[" + m.get("role").toUpperCase() + "]\n"
                        + m.get("content") + "\n\n");
            new Alert(Alert.AlertType.INFORMATION,
                    "Saved to:\n" + file.getAbsolutePath(),
                    ButtonType.OK).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Export failed: " + ex.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  DIALOGS
    // ══════════════════════════════════════════════════════════
    private void openSystemPromptDialog() {
        Stage dlg = makeDialog("Edit System Prompt", 520, 320);
        TextArea ta = new TextArea(systemPrompt);
        ta.setWrapText(true); ta.setPrefRowCount(7);
        ta.setFont(Font.font("Segoe UI", 12));
        ta.setStyle(
                "-fx-background-color:" + AppTheme.C_SURFACE + ";" +
                        "-fx-text-fill:" + AppTheme.C_TEXT + ";" +
                        "-fx-control-inner-background:" + AppTheme.C_SURFACE + ";" +
                        "-fx-border-color:" + AppTheme.C_BORDER + ";" +
                        "-fx-border-radius:8;-fx-background-radius:8;");

        Button save   = mkBtn("  Save  ", AppTheme.C_GREEN,    "white");
        Button cancel = mkBtn("  Cancel  ", AppTheme.C_SURFACE2, AppTheme.C_TEXT_MUTED);
        cancel.setOnAction(e -> dlg.close());
        save.setOnAction(e -> { systemPrompt = ta.getText().trim(); dlg.close(); });

        HBox btns = new HBox(10, save, cancel);
        btns.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(12,
                BubbleBuilder.mkLabel("System Prompt", AppTheme.C_TEXT, 14, true),
                ta, btns);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color:" + AppTheme.C_BG + ";");
        dlg.setScene(new Scene(layout, 520, 320));
        dlg.show();
    }

    private void openApiKeyDialog() {
        Stage dlg = makeDialog("Set API Key", 420, 200);
        PasswordField pf = new PasswordField();
        pf.setText(API_KEY);
        pf.setFont(Font.font("Segoe UI", 13));
        pf.setStyle(
                "-fx-background-color:" + AppTheme.C_SURFACE + ";" +
                        "-fx-text-fill:" + AppTheme.C_TEXT + ";" +
                        "-fx-border-color:" + AppTheme.C_BORDER + ";" +
                        "-fx-border-radius:8;-fx-background-radius:8;" +
                        "-fx-padding:10;" +
                        "-fx-control-inner-background:" + AppTheme.C_SURFACE + ";");

        Label link = BubbleBuilder.mkLabel(
                "Free key at: console.groq.com", AppTheme.C_ACCENT3, 11, false);

        Button save   = mkBtn("  Save Key  ", AppTheme.C_GREEN,    "white");
        Button cancel = mkBtn("  Cancel  ",   AppTheme.C_SURFACE2, AppTheme.C_TEXT_MUTED);
        cancel.setOnAction(e -> dlg.close());
        save.setOnAction(e -> {
            API_KEY = pf.getText().trim();
            updateStatus("API Key Saved", AppTheme.C_GREEN);
            dlg.close();
        });

        HBox btns = new HBox(10, save, cancel);
        btns.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(12,
                BubbleBuilder.mkLabel("Groq API Key", AppTheme.C_TEXT, 14, true),
                link, pf, btns);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color:" + AppTheme.C_BG + ";");
        dlg.setScene(new Scene(layout, 420, 200));
        dlg.show();
    }

    private void showShortcutsDialog() {
        Stage dlg = makeDialog("Keyboard Shortcuts", 390, 320);
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color:" + AppTheme.C_BG + ";");
        box.getChildren().add(
                BubbleBuilder.mkLabel("Keyboard Shortcuts", AppTheme.C_TEXT, 14, true));

        String[][] sc = {
                {"Ctrl + Enter", "Send message"},
                {"Ctrl + N",     "New conversation"},
                {"Ctrl + F",     "Search in chat"},
                {"Ctrl + L",     "Clear current chat"},
                {"Ctrl + /",     "Show shortcuts"},
                {"Escape",       "Close search bar"}
        };
        for (String[] row : sc) {
            HBox r = new HBox(14);
            r.setAlignment(Pos.CENTER_LEFT);
            Label key = new Label(row[0]);
            key.setFont(Font.font("Consolas", 11));
            key.setTextFill(Color.web(AppTheme.C_ACCENT));
            key.setStyle(
                    "-fx-background-color:" + AppTheme.C_SURFACE2 + ";" +
                            "-fx-border-color:" + AppTheme.C_BORDER + ";" +
                            "-fx-border-radius:5;-fx-background-radius:5;" +
                            "-fx-padding:3 10 3 10;");
            key.setMinWidth(120);
            r.getChildren().addAll(
                    key, BubbleBuilder.mkLabel(row[1], AppTheme.C_TEXT_SEC, 12, false));
            box.getChildren().add(r);
        }

        Button close = mkBtn("  Close  ", AppTheme.C_SURFACE2, AppTheme.C_TEXT_MUTED);
        close.setOnAction(e -> dlg.close());
        HBox btns = new HBox(close);
        btns.setAlignment(Pos.CENTER_RIGHT);
        btns.setPadding(new Insets(8, 0, 0, 0));
        box.getChildren().add(btns);

        Scene sc2 = new Scene(box, 390, 320);
        sc2.setFill(Color.web(AppTheme.C_BG));
        box.setOpacity(0); box.setScaleX(0.92); box.setScaleY(0.92);
        dlg.setScene(sc2);
        dlg.show();

        // Dialog entrance animation
        FadeTransition ft = new FadeTransition(Duration.millis(200), box);
        ft.setFromValue(0); ft.setToValue(1);
        ScaleTransition st = new ScaleTransition(Duration.millis(240), box);
        st.setFromX(0.92); st.setFromY(0.92);
        st.setToX(1.0);    st.setToY(1.0);
        st.setInterpolator(new BubbleBuilder.ElasticInterp());
        new ParallelTransition(ft, st).play();
    }

    private Stage makeDialog(String title, int w, int h) {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.initOwner(mainStage);
        dlg.setTitle(title);
        dlg.setResizable(false);
        return dlg;
    }

    // ══════════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════════
    private void updateStatus(String text, String color) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            statusLabel.setTextFill(Color.web(color));
            String bg = AppTheme.C_GREEN.equals(color)  ? "#052e16"
                    : AppTheme.C_RED.equals(color)    ? "#7f1d1d"
                      : "#422006";
            statusLabel.setStyle(
                    "-fx-padding:2 8 2 8;" +
                            "-fx-background-color:" + bg + ";" +
                            "-fx-background-radius:20;");
            ScaleTransition st = new ScaleTransition(Duration.millis(180), statusLabel);
            st.setFromX(1.2); st.setFromY(1.2);
            st.setToX(1.0);   st.setToY(1.0);
            st.setInterpolator(new BubbleBuilder.ElasticInterp());
            st.play();
        });
    }

    private void scrollToBottom() {
        Platform.runLater(() ->
                new Timeline(new KeyFrame(Duration.millis(280),
                        new KeyValue(scrollPane.vvalueProperty(), 1.0,
                                Interpolator.EASE_BOTH))).play());
    }

    private void animatePulse(Circle dot) {
        ScaleTransition p = new ScaleTransition(Duration.millis(900), dot);
        p.setFromX(1.0); p.setFromY(1.0);
        p.setToX(1.55);  p.setToY(1.55);
        p.setAutoReverse(true);
        p.setCycleCount(Animation.INDEFINITE);
        p.setInterpolator(Interpolator.EASE_BOTH);
        p.play();
    }

    private Region mkSep() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setStyle("-fx-background-color:" + AppTheme.C_BORDER + ";");
        return r;
    }

    private Button topIconBtn(String icon, String tip) {
        Button b = new Button(icon);
        b.setFont(Font.font("Segoe UI", 11));
        b.setStyle(topIconNormal());
        b.setOnMouseEntered(e -> {
            b.setStyle("-fx-background-color:" + AppTheme.C_SURFACE2 + ";" +
                    "-fx-text-fill:" + AppTheme.C_TEXT + ";" +
                    "-fx-background-radius:7;-fx-cursor:hand;-fx-padding:6 10 6 10;");
            BubbleBuilder.scaleTo(b, 1.1);
        });
        b.setOnMouseExited(e -> {
            b.setStyle(topIconNormal());
            BubbleBuilder.scaleTo(b, 1.0);
        });
        Tooltip.install(b, new Tooltip(tip));
        return b;
    }

    private String topIconNormal() {
        return "-fx-background-color:transparent;" +
                "-fx-text-fill:" + AppTheme.C_TEXT_MUTED + ";" +
                "-fx-background-radius:7;-fx-cursor:hand;-fx-padding:6 10 6 10;";
    }

    private Button sideBtn(String text) {
        String n = "-fx-background-color:" + AppTheme.C_SURFACE2 + ";" +
                "-fx-text-fill:" + AppTheme.C_TEXT + ";" +
                "-fx-border-color:" + AppTheme.C_BORDER + ";" +
                "-fx-border-radius:7;-fx-background-radius:7;" +
                "-fx-cursor:hand;-fx-padding:8 12 8 12;";
        String h = "-fx-background-color:" + AppTheme.C_SURFACE3 + ";" +
                "-fx-text-fill:" + AppTheme.C_TEXT + ";" +
                "-fx-border-color:" + AppTheme.C_BORDER2 + ";" +
                "-fx-border-radius:7;-fx-background-radius:7;" +
                "-fx-cursor:hand;-fx-padding:8 12 8 12;";
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFont(Font.font("Segoe UI", 11));
        b.setStyle(n);
        b.setOnMouseEntered(e -> { b.setStyle(h); BubbleBuilder.scaleTo(b, 1.02); });
        b.setOnMouseExited(e  -> { b.setStyle(n); BubbleBuilder.scaleTo(b, 1.0);  });
        return b;
    }

    private Button mkBtn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        b.setPadding(new Insets(8, 18, 8, 18));
        b.setStyle("-fx-background-color:" + bg + ";" +
                "-fx-text-fill:" + fg + ";" +
                "-fx-background-radius:8;-fx-cursor:hand;");
        return b;
    }

    private Label makePill(String text, String color) {
        Label p = new Label(text);
        p.setFont(Font.font("Segoe UI", 11));
        p.setTextFill(Color.web(color));
        p.setStyle("-fx-background-color:" + AppTheme.C_SURFACE2 + ";" +
                "-fx-border-color:" + color + "33;" +
                "-fx-border-radius:20;-fx-background-radius:20;" +
                "-fx-padding:4 12 4 12;-fx-border-width:1;");
        return p;
    }

    private String btnStyle(String bg, String fg) {
        return "-fx-background-color:" + bg + ";" +
                "-fx-text-fill:" + fg + ";" +
                "-fx-background-radius:9;-fx-cursor:hand;";
    }

    private String sendStyle(String bg) {
        return "-fx-background-color:" + bg + ";" +
                "-fx-text-fill:white;" +
                "-fx-background-radius:21;-fx-cursor:hand;";
    }

    private String histStyle(boolean active) {
        return "-fx-background-color:" + (active ? AppTheme.C_SURFACE2 : "transparent") + ";" +
                "-fx-background-radius:8;-fx-cursor:hand;";
    }

    private String sugStyle(boolean hover) {
        return "-fx-background-color:" + (hover ? AppTheme.C_SURFACE3 : AppTheme.C_SURFACE2) + ";" +
                "-fx-text-fill:"        + (hover ? AppTheme.C_TEXT     : AppTheme.C_TEXT_SEC)  + ";" +
                "-fx-border-color:"     + (hover ? AppTheme.C_ACCENT2  : AppTheme.C_BORDER)    + ";" +
                "-fx-border-radius:11;-fx-background-radius:11;-fx-border-width:1;" +
                "-fx-cursor:hand;-fx-padding:12 18 12 18;-fx-alignment:CENTER-LEFT;";
    }

    public static void main(String[] args) { launch(args); }
}