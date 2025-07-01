import java.io.Serializable;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;

enum CourseCategory {
    PROGRAMMING, BUSINESS, DATA_SCIENCE,
    ARTIFICIAL_INTELLIGENCE, DESIGN, MARKETING
}
enum CourseDifficulty {
    BEGINNER, INTERMEDIATE, ADVANCED
}

public class Course implements Serializable {
    private static final long serialVersionUID = 1L;

    // Core course attributes
    private final String courseID;
    private String title;
    private CourseCategory category;
    private CourseDifficulty difficulty;
    private String provider;
    private String description;
    private Map<String, List<Integer>> userRatings = new HashMap<>();
    private Map<String, List<String>> reviews = new HashMap<>();

    // Rating and metadata attributes
    private double averageRating;
    private int totalRatings;
    private int enrollmentCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private int ratingCount = 0;


    // Constructor
    public Course(String courseID, String title, CourseCategory category,
                  CourseDifficulty difficulty, String provider, String description) {
        validateInputs(courseID, title, category, difficulty, provider, description);

        this.courseID = courseID;
        this.title = title;
        this.category = category;
        this.difficulty = difficulty;
        this.provider = provider;
        this.description = description;

        // Initialize metadata
        this.averageRating = 0.0;
        this.totalRatings = 0;
        this.enrollmentCount = 0;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.userRatings = new HashMap<>();
        this.reviews = new HashMap<>();

    }

    // Input validation
    private void validateInputs(String courseID, String title, CourseCategory category,
                                CourseDifficulty difficulty, String provider, String description) {
        if (courseID == null || courseID.trim().isEmpty()) {
            throw new IllegalArgumentException("Course ID cannot be empty");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Course title cannot be empty");
        }
        if (category == null) {
            throw new IllegalArgumentException("Course category cannot be null");
        }
        if (difficulty == null) {
            throw new IllegalArgumentException("Course difficulty cannot be null");
        }
        if (provider == null || provider.trim().isEmpty()) {
            throw new IllegalArgumentException("Course provider cannot be empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Course description cannot be empty");
        }
    }

    // Rate course method with validation
    public void rateCourse(String userId, double rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }

        // Store the user's rating
        if (!userRatings.containsKey(userId)) {
            userRatings.put(userId, new ArrayList<>());
        }
        userRatings.get(userId).add((int) rating);

        // Recalculate average rating
        calculateAverageRating();

        // Update last updated timestamp
        lastUpdated = LocalDateTime.now();
    }

    // Calculate average rating from all user ratings
    private void calculateAverageRating() {
        if (userRatings.isEmpty()) {
            averageRating = 0.0;
            totalRatings = 0;
            return;
        }

        double sum = 0;
        int count = 0;
        
        for (List<Integer> ratings : userRatings.values()) {
            for (Integer rating : ratings) {
                sum += rating;
                count++;
            }
        }

        totalRatings = count;
        averageRating = sum / totalRatings;
    }

    // Check if a user has already rated this course
    public boolean hasUserRated(String userId) {
        return userRatings.containsKey(userId) && !userRatings.get(userId).isEmpty();
    }

    // Check course suitability for a user's skill level
    public boolean isSuitableForSkillLevel(CourseDifficulty userSkillLevel) {
        return this.difficulty.ordinal() <= userSkillLevel.ordinal();
    }

    // Enrollment method
    public void incrementEnrollment() {
        enrollmentCount++;
        lastUpdated = LocalDateTime.now();
    }

    // Comprehensive course information retrieval
    public Map<String, Object> getCourseDetails() {
        Map<String, Object> details = new HashMap<>();
        details.put("courseID", courseID);
        details.put("title", title);
        details.put("category", category);
        details.put("difficulty", difficulty);
        details.put("provider", provider);
        details.put("description", description);
        details.put("averageRating", averageRating);
        details.put("totalRatings", totalRatings);
        details.put("enrollmentCount", enrollmentCount);
        details.put("createdAt", createdAt);
        details.put("lastUpdated", lastUpdated);

        return details;
    }

    // Getters
    public String getCourseID() { return courseID; }
    public String getTitle() { return title; }
    public CourseCategory getCategory() { return category; }
    public CourseDifficulty getDifficulty() { return difficulty; }
    public String getProvider() { return provider; }
    public String getDescription() { return description; }
    public double getAverageRating() { return averageRating; }
    public int getEnrollmentCount() { return enrollmentCount; }
    public Map<String, List<Integer>> getUserRatings() { return new HashMap<>(userRatings); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public int getRatingCount() {
        return ratingCount;
    }
    public int getUserRating(String userId) {
        if (userRatings.containsKey(userId) && !userRatings.get(userId).isEmpty()) {
            // Return the most recent rating
            List<Integer> ratings = userRatings.get(userId);
            return ratings.get(ratings.size() - 1);
        }
        return 0;
    }
    public Map<String, List<Integer>> getAllRatings() {
        return new HashMap<>(userRatings); // Return a copy to prevent direct modification
    }
    public String getRatingString() {
        if (ratingCount == 0) {
            return "No ratings yet";
        }

        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(averageRating) + "/5 (" + ratingCount + " ratings)";
    }

    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return courseID.equals(course.courseID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseID);
    }

    public void decrementEnrollment() {
        if (enrollmentCount > 0) {
            enrollmentCount--;
            System.out.println("Enrollment count for course " + courseID + " has been decreased to " + enrollmentCount);
        }
        lastUpdated = LocalDateTime.now();
    }

    public boolean addRating(String userId, int rating) {
        if (rating < 1 || rating > 5) {
            return false;
        }
        
        // Initialize the list if it doesn't exist
        if (!userRatings.containsKey(userId)) {
            userRatings.put(userId, new ArrayList<>());
        }
        
        // Add the new rating to the list (don't overwrite)
        userRatings.get(userId).add(rating);
        
        // Update the average rating
        int sum = 0;
        int count = 0;
        
        for (List<Integer> ratings : userRatings.values()) {
            for (Integer r : ratings) {
                sum += r;
                count++;
            }
        }
        
        ratingCount = count;
        averageRating = count > 0 ? (double) sum / count : 0;

        return true;
    }

    public boolean addReview(String userID, String review) {
        if (userID == null || userID.isEmpty()) {
            System.out.println("Error: User ID cannot be empty.");
            return false;
        }

        if (review == null || review.isEmpty()) {
            System.out.println("Error: Review cannot be empty.");
            return false;
        }

        if (!reviews.containsKey(userID)) {
            reviews.put(userID, new ArrayList<>());
        }
        reviews.get(userID).add(review);
        return true;
    }
    // Sort the reviews based on their ratings or popularity
    public List<String> getTopReviews(int i) {
        List<String> allReviews = new ArrayList<>();
        for (List<String> userReviews : reviews.values()) {
            allReviews.addAll(userReviews);
        }
        // Sort the reviews based on their ratings or popularity
        //implement a custom sorting algorithm or use a built-in sorting method
        Collections.sort(allReviews, (review1, review2) -> {
            // Replace the following logic with your own sorting criteria
            return Integer.compare(getReviewRating(review2), getReviewRating(review1));
        });

        // Return the top reviews
        return allReviews.subList(0, Math.min(i, allReviews.size()));
    }

    private int getReviewRating(String review) {
        // Replace the following logic with your own rating calculation
        // For example, you can assign a rating based on the length of the review
        return review.length();
    }
    public Map<String, List<Integer>> getRatings() {
        return userRatings;
    }
    public Map<String, List<String>> getReviews() {
        return reviews;
    }
    ///  ////////////////////////////////// ML
    public double getDurationHours() {
        // Placeholder for actual duration calculation
        return 10.0; // Example: 10 hours
    }

    public char[] getCourseId() {
        // Placeholder for actual course ID retrieval
        return courseID.toCharArray(); // Example: Convert course ID to char array
    }
}