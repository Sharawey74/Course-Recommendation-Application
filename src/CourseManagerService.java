import java.time.format.DateTimeFormatter;
import java.util.*;
import java.text.*;
import java.time.*;
import java.io.*;
import java.util.regex.Pattern;
import java.nio.file.*;
import java.util.stream.Collectors;

public class CourseManagerService {
    private RecommendationEngine engine;
    private Scanner scanner;
    private static final String DATA_DIRECTORY = "data";
    private static final String COURSES_FILE = "courses.csv";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final int MIN_PASSWORD_LENGTH = 8;

    public CourseManagerService(RecommendationEngine engine, Scanner scanner) {
        this.engine = engine;
        this.scanner = scanner;
        createDataDirectoryIfNotExists();
    }

    private void createDataDirectoryIfNotExists() {
        try {
            Path dataDir = Paths.get(DATA_DIRECTORY);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                System.out.println("Created data directory for storing user information.");
            }
        } catch (IOException e) {
            System.out.println("Warning: Could not create data directory: " + e.getMessage());
        }
    }


    public void initializeSampleCourses() {
        String filePath = COURSES_FILE;
        int coursesLoaded = 0;
        int coursesSkipped = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String header = reader.readLine(); // Skip header line
            if (header == null) {
                throw new IOException("Courses file is empty or corrupted");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length < 6) {
                    System.out.println("Skipping invalid course entry (insufficient fields): " + line);
                    coursesSkipped++;
                    continue;
                }
                try {
                    // Parse CSV data
                    String courseID = data[0].trim();
                    if (courseID.isEmpty()) {
                        throw new IllegalArgumentException("Course ID cannot be empty");
                    }
                    String title = data[1].trim();
                    if (title.isEmpty()) {
                        throw new IllegalArgumentException("Course title cannot be empty");
                    }
                    CourseCategory category;
                    try {
                        category = CourseCategory.valueOf(data[2].trim());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid course category: " + data[2].trim());
                    }
                    CourseDifficulty difficulty;
                    try {
                        difficulty = CourseDifficulty.valueOf(data[3].trim());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid course difficulty: " + data[3].trim());
                    }
                    String provider = data[4].trim();
                    if (provider.isEmpty()) {
                        throw new IllegalArgumentException("Course provider cannot be empty");
                    }
                    String description = data[5].trim();
                    if (description.isEmpty()) {
                        description = "No description available";
                    }
                    // Add course to engine
                    Course course = new Course(courseID, title, category, difficulty, provider, description);
                    engine.addCourse(course);
                    coursesLoaded++;
                } catch (IllegalArgumentException e) {
                    System.out.println("Skipping invalid course: " + e.getMessage() + " in line: " + line);
                    coursesSkipped++;
                }
            }
            System.out.println("Courses loaded: " + coursesLoaded + ", Skipped: " + coursesSkipped);
            if (coursesLoaded == 0) {
                System.out.println("⚠️ Warning: No courses were loaded. Recommendations will not work properly.");
            }

        } catch (IOException e) {
            System.out.println("❌ Error loading courses from file: " + e.getMessage());
            System.out.println("Make sure the file exists at: " + new File(filePath).getAbsolutePath());
        }
    }

    public void registerUser() {
        try {
            System.out.println("\n📝 USER REGISTRATION 📝");
            // Get and validate user ID
            String userId = getStringInput("Enter user ID (alphanumeric, min 3 characters): ");
            if (userId == null || userId.length() < 3 || !userId.matches("^[a-zA-Z0-9]+$")) {
                throw new IllegalArgumentException("User ID must be at least 3 alphanumeric characters.");
            }
            // Check if user ID already exists
            if (userExists(userId)) {
                throw new IllegalArgumentException("User ID already exists. Please choose a different one.");
            }
            // Get and validate name
            String name = getStringInput("Enter your full name: ");
            if (name == null || name.trim().length() < 2) {
                throw new IllegalArgumentException("Name must be at least 2 characters long.");
            }
            // Get and validate email
            String email = getStringInput("Enter your email ( Must contain @ Symbol ): ");
            if (email == null || !isValidEmail(email)) {
                throw new IllegalArgumentException("Please enter a valid email address.");
            }
            // Get and validate password
            String password = getStringInput(
                    "Enter your password (minimum " + MIN_PASSWORD_LENGTH + " characters):\n" +
                            "Password must include:\n" +
                            "- At least 8 characters\n" +
                            "- At least 1 uppercase letter\n" +
                            "- At least 1 lowercase letter\n" +
                            "- At least 1 digit\n" +
                            "- At least 1 special symbol (!@#$%^&*)\n" +
                            "-> "
            );

            if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
                throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
            }
            // Confirm password
            String confirmPassword = getStringInput("Confirm your password: ");
            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }
            // Create user and save
            User newUser = new User(userId, name, email, password);
            newUser.saveToFile();
            engine.addUserProfile(newUser);
            System.out.println("✅ Registration successful! Please login.");
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Registration Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ An unexpected error occurred during registration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    protected boolean userExists(String userId) {
        // Check if user exists in engine
        if (engine.getUserById(userId) != null) {
            return true;
        }

        // Check actual file where user is saved
        Path userTxtFile = Paths.get("user_" + userId + ".txt");
        return Files.exists(userTxtFile);
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public User loginUser() {
        try {
            System.out.println("\n🔐 USER LOGIN 🔐");
            String userId = getStringInput("Enter your user ID: ");
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("User ID cannot be empty.");
            }
            String password = getStringInput("Enter your password: ");
            if (password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("Password cannot be empty.");
            }
            // Try to load user from file first
            User user = User.loadFromFile(userId);
            if (user == null) {
                // Try to get user from engine if not found in file
                user = engine.getUserById(userId);

                if (user == null) {
                    throw new IllegalArgumentException("User not found. Please check your user ID or register.");
                }
            }
            // Verify password
            if (!user.checkPassword(password)) {
                System.out.println("❌ Invalid password. Please try again.");
                return null;
            }
            // Make sure the user is in the engine for recommendations
            engine.addUserProfile(user);
            System.out.println("✅ Welcome back, " + user.getName() + "!");
            return user;

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Login Error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("❌ An unexpected error occurred during login: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    public void enterInterests(User currentUser) {
        try {
            if (currentUser == null) {
                throw new IllegalArgumentException("You must be logged in to update interests.");
            }
            System.out.println("\n🎯 ENTER YOUR INTERESTS 🎯");
            System.out.println("Your current interests: " +
                    (currentUser.getInterests().isEmpty() ? "None" : currentUser.getInterests()));
            displayAvailableCategories();
            String input = getStringInput("Enter comma-separated interests (or 'CLEAR' to remove all): ").toUpperCase();
            if ("CLEAR".equals(input.trim())) {
                currentUser.getInterests().clear();
                currentUser.saveToFile();
                System.out.println("✅ All interests cleared.");
                System.out.println("Updated interests: []");
                return;
            }
            if (input.trim().isEmpty()) {
                throw new IllegalArgumentException("Interests input cannot be empty.");
            }
            List<String> validInterests = new ArrayList<>();
            Set<String> invalidInterests = new HashSet<>();

            String[] selectedInterests = input.split(",");
            for (String interest : selectedInterests) {
                String trimmedInterest = interest.trim();
                try {
                    CourseCategory category = CourseCategory.valueOf(trimmedInterest);
                    validInterests.add(category.name());
                } catch (IllegalArgumentException e) {
                    invalidInterests.add(trimmedInterest);
                }
            }
            if (!invalidInterests.isEmpty()) {
                System.out.println("⚠️ Invalid categories ignored: " + invalidInterests);
            }
            if (validInterests.isEmpty()) {
                throw new IllegalArgumentException("No valid interests provided. No changes were made.");
            }
            // Update interests and save
            currentUser.getInterests().clear();
            for (String interest : validInterests) {
                currentUser.addInterest(interest);
            }
            currentUser.saveToFile();
            System.out.println("✅ Interests saved: " + currentUser.getInterests());

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayAvailableCategories() {
        CourseCategory[] categories = CourseCategory.values();
        System.out.println("Available categories:");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categories.length; i++) {
            sb.append(categories[i]);
            if (i < categories.length - 1) {
                sb.append(", ");
            }
            // Break line for readability
            if ((i + 1) % 4 == 0) {
                sb.append("\n");
            }
        }
        System.out.println(sb.toString());
    }

    public void viewRecommendations(User currentUser) {
        try {
            if (currentUser == null) {
                throw new IllegalArgumentException("You must be logged in to view recommendations.");
            }
            System.out.println("\n🌟 RECOMMENDED COURSES 🌟");

            // Check if user has interests
            if (currentUser.getInterests().isEmpty()) {
                System.out.println("⚠️ You haven't set any interests yet. Please update your interests to get personalized recommendations.");
                return;
            }
            // Refresh user profile in recommendation engine before generating recommendations
            engine.addUserProfile(currentUser);
            List<Course> recommendations = engine.generateRecommendations(currentUser.getUserID());

            if (recommendations.isEmpty()) {
                System.out.println("⚠️ No recommendations available. Try updating your interests or check back later!");
                return;
            }
            // Rest of the method remains the same
            System.out.println("Based on your interests: " + currentUser.getInterests());
            System.out.println("Here are your personalized course recommendations:");
            System.out.println("----------------------------------------------------");
            // Display recommendations in an organized format
            for (int i = 0; i < recommendations.size(); i++) {
                Course course = recommendations.get(i);

                System.out.println((i + 1) + ". " + course.getTitle());
                System.out.println("   ID: " + course.getCourseID());
                System.out.println("   Category: " + course.getCategory());
                System.out.println("   Difficulty: " + course.getDifficulty());

                // Check if user is already enrolled
                boolean alreadyEnrolled = currentUser.getEnrolledCourseIds().contains(course.getCourseID());
                boolean alreadyCompleted = currentUser.getCompletedCourses().contains(course.getCourseID());
                if (alreadyCompleted) {
                    System.out.println("   Status: ✅ You've completed this course");
                } else if (alreadyEnrolled) {
                    System.out.println("   Status: 📚 You're currently enrolled");
                }
                System.out.println("   Description: " + course.getDescription());
                System.out.println("   Provider: " + course.getProvider());
                System.out.println("----------------------------------------------------");
            }
            System.out.println("💡 Tip: Use the course ID to enroll in a course.");
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateInterests(User currentUser) {
        try {
            if (currentUser == null) {
                throw new IllegalArgumentException("You must be logged in to update interests.");
            }

            System.out.println("\n🔄 UPDATE INTERESTS 🔄");

            Set<String> currentInterests = currentUser.getInterests();
            System.out.println("Your current interests: " + (currentInterests.isEmpty() ? "None" : currentInterests));

            System.out.println("\nAvailable options:");
            System.out.println("1. Add new interests");
            System.out.println("2. Remove specific interests");
            System.out.println("3. Clear all interests");
            System.out.println("4. Cancel");

            int choice = CourseRecommendationCLI.getIntInput("Select an option (1-4): ");

            switch (choice) {
                case 1:
                    addInterests(currentUser);
                    currentUser.saveToFile();
                    break;
                case 2:
                    removeInterests(currentUser);
                    currentUser.saveToFile();
                    break;
                case 3:
                    // Properly clear all interests
                    // **Ensure interests are cleared and saved**
                    currentUser.clearInterests();
                    currentUser.saveToFile();
                    // **Force Reload the User**
                    currentUser = User.loadFromFile(currentUser.getUserID());
                    System.out.println("✅ All interests cleared.");
                    System.out.println("Updated interests: " + currentUser.getInterests());
                    break;
                case 4:
                    System.out.println("Operation canceled.");
                    break;
                default:
                    System.out.println("Invalid option selected.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addInterests(User currentUser) {
        displayAvailableCategories();

        String input = getStringInput("Enter comma-separated interests to add: ").toUpperCase();
        if (input.trim().isEmpty()) {
            System.out.println("No interests provided. No changes made.");
            return;
        }

        String[] interestsToAdd = input.split(",");
        int addedCount = 0;
        Set<String> invalidInterests = new HashSet<>();

        for (String interest : interestsToAdd) {
            String trimmedInterest = interest.trim();
            try {
                CourseCategory category = CourseCategory.valueOf(trimmedInterest);
                currentUser.addInterest(category.name());
                addedCount++;
            } catch (IllegalArgumentException e) {
                invalidInterests.add(trimmedInterest);
            }
        }

        if (!invalidInterests.isEmpty()) {
            System.out.println("⚠️ Invalid categories ignored: " + invalidInterests);
        }

        if (addedCount > 0) {
            currentUser.saveToFile();
            System.out.println("✅ Added " + addedCount + " new interests.");
            System.out.println("Updated interests: " + currentUser.getInterests());
        } else {
            System.out.println("No valid interests provided. No changes made.");
        }
    }

    private void removeInterests(User currentUser) {
        Set<String> currentInterests = currentUser.getInterests();

        if (currentInterests.isEmpty()) {
            System.out.println("You don't have any interests to remove.");
            return;
        }

        System.out.println("Current interests:");
        int index = 1;
        Map<Integer, String> interestMap = new HashMap<>();

        for (String interest : currentInterests) {
            System.out.println(index + ". " + interest);
            interestMap.put(index, interest);
            index++;
        }

        String input = getStringInput("Enter the numbers of interests to remove (comma-separated): ");
        if (input.trim().isEmpty()) {
            System.out.println("No selections made. No changes applied.");
            return;
        }

        String[] selections = input.split(",");
        List<String> interestsToRemove = new ArrayList<>();

        for (String selection : selections) {
            try {
                int selected = Integer.parseInt(selection.trim());
                String interest = interestMap.get(selected);

                if (interest != null) {
                    interestsToRemove.add(interest);
                }
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Invalid selection: " + selection + " - ignored");
            }
        }

        if (interestsToRemove.isEmpty()) {
            System.out.println("No valid selections made. No changes applied.");
            return;
        }

        // **Modify the original set directly**
        currentInterests.removeAll(interestsToRemove);

        // **Explicitly set the modified interests to ensure persistence**
        currentUser.setInterests(currentInterests);

        // **Save changes to file**
        currentUser.saveToFile();

        // **Force Reloading the User to Reflect Changes**
        currentUser = User.loadFromFile(currentUser.getUserID());

        System.out.println("✅ Removed interests: " + interestsToRemove);
        System.out.println("Updated interests: " + currentUser.getInterests());
    }


    public void enrollInCourse(User currentUser) {
        try {
            if (currentUser == null) {
                throw new IllegalArgumentException("You must be logged in to enroll in a course.");
            }

            System.out.println("\n📚 ENROLL IN COURSE 📚");

            // Option to view available courses first
            System.out.println("Would you like to:");
            System.out.println("1. View all available courses before enrolling");
            System.out.println("2. Enter a specific course ID to enroll");

            int choice = CourseRecommendationCLI.getIntInput("Select an option (1-2): ");

            String courseId;

            if (choice == 1) {
                courseId = viewAndSelectCourse(currentUser);
                if (courseId == null) {
                    return; // User canceled
                }
            } else if (choice == 2) {
                courseId = getStringInput("Enter the course ID you want to enroll in: ");
            } else {
                System.out.println("Invalid option selected. Returning to main menu.");
                return;
            }

            if (courseId == null || courseId.trim().isEmpty()) {
                throw new IllegalArgumentException("Course ID cannot be empty.");
            }

            // Get the course
            Course course = engine.getCourseById(courseId);
            if (course == null) {
                throw new IllegalArgumentException("Course not found with ID: " + courseId +
                        ". Please check the course ID and try again.");
            }

            // Check if already enrolled
            if (currentUser.getEnrolledCourseIds().contains(courseId)) {
                System.out.println("ℹ️ You're already enrolled in: " + course.getTitle());
                return;
            }

            // Check if already completed
            if (currentUser.getCompletedCourses().contains(courseId)) {
                System.out.println("ℹ️ You've already completed this course: " + course.getTitle());
                String answer = getStringInput("Would you like to enroll again? (yes/no): ");
                if (!answer.toLowerCase().startsWith("y")) {
                    return;
                }
            }

            // Display course details and confirm enrollment
            System.out.println("\nCourse Details:");
            System.out.println("Title: " + course.getTitle());
            System.out.println("Category: " + course.getCategory());
            System.out.println("Difficulty: " + course.getDifficulty());
            System.out.println("Provider: " + course.getProvider());

            String confirm = getStringInput("Confirm enrollment in this course? (yes/no): ");
            if (!confirm.toLowerCase().startsWith("y")) {
                System.out.println("Enrollment canceled.");
                return;
            }

            // Enroll user in the course
            boolean enrolled = currentUser.enrollCourse(courseId);
            if (enrolled) {
                course.incrementEnrollment();
                currentUser.saveToFile();
                System.out.println("✅ Successfully enrolled in: " + course.getTitle());
                System.out.println("You can view this course in your profile.");
            } else {
                System.out.println("❌ Enrollment failed. Please try again.");
            }

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String viewAndSelectCourse(User currentUser) {
        try {
            System.out.println("\n📋 AVAILABLE COURSES 📋");

            List<Course> allCourses = engine.getAllCourses();
            if (allCourses.isEmpty()) {
                System.out.println("No courses available in the system.");
                return null;
            }

            // Sort courses by category
            Map<CourseCategory, List<Course>> coursesByCategory = new HashMap<>();
            for (Course course : allCourses) {
                CourseCategory category = course.getCategory();
                if (!coursesByCategory.containsKey(category)) {
                    coursesByCategory.put(category, new ArrayList<>());
                }
                coursesByCategory.get(category).add(course);
            }

            // Display courses by category
            Map<Integer, Course> courseMap = new HashMap<>();
            int index = 1;

            for (Map.Entry<CourseCategory, List<Course>> entry : coursesByCategory.entrySet()) {
                System.out.println("\n=== " + entry.getKey() + " ===");

                for (Course course : entry.getValue()) {
                    // Check enrollment status
                    boolean enrolled = currentUser.getEnrolledCourseIds().contains(course.getCourseID());
                    boolean completed = currentUser.getCompletedCourses().contains(course.getCourseID());

                    String status = completed ? " [✅ COMPLETED]" :
                            enrolled ? " [📚 ENROLLED]" : "";

                    System.out.println(index + ". " + course.getTitle() + status +
                            " (ID: " + course.getCourseID() + ")" +
                            " - " + course.getDifficulty());
                    courseMap.put(index, course);
                    index++;
                }
            }

            System.out.println("\nEnter the number of the course you want to enroll in, or 0 to cancel:");
            int selection = CourseRecommendationCLI.getIntInput("Your selection: ");

            if (selection == 0) {
                System.out.println("Selection canceled.");
                return null;
            }

            if (selection < 1 || selection >= index) {
                System.out.println("Invalid selection.");
                return null;
            }

            Course selectedCourse = courseMap.get(selection);
            return selectedCourse.getCourseID();

        } catch (Exception e) {
            System.out.println("❌ Error displaying courses: " + e.getMessage());
            return null;
        }
    }

    public void completeCourse(User currentUser) {
        try {
            if (currentUser == null) {
                throw new IllegalArgumentException("You must be logged in to complete a course.");
            }

            System.out.println("\n✅ MARK COURSE AS COMPLETED ✅");

            List<String> enrolledCourses = currentUser.getEnrolledCourseIds();
            if (enrolledCourses.isEmpty()) {
                System.out.println("You aren't enrolled in any courses. Enroll in a course first.");
                return;
            }

            System.out.println("Your enrolled courses:");
            Map<Integer, String> courseMap = new HashMap<>();
            int index = 1;

            for (String courseId : enrolledCourses) {
                Course course = engine.getCourseById(courseId);
                if (course != null) {
                    System.out.println(index + ". " + course.getTitle() +
                            " (ID: " + courseId + ")" +
                            " - " + course.getDifficulty() +
                            " - " + course.getCategory());
                    courseMap.put(index, courseId);
                    index++;
                }
            }

            if (index == 1) {
                System.out.println("No valid enrolled courses found.");
                return;
            }

            int courseIndex = CourseRecommendationCLI.getIntInput("Select a course to mark as completed (1-" + (index - 1) +
                    "), or 0 to cancel: ");

            if (courseIndex == 0) {
                System.out.println("Operation canceled.");
                return;
            }

            if (courseIndex < 1 || courseIndex >= index) {
                throw new IllegalArgumentException("Invalid selection.");
            }

            String courseId = courseMap.get(courseIndex);
            Course course = engine.getCourseById(courseId);

            // Check if already completed
            if (currentUser.getCompletedCourses().contains(courseId)) {
                System.out.println("This course is already marked as completed.");
                return;
            }

            // Double check completion
            String confirm = getStringInput("Have you completed \"" + course.getTitle() +
                    "\"? (yes/no): ");
            if (!confirm.toLowerCase().startsWith("y")) {
                System.out.println("Operation canceled.");
                return;
            }

            // Complete course, update skill level, and save
            int previousSkillLevel = currentUser.getSkillLevel().ordinal();
            currentUser.completeCourse(courseId);
            currentUser.saveToFile();
            System.out.println("🎉 Congratulations on completing: " + course.getTitle() + "!");

            int newSkillLevel = currentUser.getSkillLevel().ordinal();
            if (newSkillLevel > previousSkillLevel) {
                System.out.println("🔼 Your skill level increased from " + previousSkillLevel +
                        " to " + newSkillLevel + "!");
            } else {
                System.out.println("Your current skill level: " + currentUser.getSkillLevel());
            }

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Fix for viewProfile to handle exceptions better
    public void viewProfile(User currentUser) {
        try {
            if (currentUser == null) {
                throw new IllegalArgumentException("You must be logged in to view your profile.");
            }

            System.out.println("\n👤 USER PROFILE 👤");
            System.out.println("User ID: " + currentUser.getUserID());
            System.out.println("Name: " + currentUser.getName());
            System.out.println("Email: " + currentUser.getEmail());
            System.out.println("Skill Level: " + currentUser.getSkillLevel());

            // Display interests
            Set<String> interests = currentUser.getInterests();
            System.out.println("\nInterests: " + (interests.isEmpty() ? "None" : ""));
            if (!interests.isEmpty()) {
                int i = 1;
                for (String interest : interests) {
                    System.out.println("  " + i + ". " + interest);
                    i++;
                }
            }

            // Display enrolled courses with better error handling
            List<String> enrolledCourses = currentUser.getEnrolledCourseIds();
            System.out.println("\nEnrolled Courses: " + (enrolledCourses.isEmpty() ? "None" : ""));
            if (!enrolledCourses.isEmpty()) {
                int i = 1;
                for (String courseId : enrolledCourses) {
                    try {
                        Course course = engine.getCourseById(courseId);
                        if (course != null) {
                            System.out.println("  " + i + ". " + course.getTitle() +
                                    " (ID: " + courseId + ") - " +
                                    course.getDifficulty() + " - " +
                                    course.getCategory());
                            i++;
                        } else {
                            System.out.println("  " + i + ". Unknown course (ID: " + courseId + ")");
                            i++;
                        }
                    } catch (Exception e) {
                        System.out.println("  " + i + ". Error loading course (ID: " + courseId + "): " + e.getMessage());
                        i++;
                    }
                }
            }

            // Display completed courses with better error handling
            Set<String> completedCourses;
            try {
                completedCourses = (Set<String>) currentUser.getCompletedCourses();
            } catch (ClassCastException e) {
                completedCourses = new HashSet<>(currentUser.getCompletedCourses());
            }
            System.out.println("\nCompleted Courses: " + (completedCourses.isEmpty() ? "None" : ""));
            if (!completedCourses.isEmpty()) {
                int i = 1;
                for (String courseId : completedCourses) {
                    try {
                        Course course = engine.getCourseById(courseId);
                        if (course != null) {
                            System.out.println("  " + i + ". " + course.getTitle() +
                                    " (ID: " + courseId + ") - " +
                                    course.getDifficulty() + " - " +
                                    course.getCategory());
                            i++;
                        } else {
                            System.out.println("  " + i + ". Unknown course (ID: " + courseId + ")");
                            i++;
                        }
                    } catch (Exception e) {
                        System.out.println("  " + i + ". Error loading course (ID: " + courseId + "): " + e.getMessage());
                        i++;
                    }
                }
            }

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Update a user's progress in a course//

    public void updateCourseProgress(User currentUser) {
        try {
            if (currentUser == null) {
                throw new IllegalArgumentException("You must be logged in to update course progress.");
            }

            System.out.println("\n📊 UPDATE COURSE PROGRESS 📊");

            List<String> enrolledCourses = currentUser.getEnrolledCourseIds();
            if (enrolledCourses.isEmpty()) {
                System.out.println("You aren't enrolled in any courses. Enroll in a course first.");
                return;
            }

            System.out.println("Your enrolled courses:");
            Map<Integer, String> courseMap = new HashMap<>();
            int index = 1;

            for (String courseId : enrolledCourses) {
                Course course = engine.getCourseById(courseId);
                if (course != null) {
                    int progress = currentUser.getCourseProgress(courseId);
                    System.out.println(index + ". " + course.getTitle() +
                            " (ID: " + courseId + ")" +
                            " - Current progress: " + progress + "%");
                    courseMap.put(index, courseId);
                    index++;
                }
            }

            if (index == 1) {
                System.out.println("No valid enrolled courses found.");
                return;
            }

            int courseIndex = CourseRecommendationCLI.getIntInput("Select a course to update progress (1-" + (index - 1) +
                    "), or 0 to cancel: ");

            if (courseIndex == 0) {
                System.out.println("Operation canceled.");
                return;
            }

            if (courseIndex < 1 || courseIndex >= index) {
                throw new IllegalArgumentException("Invalid selection.");
            }

            String courseId = courseMap.get(courseIndex);
            Course course = engine.getCourseById(courseId);
            int currentProgress = currentUser.getCourseProgress(courseId);

            System.out.println("Current progress for " + course.getTitle() + ": " + currentProgress + "%");

            int newProgress = CourseRecommendationCLI.getIntInput("Enter new progress percentage (0-100): ");
            if (newProgress < 0 || newProgress > 100) {
                throw new IllegalArgumentException("Progress must be between 0 and 100.");
            }

            // For simplicity, we'll use the current timestamp as the module ID
            // In a real system, you'd have actual module identifiers
            String moduleId = "module_" + System.currentTimeMillis();

            // Update progress in user object
            boolean updated = currentUser.updateCourseProgress(courseId, newProgress, moduleId);

            if (updated) {
                // Set last access time to now
                currentUser.updateLastAccessTime(courseId, LocalDateTime.now());

                // Save the updated user data to file
                currentUser.saveToFile();

                System.out.println("✅ Progress updated successfully to " + newProgress + "%");

                // If progress is 100%, ask if user wants to mark course as completed
                if (newProgress == 100 && !currentUser.getCompletedCourses().contains(courseId)) {
                    String markCompleted = getStringInput("Would you like to mark this course as completed? (yes/no): ");
                    if (markCompleted.toLowerCase().startsWith("y")) {
                        currentUser.completeCourse(courseId);
                        currentUser.saveToFile();
                        System.out.println("🎉 Course marked as completed!");
                    }
                }
            } else {
                System.out.println("❌ Failed to update progress.");
            }

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Display a user's progress across all courses with visual indicators
     */
    public void viewCourseProgress(User currentUser) {
        try {
            if (currentUser == null) {
                throw new IllegalArgumentException("You must be logged in to view course progress.");
            }

            System.out.println("\n📊 COURSE PROGRESS DASHBOARD 📊");

            List<String> enrolledCourses = currentUser.getEnrolledCourseIds();
            if (enrolledCourses.isEmpty()) {
                System.out.println("You aren't enrolled in any courses yet.");
                return;
            }

            System.out.println("Your learning progress:");
            System.out.println("----------------------------------------------------");

            // Update last access time for all courses being viewed
            LocalDateTime now = LocalDateTime.now();
            boolean needsSaving = false;

            for (String courseId : enrolledCourses) {
                Course course = engine.getCourseById(courseId);
                if (course == null) continue;

                int progress = currentUser.getCourseProgress(courseId);

                // Update last access time if viewing course details
                LocalDateTime lastAccess = currentUser.getLastAccessTime(courseId);
                String lastAccessStr = "";

                if (lastAccess != null) {
                    lastAccessStr = ", Last accessed: " +
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(lastAccess);
                } else {
                    // If this is the first time viewing, set access time
                    currentUser.updateLastAccessTime(courseId, now);
                    needsSaving = true;
                }

                System.out.println(course.getTitle() + " (" + course.getDifficulty() +
                        ")" + lastAccessStr);

                // Create visual progress bar
                StringBuilder progressBar = new StringBuilder("[");
                int barWidth = 20;
                int filledWidth = (int)((progress / 100.0) * barWidth);

                for (int i = 0; i < barWidth; i++) {
                    if (i < filledWidth) {
                        progressBar.append("█");
                    } else {
                        progressBar.append(" ");
                    }
                }
                progressBar.append("] ").append(progress).append("%");

                System.out.println(progressBar.toString());
                System.out.println("----------------------------------------------------");
            }

            // Save user data if any access times were updated
            if (needsSaving) {
                currentUser.saveToFile();
            }

            // Show completion statistics
            int completed = currentUser.getCompletedCourses().size();
            Set<String> inProgressCourses = new HashSet<>(enrolledCourses);
            int inProgress = inProgressCourses.size();
            System.out.println("\nSummary: " + completed + " course(s) completed, " +
                    inProgress + " in progress");

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Allow a user to rate a course they've enrolled in or completed
     */
    public void rateCourse(User currentUser) {
        try {
            if (currentUser == null) {
                throw new IllegalArgumentException("You must be logged in to rate a course.");
            }

            System.out.println("\n⭐ RATE A COURSE ⭐");

            // Get courses user has access to (both enrolled and completed)
            Set<String> accessibleCourses = new HashSet<>(currentUser.getEnrolledCourseIds());
            accessibleCourses.addAll(currentUser.getCompletedCourses());

            if (accessibleCourses.isEmpty()) {
                System.out.println("You haven't enrolled in any courses yet. Enroll in a course first.");
                return;
            }

            // Display courses with current ratings
            System.out.println("Your courses:");
            Map<Integer, String> courseMap = new HashMap<>();
            int index = 1;

            for (String courseId : accessibleCourses) {
                Course course = engine.getCourseById(courseId);
                if (course != null) {
                    int userRating = course.getUserRating(currentUser.getUserID());
                    String ratingStr = userRating > 0 ?
                            "Your rating: " + userRating + "/5" : "Not rated by you";

                    System.out.println(index + ". " + course.getTitle() +
                            " (ID: " + courseId + ")" +
                            " - " + ratingStr +
                            " - Overall: " + course.getRatingString());
                    courseMap.put(index, courseId);
                    index++;
                }
            }

            if (index == 1) {
                System.out.println("No valid courses found to rate.");
                return;
            }

            int courseIndex = CourseRecommendationCLI.getIntInput("Select a course to rate (1-" + (index - 1) +
                    "), or 0 to cancel: ");

            if (courseIndex == 0) {
                System.out.println("Operation canceled.");
                return;
            }

            if (courseIndex < 1 || courseIndex >= index) {
                throw new IllegalArgumentException("Invalid selection.");
            }

            String courseId = courseMap.get(courseIndex);
            Course course = engine.getCourseById(courseId);

            int currentRating = course.getUserRating(currentUser.getUserID());
            if (currentRating > 0) {
                System.out.println("Your current rating for this course is: " + currentRating + "/5");
            }

            int rating = CourseRecommendationCLI.getIntInput("Enter your rating (1-5 stars): ");
            if (rating < 1 || rating > 5) {
                throw new IllegalArgumentException("Rating must be between 1 and 5.");
            }

            // Add the rating to the course
            boolean success = course.addRating(currentUser.getUserID(), rating);

            if (success) {
                // Optional: Add a review comment
                String review = null;
                String addReview = getStringInput("Would you like to add a written review? (yes/no): ");
                if (addReview.toLowerCase().startsWith("y")) {
                    review = getStringInput("Enter your review: ");
                    if (review != null && !review.trim().isEmpty()) {
                        // Save the review
                        boolean reviewSaved = course.addReview(currentUser.getUserID(), review);
                        if (reviewSaved) {
                            System.out.println("✅ Review saved successfully!");
                        } else {
                            System.out.println("❌ Failed to save review.");
                            review = null; // Reset if failed
                        }
                    }
                }

                // Save the course ratings to file
                saveRatingToUserFile(currentUser, courseId, course.getTitle(), rating, review);

                // Also update user's rating history
                currentUser.addCourseRating(courseId, rating);

                // Save updated course data
                saveCourseRatings(course);

                System.out.println("✅ Thank you for your rating!");
                System.out.println("New overall rating for " + course.getTitle() + ": " +
                        course.getRatingString());
            } else {
                System.out.println("❌ Failed to submit rating. Please try again.");
            }

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void viewTopRatedCourses() {
        try {
            System.out.println("\n🏆 TOP RATED COURSES 🏆");

            List<Course> allCourses = engine.getAllCourses();

            // Load ratings for all courses first
            for (Course course : allCourses) {
                loadCourseRatings(course);
            }

            // Filter to courses with at least one rating
            List<Course> ratedCourses = allCourses.stream()
                    .filter(c -> c.getRatingCount() > 0)
                    .collect(Collectors.toList());

            if (ratedCourses.isEmpty()) {
                System.out.println("No courses have been rated yet.");
                return;
            }

            // Sort by average rating (highest first)
            ratedCourses.sort(Comparator.comparing(Course::getAverageRating).reversed());

            // Display top 10 courses
            int limit = Math.min(10, ratedCourses.size());
            System.out.println("Showing top " + limit + " rated courses:");
            System.out.println("----------------------------------------------------");

            for (int i = 0; i < limit; i++) {
                Course course = ratedCourses.get(i);
                System.out.println((i + 1) + ". " + course.getTitle());
                System.out.println("   Rating: " + course.getRatingString());
                System.out.println("   Category: " + course.getCategory());
                System.out.println("   Difficulty: " + course.getDifficulty());
                System.out.println("   Provider: " + course.getProvider());

                // Display sample reviews if available
                List<String> reviews = course.getTopReviews(2);  // Get up to 2 reviews
                if (!reviews.isEmpty()) {
                    System.out.println("   Sample Reviews:");
                    for (String review : reviews) {
                        System.out.println("   → " + review);
                    }
                }

                System.out.println("----------------------------------------------------");
            }

        } catch (Exception e) {
            System.out.println("❌ An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to save ratings directly to user file in the specified format
    public void saveRatingToUserFile(User user, String courseId, String courseName, int rating, String review) {
        try {
            String fileName = "user_" + user.getUserID() + ".txt";
            File file = new File(fileName);

            if (!file.exists()) {
                System.out.println("User file not found. Creating a new one.");
                user.saveToFile();
                return;
            }

            // Read all lines from the file
            List<String> lines = Files.readAllLines(file.toPath());
            List<String> updatedLines = new ArrayList<>();

            // Location for adding new course ratings
            boolean inRatingSection = false;
            boolean foundCourse = false;
            boolean ratingAdded = false;
            
            // Format current date/time for the rating timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            // Process the file line by line
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                
                // Add the current line to our updated content
                updatedLines.add(line);

                // Check if we're in the RATINGS section
                if (line.equals("RATINGS:")) {
                    inRatingSection = true;
                    continue;
                }
                
                // If we're in the RATINGS section and find the course
                if (inRatingSection && line.trim().startsWith("COURSE_ID: " + courseId)) {
                    foundCourse = true;
                    
                    // Look for the RATINGS: subsection
                    boolean hasRatingsSubsection = false;
                    
                    // Check next lines to see if there's already a RATINGS: subsection
                    for (int j = i + 1; j < lines.size(); j++) {
                        String nextLine = lines.get(j).trim();
                        
                        // Stop if we hit another course or empty line
                        if (nextLine.startsWith("COURSE_ID:") || nextLine.isEmpty()) {
                            break;
                        }
                        
                        if (nextLine.equals("RATINGS:")) {
                            hasRatingsSubsection = true;
                            break;
                        }
                    }
                    
                    // If there's no RATINGS subsection yet, add it after course name
                    if (!hasRatingsSubsection) {
                        // Skip to after course name
                        i++;
                        if (i < lines.size() && lines.get(i).trim().startsWith("COURSE_NAME:")) {
                            // Add the RATINGS: subsection after course name
                            updatedLines.add("    RATINGS:");
                        } else {
                            i--; // Go back if no course name found
                        }
                    }
                    
                    // Add the new rating entry
                    updatedLines.add("      - RATING: " + rating);
                    if (review != null && !review.isEmpty()) {
                        updatedLines.add("        REVIEW: " + review);
                    }
                    updatedLines.add("        DATE: " + timestamp);
                    updatedLines.add(""); // Empty line for readability
                    
                    ratingAdded = true;
                }
            }

            // If we didn't find the RATINGS section, add it
            if (!inRatingSection) {
                updatedLines.add("RATINGS:");
                updatedLines.add("  COURSE_ID: " + courseId);
                updatedLines.add("  COURSE_NAME: " + courseName);
                updatedLines.add("    RATINGS:");
                updatedLines.add("      - RATING: " + rating);
                if (review != null && !review.isEmpty()) {
                    updatedLines.add("        REVIEW: " + review);
                }
                updatedLines.add("        DATE: " + timestamp);
                updatedLines.add(""); // Empty line for readability
            } 
            // If we found the RATINGS section but not this specific course
            else if (!foundCourse) {
                updatedLines.add("  COURSE_ID: " + courseId);
                updatedLines.add("  COURSE_NAME: " + courseName);
                updatedLines.add("    RATINGS:");
                updatedLines.add("      - RATING: " + rating);
                if (review != null && !review.isEmpty()) {
                    updatedLines.add("        REVIEW: " + review);
                }
                updatedLines.add("        DATE: " + timestamp);
                updatedLines.add(""); // Empty line for readability
            }

            // Write updated content back to file
            Files.write(file.toPath(), updatedLines);
            System.out.println("Rating saved to user file successfully.");

        } catch (IOException e) {
            System.out.println("❌ Error saving rating to user file: " + e.getMessage());
        }
    }

    // Helper method to save course ratings to a file
    public void saveCourseRatings(Course course) {
        try {
            String fileName = "data/ratings/" + course.getCourseID() + "_ratings.txt";
            File directory = new File("data/ratings");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Get all current ratings from the course
            Map<String, List<Integer>> courseRatings = course.getAllRatings();
            Map<String, List<String>> courseReviews = course.getReviews();
            
            // Calculate total and average ratings
            int totalRatings = 0;
            int ratingSum = 0;
            
            for (Map.Entry<String, List<Integer>> entry : courseRatings.entrySet()) {
                List<Integer> ratings = entry.getValue();
                totalRatings += ratings.size();
                for (Integer rating : ratings) {
                    ratingSum += rating;
                }
            }
            
            double averageRating = totalRatings > 0 ? (double) ratingSum / totalRatings : 0.0;
            
            // Write ratings to file in structured format
            PrintWriter writer = new PrintWriter(new FileWriter(fileName));
            writer.println("COURSE_ID: " + course.getCourseID());
            writer.println("COURSE_NAME: " + course.getTitle());
            writer.println("AVERAGE_RATING: " + String.format("%.1f", averageRating));
            writer.println("TOTAL_RATINGS: " + totalRatings);
            writer.println();
            
            writer.println("USER_RATINGS:");
            for (Map.Entry<String, List<Integer>> entry : courseRatings.entrySet()) {
                String userId = entry.getKey();
                List<Integer> ratings = entry.getValue();
                
                for (int i = 0; i < ratings.size(); i++) {
                    writer.println("  USER_ID: " + userId);
                    writer.println("  RATING: " + ratings.get(i));
                    
                    // Add review if exists
                    if (courseReviews.containsKey(userId) && courseReviews.get(userId).size() > i) {
                        writer.println("  REVIEW: " + courseReviews.get(userId).get(i));
                    }
                    writer.println();
                }
            }
            
            writer.close();
            System.out.println("Course ratings saved successfully to " + fileName);

        } catch (IOException e) {
            System.err.println("Error saving course ratings: " + e.getMessage());
        }
    }

    // Method to load course ratings from file
    public void loadCourseRatings(Course course) {
        try {
            String filename = course.getCourseID() + "_ratings.txt";
            Path ratingFile = Paths.get(DATA_DIRECTORY, "ratings", filename);

            if (!Files.exists(ratingFile)) {
                return; // No ratings file exists yet
            }

            List<String> lines = Files.readAllLines(ratingFile);
            
            boolean inUserRatings = false;
            String currentUserId = null;
            Integer currentRating = null;
            String currentReview = null;
            
            for (String line : lines) {
                line = line.trim();
                
                // Skip empty lines and header section
                if (line.isEmpty() || line.startsWith("COURSE_ID:") || 
                    line.startsWith("COURSE_NAME:") || line.startsWith("AVERAGE_RATING:") || 
                    line.startsWith("TOTAL_RATINGS:")) {
                    continue;
                }
                
                if (line.equals("USER_RATINGS:")) {
                    inUserRatings = true;
                    continue;
                }
                
                if (inUserRatings) {
                    if (line.startsWith("USER_ID:")) {
                        // If we have a complete rating, add it before starting a new one
                        if (currentUserId != null && currentRating != null) {
                            course.addRating(currentUserId, currentRating);
                            if (currentReview != null) {
                                course.addReview(currentUserId, currentReview);
                            }
                        }
                        
                        // Start a new rating
                        currentUserId = line.substring(line.indexOf(':') + 1).trim();
                        currentRating = null;
                        currentReview = null;
                    } else if (line.startsWith("RATING:") && currentUserId != null) {
                        currentRating = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                    } else if (line.startsWith("REVIEW:") && currentUserId != null) {
                        currentReview = line.substring(line.indexOf(':') + 1).trim();
                    } else if (line.isEmpty() && currentUserId != null && currentRating != null) {
                        // Empty line after a rating entry - add the rating
                        course.addRating(currentUserId, currentRating);
                        if (currentReview != null) {
                            course.addReview(currentUserId, currentReview);
                        }
                        
                        // Reset for next entry
                        currentUserId = null;
                        currentRating = null;
                        currentReview = null;
                    }
                }
            }
            
            // Add the last rating if there is one pending
            if (currentUserId != null && currentRating != null) {
                course.addRating(currentUserId, currentRating);
                if (currentReview != null) {
                    course.addReview(currentUserId, currentReview);
                }
            }

            System.out.println("Loaded ratings for course: " + course.getTitle());

        } catch (IOException e) {
            System.out.println("❌ Error loading ratings: " + e.getMessage());
        }
    }

    // Helper method for CLI input
    private String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public boolean registerUser(User newUser) {
        try {
            if (newUser == null) {
                throw new IllegalArgumentException("User cannot be null.");
            }

            // Check if user already exists
            if (engine.getUserById(newUser.getUserID()) != null) {
                System.out.println("❌ User ID already exists. Please choose a different one.");
                return false;
            }

            // Save the new user to file
            newUser.saveToFile();
            System.out.println("✅ User registered successfully!");
            return true;

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("❌ An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    // Add this method to expose the engine
    public RecommendationEngine getEngine() {
        return engine;
    }
}