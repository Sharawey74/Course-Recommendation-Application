
import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Model3_2 extends Application {
    private static RecommendationEngine engine = new RecommendationEngine();
    private static User currentUser = null;
    private static CourseManagerService service = new CourseManagerService(engine, new Scanner(System.in));
    private Stage primaryStage;
    private BorderPane rootLayout;
    private VBox sidebar;
    private StackPane mainContent;
    private TextArea outputArea;
    private String currentActiveButton = "";

    @Override
    public void start(Stage primaryStage) {

        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Course Recommendation System");
        this.engine = new RecommendationEngine();
        this.service = new CourseManagerService(engine, new Scanner(System.in));
        service.initializeSampleCourses();   // Keep this — loads course data
        User.setEngine(engine);              // Add this — ensures User class can look up courses

        // Show splash screen
        showSplashScreen();

        // Initialize after splash screen
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(event -> {

            // Initialize main components first
            initializeRootLayout();
            showGuestMenu();
        });
        delay.play();
    }

    private void showSplashScreen() {
        StackPane splashLayout = new StackPane();
        splashLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #000033, #003366);");

        // Create splash content
        VBox splashContent = new VBox(20);
        splashContent.setAlignment(Pos.CENTER);

        // Enhanced glittering effect with darker blue tint
        for (int i = 0; i < 150; i++) {
            Color sparkleColor = Color.rgb(
                    135 + (int)(Math.random() * 120),
                    206 + (int)(Math.random() * 49),
                    250,
                    0.6 + Math.random() * 0.2
            );
            Circle sparkle = new Circle(Math.random() * 2 + 0.5, sparkleColor);
            sparkle.setOpacity(Math.random() * 0.4 + 0.4);

            double x = Math.random() * 1200 - 200;
            double y = Math.random() * 800 - 100;
            sparkle.setTranslateX(x);
            sparkle.setTranslateY(y);

            FadeTransition sparkleAnim = new FadeTransition(Duration.seconds(0.3 + Math.random() * 1.5), sparkle);
            sparkleAnim.setFromValue(0);
            sparkleAnim.setToValue(Math.random() * 0.8 + 0.2);
            sparkleAnim.setCycleCount(FadeTransition.INDEFINITE);
            sparkleAnim.setAutoReverse(true);
            sparkleAnim.setDelay(Duration.seconds(Math.random()));
            sparkleAnim.play();

            splashLayout.getChildren().add(sparkle);
        }
        Label title = new Label("Course Recommendation System");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setTextFill(Color.WHITE);
        title.setEffect(new javafx.scene.effect.DropShadow(20, Color.valueOf("#003366")));

        Label version = new Label("Version 2.0");
        version.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        version.setTextFill(Color.valueOf("#4d94ff"));

        splashContent.getChildren().addAll(title, version);
        splashLayout.getChildren().add(splashContent);

        StackPane glassPanel = new StackPane();
        glassPanel.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(0,51,102,0.1), rgba(0,0,0,0.05));" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,51,102,0.4), 20, 0, 0, 0);" +
                        "-fx-backdrop-filter: blur(10px);"
        );
        splashLayout.getChildren().add(glassPanel);

        Scene splashScene = new Scene(splashLayout, 800, 600);
        primaryStage.setScene(splashScene);
        primaryStage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(2), splashLayout);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void initializeRootLayout() {
        rootLayout = new BorderPane();
        rootLayout.setPadding(new Insets(15));

        // Initialize mainContent
        mainContent = new StackPane();
        mainContent.setPadding(new Insets(15));

        // Initialize output area
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setPrefHeight(100);

        // Create scrollable main content
        ScrollPane mainScroll = new ScrollPane(mainContent);
        mainScroll.setFitToWidth(true);
        mainScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        rootLayout.setCenter(mainScroll);

        Scene scene = new Scene(rootLayout, 1280, 800);
        loadStyles(scene);
        primaryStage.setScene(scene);
    }

    private void loadStyles(Scene scene) {
        if (scene == null) {
            System.err.println("Error: Cannot load styles for null scene");
            return;
        }

        // Clear any existing stylesheets
        scene.getStylesheets().clear();

        // List of possible CSS file locations
        List<String> possiblePaths = Arrays.asList(
                "/com/example/projectfx/styles.css",
                "/styles.css",
                "C:\\Users\\DELL\\IdeaProjects\\untitled20\\styles.css",
                "resources/styles.css",
                "css/styles.css"
        );

        boolean cssLoaded = false;

        for (String path : possiblePaths) {
            try {
                URL resourceUrl = getClass().getResource(path);
                if (resourceUrl != null) {
                    scene.getStylesheets().add(resourceUrl.toExternalForm());
                    System.out.println("Successfully loaded CSS from resources: " + path);
                    cssLoaded = true;
                    break;
                }

                Path filePath = Paths.get(path);
                if (Files.exists(filePath)) {
                    scene.getStylesheets().add(filePath.toUri().toString());
                    System.out.println("Successfully loaded CSS from file: " + filePath.toAbsolutePath());
                    cssLoaded = true;
                    break;
                }
            } catch (Exception e) {
                System.err.println("Error loading CSS from " + path + ": " + e.getMessage());
            }
        }

        if (!cssLoaded) {
            System.err.println("Warning: Could not load CSS from any of these locations:");
            possiblePaths.forEach(System.err::println);

            // Apply inline styles
            String inlineStyles = """
                .root {
                    -fx-font-family: 'Segoe UI', Arial, sans-serif;
                    -fx-background-color: #f5f5f5;
                }
                .sidebar {
                    -fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e);
                    -fx-padding: 20px;
                }
                .sidebar-button {
                    -fx-background-color: transparent;
                    -fx-text-fill: white;
                    -fx-font-size: 14px;
                    -fx-padding: 10px 15px;
                    -fx-alignment: center-left;
                    -fx-border-radius: 5px;
                }
                .sidebar-button:hover {
                    -fx-background-color: rgba(255,255,255,0.1);
                }
                .sidebar-button.active {
                    -fx-background-color: #3498db;
                    -fx-font-weight: bold;
                }
                .dashboard-card {
                    -fx-background-color: white;
                    -fx-background-radius: 10px;
                    -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);
                    -fx-padding: 20px;
                }
                .course-card {
                    -fx-background-color: white;
                    -fx-background-radius: 10px;
                    -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);
                    -fx-padding: 15px;
                }
                .primary-button {
                    -fx-background-color: #3498db;
                    -fx-text-fill: white;
                    -fx-font-weight: bold;
                    -fx-background-radius: 5px;
                }
                .secondary-button {
                    -fx-background-color: #95a5a6;
                    -fx-text-fill: white;
                    -fx-background-radius: 5px;
                }
                .danger-button {
                    -fx-background-color: #e74c3c;
                    -fx-text-fill: white;
                    -fx-background-radius: 5px;
                }
                .success-button {
                    -fx-background-color: #2ecc71;
                    -fx-text-fill: white;
                    -fx-background-radius: 5px;
                }
                .title-label {
                    -fx-font-size: 18px;
                    -fx-font-weight: bold;
                    -fx-text-fill: #2c3e50;
                }
                .subtitle-label {
                    -fx-font-size: 16px;
                    -fx-font-weight: bold;
                    -fx-text-fill: #34495e;
                }
            """;
            scene.getRoot().setStyle(inlineStyles);
        }
    }



    private void showGuestMenu() {
        rootLayout.setLeft(null);

        // Create full-screen container with gradient
        StackPane guestContainer = new StackPane();
        guestContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");
        guestContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        guestContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Center container with blur effect and gradient overlay
        VBox guestLayout = new VBox(30);
        guestLayout.setAlignment(Pos.CENTER);
        guestLayout.setPadding(new Insets(40));
        guestLayout.setMaxWidth(800);
        guestLayout.setMinHeight(600);
        guestLayout.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);" +
                        "-fx-backdrop-filter: blur(10px);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1px;"
        );

        // Title Section with animation
        Label titleLabel = new Label("Course Recommendation System");
        titleLabel.setStyle("-fx-font-size: 42px; " +
                "-fx-font-family: 'Arial Rounded MT Bold'; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: white; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.3), 10, 0, 0, 0);");

        // Subtitle
        Label subtitleLabel = new Label("Discover Your Learning Path");
        subtitleLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #4db6ac; -fx-font-style: italic;");

        // Action Buttons Container
        VBox buttonBox = new VBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setMaxWidth(400);
        buttonBox.setPadding(new Insets(30, 0, 0, 0));

        // Register Button
        Button registerBtn = createGuestButton("Register", "Create your new account",
                "-fx-background-color: linear-gradient(to right, #004d40, #00897b);");
        registerBtn.setOnAction(e -> showRegistrationForm());

        // Login Button
        Button loginBtn = createGuestButton("Login", "Access your existing account",
                "-fx-background-color: linear-gradient(to right, #00695c, #00bfa5);");
        loginBtn.setOnAction(e -> showLoginForm());

        // Exit Button
        Button exitBtn = createGuestButton("Exit", "Close the application",
                "-fx-background-color: linear-gradient(to right, #1a1a1a, #263238);");
        exitBtn.setOnAction(e -> primaryStage.close());

        buttonBox.getChildren().addAll(registerBtn, loginBtn, exitBtn);

        // Footer text
        Label footerLabel = new Label("© 2024 Course Recommendation System");
        footerLabel.setStyle("-fx-text-fill: #80cbc4; -fx-font-size: 14px;");
        VBox.setMargin(footerLabel, new Insets(30, 0, 0, 0));

        // Add all components to the layout
        guestLayout.getChildren().addAll(titleLabel, subtitleLabel, buttonBox, footerLabel);

        // Add fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), guestLayout);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Set up the container
        guestContainer.getChildren().setAll(guestLayout);
        mainContent.getChildren().setAll(guestContainer);
        rootLayout.setCenter(mainContent);

        // Remove any margins from BorderPane
        rootLayout.setPadding(new Insets(0));
    }

    private Button createGuestButton(String text, String tooltip, String style) {
        Button button = new Button(text);
        button.setStyle(style + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 25; " +
                "-fx-padding: 15 50; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 0);");

        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(50);
        button.setTooltip(new Tooltip(tooltip));

        // Add hover effect
        button.setOnMouseEntered(e -> {
            button.setStyle(style + "; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 16px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-background-radius: 25; " +
                    "-fx-padding: 15 50; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.3), 15, 0, 0, 0); " +
                    "-fx-scale-x: 1.05; " +
                    "-fx-scale-y: 1.05;");
        });

        button.setOnMouseExited(e -> {
            button.setStyle(style + "; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 16px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-background-radius: 25; " +
                    "-fx-padding: 15 50; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 0);");
        });

        return button;
    }

    private Button createStyledButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(40);
        return button;
    }

    private void initializeUserSidebar() {
        // Create sidebar with scrolling capability
        sidebar = new VBox(0);  // Reduced spacing between elements
        sidebar.setPrefWidth(280);
        sidebar.setMinWidth(280);
        sidebar.setMaxHeight(Double.MAX_VALUE);
        sidebar.setPadding(new Insets(0));
        sidebar.setStyle(
                "-fx-background-color: linear-gradient(to bottom, rgba(0,77,64,0.9), rgba(0,0,0,0.95));" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 0);" +
                        "-fx-backdrop-filter: blur(10px);"
        );

        // Create scrollable content container
        VBox sidebarContent = new VBox(10);
        sidebarContent.setPadding(new Insets(20, 20, 20, 20));

        // User profile section
        VBox userProfile = new VBox(8);  // Reduced spacing
        userProfile.setPadding(new Insets(15));
        userProfile.setStyle(
                "-fx-background-color: linear-gradient(to bottom, rgba(255,255,255,0.1), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: rgba(255,255,255,0.1);" +
                        "-fx-border-radius: 15;"
        );
        userProfile.setAlignment(Pos.CENTER);

        // User avatar
        Circle avatar = new Circle(30);  // Reduced size
        avatar.setFill(Color.valueOf("#00897b"));
        Label avatarText = new Label(currentUser.getName().substring(0, 1).toUpperCase());
        avatarText.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");
        StackPane avatarPane = new StackPane(avatar, avatarText);

        Label nameLabel = new Label(currentUser.getName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4db6ac;");
        Label roleLabel = new Label(currentUser.getSkillLevel().toString());
        roleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #80cbc4;");

        userProfile.getChildren().addAll(avatarPane, nameLabel, roleLabel);

        // Navigation sections
        VBox mainNav = createNavSection("Main Navigation", Arrays.asList(
                new NavButton("Dashboard", "dashboard", e -> showDashboard()),
                new NavButton("View Courses", "courses", e -> viewAndSelectCourse()),
                new NavButton("Enroll Course", "enroll", e -> enrollInCourse()),
                new NavButton("My Progress", "progress", e -> viewCourseProgress())
        ));

        VBox courseNav = createNavSection("Course Management", Arrays.asList(
                new NavButton("Recommendations", "recommendations", e -> viewRecommendations()),
                new NavButton("Enrolled Courses", "enrolled", e -> viewEnrolledCourses()),
                new NavButton("Rate Courses", "rate", e -> showRateCourses()),
                new NavButton("Top Rated", "top-rated", e -> viewTopRatedCourses()),
                new NavButton("My Interests", "interests", e -> enterInterests())
        ));

        VBox userNav = createNavSection("User preferences", Arrays.asList(
                new NavButton("Profile", "profile", e -> viewProfile()),
                new NavButton("Learning Report", "report-btn", e -> viewLearningReport())
        ));

        // Separators
        Separator sep1 = new Separator(Orientation.HORIZONTAL);
        Separator sep2 = new Separator(Orientation.HORIZONTAL);
        Separator sep3 = new Separator(Orientation.HORIZONTAL);
        sep1.setStyle("-fx-background-color: rgba(255,255,255,0.1);");
        sep2.setStyle("-fx-background-color: rgba(255,255,255,0.1);");
        sep3.setStyle("-fx-background-color: rgba(255,255,255,0.1);");

        // Logout button
        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #b71c1c33, #00000033);" +
                        "-fx-text-fill: #ef5350;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 15;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: #ef535033;" +
                        "-fx-max-width: infinity;"
        );
        logoutBtn.setOnAction(e -> logout());

        // Add all components to sidebar content
        sidebarContent.getChildren().addAll(
                userProfile,
                sep1,
                mainNav,
                sep2,
                courseNav,
                sep3,
                userNav,
                logoutBtn
        );

        // Create scroll pane
        ScrollPane scrollPane = new ScrollPane(sidebarContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-padding: 0;" +
                        "-fx-border-width: 0;" +
                        "-fx-view-order: 1000;"
        );
        scrollPane.getStyleClass().add("sidebar-scroll");

        // Add scroll pane to sidebar
        sidebar.getChildren().add(scrollPane);
        rootLayout.setLeft(sidebar);
    }

    private Button createNavButton(String text, String id, EventHandler<ActionEvent> handler) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(8, 15, 8, 15));  // Reduced padding

        String baseStyle =
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;";

        String activeStyle =
                "-fx-background-color: linear-gradient(to right, #004d4033, #00000033);" +
                        "-fx-text-fill: #4db6ac;" +
                        "-fx-border-color: #4db6ac33;" +
                        "-fx-border-radius: 8;";

        button.setStyle(id.equals(currentActiveButton) ? activeStyle : baseStyle);
        button.setOnAction(handler);

        // Hover effects
        button.setOnMouseEntered(e -> {
            if (!id.equals(currentActiveButton)) {
                button.setStyle(baseStyle + "-fx-background-color: rgba(255,255,255,0.1);");
            }
        });

        button.setOnMouseExited(e -> {
            if (!id.equals(currentActiveButton)) {
                button.setStyle(baseStyle);
            }
        });

        return button;
    }

    private VBox createNavSection(String title, List<Button> buttons) {
        VBox section = new VBox(8);
        section.setPadding(new Insets(20, 15, 0, 15));

        Label sectionTitle = new Label(title);
        sectionTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #80cbc4; -fx-font-weight: bold;");

        section.getChildren().add(sectionTitle);
        buttons.forEach(button -> section.getChildren().add(button));

        return section;
    }

    private Button createNavButton(String text, String id) {
        Button button = new Button(text);
        button.setId(id);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);

        String baseStyle =
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 12 20;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;";

        String activeStyle =
                "-fx-background-color: linear-gradient(to right, #004d4033, #00000033);" +
                        "-fx-text-fill: #4db6ac;" +
                        "-fx-border-color: #4db6ac33;" +
                        "-fx-border-radius: 10;";

        button.setStyle(currentActiveButton.equals(id) ? activeStyle : baseStyle);

        // Hover effects
        button.setOnMouseEntered(e -> {
            if (!currentActiveButton.equals(id)) {
                button.setStyle(baseStyle + "-fx-background-color: rgba(255,255,255,0.1);");
            }
        });

        button.setOnMouseExited(e -> {
            if (!currentActiveButton.equals(id)) {
                button.setStyle(baseStyle);
            }
        });

        return button;
    }

    private Button createSidebarButton(String text, String id) {
        Button button = new Button(text);
        button.getStyleClass().add("sidebar-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setId(id);

        if (currentActiveButton.equals(id)) {
            button.getStyleClass().add("active");
        }

        return button;
    }

    private void showDashboard() {
        currentActiveButton = "dashboard";
        initializeUserSidebar();

        // Root container with gradient background that fills entire space
        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");
        rootContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        rootContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Main container with glass effect
        VBox container = new VBox(30);
        container.setPadding(new Insets(40));
        container.setMaxWidth(1200);
        container.setAlignment(Pos.TOP_CENTER); // Changed to TOP_CENTER
        container.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);" +
                        "-fx-backdrop-filter: blur(10px);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1px;"
        );

        // Title with glow effect
        Label titleLabel = new Label("Dashboard");
        titleLabel.setStyle(
                "-fx-font-size: 42px;" +
                        "-fx-font-family: 'Arial Rounded MT Bold';" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.3), 10, 0, 0, 0);"
        );

        // Metrics Grid
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(20);
        metricsGrid.setVgap(20);
        metricsGrid.setAlignment(Pos.CENTER);

        // Create metric cards
        VBox skillCard = createMetricCard("Skill Level", currentUser.getSkillLevel().toString(), "#00796b");
        VBox interestsCard = createMetricCard("Interests", String.valueOf(currentUser.getInterests().size()), "#00796b");
        VBox enrolledCard = createMetricCard("Enrolled Courses", String.valueOf(currentUser.getEnrolledCourseIds().size()), "#00796b");
        VBox completedCard = createMetricCard("Completed Courses", String.valueOf(currentUser.getCompletedCourses().size()), "#00796b");

        // Add cards to grid
        metricsGrid.add(skillCard, 0, 0);
        metricsGrid.add(interestsCard, 1, 0);
        metricsGrid.add(enrolledCard, 0, 1);
        metricsGrid.add(completedCard, 1, 1);

        // Quick Actions Section
        VBox actionsContainer = new VBox(15);
        actionsContainer.setAlignment(Pos.CENTER);

        Label actionsTitle = new Label("Quick Actions");
        actionsTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4db6ac;");

        // Action Buttons in FlowPane
        FlowPane actionsPane = new FlowPane(Orientation.HORIZONTAL, 15, 15);
        actionsPane.setAlignment(Pos.CENTER);

        Button viewRecBtn = createThemedButton("View Recommendations", "#00796b", "#004d40");
        viewRecBtn.setOnAction(e -> viewRecommendations());

        Button updateIntBtn = createThemedButton("Update Interests", "#00796b", "#004d40");
        updateIntBtn.setOnAction(e -> updateInterests());

        Button enrollBtn = createThemedButton("Enroll in Course", "#00796b", "#004d40");
        enrollBtn.setOnAction(e -> enrollInCourse());

        Button progressBtn = createThemedButton("View Progress", "#00796b", "#004d40");
        progressBtn.setOnAction(e -> viewEnrolledCourses());

        actionsPane.getChildren().addAll(viewRecBtn, updateIntBtn, enrollBtn, progressBtn);
        actionsContainer.getChildren().addAll(actionsTitle, actionsPane);

        // Add all components to container
        container.getChildren().addAll(
                titleLabel,
                metricsGrid,
                actionsContainer
        );

        rootContainer.getChildren().add(container);

        // Create scrollable content
        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true); // Added to ensure full height
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-padding: 0;" +
                        "-fx-border-width: 0;"
        );

        // Set main content and remove padding
        mainContent.getChildren().setAll(scrollPane);
        mainContent.setAlignment(Pos.TOP_CENTER);
        rootLayout.setPadding(new Insets(0));

        // Ensure BorderPane fills the entire window
        rootLayout.setMaxWidth(Double.MAX_VALUE);
        rootLayout.setMaxHeight(Double.MAX_VALUE);
    }


    private void showRegistrationForm() {
        // Root container with full scene gradient
        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");

        // Make container fill entire scene
        rootContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        rootContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Center container with blur effect and gradient overlay
        VBox container = new VBox(20);
        container.setPadding(new Insets(40));
        container.setMaxWidth(800);
        container.setMinHeight(600);
        container.setAlignment(Pos.CENTER);
        container.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);" +
                        "-fx-backdrop-filter: blur(10px);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1px;"
        );

        // Remove any existing padding/margins from mainContent
        mainContent.setPadding(new Insets(0));
        mainContent.setStyle("-fx-background-color: transparent;");

        // Form grid
        GridPane form = new GridPane();
        form.setAlignment(Pos.CENTER);
        form.setHgap(15);
        form.setVgap(20);
        form.setPadding(new Insets(25));

        // Form Title
        Label titleLabel = new Label("Create New Account");
        titleLabel.setStyle("-fx-font-size: 36px; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-family: 'Arial Rounded MT Bold';");
        titleLabel.setPadding(new Insets(0, 0, 30, 0));

        // Form Fields
        TextField userIdField = createStyledTextField("Enter your user ID", "Your unique account identifier");
        TextField nameField = createStyledTextField("Enter your full name", "Your real name");
        TextField emailField = createStyledTextField("Enter your email", "Valid email address required");
        PasswordField passwordField = createStyledPasswordField("Enter your password");
        PasswordField confirmPasswordField = createStyledPasswordField("Confirm your password");

        // Error Label
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ffa0a0; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Form Layout
        form.add(createLightFormLabel("User ID:"), 0, 1);
        form.add(userIdField, 1, 1);
        form.add(createLightFormLabel("Full Name:"), 0, 2);
        form.add(nameField, 1, 2);
        form.add(createLightFormLabel("Email:"), 0, 3);
        form.add(emailField, 1, 3);
        form.add(createLightFormLabel("Password:"), 0, 4);
        form.add(passwordField, 1, 4);
        form.add(createLightFormLabel("Confirm Password:"), 0, 5);
        form.add(confirmPasswordField, 1, 5);
        form.add(errorLabel, 1, 6);

        // Buttons with new gradients
        Button registerBtn = createGradientButton("Register", "#00695c", "#00bfa5");
        registerBtn.setOnAction(e -> validateRegistration(userIdField, nameField, emailField,
                passwordField, confirmPasswordField, errorLabel));

        Button backBtn = createGradientButton("Back", "#00695c", "#00bfa5");
        backBtn.setOnAction(e -> showGuestMenu());

        HBox buttonBox = new HBox(20, registerBtn, backBtn);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 20, 0));

        // Assemble containers
        container.getChildren().addAll(titleLabel, form, buttonBox);
        rootContainer.getChildren().add(container);

        // Set main content
        mainContent.getChildren().setAll(rootContainer);
        mainContent.setAlignment(Pos.CENTER);

        // Remove any margins from BorderPane
        rootLayout.setPadding(new Insets(0));
    }


    private void showLoginForm() {
        // Root container with full scene gradient
        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");

        // Make container fill entire scene
        rootContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        rootContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Center container with blur effect and gradient overlay
        VBox container = new VBox(20);
        container.setPadding(new Insets(40));
        container.setMaxWidth(800);
        container.setMinHeight(600);
        container.setAlignment(Pos.CENTER);
        container.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);" +
                        "-fx-backdrop-filter: blur(10px);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1px;"
        );

        // Remove any existing padding/margins from mainContent
        mainContent.setPadding(new Insets(0));
        mainContent.setStyle("-fx-background-color: transparent;");


        // Form grid
        GridPane form = new GridPane();
        form.setAlignment(Pos.CENTER);
        form.setHgap(15);
        form.setVgap(20);
        form.setPadding(new Insets(25));

        // Form Title with light text
        Label titleLabel = new Label("Welcome Back");
        titleLabel.setStyle("-fx-font-size: 36px; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-family: 'Arial Rounded MT Bold';");
        titleLabel.setPadding(new Insets(0, 0, 30, 0));

        // Form fields with light styling
        TextField userIdField = createStyledTextField("Enter your user ID", "Your unique account identifier");
        PasswordField passwordField = createStyledPasswordField("Enter your password");

        // Light colored error label
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ffa0a0; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Form layout
        form.add(createLightFormLabel("User ID:"), 0, 1);
        form.add(userIdField, 1, 1);
        form.add(createLightFormLabel("Password:"), 0, 2);
        form.add(passwordField, 1, 2);
        form.add(errorLabel, 1, 3);

        // Buttons
        // Buttons
        Button loginBtn = createGradientButton("Login", "#00695c", "#00bfa5");
        loginBtn.setOnAction((ActionEvent e) -> validateLogin(userIdField, passwordField, errorLabel));

        Button backBtn = createGradientButton("Back", "#00695c", "#00bfa5");
        backBtn.setOnAction((ActionEvent e) -> showGuestMenu());


        HBox buttonBox = new HBox(20, loginBtn, backBtn);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 20, 0));

        // Assemble containers
        container.getChildren().addAll(titleLabel, form, buttonBox);
        rootContainer.getChildren().add(container);

        // Set main content - directly use rootContainer without ScrollPane
        mainContent.getChildren().setAll(rootContainer);
        mainContent.setAlignment(Pos.CENTER);
        // Remove any margins from BorderPane
        rootLayout.setPadding(new Insets(0));
    }

    // Helper method for light form labels
    private Label createLightFormLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
        return label;
    }
    private TextField createStyledTextField(String prompt, String tooltipText) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 12;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255,255,255,0.5);"
        );
        field.setPrefWidth(300);

        // Add hover effect
        field.setOnMouseEntered(e -> field.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15);" +
                        "-fx-border-color: rgba(255,255,255,0.3);" +
                        "-fx-border-radius: 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 12;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255,255,255,0.5);"
        ));

        field.setOnMouseExited(e -> field.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 12;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255,255,255,0.5);"
        ));

        field.setTooltip(new Tooltip(tooltipText));
        return field;
    }

    private PasswordField createStyledPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 12;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255,255,255,0.5);"
        );
        field.setPrefWidth(300);

        // Add hover effect
        field.setOnMouseEntered(e -> field.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15);" +
                        "-fx-border-color: rgba(255,255,255,0.3);" +
                        "-fx-border-radius: 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 12;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255,255,255,0.5);"
        ));

        field.setOnMouseExited(e -> field.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 12;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255,255,255,0.5);"
        ));

        return field;
    }

    private Button createGradientButton(String text, String color1, String color2) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: linear-gradient(to right, " + color1 + ", " + color2 + ");" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-background-radius: 20;" +
                "-fx-padding: 10 25;" +
                "-fx-cursor: hand;");

        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: linear-gradient(to right, " +
                color2 + ", " + color1 + ");" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-background-radius: 20;" +
                "-fx-padding: 10 25;"));

        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: linear-gradient(to right, " +
                color1 + ", " + color2 + ");" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-background-radius: 20;" +
                "-fx-padding: 10 25;"));

        return button;
    }

    private void enterInterests() {
        currentActiveButton = "interests";
        initializeUserSidebar();

        StackPane rootContainer = createRootContainer();
        VBox container = createGlassContainer("Select Your Interests");

        // Current interests section
        Label currentLabel = new Label("Current Interests");
        currentLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #4db6ac; -fx-font-weight: bold;");

        Label interestsLabel = new Label(currentUser.getInterests().isEmpty() ?
                "No interests selected yet" :
                String.join(", ", currentUser.getInterests()));
        interestsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #80cbc4; -fx-wrap-text: true;");
        interestsLabel.setMaxWidth(500);

        // Create categories section with glass effect
        VBox categoriesBox = new VBox(15);
        categoriesBox.setStyle(
                "-fx-background-color: rgba(0,77,64,0.2);" +
                        "-fx-padding: 25;" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: #00796b;" +
                        "-fx-border-radius: 15;" +
                        "-fx-border-width: 1px;"
        );

        Label categoriesTitle = new Label("Available Categories");
        categoriesTitle.setStyle("-fx-font-size: 20px; -fx-text-fill: #4db6ac; -fx-font-weight: bold;");

        // Create FlowPane for checkboxes
        FlowPane checkboxPane = new FlowPane(Orientation.HORIZONTAL, 20, 15);
        checkboxPane.setAlignment(Pos.CENTER_LEFT);

        // Create checkboxes for each category
        List<CheckBox> categoryCheckboxes = new ArrayList<>();
        for (CourseCategory category : CourseCategory.values()) {
            CheckBox cb = new CheckBox(category.toString());
            cb.setSelected(currentUser.getInterests().contains(category.toString()));
            cb.setStyle(
                    "-fx-text-fill: #80cbc4;" +
                            "-fx-font-size: 16px;" +
                            "-fx-padding: 8 12;" +
                            "-fx-background-radius: 5;"
            );

            // Add hover effect
            cb.setOnMouseEntered(e -> cb.setStyle(
                    "-fx-text-fill: #4db6ac;" +
                            "-fx-font-size: 16px;" +
                            "-fx-padding: 8 12;" +
                            "-fx-background-radius: 5;"
            ));

            cb.setOnMouseExited(e -> cb.setStyle(
                    "-fx-text-fill: #80cbc4;" +
                            "-fx-font-size: 16px;" +
                            "-fx-padding: 8 12;" +
                            "-fx-background-radius: 5;"
            ));

            categoryCheckboxes.add(cb);
            checkboxPane.getChildren().add(cb);
        }

        categoriesBox.getChildren().addAll(categoriesTitle, checkboxPane);

        // Buttons
        Button saveBtn = createThemedButton("Save Interests", "#00796b", "#004d40");
        saveBtn.setOnAction(e -> {
            List<String> selectedInterests = categoryCheckboxes.stream()
                    .filter(CheckBox::isSelected)
                    .map(CheckBox::getText)
                    .collect(Collectors.toList());

            if (selectedInterests.isEmpty()) {
                showAlert("Please select at least one interest");
                return;
            }

            // Convert List to Set before setting it
            Set<String> interestsSet = new HashSet<>(selectedInterests);
            currentUser.setInterests(interestsSet);
            showAlert("Interests updated successfully!");
            showDashboard();
        });

        Button clearBtn = createThemedButton("Clear All", "#b71c1c", "#880e4f");
        clearBtn.setOnAction(e -> categoryCheckboxes.forEach(cb -> cb.setSelected(false)));

        Button backBtn = createThemedButton("Back", "#004d40", "#00796b");
        backBtn.setOnAction(e -> showDashboard());

        // Button container
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(saveBtn, clearBtn, backBtn);

        // Add all components to container with proper spacing
        container.getChildren().addAll(
                currentLabel,
                interestsLabel,
                new Separator(),
                categoriesBox,
                buttonBox
        );

        // Add spacing between elements
        VBox.setMargin(currentLabel, new Insets(0, 0, 5, 0));
        VBox.setMargin(interestsLabel, new Insets(0, 0, 20, 0));
        VBox.setMargin(categoriesBox, new Insets(20, 0, 30, 0));

        rootContainer.getChildren().add(container);

        // Create scrollable content
        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        mainContent.getChildren().setAll(scrollPane);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private void viewRecommendations() {
        currentActiveButton = "recommendations";
        initializeUserSidebar();

        StackPane rootContainer = createRootContainer();
        VBox container = createGlassContainer("Recommended Courses");
        container.setPadding(new Insets(20));

        if (currentUser.getInterests().isEmpty()) {
            showAlert("Please set interests to get recommendations.");
            return;
        }

        engine.addUserProfile(currentUser);
        List<Course> recommendations = engine.generateRecommendations(currentUser.getUserID());
        if (recommendations.isEmpty()) {
            showAlert("No recommendations available.");
            return;
        }

        TilePane coursesGrid = new TilePane();
        coursesGrid.setHgap(20);
        coursesGrid.setVgap(20);
        coursesGrid.setPrefColumns(2);
        coursesGrid.setStyle("-fx-background-color: transparent;");

        for (Course course : recommendations) {
            VBox courseCard = createCourseCard(course);
            courseCard.setStyle("-fx-background-color: rgba(0,77,64,0.3); -fx-background-radius: 15;");
            coursesGrid.getChildren().add(courseCard);
        }

        Button backBtn = createThemedButton("Back", "#00695c", "#004d40");
        backBtn.setOnAction(e -> showDashboard());

        container.getChildren().addAll(coursesGrid, backBtn);
        rootContainer.getChildren().add(container);

        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        mainContent.getChildren().setAll(scrollPane);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private void updateInterests() {
        currentActiveButton = "interests";
        initializeUserSidebar();

        // Root container with gradient
        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");
        rootContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        rootContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Glass container
        VBox container = new VBox(20);
        container.setPadding(new Insets(40));
        container.setMaxWidth(800);
        container.setAlignment(Pos.CENTER);
        container.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);" +
                        "-fx-backdrop-filter: blur(10px);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1px;"
        );

        // Title with emerald glow
        Label titleLabel = new Label("Update Interests");
        titleLabel.setStyle("-fx-font-size: 42px;" +
                "-fx-font-family: 'Arial Rounded MT Bold';" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.3), 10, 0, 0, 0);"
        );

        // Current interests card
        VBox currentInterestsCard = new VBox(10);
        currentInterestsCard.setPadding(new Insets(20));
        currentInterestsCard.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 15;"
        );

        Label currentLabel = new Label("Current interests: " + String.join(", ", currentUser.getInterests()));
        currentLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #80cbc4;");
        currentInterestsCard.getChildren().add(currentLabel);

        // Action buttons
        Button addBtn = createThemedButton("Add New Interests", "#00796b", "#004d40");
        addBtn.setOnAction(e -> addInterests());

        Button removeBtn = createThemedButton("Remove Interests", "#00695c", "#004d40");
        removeBtn.setOnAction(e -> removeInterests());

        Button clearBtn = createThemedButton("Clear All", "#b71c1c", "#880e4f");
        clearBtn.setOnAction(e -> {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to clear all interests?", ButtonType.YES, ButtonType.NO);
            confirmation.setTitle("Confirm Clear");
            confirmation.setHeaderText(null);
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                currentUser.getInterests().clear();
                showAlert("All interests cleared successfully!");
                updateInterests();
            }
        });

        Button backBtn = createThemedButton("Back", "#004d40", "#00796b");
        backBtn.setOnAction(e -> showDashboard());

        VBox buttonBox = new VBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(addBtn, removeBtn, clearBtn, backBtn);

        container.getChildren().addAll(titleLabel, currentInterestsCard, buttonBox);
        rootContainer.getChildren().add(container);

        mainContent.getChildren().setAll(rootContainer);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private void addInterests() {
        currentActiveButton = "interests";
        initializeUserSidebar();

        // Root container with gradient
        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");
        rootContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        rootContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        VBox container = new VBox(20);
        container.setPadding(new Insets(40));
        container.setMaxWidth(1000);
        container.setAlignment(Pos.TOP_CENTER);
        container.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);"
        );

        Label titleLabel = new Label("Add New Interests");
        titleLabel.setStyle(
                "-fx-font-size: 42px;" +
                        "-fx-font-family: 'Arial Rounded MT Bold';" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.3), 10, 0, 0, 0);"
        );

        Label currentLabel = new Label("Select interests to add:");
        currentLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #80cbc4; -fx-font-weight: bold;");

        // Grid layout for interests
        GridPane interestsGrid = new GridPane();
        interestsGrid.setHgap(15);
        interestsGrid.setVgap(15);
        interestsGrid.setPadding(new Insets(20));
        interestsGrid.setStyle("-fx-background-color: rgba(0,77,64,0.3);");
        interestsGrid.setMaxWidth(Double.MAX_VALUE);

        // Set column constraints
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        interestsGrid.getColumnConstraints().addAll(col1, col2);

        int column = 0;
        int row = 0;
        int maxColumns = 2;

        // Get current user interests for filtering
        Set<String> currentInterests = currentUser.getInterests();

        // Add all available categories except those already selected
        for (CourseCategory category : CourseCategory.values()) {
            if (!currentInterests.contains(category.name())) {
                HBox interestBox = new HBox(10);
                interestBox.setAlignment(Pos.CENTER_LEFT);
                interestBox.setPadding(new Insets(15));
                interestBox.setMaxWidth(Double.MAX_VALUE);
                interestBox.setPrefHeight(50);
                interestBox.setStyle(
                        "-fx-background-color: rgba(0,77,64,0.5);" +
                                "-fx-background-radius: 10;"
                );

                CheckBox checkBox = new CheckBox();
                checkBox.setPrefWidth(30);

                Label interestLabel = new Label(category.name());
                interestLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                HBox.setHgrow(interestLabel, Priority.ALWAYS);

                interestBox.getChildren().addAll(checkBox, interestLabel);

                // Hover effect
                interestBox.setOnMouseEntered(e ->
                        interestBox.setStyle(
                                "-fx-background-color: rgba(0,77,64,0.7);" +
                                        "-fx-background-radius: 10;"
                        )
                );

                interestBox.setOnMouseExited(e ->
                        interestBox.setStyle(
                                "-fx-background-color: rgba(0,77,64,0.5);" +
                                        "-fx-background-radius: 10;"
                        )
                );

                GridPane.setHgrow(interestBox, Priority.ALWAYS);
                GridPane.setFillWidth(interestBox, true);
                interestsGrid.add(interestBox, column, row);

                column++;
                if (column == maxColumns) {
                    column = 0;
                    row++;
                }
            }
        }

        Button addBtn = createThemedButton("Add Selected", "#00695c", "#004d40");
        addBtn.setOnAction(e -> {
            List<String> selectedInterests = interestsGrid.getChildren().stream()
                    .filter(node -> node instanceof HBox)
                    .map(node -> (HBox) node)
                    .filter(hbox -> ((CheckBox) hbox.getChildren().get(0)).isSelected())
                    .map(hbox -> ((Label) hbox.getChildren().get(1)).getText())
                    .collect(Collectors.toList());

            if (selectedInterests.isEmpty()) {
                showAlert("No interests selected to add.");
                return;
            }

            selectedInterests.forEach(interest -> currentUser.addInterest(interest));
            currentUser.saveToFile();

            showAlert("Added interests: " + selectedInterests + "\nUpdated interests: " + currentUser.getInterests());
            updateInterests();
        });

        Button backBtn = createThemedButton("Back", "#004d40", "#00796b");
        backBtn.setOnAction(e -> updateInterests());

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(addBtn, backBtn);

        container.getChildren().addAll(
                titleLabel,
                currentLabel,
                interestsGrid,
                buttonBox
        );

        rootContainer.getChildren().add(container);

        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-padding: 0;" +
                        "-fx-border-width: 0;"
        );

        mainContent.getChildren().setAll(scrollPane);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private void removeInterests() {
        currentActiveButton = "interests";
        initializeUserSidebar();

        Set<String> currentInterests = currentUser.getInterests();

        if (currentInterests.isEmpty()) {
            showAlert("You don't have any interests to remove.");
            return;
        }

        // Root container with gradient
        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");
        rootContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        rootContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        VBox container = new VBox(20);
        container.setPadding(new Insets(40));
        container.setMaxWidth(1000); // Increased max width
        container.setAlignment(Pos.TOP_CENTER);
        container.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);"
        );

        Label titleLabel = new Label("Remove Interests");
        titleLabel.setStyle(
                "-fx-font-size: 42px;" +
                        "-fx-font-family: 'Arial Rounded MT Bold';" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.3), 10, 0, 0, 0);"
        );

        Label currentLabel = new Label("Select interests to remove:");
        currentLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #80cbc4; -fx-font-weight: bold;");

        // Grid layout for interests with full width
        GridPane interestsGrid = new GridPane();
        interestsGrid.setHgap(15);
        interestsGrid.setVgap(15);
        interestsGrid.setPadding(new Insets(20));
        interestsGrid.setStyle("-fx-background-color: rgba(0,77,64,0.3);");
        interestsGrid.setMaxWidth(Double.MAX_VALUE);

        // Set column constraints for equal width columns
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        interestsGrid.getColumnConstraints().addAll(col1, col2);

        int column = 0;
        int row = 0;
        int maxColumns = 2;

        for (String interest : currentInterests) {
            HBox interestBox = new HBox(10);
            interestBox.setAlignment(Pos.CENTER_LEFT);
            interestBox.setPadding(new Insets(15));
            interestBox.setMaxWidth(Double.MAX_VALUE);
            interestBox.setPrefHeight(50); // Fixed height for consistent sizing
            interestBox.setStyle(
                    "-fx-background-color: rgba(0,77,64,0.5);" +
                            "-fx-background-radius: 10;"
            );

            CheckBox checkBox = new CheckBox();
            checkBox.setPrefWidth(30); // Fixed width for checkbox

            Label interestLabel = new Label(interest);
            interestLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            HBox.setHgrow(interestLabel, Priority.ALWAYS);

            interestBox.getChildren().addAll(checkBox, interestLabel);

            // Hover effect
            interestBox.setOnMouseEntered(e ->
                    interestBox.setStyle(
                            "-fx-background-color: rgba(0,77,64,0.7);" +
                                    "-fx-background-radius: 10;"
                    )
            );

            interestBox.setOnMouseExited(e ->
                    interestBox.setStyle(
                            "-fx-background-color: rgba(0,77,64,0.5);" +
                                    "-fx-background-radius: 10;"
                    )
            );

            GridPane.setHgrow(interestBox, Priority.ALWAYS);
            GridPane.setFillWidth(interestBox, true);
            interestsGrid.add(interestBox, column, row);

            column++;
            if (column == maxColumns) {
                column = 0;
                row++;
            }
        }

        Button removeBtn = createThemedButton("Remove Selected", "#b71c1c", "#880e4f");
        removeBtn.setOnAction(e -> {
            List<String> selectedInterests = interestsGrid.getChildren().stream()
                    .filter(node -> node instanceof HBox)
                    .map(node -> (HBox) node)
                    .filter(hbox -> ((CheckBox) hbox.getChildren().get(0)).isSelected())
                    .map(hbox -> ((Label) hbox.getChildren().get(1)).getText())
                    .collect(Collectors.toList());

            if (selectedInterests.isEmpty()) {
                showAlert("No interests selected for removal.");
                return;
            }

            currentInterests.removeAll(selectedInterests);
            currentUser.setInterests(currentInterests);
            currentUser.saveToFile();

            showAlert("Removed interests: " + selectedInterests + "\nUpdated interests: " + currentUser.getInterests());
            updateInterests();
        });

        Button backBtn = createThemedButton("Back", "#004d40", "#00796b");
        backBtn.setOnAction(e -> updateInterests());

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(removeBtn, backBtn);

        container.getChildren().addAll(
                titleLabel,
                currentLabel,
                interestsGrid,
                buttonBox
        );

        rootContainer.getChildren().add(container);

        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-padding: 0;" +
                        "-fx-border-width: 0;"
        );

        mainContent.getChildren().setAll(scrollPane);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private void enrollInCourse() {
        currentActiveButton = "enrolled";
        initializeUserSidebar();

        // Root container with gradient
        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");
        rootContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        rootContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Glass container
        VBox container = new VBox(30);
        container.setPadding(new Insets(40));
        container.setMaxWidth(800);
        container.setAlignment(Pos.CENTER);
        container.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);" +
                        "-fx-backdrop-filter: blur(10px);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1px;"
        );

        // Title with emerald glow
        Label titleLabel = new Label("Course Enrollment");
        titleLabel.setStyle("-fx-font-size: 42px;" +
                "-fx-font-family: 'Arial Rounded MT Bold';" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.3), 10, 0, 0, 0);"
        );

        Label subtitleLabel = new Label("Choose your enrollment method");
        subtitleLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #4db6ac;");

        // Action buttons
        Button viewCoursesBtn = createThemedButton("Browse All Courses", "#00796b", "#004d40");
        viewCoursesBtn.setOnAction(e -> viewAndSelectCourse());

        Button enterIdBtn = createThemedButton("Enter Course ID", "#00695c", "#004d40");
        enterIdBtn.setOnAction(e -> showCourseIdInput());

        Button backBtn = createThemedButton("Back", "#004d40", "#00796b");
        backBtn.setOnAction(e -> showDashboard());

        VBox buttonBox = new VBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(viewCoursesBtn, enterIdBtn, backBtn);

        container.getChildren().addAll(titleLabel, subtitleLabel, buttonBox);
        rootContainer.getChildren().add(container);

        mainContent.getChildren().setAll(rootContainer);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private void viewAndSelectCourse() {
        currentActiveButton = "courses";
        initializeUserSidebar();

        StackPane rootContainer = createRootContainer();
        VBox container = createGlassContainer("Available Courses");
        container.setPadding(new Insets(20));

        TilePane coursesGrid = new TilePane();
        coursesGrid.setHgap(20);
        coursesGrid.setVgap(20);
        coursesGrid.setPrefColumns(2);
        coursesGrid.setStyle("-fx-background-color: transparent;");

        for (Course course : engine.getAllCourses()) {
            VBox courseCard = createCourseCard(course);
            courseCard.setStyle("-fx-background-color: rgba(0,77,64,0.3); -fx-background-radius: 15;");
            coursesGrid.getChildren().add(courseCard);
        }

        Button backBtn = createThemedButton("Back", "#004d40", "#00796b");
        backBtn.setOnAction(e -> enrollInCourse());

        container.getChildren().addAll(coursesGrid, backBtn);
        rootContainer.getChildren().add(container);

        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        mainContent.getChildren().setAll(scrollPane);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private void showCourseIdInput() {
        currentActiveButton = "enroll";
        initializeUserSidebar();

        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");

        VBox container = new VBox(20);
        container.setPadding(new Insets(40));
        container.setMaxWidth(600);
        container.setAlignment(Pos.CENTER);
        container.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1); " +
                        "-fx-background-radius: 20; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);"
        );

        Label titleLabel = new Label("Enter Course ID");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        TextField courseIdField = new TextField();
        courseIdField.setPromptText("Enter course ID");
        courseIdField.setStyle(
                "-fx-background-color: rgba(255,255,255,0.2); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 16px; " +
                        "-fx-padding: 10;"
        );

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");

        Button enrollBtn = createThemedButton("Enroll", "#00796b", "#004d40");
        enrollBtn.setOnAction(e -> {
            String courseId = courseIdField.getText().trim();
            if (courseId.isEmpty()) {
                errorLabel.setText("Course ID cannot be empty.");
                return;
            }

            Course course = engine.getCourseById(courseId); // Ensure this method exists in your engine
            if (course == null) {
                errorLabel.setText("Invalid Course ID. Please try again.");
                return;
            }

            if (currentUser.getEnrolledCourseIds().contains(courseId)) {
                errorLabel.setText("You are already enrolled in this course.");
                return;
            }

            currentUser.enrollInCourse(courseId); // Ensure this method updates the user's enrolled courses
            showAlert("Successfully enrolled in course: " + course.getTitle());
            showDashboard();
        });

        Button backBtn = createThemedButton("Back", "#004d40", "#00796b");
        backBtn.setOnAction(e -> enrollInCourse());

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(enrollBtn, backBtn);

        container.getChildren().addAll(titleLabel, courseIdField, errorLabel, buttonBox);
        rootContainer.getChildren().add(container);

        mainContent.getChildren().setAll(rootContainer);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private void confirmEnrollment(Course course) {
        // Root container with gradient background
        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");
        rootContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        rootContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Main content container
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(40));
        layout.setPrefWidth(Region.USE_COMPUTED_SIZE);
        layout.setMaxWidth(Double.MAX_VALUE);
        layout.setStyle(
                "-fx-background-color: transparent;" +   // Make the VBox background fully transparent
                        "-fx-background-radius: 20;" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-width: 1px;"
        );

        // Title with emerald glow
        Label titleLabel = new Label("Confirm Enrollment");
        titleLabel.setStyle(
                "-fx-font-size: 42px;" +
                        "-fx-font-family: 'Arial Rounded MT Bold';" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.4), 10, 0, 0, 0);"
        );
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        // Check enrollments
        if (currentUser.getEnrolledCourseIds().contains(course.getCourseID())) {
            showAlert("You are already enrolled in this course!");
            return;
        }

        if (currentUser.getCompletedCourses().contains(course.getCourseID())) {
            showAlert("You have already completed this course!");
            return;
        }

        // Course details card with glass effect
        VBox detailsCard = new VBox(10);
        detailsCard.setMaxWidth(Double.MAX_VALUE);
        detailsCard.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 15;" +
                        "-fx-padding: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 0);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 15;" +
                        "-fx-border-width: 1px;"
        );

        Label courseTitle = new Label(course.getTitle());
        courseTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4db6ac;");
        courseTitle.setMaxWidth(Double.MAX_VALUE);
        courseTitle.setAlignment(Pos.CENTER);

        Label categoryLabel = new Label("Category: " + course.getCategory());
        Label difficultyLabel = new Label("Difficulty: " + course.getDifficulty());
        Label providerLabel = new Label("Provider: " + course.getProvider());

        // Style info labels
        String labelStyle = "-fx-font-size: 14px; -fx-text-fill: #80cbc4;";
        categoryLabel.setStyle(labelStyle);
        difficultyLabel.setStyle(labelStyle);
        providerLabel.setStyle(labelStyle);

        // Description area with themed style
        TextArea descriptionArea = new TextArea(course.getDescription());
        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefHeight(200);
        descriptionArea.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(descriptionArea, Priority.ALWAYS);
        descriptionArea.setStyle(
                "-fx-control-inner-background: rgba(0,77,64,0.5);" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 10;" +
                        "-fx-padding: 10;"
        );

        detailsCard.getChildren().addAll(courseTitle, categoryLabel, difficultyLabel, providerLabel, descriptionArea);
        VBox.setVgrow(detailsCard, Priority.ALWAYS);

        // Themed buttons
        Button confirmBtn = createThemedButton("Confirm Enrollment", "#00796b", "#004d40");
        confirmBtn.setOnAction(e -> completeEnrollment(course));

        Button cancelBtn = createThemedButton("Cancel", "#004d40", "#00796b");
        cancelBtn.setOnAction(e -> showDashboard());

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(confirmBtn, cancelBtn);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));

        // Assemble the layout
        layout.getChildren().addAll(titleLabel, detailsCard, buttonBox);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setFillWidth(true);

        rootContainer.getChildren().add(layout);

        // Create scrollable content
        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-width: 0;" +
                        "-fx-padding: 0;"
        );

        // Set main content
        mainContent.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        mainContent.getChildren().setAll(scrollPane);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }


    private void completeEnrollment(Course course) {
        try {
            boolean enrolled = currentUser.enrollCourse(course.getCourseID());
            if (enrolled) {
                course.incrementEnrollment();
                currentUser.saveToFile();
                showAlert("Successfully enrolled in: " + course.getTitle());
                showDashboard();
            } else {
                showAlert("Enrollment failed. Please try again.");
            }
        } catch (Exception ex) {
            showAlert("Error: " + ex.getMessage());
        }
    }

    private void viewEnrolledCourses() {
        currentActiveButton = "enrolled";
        initializeUserSidebar();

        // Root container with gradient
        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");

        // Main container with glass effect
        VBox container = new VBox(20);
        container.setPadding(new Insets(40));
        container.setMaxWidth(1200);
        container.setAlignment(Pos.CENTER);
        container.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);" +
                        "-fx-backdrop-filter: blur(10px);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1px;"
        );

        // Title with glow effect
        Label titleLabel = new Label("My Enrolled Courses");
        titleLabel.setStyle(
                "-fx-font-size: 42px;" +
                        "-fx-font-family: 'Arial Rounded MT Bold';" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.3), 10, 0, 0, 0);"
        );

        List<String> enrolledCourses = currentUser.getEnrolledCourseIds();
        if (enrolledCourses.isEmpty()) {
            Label emptyLabel = new Label("You aren't enrolled in any courses yet.");
            emptyLabel.setStyle("-fx-text-fill: #80cbc4; -fx-font-size: 18px;");
            Button backBtn = createThemedButton("Back to Dashboard", "#004d40", "#00796b");
            backBtn.setOnAction(e -> showDashboard());
            container.getChildren().addAll(titleLabel, emptyLabel, backBtn);
        } else {
            // Courses grid with glass effect
            TilePane coursesGrid = new TilePane();
            coursesGrid.setHgap(20);
            coursesGrid.setVgap(20);
            coursesGrid.setPrefColumns(2);
            coursesGrid.setStyle("-fx-background-color: transparent;");

            for (String courseId : enrolledCourses) {
                Course course = engine.getCourseById(courseId);
                if (course != null) {
                    coursesGrid.getChildren().add(createEnrolledCourseCard(course));
                }
            }

            Button backBtn = createThemedButton("Back to Dashboard", "#004d40", "#00796b");
            backBtn.setOnAction(e -> showDashboard());

            container.getChildren().addAll(titleLabel, coursesGrid, backBtn);
        }

        rootContainer.getChildren().add(container);

        // Add scrolling capability
        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-padding: 0;" +
                        "-fx-border-width: 0;"
        );

        mainContent.getChildren().setAll(scrollPane);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private VBox createEnrolledCourseCard(Course course) {
        // Card container with glass effect
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setPrefWidth(350);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.1), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: rgba(255,255,255,0.1);" +
                        "-fx-border-radius: 15;"
        );

        // Course title
        Label titleLabel = new Label(course.getTitle());
        titleLabel.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #4db6ac;"
        );
        titleLabel.setWrapText(true);

        // Course details
        Label categoryLabel = new Label("Category: " + course.getCategory());
        categoryLabel.setStyle("-fx-text-fill: #80cbc4; -fx-font-size: 14px;");

        Label difficultyLabel = new Label("Difficulty: " + course.getDifficulty());
        difficultyLabel.setStyle("-fx-text-fill: #80cbc4; -fx-font-size: 14px;");

        // Progress section
        int progress = currentUser.getCourseProgress(course.getCourseID());
        Label progressLabel = new Label(String.format("Progress: %d%%", progress));
        progressLabel.setStyle("-fx-text-fill: #80cbc4; -fx-font-size: 14px;");

        ProgressBar progressBar = new ProgressBar(progress / 100.0);
        progressBar.setStyle(
                "-fx-accent: #00796b;" +
                        "-fx-control-inner-background: rgba(255,255,255,0.1);"
        );
        progressBar.setPrefWidth(Double.MAX_VALUE);

        // Action buttons
        Button updateBtn = createThemedButton("Update Progress", "#00796b", "#004d40");
        updateBtn.setOnAction(e -> updateCourseProgress(course));
        updateBtn.setPrefWidth(Double.MAX_VALUE);

        Button completeBtn = createThemedButton("Mark as Completed", "#004d40", "#00796b");
        completeBtn.setOnAction(e -> markCourseAsCompleted(course));
        completeBtn.setPrefWidth(Double.MAX_VALUE);

        card.getChildren().addAll(
                titleLabel,
                categoryLabel,
                difficultyLabel,
                progressLabel,
                progressBar,
                updateBtn,
                completeBtn
        );

        // Add hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.1));" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 15;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.1), 10, 0, 0, 0);"
        ));

        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.1), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: rgba(255,255,255,0.1);" +
                        "-fx-border-radius: 15;"
        ));

        return card;
    }

    private void markCourseAsCompleted(Course course) {
        try {
            String courseId = course.getCourseID();

            if (currentUser.getCompletedCourses().contains(courseId)) {
                showAlert("This course is already marked as completed.");
                return;
            }

            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Confirm Completion");
            confirmDialog.setHeaderText("Have you completed \"" + course.getTitle() + "\"?");
            confirmDialog.setContentText("This will update your skill level.");

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                int previousSkillLevel = currentUser.getSkillLevel().ordinal();
                currentUser.completeCourse(courseId);
                currentUser.saveToFile();

                showAlert("Congratulations on completing: " + course.getTitle() + "!");

                int newSkillLevel = currentUser.getSkillLevel().ordinal();
                if (newSkillLevel > previousSkillLevel) {
                    showAlert("Your skill level increased from " + previousSkillLevel +
                            " to " + newSkillLevel + "!");
                } else {
                    showAlert("Your current skill level: " + currentUser.getSkillLevel());
                }

                viewEnrolledCourses();
            }
        } catch (Exception ex) {
            showAlert("Error: " + ex.getMessage());
        }
    }

    private void updateCourseProgress(Course course) {
        currentActiveButton = "progress";
        initializeUserSidebar();

        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");

        VBox container = new VBox(20);
        container.setPadding(new Insets(40));
        container.setMaxWidth(600);
        container.setAlignment(Pos.CENTER);
        container.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);" +
                        "-fx-backdrop-filter: blur(10px);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1px;"
        );

        Label titleLabel = new Label("Update Course Progress");
        titleLabel.setStyle(
                "-fx-font-size: 42px;" +
                        "-fx-font-family: 'Arial Rounded MT Bold';" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.3), 10, 0, 0, 0);"
        );

        Label courseLabel = new Label(course.getTitle());
        courseLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #4db6ac; -fx-font-weight: bold;");

        int currentProgress = currentUser.getCourseProgress(course.getCourseID());
        ProgressBar progressBar = new ProgressBar(currentProgress / 100.0);
        progressBar.setStyle("-fx-accent: #00796b;");
        progressBar.setPrefWidth(300);

        Label currentProgressLabel = new Label(String.format("Current Progress: %d%%", currentProgress));
        currentProgressLabel.setStyle("-fx-text-fill: #80cbc4; -fx-font-size: 16px;");

        TextField progressField = new TextField();
        progressField.setPromptText("Enter new progress (0-100)");
        progressField.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #80cbc4;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #004d40;" +
                        "-fx-border-radius: 10;" +
                        "-fx-padding: 10;"
        );

        Button updateBtn = createThemedButton("Update Progress", "#00796b", "#004d40");
        updateBtn.setOnAction(e -> {
            try {
                int newProgress = Integer.parseInt(progressField.getText().trim());
                if (newProgress < 0 || newProgress > 100) {
                    showAlert("Progress must be between 0 and 100.");
                    return;
                }
                currentUser.updateCourseProgress(course.getCourseID(), newProgress, String.valueOf(LocalDateTime.now()));
                showAlert("Progress updated successfully!");
                viewEnrolledCourses();
            } catch (NumberFormatException ex) {
                showAlert("Please enter a valid number for progress.");
            }
        });

        Button backBtn = createThemedButton("Back", "#004d40", "#00796b");
        backBtn.setOnAction(e -> viewEnrolledCourses());

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(updateBtn, backBtn);

        container.getChildren().addAll(titleLabel, courseLabel, currentProgressLabel,
                progressBar, progressField, buttonBox);
        rootContainer.getChildren().add(container);

        mainContent.getChildren().setAll(rootContainer);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private void viewCourseProgress() {
        currentActiveButton = "progress";
        initializeUserSidebar();

        StackPane rootContainer = createRootContainer();
        VBox container = createGlassContainer("Course Progress Dashboard");

        List<String> enrolledCourses = currentUser.getEnrolledCourseIds();
        if (enrolledCourses.isEmpty()) {
            showAlert("No enrolled courses.");
            return;
        }

        VBox progressBox = new VBox(15);
        LocalDateTime now = LocalDateTime.now();
        boolean needsSaving = false;

        for (String courseId : enrolledCourses) {
            Course course = engine.getCourseById(courseId);
            if (course == null) continue;

            int progress = currentUser.getCourseProgress(courseId);

            VBox progressCard = new VBox(10);
            progressCard.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.1);" +
                            "-fx-background-radius: 15;" +
                            "-fx-padding: 15;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 0);"
            );

            Label courseLabel = new Label(course.getTitle());
            courseLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #4db6ac;");

            LocalDateTime lastAccess = currentUser.getLastAccessTime(courseId);
            String lastAccessStr = lastAccess != null ?
                    "Last accessed: " + lastAccess.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) :
                    "First access";
            Label accessLabel = new Label(lastAccessStr);
            accessLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #80cbc4;");

            ProgressBar progressBar = new ProgressBar(progress / 100.0);
            progressBar.setPrefWidth(300);
            progressBar.setStyle("-fx-accent: #00796b;");

            Label progressLabel = new Label("Progress: " + progress + "%");
            progressLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");

            progressCard.getChildren().addAll(courseLabel, accessLabel, progressBar, progressLabel);
            progressBox.getChildren().add(progressCard);

            if (lastAccess == null) {
                currentUser.updateLastAccessTime(courseId, now);
                needsSaving = true;
            }
        }

        if (needsSaving) {
            currentUser.saveToFile();
        }

        int completed = currentUser.getCompletedCourses().size();
        int inProgress = enrolledCourses.size();
        VBox statsCard = createMetricCard("Learning Statistics", "Completed: " + completed + "\nIn Progress: " + inProgress, "#00796b");

        Button backBtn = createThemedButton("Back", "#00695c", "#004d40");
        backBtn.setOnAction(e -> showDashboard());

        container.getChildren().addAll(statsCard, progressBox, backBtn);
        rootContainer.getChildren().add(container);

        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);
        mainContent.getChildren().setAll(scrollPane);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private void viewProfile() {
        currentActiveButton = "profile";
        initializeUserSidebar();

        // Root container with gradient
        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");
        rootContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        rootContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Main container with glass effect
        VBox container = new VBox(30);
        container.setPadding(new Insets(40));
        container.setMaxWidth(900);
        container.setMinHeight(600);
        container.setAlignment(Pos.CENTER);
        container.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);");

        // Title with emerald glow
        Label titleLabel = new Label("User Profile");
        titleLabel.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, #00796b, 10, 0, 0, 0);");

        // Profile header card
        VBox headerCard = new VBox(15);
        headerCard.setPadding(new Insets(25));
        headerCard.setStyle("-fx-background-color: rgba(0,77,64,0.3); -fx-background-radius: 15; -fx-border-color: #00796b; -fx-border-radius: 15; -fx-border-width: 2;");

        // User name with emerald accent
        Label nameLabel = new Label(currentUser.getName());
        nameLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #4db6ac;");

        // Metrics grid
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(30);
        metricsGrid.setVgap(20);
        metricsGrid.add(createProfileMetric("User ID", currentUser.getUserID()), 0, 0);
        metricsGrid.add(createProfileMetric("Email", currentUser.getEmail()), 1, 0);
        metricsGrid.add(createProfileMetric("Skill Level", currentUser.getSkillLevel().toString()), 0, 1);
        metricsGrid.add(createProfileMetric("Member Since", "2024"), 1, 1);

        headerCard.getChildren().addAll(nameLabel, new Separator(), metricsGrid);

        // Create sections with modern cards
        VBox interestsCard = createProfileSectionCard("Interests",
                String.join(", ", currentUser.getInterests().isEmpty() ?
                        Collections.singleton("No interests added yet") : currentUser.getInterests()));

        VBox enrolledCard = createProfileSectionCard("Enrolled Courses",
                String.join("\n", currentUser.getEnrolledCourseIds().isEmpty() ?
                        Collections.singleton("No courses enrolled") :
                        currentUser.getEnrolledCourseIds().stream()
                                .map(id -> "• " + engine.getCourse(id).getTitle())
                                .collect(Collectors.toList())));

        VBox completedCard = createProfileSectionCard("Completed Courses",
                String.join("\n", currentUser.getCompletedCourses().isEmpty() ?
                        Collections.singleton("No courses completed") :
                        currentUser.getCompletedCourses().stream()
                                .map(id -> "• " + engine.getCourse(id).getTitle())
                                .collect(Collectors.toList())));

        // Button container
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        Button editBtn = createThemedButton("Edit Profile", "#00796b", "#004d40");
        editBtn.setOnAction(e -> showEditProfile());

        Button backBtn = createThemedButton("Back", "#004d40", "#00796b");
        backBtn.setOnAction(e -> showDashboard());

        buttonBox.getChildren().addAll(editBtn, backBtn);

        // Assemble the layout
        container.getChildren().addAll(titleLabel, headerCard, interestsCard, enrolledCard, completedCard, buttonBox);
        rootContainer.getChildren().add(container);

        // Create scrollable content
        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Set the content
        mainContent.getChildren().setAll(scrollPane);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private VBox createProfileSectionCard(String title, String content) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: rgba(77,182,172,0.5);" + // Teal border with opacity
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 10px;"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #4db6ac;"
        );

        TextArea contentArea = new TextArea(content);
        contentArea.setWrapText(true);
        contentArea.setEditable(false);
        contentArea.setPrefRowCount(3);
        contentArea.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: rgba(255,255,255,0.8);" +
                        "-fx-control-inner-background: transparent;" +
                        "-fx-border-color: rgba(77,182,172,0.3);" +
                        "-fx-border-radius: 5px;" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );

        card.getChildren().addAll(titleLabel, contentArea);
        return card;
    }

    private void showEditProfile() {
        currentActiveButton = "profile";
        initializeUserSidebar();

        // Root container with gradient
        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");
        rootContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        rootContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Main container with glass effect
        VBox container = new VBox(30);
        container.setPadding(new Insets(40));
        container.setMaxWidth(900);
        container.setMinHeight(600);
        container.setAlignment(Pos.CENTER);
        container.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);" +
                        "-fx-backdrop-filter: blur(10px);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1px;"
        );

        // Title with emerald glow
        Label titleLabel = new Label("Edit Profile");
        titleLabel.setStyle("-fx-font-size: 42px; " +
                "-fx-font-family: 'Arial Rounded MT Bold';" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.3), 10, 0, 0, 0);");

        // Form container
        VBox formBox = new VBox(20);
        formBox.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-background-radius: 15;" +
                        "-fx-padding: 25;" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 15;"
        );
        formBox.setMaxWidth(600);

        // Form fields
        TextField emailField = createStyledTextField(currentUser.getEmail(), "Enter your email");
        PasswordField currentPasswordField = createStyledPasswordField("Current Password");
        PasswordField newPasswordField = createStyledPasswordField("New Password");
        PasswordField confirmPasswordField = createStyledPasswordField("Confirm New Password");

        // Error label
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ff5252; -fx-font-size: 14px;");

        // Buttons
        Button saveBtn = createThemedButton("Save Changes", "#00796b", "#004d40");
        saveBtn.setOnAction(e -> {
            if (validateProfileChanges(emailField, currentPasswordField, newPasswordField, confirmPasswordField, errorLabel)) {
                currentUser.setEmail(emailField.getText());
                if (!newPasswordField.getText().isEmpty()) {
                    currentUser.setPassword(newPasswordField.getText());
                }
                showAlert("Profile updated successfully!");
                viewProfile();
            }
        });

        Button cancelBtn = createThemedButton("Cancel", "#004d40", "#00796b");
        cancelBtn.setOnAction(e -> viewProfile());

        HBox buttonBox = new HBox(20, saveBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER);

        // Add all components to form
        formBox.getChildren().addAll(
                createFormLabel("Email:"), emailField,
                createFormLabel("Current Password:"), currentPasswordField,
                createFormLabel("New Password:"), newPasswordField,
                createFormLabel("Confirm New Password:"), confirmPasswordField,
                errorLabel,
                buttonBox
        );

        // Add components to main container
        container.getChildren().addAll(titleLabel, formBox);
        rootContainer.getChildren().add(container);

        // Set the content
        mainContent.getChildren().setAll(rootContainer);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private void showRateCourses() {
        currentActiveButton = "rate";
        initializeUserSidebar();

        StackPane rootContainer = createRootContainer();
        VBox container = createGlassContainer("Rate Your Courses");

        // Get accessible courses
        Set<String> accessibleCourses = new HashSet<>(currentUser.getEnrolledCourseIds());
        accessibleCourses.addAll(currentUser.getCompletedCourses());

        if (accessibleCourses.isEmpty()) {
            Label emptyLabel = new Label("You haven't enrolled in any courses yet. Enroll in a course first.");
            emptyLabel.setStyle("-fx-text-fill: #80cbc4; -fx-font-size: 18px;");
            Button backBtn = createThemedButton("Back", "#004d40", "#00796b");
            backBtn.setOnAction(e -> showDashboard());
            container.getChildren().addAll(emptyLabel, backBtn);
        } else {
            // Create scrollable grid for courses
            VBox coursesBox = new VBox(15);
            coursesBox.setStyle("-fx-background-color: transparent;");

            for (String courseId : accessibleCourses) {
                Course course = engine.getCourseById(courseId);
                if (course != null) {
                    coursesBox.getChildren().add(createRatingCard(course));
                }
            }

            Button backBtn = createThemedButton("Back", "#004d40", "#00796b");
            backBtn.setOnAction(e -> showDashboard());

            container.getChildren().addAll(coursesBox, backBtn);
        }

        rootContainer.getChildren().add(container);

        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        mainContent.getChildren().setAll(scrollPane);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private VBox createRatingCard(Course course) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: rgba(0,77,64,0.3);" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 15;"
        );

        Label titleLabel = new Label(course.getTitle());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4db6ac;");

        int currentRating = course.getUserRating(currentUser.getUserID());
        Label currentRatingLabel = new Label(currentRating > 0 ?
                "Your current rating: " + currentRating + "/5" : "Not rated yet");
        currentRatingLabel.setStyle("-fx-text-fill: #80cbc4;");

        Label overallRatingLabel = new Label("Overall rating: " + course.getRatingString());
        overallRatingLabel.setStyle("-fx-text-fill: #80cbc4;");

        // Rating selection - Enhanced ComboBox
        HBox ratingBox = new HBox(10);
        ratingBox.setAlignment(Pos.CENTER_LEFT);
        ComboBox<Integer> ratingCombo = new ComboBox<>();
        ratingCombo.getItems().addAll(1, 2, 3, 4, 5);
        ratingCombo.setPromptText("Select rating");
        ratingCombo.setStyle(
                "-fx-background-color: rgba(0,77,64,0.5);" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #80cbc4;" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: rgba(255,255,255,0.3);" +
                        "-fx-border-radius: 15;" +
                        "-fx-padding: 8 15;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);"
        );

        // Style the dropdown list cells with hover effects
        ratingCombo.setCellFactory(lv -> new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: white; " +
                            "-fx-background-color: rgba(0,77,64,0.7); " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 8 15;");

                    // Hover effect for list items
                    setOnMouseEntered(e -> setStyle("-fx-text-fill: #004d40; " +
                            "-fx-background-color: #80cbc4; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 8 15;"));
                    setOnMouseExited(e -> setStyle("-fx-text-fill: white; " +
                            "-fx-background-color: rgba(0,77,64,0.7); " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 8 15;"));
                }
            }
        });

        // Style the button cell (selected value)
        ratingCombo.setButtonCell(new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold;");
                }
            }
        });

        // Add hover effect for the combo box itself
        ratingCombo.setOnMouseEntered(e -> ratingCombo.setStyle(
                "-fx-background-color: rgba(0,77,64,0.7);" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #80cbc4;" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: rgba(255,255,255,0.5);" +
                        "-fx-border-radius: 15;" +
                        "-fx-padding: 8 15;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 2);"
        ));

        ratingCombo.setOnMouseExited(e -> ratingCombo.setStyle(
                "-fx-background-color: rgba(0,77,64,0.5);" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #80cbc4;" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: rgba(255,255,255,0.3);" +
                        "-fx-border-radius: 15;" +
                        "-fx-padding: 8 15;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);"
        ));

        // Review textarea
        TextArea reviewArea = new TextArea();
        reviewArea.setPromptText("Write your review (optional)");
        reviewArea.setWrapText(true);
        reviewArea.setPrefRowCount(3);
        reviewArea.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #80cbc4;" +
                        "-fx-control-inner-background: rgba(0,77,64,0.3);"
        );

        Button submitBtn = createThemedButton("Submit Rating", "#00796b", "#004d40");
        submitBtn.setOnAction(e -> {
            if (ratingCombo.getValue() == null) {
                showAlert("Please select a rating.");
                return;
            }

            int rating = ratingCombo.getValue();
            String review = reviewArea.getText().trim();

            boolean success = course.addRating(currentUser.getUserID(), rating);
            if (success) {
                if (!review.isEmpty()) {
                    course.addReview(currentUser.getUserID(), review);
                }

                // Save the rating
                service.saveRatingToUserFile(currentUser, course.getCourseID(),
                        course.getTitle(), rating, review);
                currentUser.addCourseRating(course.getCourseID(), rating);
                service.saveCourseRatings(course);

                showAlert("Rating submitted successfully!");
                showRateCourses(); // Refresh the view
            } else {
                showAlert("Failed to submit rating. Please try again.");
            }
        });

        card.getChildren().addAll(
                titleLabel,
                currentRatingLabel,
                overallRatingLabel,
                new Label("New Rating:"),
                ratingCombo,
                reviewArea,
                submitBtn
        );

        // Hover effect for the entire card
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: rgba(0,77,64,0.5);" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 15;" +
                        "-fx-text-fill: white;" +
                        "-fx-control-inner-background: transparent;"
        ));

        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: rgba(0,77,64,0.3);" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 15;" +
                        "-fx-text-fill: white;" +
                        "-fx-control-inner-background: transparent;"
        ));

        return card;
    };


    private Node createFormLabel(String s) {
        Label label = new Label(s);
        label.setStyle("-fx-text-fill: #80cbc4; -fx-font-size: 16px;");
        return label;
    }

    // Updated helper method for metrics
    private VBox createProfileMetric(String title, String value) {
        VBox box = new VBox(8);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #80cbc4; -fx-font-size: 14px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        box.getChildren().addAll(titleLabel, valueLabel);
        return box;
    }


    private void viewTopRatedCourses() {
        currentActiveButton = "top-rated";
        initializeUserSidebar();

        // Root container with gradient
        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");

        // Glass container
        VBox container = new VBox(20);
        container.setPadding(new Insets(40));
        container.setMaxWidth(1000);
        container.setAlignment(Pos.CENTER);
        container.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1px;"
        );

        Label titleLabel = new Label("Top Rated Courses");
        titleLabel.setStyle("-fx-font-size: 42px;" +
                "-fx-font-family: 'Arial Rounded MT Bold';" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.3), 10, 0, 0, 0);"
        );

        List<Course> allCourses = engine.getAllCourses();

        // Load ratings for all courses
        for (Course course : allCourses) {
            service.loadCourseRatings(course);
        }

        // Filter to courses with at least one rating
        List<Course> ratedCourses = allCourses.stream()
                .filter(c -> c.getRatingCount() > 0)
                .collect(Collectors.toList());

        if (ratedCourses.isEmpty()) {
            Label noCoursesLabel = new Label("No courses have been rated yet.");
            noCoursesLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #80cbc4;");
            container.getChildren().addAll(titleLabel, noCoursesLabel);
        } else {
            // Sort by average rating (highest first)
            ratedCourses.sort(Comparator.comparing(Course::getAverageRating).reversed());

            // Display top courses
            TilePane coursesGrid = new TilePane();
            coursesGrid.setHgap(20);
            coursesGrid.setVgap(20);
            coursesGrid.setPrefColumns(2);
            coursesGrid.setStyle("-fx-background-color: transparent;");

            int limit = Math.min(10, ratedCourses.size());
            for (int i = 0; i < limit; i++) {
                Course course = ratedCourses.get(i);

                VBox courseCard = new VBox(10);
                courseCard.setPadding(new Insets(20));
                courseCard.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.1);" +
                                "-fx-background-radius: 15;" +
                                "-fx-border-color: rgba(255,255,255,0.2);" +
                                "-fx-border-radius: 15;"
                );

                Label rankLabel = new Label((i + 1) + ". " + course.getTitle());
                rankLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #4db6ac;");

                Label ratingLabel = new Label("Rating: " + course.getRatingString());
                ratingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #80cbc4;");

                Label categoryLabel = new Label("Category: " + course.getCategory());
                categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #b2dfdb;");

                Label difficultyLabel = new Label("Difficulty: " + course.getDifficulty());
                difficultyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #b2dfdb;");

                // Display sample reviews if available
                List<String> reviews = course.getTopReviews(2);
                VBox reviewsBox = new VBox(5);
                if (!reviews.isEmpty()) {
                    Label reviewsLabel = new Label("Sample Reviews:");
                    reviewsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4db6ac;");
                    reviewsBox.getChildren().add(reviewsLabel);

                    for (String review : reviews) {
                        Label reviewLabel = new Label("• \"" + review + "\"");
                        reviewLabel.setStyle("-fx-text-fill: white; -fx-wrap-text: true;");
                        reviewsBox.getChildren().add(reviewLabel);
                    }
                }

                courseCard.getChildren().addAll(rankLabel, ratingLabel, categoryLabel, difficultyLabel, reviewsBox);
                coursesGrid.getChildren().add(courseCard);
            }

            container.getChildren().addAll(titleLabel, coursesGrid);
        }

        Button backBtn = createThemedButton("Back", "#004d40", "#00796b");
        backBtn.setOnAction(e -> showDashboard());
        container.getChildren().add(backBtn);

        rootContainer.getChildren().add(container);

        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        mainContent.getChildren().setAll(scrollPane);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private void logout() {
        if (currentUser != null) {
            currentUser.saveToFile();
            showAlert("Logging out. Goodbye, " + currentUser.getName());
            currentUser = null;
        }
        showGuestMenu();
    }

    private void viewLearningReport() {
        // Create root container with gradient background
        StackPane rootContainer = createRootContainer();

        // Create glass container for content
        VBox container = createGlassContainer("Learning Report");

        // Create the report generator
        UserReportGenerator reportGenerator = new UserReportGenerator(currentUser, engine);

        // Create metrics section
        HBox metricsSection = new HBox(20);
        metricsSection.setAlignment(Pos.CENTER);

        Map<String, Integer> stats = reportGenerator.calculateCourseStatistics();
        metricsSection.getChildren().addAll(
                createMetricCard("Completed Courses", String.valueOf(stats.get("completed")), "#2ecc71"),
                createMetricCard("In Progress", String.valueOf(stats.get("inProgress")), "#f1c40f"),
                createMetricCard("Learning Hours", String.format("%.1f", stats.get("totalHours") / 10.0), "#3498db")
        );

        // Create course progress section
        VBox progressSection = new VBox(15);
        progressSection.setAlignment(Pos.CENTER);
        Label progressTitle = new Label("Course Progress");
        progressTitle.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");
        progressSection.getChildren().add(progressTitle);

        // Add enrolled courses progress bars
        for (String courseId : currentUser.getEnrolledCourseIds()) {
            Course course = engine.getCourseById(courseId);
            if (course != null) {
                int progress = currentUser.getCourseProgress(courseId);
                progressSection.getChildren().add(createProgressBar(course, progress));
            }
        }

        // Create recommendations section
        VBox recommendationsSection = new VBox(15);
        recommendationsSection.setAlignment(Pos.CENTER);
        Label recomTitle = new Label("Recommended Courses");
        recomTitle.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");
        recommendationsSection.getChildren().add(recomTitle);

        // Add recommended courses
        FlowPane coursesFlow = new FlowPane(Orientation.HORIZONTAL, 20, 20);
        coursesFlow.setAlignment(Pos.CENTER);

        List<Course> recommendations = engine.generateRecommendations(currentUser.getUserID());
        recommendations.stream()
                .limit(3)
                .map(this::createCourseCard)
                .forEach(card -> coursesFlow.getChildren().add(card));
        recommendationsSection.getChildren().add(coursesFlow);

        // Export buttons
        HBox exportButtons = new HBox(20);
        exportButtons.setAlignment(Pos.CENTER);

        Button exportHtmlBtn = createGradientButton("Export as HTML", "#1a5276", "#5dade2");
        exportHtmlBtn.setOnAction(e -> {
            String path = reportGenerator.exportHtmlReport();
            if (path != null) {
                showAlert("Report exported successfully to: " + path);
                // Add this line to open the file after export
                new ReportExporter().openFile(path);
            }
        });

        Button exportTextBtn = createGradientButton("Export as Text", "#004d40", "#00796b");
        exportTextBtn.setOnAction(e -> {
            String path = reportGenerator.exportTextReport();
            if (path != null) {
                showAlert("Report exported successfully to: " + path);
                // Add this line to open the file after export
                new ReportExporter().openFile(path);
            }
        });

        Button exportAllBtn = createGradientButton("Export All Formats", "#0D47A1", "#42A5F5");
        exportAllBtn.setOnAction(e -> {
            Map<String, String> paths = reportGenerator.exportAllFormats();
            if (!paths.isEmpty()) {
                StringBuilder message = new StringBuilder("Reports exported to:\n");
                paths.forEach((format, path) -> message.append(format).append(": ").append(path).append("\n"));

                // Show alert with all paths
                showAlert(message.toString());

                // Ask which format to preview
                String formatToPreview = showFormatChoiceDialog();
                if (formatToPreview != null && !formatToPreview.equals("NONE")) {
                    String path = paths.get(formatToPreview);
                    if (path != null) {
                        new ReportExporter().openFile(path);
                    }
                }
            } else {
                showAlert("Failed to export reports.");
            }
        });

        Button backButton = createThemedButton("Back", "#c0392b", "#e74c3c");
        backButton.setOnAction(e -> showDashboard());

        exportButtons.getChildren().addAll(exportHtmlBtn, exportTextBtn,exportAllBtn, backButton);

        // Add all sections to container
        container.getChildren().addAll(
                metricsSection,
                new Separator(),
                progressSection,
                new Separator(),
                recommendationsSection,
                new Separator(),
                exportButtons
        );

        rootContainer.getChildren().add(container);

        // Create scroll pane
        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Set main content
        mainContent.getChildren().setAll(scrollPane);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    private String showFormatChoiceDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Preview Report");
        dialog.setHeaderText("Which format would you like to preview?");

        // Set the button types
        ButtonType htmlButtonType = new ButtonType("HTML", ButtonBar.ButtonData.OK_DONE);
        ButtonType textButtonType = new ButtonType("TEXT", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("None", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(htmlButtonType, textButtonType, cancelButtonType);

        // Style the dialog
        dialog.getDialogPane().setStyle("-fx-background-color: #34495e; -fx-text-fill: white;");

        // Set the result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == htmlButtonType) {
                return "HTML";
            } else if (dialogButton == textButtonType) {
                return "TEXT";
            }
            return "NONE";
        });

        Optional<String> result = dialog.showAndWait();
        return result.orElse("NONE");
    }

    private HBox createProgressBar(Course course, int progress) {
        HBox container = new HBox(15);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(10));
        container.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-background-radius: 10;"
        );

        Label titleLabel = new Label(course.getTitle());
        titleLabel.setMinWidth(200);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");

        ProgressBar progressBar = new ProgressBar(progress / 100.0);
        progressBar.setPrefWidth(300);
        progressBar.setStyle(
                "-fx-accent: " + getProgressColor(progress) + ";" +
                        "-fx-control-inner-background: rgba(255,255,255,0.2);"
        );

        Label percentLabel = new Label(progress + "%");
        percentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");

        container.getChildren().addAll(titleLabel, progressBar, percentLabel);
        return container;
    }

    private String getProgressColor(int progress) {
        if (progress >= 100) return "#2ecc71";
        if (progress >= 50) return "#f1c40f";
        return "#e74c3c";
    }


    ///////////////////////////////////////////////////////////

    private void validateRegistration(TextField userIdField, TextField nameField,
                                      TextField emailField, PasswordField passwordField,
                                      PasswordField confirmPasswordField, Label errorLabel) {
        try {
            // Get field values
            String userId = userIdField.getText().trim();
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            // Reset error state
            errorLabel.setText("");
            clearFieldErrors(userIdField, nameField, emailField, passwordField, confirmPasswordField);

            // Validate fields - matching CourseManagerService exactly
            if (userId.isEmpty() || userId.length() < 3 || !userId.matches("^[a-zA-Z0-9]+$")) {
                showFieldError(userIdField, "User ID must be at least 3 alphanumeric characters");
                throw new IllegalArgumentException("User ID must be at least 3 alphanumeric characters.");
            }

            if (name.isEmpty() || name.trim().length() < 2) {
                showFieldError(nameField, "Name must be at least 2 characters long");
                throw new IllegalArgumentException("Name must be at least 2 characters long.");
            }

            if (email.isEmpty() || !email.contains("@")) {
                showFieldError(emailField, "Must contain @ symbol");
                throw new IllegalArgumentException("Please enter a valid email address.");
            }

            if (password.isEmpty()) {
                showFieldError(passwordField, getPasswordRequirements());
                throw new IllegalArgumentException("Password cannot be empty.");
            }

            if (password.length() < 8) {
                showFieldError(passwordField, getPasswordRequirements());
                throw new IllegalArgumentException("Password must be at least 8 characters long.");
            }

            // Check password complexity
            if (!password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{8,}$")) {
                showFieldError(passwordField, getPasswordRequirements());
                throw new IllegalArgumentException("Password doesn't meet complexity requirements");
            }

            if (!password.equals(confirmPassword)) {
                showFieldError(confirmPasswordField, "Passwords do not match");
                throw new IllegalArgumentException("Passwords do not match.");
            }

            // Check for existing user
            if (service.userExists(userId)) {
                showFieldError(userIdField, "User ID already exists");
                throw new IllegalArgumentException("User ID already exists. Please choose a different one.");
            }

            // Create and save new user
            User newUser = new User(userId, name, email, password);
            if (service.registerUser(newUser)) {
                showAlert("✅ Registration successful! Please login.");
                showGuestMenu();
            }

        } catch (IllegalArgumentException ex) {
            errorLabel.setText(ex.getMessage());
        } catch (Exception ex) {
            errorLabel.setText("❌ Registration failed: " + ex.getMessage());
        }
    }

    private String getPasswordRequirements() {
        return "Password must include:\n" +
                "- At least 8 characters\n" +
                "- At least 1 uppercase letter\n" +
                "- At least 1 lowercase letter\n" +
                "- At least 1 digit\n" +
                "- At least 1 special symbol (!@#$%^&*)\n";
    }

    private void validateLogin(TextField userIdField, PasswordField passwordField, Label errorLabel) {
        try {
            // Get field values
            String userId = userIdField.getText().trim();
            String password = passwordField.getText();

            // Reset error state
            errorLabel.setText("");
            clearFieldErrors(userIdField, passwordField);

            // Validate fields - matching CourseManagerService exactly
            if (userId.isEmpty()) {
                showFieldError(userIdField, "User ID cannot be empty");
                throw new IllegalArgumentException("User ID cannot be empty.");
            }

            if (password.isEmpty()) {
                showFieldError(passwordField, "Password cannot be empty");
                throw new IllegalArgumentException("Password cannot be empty.");
            }

            // Attempt to load user
            User user = User.loadFromFile(userId);
            if (user == null) {
                user = engine.getUserById(userId);
                if (user == null) {
                    showFieldError(userIdField, "User not found. Please check your user ID or register.");
                    throw new IllegalArgumentException("User not found. Please check your user ID or register.");
                }
            }

            // Verify password
            if (!user.checkPassword(password)) {
                showFieldError(passwordField, "Incorrect password. Please try again.");
                throw new IllegalArgumentException("Incorrect password. Please try again.");
            }

            // Login successful
            engine.addUserProfile(user);
            currentUser = user;
            showAlert("✅ Welcome back, " + user.getName() + "!");
            showDashboard();

        } catch (IllegalArgumentException ex) {
            errorLabel.setText(ex.getMessage());
        } catch (Exception ex) {
            errorLabel.setText("❌ Login failed: " + ex.getMessage());
        }
    }

    private void showFieldError(Control field, String message) {
        // Remove any existing error styling first
        clearFieldError(field);

        // Apply error styling
        String errorStyle = "-fx-border-color: #e74c3c; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-color: white;";

        field.setStyle(errorStyle);

        // Create or update tooltip
        Tooltip tooltip = new Tooltip(message);
        tooltip.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c;");
        field.setTooltip(tooltip);

        // Show tooltip immediately
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.show(field,
                field.localToScreen(field.getBoundsInLocal()).getMinX(),
                field.localToScreen(field.getBoundsInLocal()).getMaxY() + 5);

        // Store the listener reference for later removal
        InvalidationListener focusListener = obs -> clearFieldError(field);
        field.focusedProperty().addListener(focusListener);
        field.getProperties().put("errorFocusListener", focusListener);
    }

    private void clearFieldError(Control field) {
        // Reset to normal style
        field.setStyle(getDefaultFieldStyle());

        // Hide and remove tooltip
        if (field.getTooltip() != null) {
            field.getTooltip().hide();
            field.setTooltip(null);
        }

        // Remove focus listener if exists
        if (field.getProperties().containsKey("errorFocusListener")) {
            InvalidationListener listener = (InvalidationListener) field.getProperties().get("errorFocusListener");
            field.focusedProperty().removeListener(listener);
            field.getProperties().remove("errorFocusListener");
        }
    }

    private void clearFieldErrors(Control... fields) {
        for (Control field : fields) {
            clearFieldError(field);
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean validateProfileChanges(TextField emailField, PasswordField currentPassword,
                                           PasswordField newPassword, PasswordField confirmPassword,
                                           Label errorLabel) {
        try {
            // Clear previous errors
            clearFieldErrors(emailField, currentPassword, newPassword, confirmPassword);
            errorLabel.setText("");

            // Get field values
            String email = emailField.getText().trim();
            String currPass = currentPassword.getText();
            String newPass = newPassword.getText();
            String confirmPass = confirmPassword.getText();

            // Validate email - matches CourseManagerService exactly
            if (email.isEmpty() || !email.contains("@")) {
                showFieldError(emailField, "Must contain @ symbol");
                throw new IllegalArgumentException("Please enter a valid email address.");
            }

            // Check if any password fields are filled (indicating password change attempt)
            boolean changingPassword = !currPass.isEmpty() || !newPass.isEmpty() || !confirmPass.isEmpty();

            if (changingPassword) {
                // Validate current password
                if (currPass.isEmpty()) {
                    showFieldError(currentPassword, "Current password is required");
                    throw new IllegalArgumentException("Current password is required.");
                }

                if (!currentUser.checkPassword(currPass)) {
                    showFieldError(currentPassword, "Incorrect current password");
                    throw new IllegalArgumentException("Incorrect current password.");
                }

                // Validate new password - matches CourseManagerService requirements
                if (newPass.isEmpty()) {
                    showFieldError(newPassword, getPasswordRequirements());
                    throw new IllegalArgumentException("New password cannot be empty.");
                }

                if (newPass.length() < 8) {
                    showFieldError(newPassword, getPasswordRequirements());
                    throw new IllegalArgumentException("Password must be at least 8 characters long.");
                }

                if (!newPass.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{8,}$")) {
                    showFieldError(newPassword, getPasswordRequirements());
                    throw new IllegalArgumentException("Password doesn't meet complexity requirements");
                }

                // Confirm password match
                if (!newPass.equals(confirmPass)) {
                    showFieldError(confirmPassword, "Passwords do not match");
                    throw new IllegalArgumentException("Passwords do not match.");
                }

                // Update password if all validations pass
                currentUser.setPassword(newPass);
            }

            return true;

        } catch (IllegalArgumentException ex) {
            errorLabel.setText(ex.getMessage());
            return false;
        } catch (Exception ex) {
            errorLabel.setText("Profile update failed: " + ex.getMessage());
            return false;
        }
    }

    private String getDefaultFieldStyle() {
        return "-fx-border-color: #80cbc4; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-color: white; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 0);";
    }

    private void showContentLayout(String title, Node content) {
        // Root container with full scene gradient
        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");
        rootContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        rootContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Center container with blur effect and gradient overlay
        VBox container = new VBox(20);
        container.setPadding(new Insets(40));
        container.setMaxWidth(800);
        container.setMinHeight(600);
        container.setAlignment(Pos.CENTER);
        container.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);" +
                        "-fx-backdrop-filter: blur(10px);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1px;"
        );

        // Title with consistent styling
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 36px; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-family: 'Arial Rounded MT Bold';" +
                "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.3), 10, 0, 0, 0);");
        titleLabel.setPadding(new Insets(0, 0, 30, 0));

        // Add components to container
        container.getChildren().addAll(titleLabel, content);
        rootContainer.getChildren().add(container);

        // Set main content
        mainContent.getChildren().setAll(rootContainer);
        mainContent.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(0));
    }

    Button actionButton = createThemedButton("Action", "#004d40", "#00796b");
    Button backButton = createThemedButton("Back", "#00695c", "#004d40");

    private StackPane createRootContainer() {
        StackPane rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #000000, #004d40);");
        rootContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        rootContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return rootContainer;
    }

    private VBox createGlassContainer(String titleText) {
        VBox container = new VBox(30);
        container.setPadding(new Insets(40));
        container.setMaxWidth(900);
        container.setMinHeight(600);
        container.setAlignment(Pos.CENTER);
        container.setStyle(
                "    -fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.15), rgba(255,255,255,0.05));\n" +
                        "    -fx-background-radius: 20;" +
                        "    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);\n" +
                        "    -fx-border-color: rgba(255,255,255,0.2);\n" +
                        "    -fx-border-radius: 20;\n" +
                        "    -fx-border-width: 1px;\n" +
                        "    -fx-control-inner-background: transparent;\n" +
                        "    -fx-focus-color: transparent;\n" +
                        "    -fx-faint-focus-color: transparent;\n"
        );

        Label titleLabel = new Label(titleText);
        titleLabel.setStyle(
                "-fx-font-size: 42px;" +
                        "-fx-font-family: 'Arial Rounded MT Bold';" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.3), 10, 0, 0, 0);"
        );
        container.getChildren().add(titleLabel);
        return container;
    }

    private Button createThemedButton(String text, String primary, String secondary) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: linear-gradient(to right, " + primary + ", " + secondary + ");" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 12 30;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 0);"
        );

        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: linear-gradient(to right, " + secondary + ", " + primary + ");" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 12 30;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.3), 12, 0, 0, 0);" +
                        "-fx-scale-x: 1.05;" +
                        "-fx-scale-y: 1.05;"
        ));

        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: linear-gradient(to right, " + primary + ", " + secondary + ");" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 12 30;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 0);"
        ));

        return button;
    }

    private VBox createMetricCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 0 0 3 0;" +
                        "-fx-padding: 15;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 0);"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #80cbc4; -fx-font-family: 'Arial';");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private VBox createCourseCard(Course course) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setPrefWidth(350);
        card.setStyle("-fx-background-radius: 15;");

        Label titleLabel = new Label(course.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #4db6ac;");

        Label categoryLabel = new Label("Category: " + course.getCategory());
        categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #80cbc4;");

        Label difficultyLabel = new Label("Difficulty: " + course.getDifficulty());
        difficultyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #80cbc4;");

        Label providerLabel = new Label("Provider: " + course.getProvider());
        providerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #80cbc4;");

        TextArea descriptionArea = new TextArea(course.getDescription());
        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefHeight(80);
        descriptionArea.setStyle(
                "-fx-control-inner-background: rgba(0,77,64,0.5);" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 10;" +
                        "-fx-padding: 10;"
        );

        boolean alreadyEnrolled = currentUser.getEnrolledCourseIds().contains(course.getCourseID());
        boolean alreadyCompleted = currentUser.getCompletedCourses().contains(course.getCourseID());

        Button actionBtn;
        if (alreadyCompleted) {
            actionBtn = createThemedButton("Completed", "#2ecc71", "#27ae60");
            actionBtn.setDisable(true);
        } else if (alreadyEnrolled) {
            actionBtn = createThemedButton("Enrolled", "#95a5a6", "#7f8c8d");
            actionBtn.setDisable(true);
        } else {
            actionBtn = createThemedButton("Enroll", "#00796b", "#004d40");
            actionBtn.setOnAction(e -> confirmEnrollment(course));
        }

        card.getChildren().addAll(titleLabel, categoryLabel, difficultyLabel, providerLabel, descriptionArea, actionBtn);

        // Add hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: rgba(0,77,64,0.5);" +
                        "-fx-background-radius: 15;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.2), 10, 0, 0, 0);"
        ));

        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: rgba(0,77,64,0.3);" +
                        "-fx-background-radius: 15;"
        ));

        return card;
    }

    public static void main(String[] args) {
        launch(args);
    }
}