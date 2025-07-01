import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

public class RecommendationEngine {
    private List<Course> courseDatabase;
    private Map<String, User> userProfiles;
    private Map<String, Map<String, Double>> userPreferenceScores;
    private ExecutorService executorService;
    private static final int MAX_RECOMMENDATIONS = 10;
    private static final double RATING_WEIGHT = 0.4;
    private static final double RECENCY_WEIGHT = 0.3;
    private static final double ENROLLMENT_WEIGHT = 0.3;
    private static final double INTEREST_BONUS_WEIGHT = 0.2;

    public RecommendationEngine() {
        courseDatabase = new ArrayList<>();
        userProfiles = new HashMap<>();
        userPreferenceScores = new HashMap<>();
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public static RecommendationEngine getInstance() {
        return new RecommendationEngine();
    }

    public void addCourse(Course course) {
        if (course == null) throw new IllegalArgumentException("Course cannot be null");
        courseDatabase.add(course);
    }

    public void addUserProfile(User user) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        userProfiles.put(user.getUserID(), user);
        userPreferenceScores.put(user.getUserID(), initializePreferenceScores());
    }

    private Map<String, Double> initializePreferenceScores() {
        Map<String, Double> scores = new HashMap<>();
        for (CourseCategory category : CourseCategory.values()) {
            scores.put(category.name(), 1.0);
        }
        return scores;
    }

    public List<Course> generateRecommendations(String userID) {
        User user = userProfiles.get(userID);
        if (user == null) throw new IllegalArgumentException("User not found");

        // Get user interests - make sure we're using the updated user data
        Set<String> userInterests = user.getInterests();

        return courseDatabase.parallelStream()
                .filter(course -> isValidRecommendation(user, course))
                // Only show courses matching user interests if they have any
                .filter(course -> userInterests.isEmpty() || userInterests.contains(course.getCategory().name()))
                .sorted(Comparator.comparingDouble(course -> calculateRecommendationScore(user, (Course) course)).reversed())
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());
    }

    private boolean isValidRecommendation(User user, Course course) {
        return !user.getCompletedCourses().contains(course.getCourseID()) &&
                !user.getEnrolledCourseIds().contains(course.getCourseID()) &&
                user.getInterests().contains(course.getCategory().name());
    }

    private double calculateRecommendationScore(User user, Course course) {
        double ratingScore = (course.getAverageRating() / 5.0) * RATING_WEIGHT;
        double recencyScore = calculateRecencyScore(course) * RECENCY_WEIGHT;
        double enrollmentScore = calculateEnrollmentScore(course) * ENROLLMENT_WEIGHT;
        double interestBonus = calculateInterestBonus(user, course) * INTEREST_BONUS_WEIGHT;
        return ratingScore + recencyScore + enrollmentScore + interestBonus;
    }

    private double calculateRecencyScore(Course course) {
        long days = ChronoUnit.DAYS.between(course.getCreatedAt(), LocalDateTime.now());
        return Math.exp(-days / 365.0);
    }

    private double calculateEnrollmentScore(Course course) {
        int maxEnrollment = courseDatabase.stream()
                .mapToInt(Course::getEnrollmentCount)
                .max().orElse(1);
        return (double) course.getEnrollmentCount() / maxEnrollment;
    }

    private double calculateInterestBonus(User user, Course course) {
        return user.getInterests().contains(course.getCategory().name()) ?
                userPreferenceScores.get(user.getUserID()).getOrDefault(course.getCategory().name(), 1.0) : 0.0;
    }

    public User getUserById(String userId) {
        return userProfiles.get(userId);
    }

    public Course getCourseById(String courseId) {
        return courseDatabase.stream()
                .filter(c -> c.getCourseID().equals(courseId))
                .findFirst()
                .orElse(null);
    }

    public List<Course> getAllCourses() {
        return new ArrayList<>(courseDatabase);
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    /// /////////////////////////////GUi
    public List<Course> getRecommendedCourses(User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        return generateRecommendations(currentUser.getUserID());
    }

    private final Map<String, List<Integer>> courseRatings = new HashMap<>();


    public List<User> getAllUsers() {
        return new ArrayList<>(userProfiles.values());
    }

    public Course getCourse(String id) {
        for (Course course : courseDatabase) {
            if (course.getCourseID().equals(id)) {
                return course;
            }
        }
        return null;
    }
}
