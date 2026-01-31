package com.topzurdo.launcher.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.topzurdo.launcher.Constants;
import com.topzurdo.launcher.TopZurdoLauncher;
import com.topzurdo.launcher.config.JvmProfile;
import com.topzurdo.launcher.config.LauncherConfig;
import com.topzurdo.launcher.config.LauncherMetadata;
import com.topzurdo.launcher.config.theme.ThemeManager;
import com.topzurdo.launcher.service.AuthService;
import com.topzurdo.launcher.service.DownloadService;
import com.topzurdo.launcher.service.GameService;
import com.topzurdo.launcher.service.LauncherServices;
import com.topzurdo.launcher.service.OptimizationModService;
import com.topzurdo.launcher.service.ServerStatusService;
import com.topzurdo.launcher.ui.components.SmartOptimizationDialog;
import com.topzurdo.launcher.ui.components.StatusPill;
import com.topzurdo.launcher.ui.effects.EffectsEngine;
import com.topzurdo.launcher.ui.views.AboutView;
import com.topzurdo.launcher.ui.views.HomeView;
import com.topzurdo.launcher.ui.views.MainView;
import com.topzurdo.launcher.ui.views.SettingsView;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Main controller: composes MainView (TitleBar, NavRail, content), HomeView, SettingsView, AboutView.
 * No FXML. init() then createView(); services and logic stay here.
 */
public class MainController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    private LauncherConfig config;
    private LauncherServices services;
    private AuthService authService;
    private GameService gameService;
    private DownloadService downloadService;
    private ServerStatusService serverStatusService;
    private EffectsEngine effectsEngine;
    private OptimizationModService optimizationModService;

    private MainView mainView;
    private HomeView homeView;
    private SettingsView settingsView;
    private AboutView aboutView;

    private String currentPanel = "home";
    private boolean hotkeysInstalled;
    private Parent root;
    private Stage stage;
    private double dragOffsetX;
    private double dragOffsetY;

    public MainController(Stage stage) {
        this.stage = stage;
        init();
        this.root = createView();
    }

    public Parent getRoot() { return root; }

    public void init() {
        LOGGER.info("Initializing Zurdo Launcher");

        // Lazy initialization with background loading
        initializeAsync();
    }

    /**
     * Initialize components asynchronously for better startup performance
     */
    private void initializeAsync() {
        // Load critical components first
        config = LauncherConfig.load();
        ThemeManager.setMode(config.isLightTheme() ? ThemeManager.Mode.LIGHT : ThemeManager.Mode.DARK);

        services = LauncherServices.startAsync();
        LOGGER.info("Zurdo Launcher async init started");
    }

    public javafx.scene.Parent createView() {
        mainView = new MainView();
        homeView = new HomeView();
        settingsView = new SettingsView();
        aboutView = new AboutView();

        setupUI();
        loadUserSettings();
        setupServiceCallbacks();
        wireHome();
        wireSettings();
        wireTitleBar();
        wireNav();

        mainView.setContent(homeView);
        mainView.getNavRail().setActive("home");

        StackPane root = new StackPane();
        root.getStyleClass().add("root-stack");
        root.getChildren().add(mainView);

        // Effects: initialize when engine is ready
        services.onEffectsReady(engine -> Platform.runLater(() -> {
            effectsEngine = engine;
            effectsEngine.createParticleEffect("main-particles", root);
        }));

        checkServerStatus();
        LauncherMetadata meta = LauncherMetadata.load();
        mainView.getNavRail().setVersion(meta.getVersionLine());
        homeView.setVersionStat(meta.getLoader() + " " + meta.getMcVersion());
        checkExistingAuth();

        root.sceneProperty().addListener((o, old, scene) -> {
            if (scene != null) {
                if (!hotkeysInstalled) setupHotkeys(scene);
                applyAccentColor();
            }
        });

        playEntranceAnimation(mainView);
        LOGGER.info("Zurdo Launcher view created");
        return root;
    }

    private void setupUI() {
        long totalMb = com.topzurdo.launcher.util.OSUtils.getTotalMemoryMb();
        int maxRam = totalMb > 0 ? (int) Math.min(totalMb, Constants.MAX_RAM_MB) : Constants.MAX_RAM_MB;
        settingsView.getRamSlider().setMin(Constants.MIN_RAM_MB);
        settingsView.getRamSlider().setMax(Math.max(Constants.MIN_RAM_MB, maxRam));
        settingsView.getRamSlider().setBlockIncrement(512);
        settingsView.getRamSlider().valueProperty().addListener((obs, o, v) ->
            settingsView.getRamLabel().setText(v.intValue() + " MB"));
        settingsView.getAutoRamCheck().setOnAction(e -> {
            if (settingsView.getAutoRamCheck().isSelected()) {
                int rec = LauncherConfig.getRecommendedRam();
                settingsView.getRamSlider().setValue(rec);
                settingsView.getRamLabel().setText(rec + " MB");
                settingsView.getRamSlider().setDisable(true);
                long totalMbRam = com.topzurdo.launcher.util.OSUtils.getTotalMemoryMb();
                if (totalMbRam <= 0) totalMbRam = 8192;
                settingsView.setRamBreakdown(rec, totalMbRam);
            } else {
                settingsView.getRamSlider().setDisable(false);
                settingsView.hideRamBreakdown();
            }
        });

        settingsView.getResolutionCombo().getItems().addAll(
            "1280x720", "1366x768", "1600x900", "1920x1080", "2560x1440", "3840x2160");
        settingsView.getResolutionCombo().setValue("1920x1080");

        settingsView.getVersionCombo().getItems().addAll(
            "1.16.5 (Рекомендуется)", "1.18.2", "1.19.4", "1.20.1");
        settingsView.getVersionCombo().setValue("1.16.5 (Рекомендуется)");
        settingsView.getVersionCombo().setPromptText(
            settingsView.getVersionCombo().getItems().isEmpty() ? Constants.Messages.VERSIONS_EMPTY : "");

        // Убрали валидацию ника - используем дефолтный "Player"

        homeView.getProgressSection().setVisible(false);
        homeView.getStatusLabel().setText(Constants.Messages.WAITING);
        settingsView.getServerField().setPromptText(Constants.DEFAULT_SERVER);
        settingsView.getPortField().setPromptText(String.valueOf(Constants.DEFAULT_PORT));
    }

    private void loadUserSettings() {
        if (config.getUsername() != null) homeView.setUsername(config.getUsername());
        homeView.setNicknameHistory(config.getNicknameHistory());
        settingsView.getRamSlider().setValue(config.getAllocatedRamMb());
        settingsView.getRamLabel().setText(config.getAllocatedRamMb() + " MB");
        if (config.getJavaPath() != null) settingsView.getJavaPathField().setText(config.getJavaPath());
        settingsView.getAutoConnectCheck().setSelected(config.isAutoConnect());
        settingsView.getFullscreenCheck().setSelected(config.isFullscreen());
        settingsView.getEffectsCheck().setSelected(true);
        settingsView.getFabricDebugCheck().setSelected(config.isFabricDebugLogging());
        if (config.getLastServer() != null && !config.getLastServer().isEmpty()) {
            String s = config.getLastServer();
            if (s.contains(":")) {
                String[] p = s.split(":", 2);
                settingsView.getServerField().setText(p[0]);
                settingsView.getPortField().setText(p.length > 1 ? p[1] : String.valueOf(Constants.DEFAULT_PORT));
            } else {
                settingsView.getServerField().setText(s);
                settingsView.getPortField().setText(String.valueOf(Constants.DEFAULT_PORT));
            }
        }
        settingsView.getLightThemeCheck().setSelected(config.isLightTheme());
        if (config.getJvmProfile() != null) {
            config.setJvmProfile(config.getJvmProfile()); // sync jvmArgs from profile
            JvmProfile p = JvmProfile.fromId(config.getJvmProfile());
            settingsView.getJvmProfileCombo().setValue(p.getDisplayName());
        }
        settingsView.getAutoRamCheck().setSelected(config.isAutoRam());
        if (config.isAutoRam()) {
            int rec = LauncherConfig.getRecommendedRam();
            settingsView.getRamSlider().setValue(rec);
            settingsView.getRamLabel().setText(rec + " MB");
            settingsView.getRamSlider().setDisable(true);
            long totalMbRam = com.topzurdo.launcher.util.OSUtils.getTotalMemoryMb();
            if (totalMbRam <= 0) totalMbRam = 8192;
            settingsView.setRamBreakdown(rec, totalMbRam);
        } else {
            settingsView.hideRamBreakdown();
        }
        settingsView.getCustomColorCheck().setSelected(config.isCustomColorEnabled());
        settingsView.setPreferredColorRGB(config.getPreferredColor());
    }

    private void setupServiceCallbacks() {
        if (services == null) return;
        services.onAuthReady(service -> {
            authService = service;
            authService.setStatusCallback(s -> Platform.runLater(() -> homeView.getStatusLabel().setText(s)));
            Platform.runLater(this::updateAuthUI);
        });
        services.onDownloadReady(service -> {
            downloadService = service;
            downloadService.setProgressCallback(p -> Platform.runLater(() -> {
                homeView.getDownloadProgress().setProgress(p);
                homeView.getProgressPercentLabel().setText(String.format("%.0f%%", p * 100));
            }));
            downloadService.setStatusCallback(s -> Platform.runLater(() -> homeView.getStatusLabel().setText(s)));
        });
        services.onGameReady(service -> {
            gameService = service;
            gameService.setStatusCallback(s -> Platform.runLater(() -> homeView.getStatusLabel().setText(s)));
            gameService.setOnGameStarted(() -> Platform.runLater(() -> TopZurdoLauncher.getInstance().minimizeWindow()));
            gameService.setOnGameExited(() -> Platform.runLater(this::resetPlayButton));
        });
        services.onServerStatusReady(service -> serverStatusService = service);
    }

    private void wireHome() {
        homeView.getPlayButton().setOnAction(e -> onPlayClicked());
        // Убрали обработчик Microsoft авторизации
    }

    private void wireSettings() {
        settingsView.getSaveButton().setOnAction(e -> onSaveSettings());
        settingsView.getResetButton().setOnAction(e -> onResetSettings());
        settingsView.getBrowseJavaButton().setOnAction(e -> onBrowseJava());
        settingsView.getLightThemeCheck().setOnAction(e -> applyThemeFromSettings());
        settingsView.getJvmProfileCombo().setOnAction(e -> {
            String sel = settingsView.getJvmProfileCombo().getValue();
            for (JvmProfile p : JvmProfile.values()) {
                if (p.getDisplayName().equals(sel)) {
                    config.setJvmProfile(p.name());
                    break;
                }
            }
        });
        settingsView.getInstallOptModsButton().setOnAction(e -> onInstallOptMods());
        settingsView.getApplyBestButton().setOnAction(e -> onApplyBestSettingsWithDialog());
    }

    private void wireTitleBar() {
        mainView.getTitleBar().setOnMinimize(() -> TopZurdoLauncher.getInstance().minimizeWindow());
        mainView.getTitleBar().setOnClose(this::onCloseClicked);
        StatusPill pill = mainView.getTitleBar().getServerPill();
        pill.setCursor(javafx.scene.Cursor.HAND);
        Tooltip.install(pill, new Tooltip("Нажмите для повторной проверки"));
        pill.setOnMouseClicked(e -> checkServerStatus());
        // Перетаскивание окна за шапку (не за кнопки)
        javafx.scene.Node titleBar = mainView.getTitleBar();
        titleBar.setOnMousePressed(e -> {
            if (e.getTarget() instanceof javafx.scene.control.Button) return;
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });
        titleBar.setOnMouseDragged(e -> {
            if (e.getTarget() instanceof javafx.scene.control.Button) return;
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });
        titleBar.setCursor(javafx.scene.Cursor.DEFAULT);
    }

    private void wireNav() {
        mainView.getNavRail().getHome().setOnAction(e -> showPanel("home"));
        mainView.getNavRail().getSettings().setOnAction(e -> showPanel("settings"));
        mainView.getNavRail().getAbout().setOnAction(e -> showPanel("about"));
    }

    private void setupHotkeys(Scene scene) {
        if (scene == null || hotkeysInstalled) return;
        hotkeysInstalled = true;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.isControlDown() && ev.getCode() == KeyCode.S) {
                if ("settings".equals(currentPanel)) { onSaveSettings(); ev.consume(); }
                return;
            }
            if (ev.isControlDown() && ev.getCode() == KeyCode.DIGIT1) { showPanel("home"); ev.consume(); return; }
            if (ev.isControlDown() && ev.getCode() == KeyCode.DIGIT2) { showPanel("settings"); ev.consume(); return; }
            if (ev.isControlDown() && ev.getCode() == KeyCode.DIGIT3) { showPanel("about"); ev.consume(); return; }
            if (ev.getCode() == KeyCode.ESCAPE) { onCloseClicked(); ev.consume(); }
        });
    }

    private void checkServerStatus() {
        if (serverStatusService == null) {
            StatusPill pill = mainView.getTitleBar().getServerPill();
            pill.setText(Constants.Messages.SERVER_LOADING);
            pill.setStatus(StatusPill.Status.LOADING);
            return;
        }
        String server = config.getLastServer();
        if (server == null || server.isEmpty()) server = Constants.DEFAULT_SERVER;
        String host = server;
        int port = Constants.DEFAULT_PORT;
        if (server.contains(":")) {
            String[] p = server.split(":", 2);
            host = p[0];
            try { port = Integer.parseInt(p[1]); } catch (NumberFormatException ignored) {}
        }

        StatusPill pill = mainView.getTitleBar().getServerPill();
        pill.setText(Constants.Messages.SERVER_LOADING);
        pill.setStatus(StatusPill.Status.LOADING);

        serverStatusService.checkStatusAsync(host, port, status -> Platform.runLater(() -> {
            if (status.hasError()) {
                pill.setStatus(StatusPill.Status.ERROR);
                pill.setText(Constants.Messages.SERVER_ERROR_RETRY);
                mainView.getTitleBar().setWipeBannerVisible(false);
            } else {
                pill.setStatus(status.isOnline() ? StatusPill.Status.OK : StatusPill.Status.ERROR);
                String text = status.isOnline() ? Constants.Messages.SERVER_ONLINE : Constants.Messages.SERVER_OFFLINE;
                if (status.isOnline() && status.players >= 0 && status.maxPlayers > 0) {
                    text = text + " (" + status.players + "/" + status.maxPlayers + ")";
                }
                pill.setText(text);
                mainView.getTitleBar().setWipeBannerVisible(status.isRecentWipe());
            }
            Tooltip.install(pill, new Tooltip(status.getHost() != null ? status.getHost() : "—"));
        }));
    }

    private void checkExistingAuth() {
        if (authService != null && authService.hasExistingAuth()) {
            Platform.runLater(this::updateAuthUI);
        }
    }

    private void updateAuthUI() {
        // Убрали авторизацию - всегда используем дефолтный ник
        mainView.getTitleBar().setAuthVisible(false);
        homeView.setMicrosoftLoginVisible(false);
        homeView.setUsername("Player");
        homeView.setNicknameHistory(config.getNicknameHistory());
        homeView.getStatusLabel().setText("Готов к запуску");
    }

    private void playEntranceAnimation(Node n) {
        if (n == null) return;
        n.setOpacity(0);
        n.setScaleX(0.95);
        n.setScaleY(0.95);
        ParallelTransition pt = new ParallelTransition(
            createFade(n, 0, 1, Duration.millis(500)),
            createScale(n, 0.95, 1.0, Duration.millis(600))
        );
        pt.setDelay(Duration.millis(100));
        pt.play();
    }

    private FadeTransition createFade(Node n, double from, double to, Duration d) {
        FadeTransition f = new FadeTransition(d, n);
        f.setFromValue(from);
        f.setToValue(to);
        f.setInterpolator(Interpolator.EASE_BOTH);
        return f;
    }

    private ScaleTransition createScale(Node n, double from, double to, Duration d) {
        ScaleTransition s = new ScaleTransition(d, n);
        s.setFromX(from);
        s.setFromY(from);
        s.setToX(to);
        s.setToY(to);
        s.setInterpolator(Interpolator.EASE_BOTH);
        return s;
    }

    private void showPanel(String p) {
        if (p.equals(currentPanel)) return;
        currentPanel = p;
        if ("home".equals(p)) mainView.setContent(homeView);
        else if ("settings".equals(p)) mainView.setContent(settingsView);
        else mainView.setContent(aboutView);
        mainView.getNavRail().setActive(p);
    }

    private void onPlayClicked() {
        String u = homeView.getNicknameText();
        if (u == null || (u = u.trim()).isEmpty()) u = "Player";
        if (u.length() < 3 || u.length() > 16 || !u.matches("^[a-zA-Z0-9_]+$")) u = "Player";
        config.addUsernameToHistory(u);
        config.setUsername(u);
        config.save();
        homeView.setNicknameHistory(config.getNicknameHistory());
        if (downloadService == null) {
            showElegantError("Ошибка", "Сервис загрузки ещё не инициализирован. Подождите несколько секунд.");
            return;
        }
        if (!downloadService.isFullyInstalled()) startDownload();
        else launchGame();
    }

    private void onMicrosoftLogin() {
        if (authService == null) {
            showElegantError("Ошибка", "Сервис авторизации ещё не инициализирован. Подождите несколько секунд.");
            return;
        }
        homeView.getMicrosoftLoginBtn().setDisable(true);
        homeView.getMicrosoftLoginBtn().setText("Авторизация...");
        authService.authenticateMicrosoft()
            .thenAccept(r -> Platform.runLater(() -> {
                if (r.isSuccess()) {
                    updateAuthUI();
                    showElegantInfo("Добро пожаловать", "Авторизация успешна, " + r.getUsername() + "!");
                } else showElegantError("Ошибка авторизации", r.getErrorMessage());
                homeView.getMicrosoftLoginBtn().setDisable(false);
                homeView.getMicrosoftLoginBtn().setText("◆ Войти через Microsoft");
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    showElegantError("Ошибка", e.getMessage());
                    homeView.getMicrosoftLoginBtn().setDisable(false);
                    homeView.getMicrosoftLoginBtn().setText("◆ Войти через Microsoft");
                });
                return null;
            });
    }

    private void startDownload() {
        if (downloadService.isDownloading()) return;
        homeView.getPlayButton().setDisable(true);
        homeView.getPlayButton().setText("ПОДГОТОВКА...");
        homeView.getProgressSection().setVisible(true);
        homeView.getDownloadProgress().setProgress(0);
        downloadService.startDownload()
            .thenAccept(ok -> Platform.runLater(() -> {
                homeView.getProgressSection().setVisible(false);
                resetPlayButton();
                if (ok) { homeView.getStatusLabel().setText(Constants.Messages.READY); launchGame(); }
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    homeView.getProgressSection().setVisible(false);
                    resetPlayButton();
                    showElegantError("Прерывание подготовки", e.getMessage());
                });
                return null;
            });
    }

    private void resetPlayButton() {
        homeView.getPlayButton().setDisable(false);
        homeView.getPlayButton().setText("ПРИСТУПИТЬ К ИГРЕ");
    }

    private void launchGame() {
        if (gameService == null) {
            showElegantError("Ошибка", "Сервис игры ещё не инициализирован. Подождите несколько секунд.");
            return;
        }
        if (gameService.isGameRunning()) {
            showElegantError(Constants.Messages.ERROR_GAME_RUNNING, Constants.Messages.ERROR_GAME_RUNNING_DETAILS);
            return;
        }
        if (gameService.isLaunchInProgress()) return;
        homeView.getPlayButton().setDisable(true);
        homeView.getPlayButton().setText("ЗАПУСК...");
        if (effectsEngine != null) {
            StackPane root = (StackPane) mainView.getParent();
            if (root != null)
                effectsEngine.triggerBurst("main-particles", root.getWidth() / 2, root.getHeight() / 2, Constants.Particles.BURST_COUNT);
        }
        gameService.launchGame(config).exceptionally(e -> {
            Platform.runLater(() -> { resetPlayButton(); showElegantError("Прерывание запуска", e.getMessage()); });
            return false;
        });
    }

    private void onCloseClicked() {
        if (mainView != null) {
            ParallelTransition exit = new ParallelTransition(
                createFade(mainView, 1, 0, Duration.millis(300)),
                createScale(mainView, 1.0, 0.95, Duration.millis(300))
            );
            exit.setOnFinished(e -> {
                config.save();
                if (effectsEngine != null) effectsEngine.shutdown();
                TopZurdoLauncher.getInstance().closeWindow();
            });
            exit.play();
        } else {
            config.save();
            if (effectsEngine != null) effectsEngine.shutdown();
            TopZurdoLauncher.getInstance().closeWindow();
        }
    }

    private void applyThemeFromSettings() {
        config.setLightTheme(settingsView.getLightThemeCheck().isSelected());
        config.save();
        ThemeManager.setMode(config.isLightTheme() ? ThemeManager.Mode.LIGHT : ThemeManager.Mode.DARK);
        javafx.scene.Scene sc = mainView != null ? mainView.getScene() : null;
        if (sc != null) {
            var url = MainController.class.getResource("/css/style.css");
            ThemeManager.reapplyToScene(sc, url != null ? url.toExternalForm() : null);
        }
    }

    private void onInstallOptMods() {
        if (optimizationModService == null) {
            optimizationModService = new OptimizationModService();
        }
        settingsView.getInstallOptModsButton().setDisable(true);
        settingsView.getInstallOptModsButton().setText("Установка...");
        optimizationModService.installOptimizationMods((modName, current, total) ->
            Platform.runLater(() -> settingsView.getInstallOptModsButton().setText(modName + " " + current + "/" + total)))
            .thenAccept(result -> Platform.runLater(() -> {
                settingsView.getInstallOptModsButton().setDisable(false);
                settingsView.getInstallOptModsButton().setText("◆ Установить моды оптимизации");
                if (result.hasInstalled()) {
                    showElegantInfo("Моды установлены", "Установлено: " + String.join(", ", result.installed) +
                        (result.failed.isEmpty() ? "" : "\nНе удалось: " + String.join(", ", result.failed)));
                } else if (!result.failed.isEmpty()) {
                    showElegantError("Ошибка установки", String.join("\n", result.failed));
                }
            }));
    }

    private void onApplyBestSettingsWithDialog() {
        SmartOptimizationDialog dialog = new SmartOptimizationDialog();
        styleDialogPane(dialog.getDialogPane());
        dialog.show();
        dialog.runScan(() -> {
            onApplyBestSettings();
            dialog.close();
            Platform.runLater(() -> showElegantInfo("Готово", "Настройки подобраны под ваш ПК: RAM " + config.getAllocatedRamMb() + " MB, профиль JVM «" + JvmProfile.fromId(config.getJvmProfile()).getDisplayName() + "». Нажмите «Сохранено» или сохраните другие изменения."));
        });
    }

    private void onApplyBestSettings() {
        config.applyBestDefaults();
        config.save();
        loadUserSettings();
    }

    private void onSaveSettings() {
        int ramMb = config.isAutoRam() ? LauncherConfig.getRecommendedRam() : (int) settingsView.getRamSlider().getValue();
        config.setAllocatedRamMb(ramMb);
        if (!settingsView.getJavaPathField().getText().isEmpty())
            config.setJavaPath(settingsView.getJavaPathField().getText());
        config.setAutoConnect(settingsView.getAutoConnectCheck().isSelected());
        config.setFullscreen(settingsView.getFullscreenCheck().isSelected());
        config.setFabricDebugLogging(settingsView.getFabricDebugCheck().isSelected());
        config.setLightTheme(settingsView.getLightThemeCheck().isSelected());
        config.setAutoRam(settingsView.getAutoRamCheck().isSelected());
        config.setCustomColorEnabled(settingsView.getCustomColorCheck().isSelected());
        config.setPreferredColor(settingsView.getPreferredColorRGB());
        String sel = settingsView.getJvmProfileCombo().getValue();
        for (JvmProfile p : JvmProfile.values()) {
            if (p.getDisplayName().equals(sel)) {
                config.setJvmProfile(p.name());
                break;
            }
        }
        String res = settingsView.getResolutionCombo().getValue();
        if (res != null && res.contains("x")) {
            String[] p = res.split("x");
            config.setWindowWidth(Integer.parseInt(p[0]));
            config.setWindowHeight(Integer.parseInt(p[1]));
        }
        String host = settingsView.getServerField().getText().trim();
        if (!host.isEmpty()) {
            String portStr = settingsView.getPortField().getText().trim();
            int port = Constants.DEFAULT_PORT;
            try {
                if (!portStr.isEmpty()) port = Integer.parseInt(portStr);
            } catch (NumberFormatException ignored) {}
            config.setLastServer(port == Constants.DEFAULT_PORT ? host : host + ":" + port);
        }
        config.save();
        applyAccentColor();
        showElegantInfo("Сохранено", Constants.Messages.SETTINGS_SAVED);
    }

    /** Применяет цвет акцента из конфига к корню сцены (-accent), если включён свой цвет. */
    private void applyAccentColor() {
        javafx.scene.Scene sc = mainView != null ? mainView.getScene() : null;
        if (sc == null) return;
        javafx.scene.Node root = sc.getRoot();
        if (root == null) return;
        if (config.isCustomColorEnabled()) {
            String hex = "#" + config.getPreferredColorHex();
            root.setStyle("-accent: " + hex + "; -accent-muted: " + hex + "4d; -accent-hover: " + hex + ";");
        } else {
            root.setStyle("");
        }
    }

    private void onResetSettings() {
        settingsView.getRamSlider().setValue(Constants.DEFAULT_RAM_MB);
        settingsView.getRamSlider().setDisable(false);
        settingsView.getAutoRamCheck().setSelected(false);
        settingsView.getJvmProfileCombo().setValue(JvmProfile.MEDIUM.getDisplayName());
        settingsView.getJavaPathField().clear();
        settingsView.getAutoConnectCheck().setSelected(true);
        settingsView.getFullscreenCheck().setSelected(false);
        settingsView.getFabricDebugCheck().setSelected(false);
        settingsView.getLightThemeCheck().setSelected(false);
        settingsView.getResolutionCombo().setValue("1920x1080");
        settingsView.getServerField().setText(Constants.DEFAULT_SERVER);
        settingsView.getPortField().setText(String.valueOf(Constants.DEFAULT_PORT));
        settingsView.getCustomColorCheck().setSelected(false);
        settingsView.setPreferredColorRGB(0x22D3EE);
        applyThemeFromSettings(); // apply dark theme after reset
        applyAccentColor();
        showElegantInfo("Сброшено", Constants.Messages.SETTINGS_RESET);
    }

    private void onBrowseJava() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Выберите исполняемый файл Java");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Java", "java.exe", "java", "javaw.exe"));
        java.io.File f = fc.showOpenDialog(TopZurdoLauncher.getInstance().getPrimaryStage());
        if (f != null) settingsView.getJavaPathField().setText(f.getAbsolutePath());
    }

    private void showElegantError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Zurdo — Внимание");
        a.setHeaderText(title);
        a.setContentText(msg);
        styleDialog(a);
        a.showAndWait();
    }

    private void showElegantInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Zurdo");
        a.setHeaderText(title);
        a.setContentText(msg);
        styleDialog(a);
        a.showAndWait();
    }

    private void styleDialog(Alert a) {
        styleDialogPane(a.getDialogPane());
        Node c = a.getDialogPane().lookup(".content.label");
        if (c != null) c.setStyle("-fx-text-fill: " + Constants.HEX_TEXT + "; -fx-font-size: 13px; -fx-font-family: 'Segoe UI', sans-serif;");
    }

    private void styleDialogPane(DialogPane dp) {
        dp.setStyle("-fx-background-color: " + Constants.HEX_BG + "; -fx-border-color: " + Constants.HEX_BORDER_SUBTLE + "; -fx-border-width: 1px; -fx-border-radius: 12px; -fx-background-radius: 12px;");
        Node h = dp.lookup(".header-panel");
        if (h != null) h.setStyle("-fx-background-color: " + Constants.HEX_BG_DARK + ";");
    }

    public void shutdown() {
        LOGGER.info("Shutting down MainController");
        if (optimizationModService != null) optimizationModService.shutdown();
        if (effectsEngine != null) effectsEngine.shutdown();
        if (gameService != null) gameService.shutdown();
        if (downloadService != null) downloadService.shutdown();
        if (serverStatusService != null) serverStatusService.shutdown();
    }
}
