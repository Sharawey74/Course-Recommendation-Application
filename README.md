# 🎓 Course Recommendation Application

A sophisticated JavaFX-based desktop application that provides personalized course recommendations based on user interests, past enrollments, and course categories. The system helps students make informed decisions efficiently by filtering courses relevant to their needs.

![Java](https://img.shields.io/badge/Java-17+-blue.svg)
![JavaFX](https://img.shields.io/badge/JavaFX-17+-green.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

## 🌟 Features

### 🔐 User Authentication & Management
- **Secure Registration**: Strong password validation with encryption
- **User Login**: Password-based authentication with validation
- **Profile Management**: Track user preferences, skill levels, and learning progress
- **Interest Management**: Add/remove learning interests for personalized recommendations

### 📚 Course Management
- **Course Catalog**: Browse comprehensive course database with categories:
  - Programming
  - Business
  - Data Science
  - Artificial Intelligence
  - Design
  - Marketing
- **Course Enrollment**: Easy enrollment process with confirmation
- **Progress Tracking**: Monitor course completion and learning progress
- **Rating System**: Rate completed courses (1-5 stars)

### 🤖 Intelligent Recommendation Engine
- **Multi-factor Algorithm**: Considers multiple factors for recommendations:
  - Course ratings (40% weight)
  - Content recency (30% weight)
  - Enrollment popularity (30% weight)
  - User interest alignment (20% bonus)
- **Personalized Suggestions**: Tailored recommendations based on user profile
- **Parallel Processing**: Optimized performance with concurrent processing

### 📊 Analytics & Reporting
- **Learning Reports**: HTML-based progress reports with visual analytics
- **Dashboard**: Real-time overview of learning metrics and quick actions
- **Progress Visualization**: Track completion rates and learning paths

### 🎨 Modern UI/UX
- **Glass Morphism Design**: Modern translucent interface with blur effects
- **Responsive Layout**: Adaptive design that works across different screen sizes
- **Dark Theme Support**: Professional color scheme with gradient backgrounds
- **Interactive Elements**: Hover effects and smooth transitions

## 🏗️ Architecture

### Core Components

```
src/
├── Model3_2.java           # Main application class (JavaFX Application)
├── User.java               # User entity with authentication & preferences
├── Course.java             # Course entity with ratings & metadata
├── RecommendationEngine.java # AI-powered recommendation system
├── NavButton.java          # Custom navigation button component
├── WrappedTextField.java   # Enhanced text field with floating labels
└── CourseManagerService.java # Business logic layer (referenced)
```

### Key Classes

**Model3_2.java** - Main Application Controller
- Entry point of the application
- Manages all UI screens and navigation
- Handles user interactions and business logic integration

**User.java** - User Management
- Secure password hashing and validation
- Email format validation
- Interest and enrollment tracking
- Serializable for data persistence

**Course.java** - Course Entity
- Comprehensive course metadata management
- Rating system with user validation
- Category and difficulty level classification
- Timestamp tracking for creation and updates

**RecommendationEngine.java** - AI Recommendation System
- Multi-weighted scoring algorithm
- Parallel processing for performance
- Interest-based filtering
- Collaborative filtering capabilities

## 🚀 Getting Started

### Prerequisites

- **Java 17+** (OpenJDK or Oracle JDK)
- **JavaFX 17+** (if not included in your JDK)
- **IDE** (IntelliJ IDEA, Eclipse, or VS Code with Java extensions)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Sharawey74/Course-Recommendation-Application.git
   cd Course-Recommendation-Application
   ```

2. **Compile the application**
   ```bash
   javac -cp ".:javafx-lib/*" src/*.java
   ```

3. **Run the application**
   ```bash
   java -cp ".:javafx-lib/*" --module-path javafx-lib --add-modules javafx.controls,javafx.fxml src.Model3_2
   ```

### Quick Start

1. **Launch Application**: Run `Model3_2.java`
2. **Create Account**: Register with strong password requirements
3. **Set Interests**: Choose your learning interests from available categories
4. **Browse Courses**: Explore the course catalog or view personalized recommendations
5. **Enroll**: Select and enroll in courses that match your interests
6. **Track Progress**: Monitor your learning journey through the dashboard

## 🎯 Usage Guide

### Registration Requirements
- **User ID**: Unique identifier (required)
- **Name**: Full name (required)
- **Email**: Valid email format (required)
- **Password**: Must contain:
  - 8+ characters
  - 1 uppercase letter
  - 1 lowercase letter
  - 1 digit
  - 1 special character (!@#$%^&*)

### Navigation

**Guest Menu**
- Register new account
- Login to existing account
- Exit application

**User Dashboard**
- View personalized recommendations
- Browse course catalog
- Manage enrollments
- Track learning progress
- Update interests and preferences
- Generate learning reports

**Course Management**
- Enroll in courses by browsing or entering course ID
- Rate completed courses
- Mark courses as completed
- Update learning progress

## 🎨 UI Design System

### Color Palette
- **Primary**: `#4361ee` (Professional Blue)
- **Secondary**: `#f72585` (Accent Pink)
- **Success**: `#06d6a0` (Success Green)
- **Warning**: `#ffd166` (Warning Yellow)
- **Background**: `#f8f9fa` (Light Gray)

### Typography
- **Font Family**: Inter, Segoe UI, Roboto, Helvetica
- **Base Size**: 14px
- **Headings**: 28px-48px with bold weights
- **Body**: 14px-20px with regular weights

## 🔧 Technical Specifications

### Performance Features
- **Parallel Processing**: Multi-threaded recommendation engine
- **Efficient Filtering**: Optimized course filtering algorithms
- **Memory Management**: Serializable objects for data persistence
- **Responsive UI**: Smooth transitions and hover effects

### Security Features
- **Password Hashing**: Base64 encoding for password storage
- **Input Validation**: Comprehensive validation for all user inputs
- **Email Validation**: Regex-based email format checking
- **Error Handling**: Robust exception handling throughout the application

## 📈 Recommendation Algorithm

The recommendation engine uses a sophisticated multi-factor scoring system:

```java
Score = (Rating × 0.4) + (Recency × 0.3) + (Popularity × 0.3) + (Interest Match × 0.2)
```

**Factors:**
- **Rating Score**: Course average rating normalized to 0-1
- **Recency Score**: Exponential decay based on course age
- **Popularity Score**: Based on enrollment count
- **Interest Bonus**: Additional weight for matching user interests

## 📊 Data Management

### User Data Storage
- Serialized user profiles with preferences
- Course enrollment and completion tracking
- Rating history with timestamps
- Progress tracking per course

### Course Database
- Comprehensive course metadata
- Category and difficulty classification
- User ratings and reviews
- Enrollment statistics

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Sharawey74**
- GitHub: [@Sharawey74](https://github.com/Sharawey74)

## 🙏 Acknowledgments

- JavaFX community for UI framework
- Font Awesome for icons in HTML reports
- Modern CSS techniques for glass morphism effects

---

**© 2024 Course Recommendation System** - Built with ❤️ using Java & JavaFX
