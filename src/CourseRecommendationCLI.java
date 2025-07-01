import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class CourseRecommendationCLI {
    private static Scanner scanner = new Scanner(System.in);
    public static RecommendationEngine engine = new RecommendationEngine();
    private static User currentUser = null;
    private static CourseManagerService service = new CourseManagerService(engine, scanner);

    public static void main(String[] args) {
        try {
            System.out.println("🎓 Welcome to Course Recommendation System 🎓");
            System.out.println("Current User: Sharawey74");
            System.out.println("Current Date: " + LocalDate.now().toString());
            service.initializeSampleCourses();

            boolean running = true;
            while (running) {
                if (currentUser == null) {
                    printGuestMenu();
                    int choice = getIntInput("Enter your choice: ");

                    switch (choice) {
                        case 1:
                            service.registerUser(); // Register but don't set currentUser
                            break;
                        case 2:
                            currentUser = service.loginUser();
                            if (currentUser != null) {
                                User loadedUser = User.loadFromFile(currentUser.getUserID());
                                if (loadedUser != null) {
                                    currentUser = loadedUser;
                                }
                            }
                            break;
                        case 3:
                            System.out.println("Thank you for using Course Recommendation System. Goodbye");
                            running = false;
                            break;
                        default:
                            System.out.println("Invalid choice. Please try again.");
                    }
                } else {
                    printUserMenu();
                    int choice = getIntInput("Enter your choice: ");

                    switch (choice) {
                        case 1:
                            service.enterInterests(currentUser);
                            break;
                        case 2:
                            service.viewRecommendations(currentUser);
                            break;
                        case 3:
                            service.updateInterests(currentUser);
                            break;
                        case 4:
                            service.enrollInCourse(currentUser);
                            break;
                        case 5:
                            service.completeCourse(currentUser);
                            break;
                        case 6:
                            service.viewProfile(currentUser);
                            break;
                        case 7:
                            service.viewCourseProgress(currentUser);
                            break;
                        case 8:
                            service.updateCourseProgress(currentUser);
                            break;
                        case 9:
                            service.rateCourse(currentUser);
                            break;
                        case 10:
                            service.viewTopRatedCourses();
                            break;
                        case 11:
                            currentUser.saveToFile();
                            System.out.println("Logging out. Goodbye, " + currentUser.getName());
                            currentUser = null;
                            break;
                        case 12:
                            generateAndExportReport();
                            break;
                        default:
                            System.out.println("Invalid choice. Please try again.");
                    }
                }
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        } catch (Exception e) {
            System.out.println("⚠️ Critical error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    /**
     * Handles the report generation, export, and preview functionality
     */
    private static void generateAndExportReport() {
        UserReportGenerator reportGenerator = new UserReportGenerator(currentUser, engine);

        System.out.println("\n📊 REPORT GENERATION OPTIONS 📊");
        System.out.println("1. Generate Text Report");
        System.out.println("2. Generate HTML Report");
        System.out.println("3. Generate Both Formats");
        System.out.println("4. Back to Main Menu");

        int choice = getIntInput("Enter your choice: ");
        String filePath = null;

        switch (choice) {

            case 1:
                filePath = reportGenerator.exportHtmlReport();
                if (filePath != null) {
                    System.out.println("HTML report generated successfully: " + filePath);
                    if (confirmAction("Would you like to preview this report in your browser? (y/n): ")) {
                        new ReportExporter().openFile(filePath);
                    }
                } else {
                    System.out.println("Failed to generate HTML report");
                }
                break;

            case 2:
                Map<String, String> allReports = reportGenerator.exportAllFormats();
                if (!allReports.isEmpty()) {
                    System.out.println("Reports generated successfully in multiple formats:");
                    for (Map.Entry<String, String> entry : allReports.entrySet()) {
                        System.out.println("- " + entry.getKey() + ": " + entry.getValue());
                    }

                    System.out.println("\nWhich format would you like to preview?");
                    System.out.println("1. Text");
                    System.out.println("2. HTML");
                    System.out.println("3. None (Don't preview)");

                    int previewChoice = getIntInput("Enter your choice: ");
                    if (previewChoice >= 1 && previewChoice <= 2) {
                        String format = previewChoice == 1 ? "TEXT" : "HTML";

                        if (allReports.containsKey(format)) {
                            new ReportExporter().openFile(allReports.get(format));
                        } else {
                            System.out.println("Report in " + format + " format not available");
                        }
                    }
                } else {
                    System.out.println("Failed to generate reports");
                }
                break;

            case 4:
                // Return to main menu
                return;

            default:
                System.out.println("Invalid choice");
        }
    }

    private static boolean confirmAction(String prompt) {
        System.out.print(prompt);
        String response = scanner.nextLine().trim().toLowerCase();
        return response.equals("y") || response.equals("yes");
    }

    private static void printGuestMenu() {
        System.out.println("\n📋 MAIN MENU 📋");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Exit");
    }

    private static void printUserMenu() {
        System.out.println("\n📋 USER MENU 📋");
        System.out.println("1. Enter Your Interests");
        System.out.println("2. View Recommended Courses");
        System.out.println("3. Update Interests");
        System.out.println("4. Enroll in a Course");
        System.out.println("5. Mark Course as Completed");
        System.out.println("6. View Profile");
        System.out.println("7. View Course Progress");
        System.out.println("8. Update Course Progress");
        System.out.println("9. Rate a Course");
        System.out.println("10. View Top Rated Courses");
        System.out.println("11. Logout");
        System.out.println("12. Generate User Learning Report");
    }

    public static int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private static String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
}