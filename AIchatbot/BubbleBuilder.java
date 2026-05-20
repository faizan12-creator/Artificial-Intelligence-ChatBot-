package AIchatbot;

import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.util.Duration;


import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

/**
 * BubbleBuilder — factory for chat bubbles, typing dots,
 * markdown renderer, code blocks, and shared animation helpers.
 */
public class BubbleBuilder {

    // ══════════════════════════════════════════════════════════
    //  ELASTIC INTERPOLATOR
    // ══════════════════════════════════════════════════════════
    public static class ElasticInterp extends Interpolator {
        @Override
        protected double curve(double t) {
            if (t == 0 || t == 1) return t;
            double p = 0.3, s = p / 4;
            return Math.pow(2, -10 * t)
                    * Math.sin((t - s) * (2 * Math.PI) / p) + 1;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  USER BUBBLE
    //  onEdit: (originalRow, msgIndex) -> void
    // ══════════════════════════════════════════════════════════
    public static HBox buildUserBubble(String text,
                                       int msgIndex,
                                       double fontSize,
                                       BiConsumer<HBox, Integer> onEdit) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_RIGHT);

        VBox bubble = new VBox(5);
        bubble.setMaxWidth(660);
        bubble.setStyle(
                "-fx-background-color:" + AppTheme.C_USER_BUBBLE + ";" +
                        "-fx-background-radius:20 20 5 20;" +
                        "-fx-padding:14 18 12 18;");

        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(630);
        msg.setFont(Font.font("Segoe UI", fontSize));
        msg.setTextFill(Color.WHITE);
        msg.setLineSpacing(4.5);

        // Footer: Edit button + timestamp
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("Edit");
        editBtn.setFont(Font.font("Segoe UI", 10));
        String en = "-fx-background-color:rgba(255,255,255,0.12);" +
                "-fx-text-fill:rgba(255,255,255,0.55);" +
                "-fx-background-radius:4;-fx-cursor:hand;-fx-padding:2 8 2 8;";
        String eh = "-fx-background-color:rgba(255,255,255,0.25);" +
                "-fx-text-fill:white;" +
                "-fx-background-radius:4;-fx-cursor:hand;-fx-padding:2 8 2 8;";
        editBtn.setStyle(en);
        editBtn.setOnMouseEntered(e -> editBtn.setStyle(eh));
        editBtn.setOnMouseExited(e  -> editBtn.setStyle(en));
        editBtn.setOnAction(e -> onEdit.accept(row, msgIndex));

        Label time = new Label(nowTime());
        time.setFont(Font.font("Segoe UI", 10));
        time.setTextFill(Color.rgb(255, 255, 255, 0.4));

        footer.getChildren().addAll(editBtn, time);
        bubble.getChildren().addAll(msg, footer);
        row.getChildren().addAll(bubble, buildUserAvatar());

        // Animate in from right
        row.setOpacity(0);
        row.setTranslateX(50);
        row.setScaleY(0.88);

        FadeTransition ft = new FadeTransition(Duration.millis(300), row);
        ft.setFromValue(0); ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(360), row);
        tt.setFromX(50); tt.setToX(0);
        tt.setInterpolator(new ElasticInterp());

        ScaleTransition st = new ScaleTransition(Duration.millis(280), row);
        st.setFromY(0.88); st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(ft, tt, st).play();
        return row;
    }

    // ══════════════════════════════════════════════════════════
    //  BOT BUBBLE
    // ══════════════════════════════════════════════════════════
    public static HBox buildBotBubble(String text, double fontSize) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.TOP_LEFT);

        VBox wrapper = new VBox(8);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        wrapper.setMaxWidth(Double.MAX_VALUE);

        VBox contentBox = new VBox(0);
        contentBox.setMaxWidth(Double.MAX_VALUE);
        contentBox.setStyle(
                "-fx-background-color:" + AppTheme.C_BOT_BUBBLE + ";" +
                        "-fx-background-radius:5 20 20 20;" +
                        "-fx-border-color:" + AppTheme.C_BORDER + ";" +
                        "-fx-border-radius:5 20 20 20;" +
                        "-fx-border-width:1;");

        // Check for code blocks
        if (text.contains("```")) {
            String[] parts = text.split("```");
            for (int i = 0; i < parts.length; i++) {
                if (i % 2 == 0) {
                    // Prose
                    String prose = parts[i].trim();
                    if (!prose.isEmpty()) {
                        VBox pb = renderMarkdown(prose, fontSize);
                        pb.setPadding(new Insets(i == 0 ? 16 : 10, 20, 10, 20));
                        contentBox.getChildren().add(pb);
                    }
                } else {
                    // Code
                    String raw  = parts[i];
                    String lang = "", code = raw;
                    if (raw.contains("\n")) {
                        lang = raw.substring(0, raw.indexOf("\n")).trim();
                        code = raw.substring(raw.indexOf("\n") + 1);
                    }
                    contentBox.getChildren().add(buildCodeBlock(code.trim(), lang));
                }
            }
        } else {
            VBox pb = renderMarkdown(text, fontSize);
            pb.setPadding(new Insets(16, 20, 14, 20));
            contentBox.getChildren().add(pb);
        }

        // Action bar
        HBox actions = buildBotActions(text);
        wrapper.getChildren().addAll(contentBox, actions);
        row.getChildren().addAll(buildAvatar("AI", 36), wrapper);

        // Animate in from left
        row.setOpacity(0);
        row.setTranslateX(-50);
        row.setScaleY(0.88);

        FadeTransition ft = new FadeTransition(Duration.millis(320), row);
        ft.setFromValue(0); ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(380), row);
        tt.setFromX(-50); tt.setToX(0);
        tt.setInterpolator(new ElasticInterp());

        ScaleTransition sc = new ScaleTransition(Duration.millis(300), row);
        sc.setFromY(0.88); sc.setToY(1.0);
        sc.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(ft, tt, sc).play();
        return row;
    }

    // ── Bot actions row (Copy + Like/Dislike + time) ──────────
    private static HBox buildBotActions(String text) {
        HBox bar = new HBox(6);
        bar.setPadding(new Insets(4, 4, 0, 4));
        bar.setAlignment(Pos.CENTER_LEFT);

        Button copyBtn = smallBtn("Copy");
        copyBtn.setOnAction(e -> {
            ClipboardContent cp = new ClipboardContent();
            cp.putString(text);
            Clipboard.getSystemClipboard().setContent(cp);
            copyBtn.setText("Copied!");
            new Timeline(new KeyFrame(Duration.seconds(2),
                    ev -> copyBtn.setText("Copy"))).play();
        });

        int[] likes    = {0};
        int[] dislikes = {0};
        Label lCnt = mkLabel("", AppTheme.C_TEXT_MUTED, 10, false);
        Label dCnt = mkLabel("", AppTheme.C_TEXT_MUTED, 10, false);

        Button likeBtn    = reactionBtn("+1");
        Button dislikeBtn = reactionBtn("-1");

        likeBtn.setOnAction(e -> {
            likes[0]++;
            likeBtn.setStyle(reactionActiveStyle(true));
            lCnt.setText(String.valueOf(likes[0]));
        });
        dislikeBtn.setOnAction(e -> {
            dislikes[0]++;
            dislikeBtn.setStyle(reactionActiveStyle(false));
            dCnt.setText(String.valueOf(dislikes[0]));
        });

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label timeLbl = mkLabel(nowTime(), AppTheme.C_TEXT_MUTED, 10, false);

        bar.getChildren().addAll(
                copyBtn, likeBtn, lCnt, dislikeBtn, dCnt, sp, timeLbl);
        return bar;
    }

    // ══════════════════════════════════════════════════════════
    //  TYPING DOTS  (wave animation)
    // ══════════════════════════════════════════════════════════
    public static HBox buildTypingDots() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        HBox dots = new HBox(9);
        dots.setAlignment(Pos.CENTER);
        dots.setPadding(new Insets(14, 24, 14, 24));
        dots.setStyle(
                "-fx-background-color:" + AppTheme.C_BOT_BUBBLE + ";" +
                        "-fx-background-radius:5 18 18 18;" +
                        "-fx-border-color:" + AppTheme.C_BORDER + ";" +
                        "-fx-border-radius:5 18 18 18;" +
                        "-fx-border-width:1;");

        Color[] colors = {
                Color.web(AppTheme.C_ACCENT2),
                Color.web(AppTheme.C_ACCENT3),
                Color.web(AppTheme.C_ACCENT)
        };

        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(5, Color.web(AppTheme.C_TEXT_MUTED));

            TranslateTransition up = new TranslateTransition(Duration.millis(340), dot);
            up.setFromY(0); up.setToY(-8);
            up.setInterpolator(Interpolator.EASE_BOTH);

            TranslateTransition dn = new TranslateTransition(Duration.millis(340), dot);
            dn.setFromY(-8); dn.setToY(0);
            dn.setInterpolator(Interpolator.EASE_BOTH);

            FillTransition cu = new FillTransition(Duration.millis(340), dot);
            cu.setFromValue(Color.web(AppTheme.C_TEXT_MUTED));
            cu.setToValue(colors[i]);

            FillTransition cd = new FillTransition(Duration.millis(340), dot);
            cd.setFromValue(colors[i]);
            cd.setToValue(Color.web(AppTheme.C_TEXT_MUTED));

            SequentialTransition wave = new SequentialTransition(
                    new ParallelTransition(up, cu),
                    new ParallelTransition(dn, cd));
            wave.setDelay(Duration.millis(i * 150));
            wave.setCycleCount(Animation.INDEFINITE);
            wave.play();

            dots.getChildren().add(dot);
        }

        row.setOpacity(0);
        row.setTranslateX(-30);
        row.getChildren().addAll(buildAvatar("AI", 36), dots);

        FadeTransition ft = new FadeTransition(Duration.millis(220), row);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(250), row);
        tt.setFromX(-30); tt.setToX(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();

        return row;
    }

    // ══════════════════════════════════════════════════════════
    //  MARKDOWN RENDERER
    //  Supports: # h1, ## h2, - bullet, 1. numbered, --- divider
    // ══════════════════════════════════════════════════════════
    public static VBox renderMarkdown(String text, double fontSize) {
        VBox box = new VBox(6);
        String[]      lines = text.split("\n");
        StringBuilder para  = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("# ")) {
                flushPara(box, para, fontSize);
                Label h = mkLabel(line.substring(2).trim(),
                        AppTheme.C_TEXT, (int)(fontSize + 3), true);
                h.setWrapText(true);
                VBox.setMargin(h, new Insets(4, 0, 2, 0));
                box.getChildren().add(h);

            } else if (line.startsWith("## ")) {
                flushPara(box, para, fontSize);
                Label h = mkLabel(line.substring(3).trim(),
                        AppTheme.C_ACCENT3, (int)(fontSize + 1), true);
                h.setWrapText(true);
                VBox.setMargin(h, new Insets(3, 0, 1, 0));
                box.getChildren().add(h);

            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                flushPara(box, para, fontSize);
                HBox bullet = new HBox(9);
                bullet.setAlignment(Pos.TOP_LEFT);
                Label dot = mkLabel("->", AppTheme.C_ACCENT2, (int)fontSize, true);
                dot.setMinWidth(20);
                Label txt = mkLabel(stripMd(line.substring(2)),
                        AppTheme.C_TEXT, (int)fontSize, false);
                txt.setWrapText(true);
                txt.setLineSpacing(3);
                HBox.setHgrow(txt, Priority.ALWAYS);
                bullet.getChildren().addAll(dot, txt);
                box.getChildren().add(bullet);

            } else if (line.matches("^\\d+\\.\\s.*")) {
                flushPara(box, para, fontSize);
                int di = line.indexOf(". ");
                HBox item = new HBox(9);
                item.setAlignment(Pos.TOP_LEFT);
                Label num = mkLabel(line.substring(0, di + 1),
                        AppTheme.C_ACCENT, (int)fontSize - 1, true);
                num.setMinWidth(24);
                Label txt = mkLabel(stripMd(line.substring(di + 2)),
                        AppTheme.C_TEXT, (int)fontSize, false);
                txt.setWrapText(true);
                txt.setLineSpacing(3);
                HBox.setHgrow(txt, Priority.ALWAYS);
                item.getChildren().addAll(num, txt);
                box.getChildren().add(item);

            } else if (line.equals("---") || line.equals("***")) {
                flushPara(box, para, fontSize);
                Region hr = new Region();
                hr.setPrefHeight(1);
                hr.setStyle("-fx-background-color:" + AppTheme.C_BORDER2 + ";");
                VBox.setMargin(hr, new Insets(4, 0, 4, 0));
                box.getChildren().add(hr);

            } else if (line.trim().isEmpty()) {
                if (para.length() > 0) flushPara(box, para, fontSize);

            } else {
                if (para.length() > 0) para.append("\n");
                para.append(line);
            }
        }
        flushPara(box, para, fontSize);
        return box;
    }

    private static void flushPara(VBox box, StringBuilder para, double fontSize) {
        if (para.length() == 0) return;
        Label lbl = mkLabel(stripMd(para.toString().trim()),
                AppTheme.C_TEXT, (int)fontSize, false);
        lbl.setWrapText(true);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setLineSpacing(5);
        box.getChildren().add(lbl);
        para.setLength(0);
    }

    private static String stripMd(String s) {
        return s.replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*",       "$1")
                .replaceAll("`(.+?)`",            "$1");
    }

    // ══════════════════════════════════════════════════════════
    //  CODE BLOCK
    // ══════════════════════════════════════════════════════════
    public static VBox buildCodeBlock(String code, String lang) {
        VBox block = new VBox(0);
        block.setStyle("-fx-border-color:" + AppTheme.C_BORDER +
                ";-fx-border-width:1 0 0 0;");

        // Header bar
        HBox header = new HBox(8);
        header.setPadding(new Insets(8, 14, 8, 14));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:" + AppTheme.C_SURFACE3 + ";");

        Circle r = new Circle(5, Color.web("#ff5f57"));
        Circle y = new Circle(5, Color.web("#ffbd2e"));
        Circle g = new Circle(5, Color.web("#28c840"));

        Label langLbl = mkLabel(
                lang.isEmpty() ? "code" : lang.toLowerCase(),
                AppTheme.C_TEXT_MUTED, 11, false);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button copyCode = new Button("Copy code");
        copyCode.setFont(Font.font("Segoe UI", 10));
        copyCode.setStyle(
                "-fx-background-color:" + AppTheme.C_SURFACE2 + ";" +
                        "-fx-text-fill:" + AppTheme.C_TEXT_MUTED + ";" +
                        "-fx-cursor:hand;-fx-background-radius:5;" +
                        "-fx-padding:3 10 3 10;" +
                        "-fx-border-color:" + AppTheme.C_BORDER + ";" +
                        "-fx-border-radius:5;");
        copyCode.setOnAction(e -> {
            ClipboardContent cp = new ClipboardContent();
            cp.putString(code);
            Clipboard.getSystemClipboard().setContent(cp);
            copyCode.setText("Copied!");
            new Timeline(new KeyFrame(Duration.seconds(2),
                    ev -> copyCode.setText("Copy code"))).play();
        });

        header.getChildren().addAll(r, y, g, langLbl, sp, copyCode);

        // Code body
        Label codeLabel = new Label(code);
        codeLabel.setWrapText(true);
        codeLabel.setMaxWidth(Double.MAX_VALUE);
        codeLabel.setFont(Font.font("Consolas", 12.5));
        codeLabel.setTextFill(Color.web(AppTheme.C_CODE_TEXT));
        codeLabel.setLineSpacing(4);
        codeLabel.setPadding(new Insets(14, 18, 14, 18));
        codeLabel.setStyle("-fx-background-color:" + AppTheme.C_CODE_BG + ";");

        block.getChildren().addAll(header, codeLabel);
        return block;
    }

    // ══════════════════════════════════════════════════════════
    //  SHARED WIDGET HELPERS
    // ══════════════════════════════════════════════════════════

    public static StackPane buildAvatar(String text, int size) {
        Circle bg = new Circle(size / 2.0);
        bg.setFill(new LinearGradient(0, 0, 1, 1, true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(AppTheme.C_ACCENT2)),
                new Stop(1, Color.web(AppTheme.C_ACCENT))));
        Label lbl = mkLabel(text, "#ffffff", Math.max(10, size / 3), true);
        StackPane sp = new StackPane(bg, lbl);
        sp.setMinSize(size, size);
        sp.setMaxSize(size, size);
        return sp;
    }

    public static StackPane buildUserAvatar() {
        Circle bg = new Circle(17, Color.web(AppTheme.C_USER_BUBBLE));
        Label lbl = mkLabel("U", "#ffffff", 11, true);
        StackPane sp = new StackPane(bg, lbl);
        sp.setMinSize(34, 34);
        sp.setMaxSize(34, 34);
        return sp;
    }

    public static Label mkLabel(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI",
                bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        l.setTextFill(Color.web(color));
        return l;
    }

    // ══════════════════════════════════════════════════════════
    //  ANIMATION HELPERS  (called from AIChatBot)
    // ══════════════════════════════════════════════════════════

    /** Quick press-and-bounce on any node */
    public static void animatePress(javafx.scene.Node node) {
        ScaleTransition dn = new ScaleTransition(Duration.millis(80), node);
        dn.setToX(0.88); dn.setToY(0.88);
        dn.setInterpolator(Interpolator.EASE_IN);
        ScaleTransition up = new ScaleTransition(Duration.millis(180), node);
        up.setToX(1.0); up.setToY(1.0);
        up.setInterpolator(new ElasticInterp());
        new SequentialTransition(dn, up).play();
    }

    /** Horizontal shake for empty-send warning */
    public static void shakeNode(javafx.scene.Node node) {
        double ox = node.getTranslateX();
        new Timeline(
                new KeyFrame(Duration.millis(0),   new KeyValue(node.translateXProperty(), ox)),
                new KeyFrame(Duration.millis(55),  new KeyValue(node.translateXProperty(), ox + 9)),
                new KeyFrame(Duration.millis(110), new KeyValue(node.translateXProperty(), ox - 9)),
                new KeyFrame(Duration.millis(165), new KeyValue(node.translateXProperty(), ox + 6)),
                new KeyFrame(Duration.millis(220), new KeyValue(node.translateXProperty(), ox - 6)),
                new KeyFrame(Duration.millis(275), new KeyValue(node.translateXProperty(), ox + 3)),
                new KeyFrame(Duration.millis(330), new KeyValue(node.translateXProperty(), ox))
        ).play();
    }

    /** Scale a node to target value smoothly */
    public static void scaleTo(javafx.scene.Node node, double target) {
        ScaleTransition st = new ScaleTransition(Duration.millis(110), node);
        st.setToX(target); st.setToY(target);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
    }

    // ── Private helpers ───────────────────────────────────────

    private static Button reactionBtn(String label) {
        Button b = new Button(label);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        b.setStyle("-fx-background-color:transparent;" +
                "-fx-text-fill:" + AppTheme.C_TEXT_MUTED + ";" +
                "-fx-background-radius:5;-fx-cursor:hand;-fx-padding:2 8 2 8;");
        b.setOnMouseEntered(e -> scaleTo(b, 1.25));
        b.setOnMouseExited(e  -> scaleTo(b, 1.0));
        return b;
    }

    private static String reactionActiveStyle(boolean isLike) {
        return isLike
                ? "-fx-background-color:#052e16;-fx-text-fill:#22c55e;" +
                  "-fx-background-radius:5;-fx-cursor:hand;-fx-padding:2 8 2 8;"
                : "-fx-background-color:#7f1d1d;-fx-text-fill:#ef4444;" +
                  "-fx-background-radius:5;-fx-cursor:hand;-fx-padding:2 8 2 8;";
    }

    private static Button smallBtn(String text) {
        String n = "-fx-background-color:transparent;" +
                "-fx-text-fill:" + AppTheme.C_TEXT_MUTED + ";" +
                "-fx-cursor:hand;-fx-padding:2 8 2 8;-fx-background-radius:4;";
        String h = "-fx-background-color:" + AppTheme.C_SURFACE2 + ";" +
                "-fx-text-fill:" + AppTheme.C_TEXT + ";" +
                "-fx-cursor:hand;-fx-padding:2 8 2 8;-fx-background-radius:4;";
        Button b = new Button(text);
        b.setFont(Font.font("Segoe UI", 10));
        b.setStyle(n);
        b.setOnMouseEntered(e -> b.setStyle(h));
        b.setOnMouseExited(e  -> b.setStyle(n));
        return b;
    }

    private static String nowTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}