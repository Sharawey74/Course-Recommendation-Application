import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.io.*;


enum SkillLevel {
    BEGINNER, INTERMEDIATE, ADVANCED
}

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String userID;
    private String name;
    private String email;
    private String password; // Hashed password
    private SkillLevel skillLevel;
    private Set<String> interests;
    private List<String> completedCourses;
    private List<String> enrolledCourseIds;
    private Map<String, Integer> courseProgress = new HashMap<>();
    private Map<String, String> lastAccessedModule = new HashMap<>();
    private Map<String, LocalDateTime> lastAccessTime = new HashMap<>();
    private Map<String, List<Integer>> courseRatings = new HashMap<>();
    private Map<String, List<RatingEntry>> courseRatingsWithTimestamps = new HashMap<>();
    private static RecommendationEngine engineReference;

    public static void setEngine(RecommendationEngine engine) {
        engineReference = engine;
    }

    public User(String userID, String name, String email, String password) {
        validateInputs(userID, name, email, password);
        this.userID = userID;
        this.name = name;
        this.email = email;
        this.password = hashPassword(password);
        this.skillLevel = SkillLevel.BEGINNER;
        this.interests = new HashSet<>();
        this.completedCourses = new ArrayList<>();
        this.enrolledCourseIds = new ArrayList<>();
        this.courseProgress = new HashMap<>();
        this.lastAccessedModule = new HashMap<>();
        this.lastAccessTime = new HashMap<>();
        this.courseRatings = new HashMap<>();
    }

    private String hashPassword(String password) {
        return Base64.getEncoder().encodeToString(password.getBytes());
    }

    public boolean checkPassword(String plainTextPassword) {
        String hashedInput = hashPassword(plainTextPassword);
        return this.password.equals(hashedInput);  // Verify the password during login
    }

    private boolean isStrongPassword(String password) {
        return password != null &&
                password.length() >= 8 &&
                Pattern.compile(".*[A-Z].*").matcher(password).matches() &&
                Pattern.compile(".*[a-z].*").matcher(password).matches() &&
                Pattern.compile(".*\\d.*").matcher(password).matches() &&
                Pattern.compile(".*[!@#$%^&*()].*").matcher(password).matches();
    }

    // Validate inputs during registration
    private void validateInputs(String userID, String name, String email, String password) {
        if (userID == null || userID.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        ///  runtime exception
        if (!isStrongPassword(password)) {
            throw new IllegalArgumentException(
                    "Password must have:\n" +
                            "- 8+ characters\n" +
                            "- 1 uppercase letter\n" +
                            "- 1 lowercase letter\n" +
                            "- 1 digit\n" +
                            "- 1 special symbol (!@#$%^&*)"
            );
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Pattern.matches(emailRegex, email);
    }

    public void addInterest(String interest) {
        if (interest != null && !interest.trim().isEmpty()) {
            interests.add(interest);
        }
    }

    public void updateProfile(String name, String email, Set<String> interests) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }

        if (email != null && isValidEmail(email)) {
            this.email = email;
        }

        if (interests != null && !interests.isEmpty()) {
            this.interests = new HashSet<>(interests);
        }

        saveToFile();
    }

    public boolean changePassword(String oldPassword, String newPassword) {
        if (!checkPassword(oldPassword)) {
            return false;
        }
        if (!isStrongPassword(newPassword)) {
            return false;
        }
        this.password = hashPassword(newPassword);
        saveToFile();
        return true;
    }

    public boolean enrollCourse(String courseId) {
        if (!enrolledCourseIds.contains(courseId)) {
            enrolledCourseIds.add(courseId);
            saveToFile();
            return true;
        }
        return false;
    }

    public void completeCourse(String courseId) {
        if (enrolledCourseIds.contains(courseId)) {
            enrolledCourseIds.remove(courseId);

            if (!completedCourses.contains(courseId)) {
                completedCourses.add(courseId);
            }
            updateSkillLevel();
            saveToFile();
        }
    }

    public void updateSkillLevel() {
        int completedCoursesCount = completedCourses.size();

        if (completedCoursesCount >= 10) {
            skillLevel = SkillLevel.ADVANCED;
        } else if (completedCoursesCount >= 5) {
            skillLevel = SkillLevel.INTERMEDIATE;
        }
    }
    public void saveToFile() {
        try {
            String fileName = "user_" + userID + ".txt";
            PrintWriter writer = new PrintWriter(new FileWriter(fileName));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            // Basic user info
            writer.println("USER_ID: " + userID);
            writer.println("NAME: " + name);
            writer.println("EMAIL: " + email);
            writer.println("PASSWORD: " + password);
            writer.println("SKILL_LEVEL: " + skillLevel);

            // Interests
            writer.println("INTERESTS:");
            for (String interest : interests) {
                writer.println("  " + interest);
            }

            java.util.function.Function<String, String> getCourseTitle = (courseId) -> {
                Course course = engineReference != null ? engineReference.getCourseById(courseId) : null;
                return (course != null && course.getTitle() != null && !course.getTitle().isEmpty())
                        ? course.getTitle()
                        : "(Course not found)";
            };

            // Enrolled courses
            writer.println("ENROLLED_COURSES:");
            for (String courseId : enrolledCourseIds) {
                writer.println("  " + courseId + " - " + getCourseTitle.apply(courseId));
            }

            // Completed courses
            writer.println("COMPLETED_COURSES:");
            for (String courseId : completedCourses) {
                writer.println("  " + courseId + " - " + getCourseTitle.apply(courseId));
            }

            // Course progress
            writer.println("COURSE_PROGRESS:");
            for (Map.Entry<String, Integer> entry : courseProgress.entrySet()) {
                String courseId = entry.getKey();
                int progress = entry.getValue();
                int userRating = 0;
                Course course = engineReference != null ? engineReference.getCourseById(courseId) : null;
                if (course != null) {
                    userRating = course.getUserRating(userID);
                }
                writer.println("  COURSE_ID: " + courseId);
                writer.println("  COURSE_NAME: " + getCourseTitle.apply(courseId));
                writer.println("  PROGRESS: " + progress + "%");
                writer.println("  YOUR_RATING: " + (userRating > 0 ? userRating + "/5" : "Not rated"));
            }

            // Last access time
            writer.println("LAST_ACCESS_TIME:");
            for (Map.Entry<String, LocalDateTime> entry : lastAccessTime.entrySet()) {
                String courseId = entry.getKey();
                LocalDateTime accessTime = entry.getValue();
                writer.println("  COURSE_ID: " + courseId);
                writer.println("  COURSE_NAME: " + getCourseTitle.apply(courseId));
                writer.println("  TIME: " + accessTime.format(formatter));
            }
            
            // Enhanced ratings with timestamps
            writer.println("RATINGS:");
            for (Map.Entry<String, List<RatingEntry>> entry : courseRatingsWithTimestamps.entrySet()) {
                String courseId = entry.getKey();
                List<RatingEntry> ratings = entry.getValue();
                
                if (!ratings.isEmpty()) {
                    writer.println("  COURSE_ID: " + courseId);
                    writer.println("  COURSE_NAME: " + getCourseTitle.apply(courseId));
                    writer.println("    RATINGS:");
                    
                    for (RatingEntry ratingEntry : ratings) {
                        writer.println("      - RATING: " + ratingEntry.getRating());
                        if (ratingEntry.getReview() != null && !ratingEntry.getReview().isEmpty()) {
                            writer.println("        REVIEW: " + ratingEntry.getReview());
                        }
                        writer.println("        DATE: " + ratingEntry.getTimestamp().format(formatter));
                    }
                }
            }

            writer.close();
            System.out.println("✅ User data saved successfully to " + fileName);
        } catch (IOException e) {
            System.err.println("❌ Error saving user data: " + e.getMessage());
        }
    }


    public static User loadFromFile(String userId) {
        try {
            String fileName = "user_" + userId + ".txt";
            File file = new File(fileName);
            if (!file.exists()) {
                return null;
            }

            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line;
            String name = "";
            String email = "";
            String password = "";
            SkillLevel skillLevel = SkillLevel.BEGINNER;
            Set<String> interests = new HashSet<>();
            List<String> enrolledCourses = new ArrayList<>();
            List<String> completedCourses = new ArrayList<>();
            Map<String, Integer> courseProgress = new HashMap<>();
            Map<String, String> lastAccessedModule = new HashMap<>();
            Map<String, LocalDateTime> lastAccessTime = new HashMap<>();
            Map<String, List<Integer>> courseRatings = new HashMap<>();
            Map<String, List<RatingEntry>> courseRatingsWithTimestamps = new HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            String section = "";
            String currentCourseId = "";
            boolean inRatingsSubsection = false;
            int ratingValue = 0;
            String reviewText = null;
            LocalDateTime ratingDate = null;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("USER_ID:")) {
                    // Skip, already have userId
                } else if (line.startsWith("NAME:")) {
                    name = line.substring(5).trim();
                } else if (line.startsWith("EMAIL:")) {
                    email = line.substring(6).trim();
                } else if (line.startsWith("PASSWORD:")) {
                    password = line.substring(9).trim();
                    // Validate Base64 password format
                    if (!password.matches("^[A-Za-z0-9+/=]+$")) {
                        throw new IOException("Invalid password format in file.");
                    }
                } else if (line.startsWith("SKILL_LEVEL:")) {
                    skillLevel = SkillLevel.valueOf(line.substring(12).trim());
                } else if (line.equals("INTERESTS:")) {
                    section = "INTERESTS";
                } else if (line.equals("ENROLLED_COURSES:")) {
                    section = "ENROLLED_COURSES";
                } else if (line.equals("COMPLETED_COURSES:")) {
                    section = "COMPLETED_COURSES";
                } else if (line.equals("COURSE_PROGRESS:")) {
                    section = "COURSE_PROGRESS";
                } else if (line.equals("LAST_ACCESSED_MODULE:")) {
                    section = "LAST_ACCESSED_MODULE";
                } else if (line.equals("LAST_ACCESS_TIME:")) {
                    section = "LAST_ACCESS_TIME";
                } else if (line.equals("RATINGS:")) {
                    section = "RATINGS";
                    inRatingsSubsection = false;
                } else if (line.startsWith("  ")) {
                    String item = line.substring(2).trim();
                    
                    switch (section) {
                        case "INTERESTS":
                            interests.add(item);
                            break;
                        case "ENROLLED_COURSES":
                            // Extract course ID (ignore name)
                            enrolledCourses.add(item.split(" - ")[0].trim());
                            break;
                        case "COMPLETED_COURSES":
                            // Extract course ID (ignore name)
                            completedCourses.add(item.split(" - ")[0].trim());
                            break;
                        case "COURSE_PROGRESS":
                            if (item.startsWith("COURSE_ID:")) {
                                currentCourseId = item.substring(10).trim();
                            } else if (item.startsWith("PROGRESS:") && !currentCourseId.isEmpty()) {
                                String progressStr = item.substring(9).trim();
                                progressStr = progressStr.replace("%", "").trim();
                                try {
                                    int progress = Integer.parseInt(progressStr);
                                    courseProgress.put(currentCourseId, progress);
                                } catch (NumberFormatException e) {
                                    System.err.println("Invalid progress value: " + progressStr);
                                }
                            }
                            break;
                        case "LAST_ACCESSED_MODULE":
                            if (item.startsWith("COURSE_ID:")) {
                                currentCourseId = item.substring(10).trim();
                            } else if (item.startsWith("MODULE:") && !currentCourseId.isEmpty()) {
                                String moduleId = item.substring(7).trim();
                                lastAccessedModule.put(currentCourseId, moduleId);
                            }
                            break;
                        case "LAST_ACCESS_TIME":
                            if (item.startsWith("COURSE_ID:")) {
                                currentCourseId = item.substring(10).trim();
                            } else if (item.startsWith("TIME:") && !currentCourseId.isEmpty()) {
                                String timeStr = item.substring(5).trim();
                                try {
                                    LocalDateTime time = LocalDateTime.parse(timeStr, formatter);
                                    lastAccessTime.put(currentCourseId, time);
                                } catch (Exception e) {
                                    System.err.println("Invalid time format: " + timeStr);
                                }
                            }
                            break;
                        case "RATINGS":
                            if (item.startsWith("COURSE_ID:")) {
                                currentCourseId = item.substring(10).trim();
                                inRatingsSubsection = false;
                            } else if (item.equals("RATINGS:")) {
                                inRatingsSubsection = true;
                            } else if (inRatingsSubsection) {
                                if (item.startsWith("- RATING:")) {
                                    // Start a new rating entry
                                    // Save previous entry if exists
                                    if (ratingValue > 0 && currentCourseId != null) {
                                        // Add to traditional ratings for backward compatibility
                                        if (!courseRatings.containsKey(currentCourseId)) {
                                            courseRatings.put(currentCourseId, new ArrayList<>());
                                        }
                                        courseRatings.get(currentCourseId).add(ratingValue);
                                        
                                        // Create and add the enhanced rating entry if we have a date
                                        if (ratingDate != null) {
                                            if (!courseRatingsWithTimestamps.containsKey(currentCourseId)) {
                                                courseRatingsWithTimestamps.put(currentCourseId, new ArrayList<>());
                                            }
                                            
                                            // Create a new rating entry with the collected data
                                            RatingEntry entry = new RatingEntry(ratingValue, reviewText);
                                            // Use reflection to set the timestamp field directly
                                            try {
                                                java.lang.reflect.Field timestampField = RatingEntry.class.getDeclaredField("timestamp");
                                                timestampField.setAccessible(true);
                                                timestampField.set(entry, ratingDate);
                                            } catch (Exception e) {
                                                System.err.println("Error setting timestamp: " + e.getMessage());
                                            }
                                            
                                            courseRatingsWithTimestamps.get(currentCourseId).add(entry);
                                        }
                                    }
                                    
                                    // Parse new rating
                                    String ratingStr = item.substring(9).trim();
                                    try {
                                        ratingValue = Integer.parseInt(ratingStr);
                                        reviewText = null;
                                        ratingDate = null;
                                    } catch (NumberFormatException e) {
                                        System.err.println("Invalid rating value: " + ratingStr);
                                        ratingValue = 0;
                                    }
                                } else if (item.startsWith("REVIEW:") && ratingValue > 0) {
                                    reviewText = item.substring(7).trim();
                                } else if (item.startsWith("DATE:") && ratingValue > 0) {
                                    String dateStr = item.substring(5).trim();
                                    try {
                                        ratingDate = LocalDateTime.parse(dateStr, formatter);
                                    } catch (Exception e) {
                                        System.err.println("Invalid date format: " + dateStr);
                                    }
                                }
                            }
                            break;
                    }
                }
            }
            
            // Save the last rating entry if we have one
            if (ratingValue > 0 && currentCourseId != null && section.equals("RATINGS")) {
                // Add to traditional ratings for backward compatibility
                if (!courseRatings.containsKey(currentCourseId)) {
                    courseRatings.put(currentCourseId, new ArrayList<>());
                }
                courseRatings.get(currentCourseId).add(ratingValue);
                
                // Create and add the enhanced rating entry if we have a date
                if (ratingDate != null) {
                    if (!courseRatingsWithTimestamps.containsKey(currentCourseId)) {
                        courseRatingsWithTimestamps.put(currentCourseId, new ArrayList<>());
                    }
                    
                    // Create a new rating entry with the collected data
                    RatingEntry entry = new RatingEntry(ratingValue, reviewText);
                    // Use reflection to set the timestamp field directly
                    try {
                        java.lang.reflect.Field timestampField = RatingEntry.class.getDeclaredField("timestamp");
                        timestampField.setAccessible(true);
                        timestampField.set(entry, ratingDate);
                    } catch (Exception e) {
                        System.err.println("Error setting timestamp: " + e.getMessage());
                    }
                    
                    courseRatingsWithTimestamps.get(currentCourseId).add(entry);
                }
            }

            reader.close();

            // Create and configure user
            User user = createUserWithoutValidation(userId, name, email, password);
            user.skillLevel = skillLevel;
            user.interests = interests;
            user.enrolledCourseIds = enrolledCourses;
            user.completedCourses = completedCourses;
            user.courseProgress = courseProgress;
            user.lastAccessedModule = lastAccessedModule;
            user.lastAccessTime = lastAccessTime;
            user.courseRatings = courseRatings;
            user.courseRatingsWithTimestamps = courseRatingsWithTimestamps;

            return user;
        } catch (IOException e) {
            System.err.println("❌ Error loading user data: " + e.getMessage());
            return null;
        }
    }
    // Create a user without validation for file loading purposes
    private static User createUserWithoutValidation(String userId, String name, String email, String password) {
        User user = new User(userId, name, email, "temp123A!");
        // Override the password with the stored hash directly
        user.password = password;
        return user;
    }
    public String getUserID() {
        return userID;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public Set<String> getInterests() {
        return new HashSet<>(interests);
    }

    public List<String> getCompletedCourses() {
        return new ArrayList<>(completedCourses);
    }

    public List<String> getEnrolledCourseIds() {
        return new ArrayList<>(enrolledCourseIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userID.equals(user.userID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID);
    }

    public void clearInterests() {
        interests.clear();  // Remove all interests
        setInterests(new HashSet<>()); // Explicitly set to an empty set
        saveToFile();  // Ensure changes are saved
    }

    public void setInterests(Set<String> newInterests) {
        interests = new HashSet<>(newInterests);
    }

    public void enrollInCourse(String courseId) {
        if (!enrolledCourseIds.contains(courseId)) {
            enrolledCourseIds.add(courseId);
            saveToFile();
        }
    }

    public void setSkillLevel(SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
        saveToFile();
    }
    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
            saveToFile();
        }
    }

    public void setEmail(String email) {
        if (email != null && isValidEmail(email)) {
            this.email = email;
            saveToFile();
        }
    }

    public void setPassword(String password) {
        if (isStrongPassword(password)) {
            this.password = hashPassword(password);
            saveToFile();
        }
    }
    public void removeEnrolledCourse(String courseID) {
        if (courseID == null || courseID.isEmpty()) {
            System.out.println("Error: Course ID cannot be empty.");
            return;
        }

        if (enrolledCourseIds.contains(courseID)) {
            enrolledCourseIds.remove(courseID);
            System.out.println("Course " + courseID + " has been removed from your enrolled courses.");

            // Save the changes to the user's file
            saveToFile();
        } else {
            System.out.println("You are not enrolled in course " + courseID + ".");
        }

    }

    public boolean updateCourseProgress(String courseId, int progressPercentage, String moduleId) {
        if (!enrolledCourseIds.contains(courseId)) {
            return false;
        }

        // Validate progress percentage
        int validProgress = Math.max(0, Math.min(100, progressPercentage));

        // Update progress tracking
        courseProgress.put(courseId, validProgress);
        lastAccessedModule.put(courseId, moduleId);
        lastAccessTime.put(courseId, LocalDateTime.now());

        // If progress is 100%, mark as completed
        if (validProgress == 100) {
            completeCourse(courseId);
        }

        // Save changes
        saveToFile();
        return true;
    }


    public int getCourseProgress(String courseId) {
        if (!enrolledCourseIds.contains(courseId)) {
            return -1;
        }
        return courseProgress.getOrDefault(courseId, 0);
    }


    public String getLastAccessedModule(String courseId) {
        if (!enrolledCourseIds.contains(courseId)) {
            return null;
        }
        return lastAccessedModule.get(courseId);
    }


    public LocalDateTime getLastAccessTime(String courseId) {
        if (!enrolledCourseIds.contains(courseId)) {
            return null;
        }
        return lastAccessTime.get(courseId);
    }

    public Map<String, Map<String, Object>> getAllCourseProgress() {
        Map<String, Map<String, Object>> result = new HashMap<>();

        for (String courseId : enrolledCourseIds) {
            Map<String, Object> progressInfo = new HashMap<>();
            progressInfo.put("progress", courseProgress.getOrDefault(courseId, 0));
            progressInfo.put("lastModule", lastAccessedModule.get(courseId));
            progressInfo.put("lastAccess", lastAccessTime.get(courseId));
            progressInfo.put("completed", completedCourses.contains(courseId));

            result.put(courseId, progressInfo);
        }

        return result;
    }
    public void updateLastAccessTime(String courseId, LocalDateTime now) {
        lastAccessTime.put(courseId, now);
        saveToFile();  // Ensure changes are saved
    }

    // Inner class to store rating with timestamp
    public static class RatingEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        private int rating;
        private LocalDateTime timestamp;
        private String review;
        
        public RatingEntry(int rating, String review) {
            this.rating = rating;
            this.timestamp = LocalDateTime.now();
            this.review = review;
        }
        
        public int getRating() {
            return rating;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public String getReview() {
            return review;
        }
    }
    
    // Enhanced method to add course rating with timestamp and review
    public void addCourseRating(String courseId, int rating, String review) {
        if (!courseRatingsWithTimestamps.containsKey(courseId)) {
            courseRatingsWithTimestamps.put(courseId, new ArrayList<>());
        }
        courseRatingsWithTimestamps.get(courseId).add(new RatingEntry(rating, review));
        
        // Also maintain backward compatibility with the old rating structure
        if (!courseRatings.containsKey(courseId)) {
            courseRatings.put(courseId, new ArrayList<>());
        }
        courseRatings.get(courseId).add(rating);
        
        saveToFile();  // Ensure changes are saved
    }
    
    // Maintain backward compatibility for code that uses the old method
    public void addCourseRating(String courseId, int rating) {
        addCourseRating(courseId, rating, null);
    }
    
    // Get all ratings with timestamps for a specific course
    public List<RatingEntry> getCourseRatingsWithTimestamps(String courseId) {
        return courseRatingsWithTimestamps.getOrDefault(courseId, new ArrayList<>());
    }
    
    // Get all ratings with timestamps
    public Map<String, List<RatingEntry>> getAllCourseRatingsWithTimestamps() {
        // Return a defensive copy to prevent external modifications
        Map<String, List<RatingEntry>> result = new HashMap<>();
        for (Map.Entry<String, List<RatingEntry>> entry : courseRatingsWithTimestamps.entrySet()) {
            // Create a new list to avoid external modifications to the internal ratings
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }

    public void unenrollCourse(String courseID) {
        if (enrolledCourseIds.contains(courseID)) {
            enrolledCourseIds.remove(courseID);
            saveToFile();
        }
    }

    public LocalDateTime getCompletionDate(String courseId) {
        if (completedCourses.contains(courseId)) {
            return lastAccessTime.getOrDefault(courseId, null);
        }
        return null;
    }

    public String getPassword() {
        return password;
    }
    // Add this method to expose progress data
    public Map<String, Integer> getCourseProgressMap() {
        return new HashMap<>(courseProgress);}
    /// /////////////////////////////////////////
    ///
    public int getLearningStreak() {
        int streak = 0;
        LocalDateTime now = LocalDateTime.now();
        for (String courseId : enrolledCourseIds) {
            LocalDateTime lastAccess = lastAccessTime.get(courseId);
            if (lastAccess != null && lastAccess.toLocalDate().isEqual(now.toLocalDate())) {
                streak++;
            }
        }
        return streak;
    }
}


