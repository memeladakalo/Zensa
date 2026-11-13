# Zensa – Student Productivity & Study Management App

**Tagline: Calm, Focused Studying.**

## 📖 About the Project

Zensa is an Android-based student productivity and study management application designed to help students organise their academic responsibilities, manage assignments, create study notes, track progress, and develop consistent study habits.

The name **Zensa** combines the ideas of *Zen* and *Sense*, representing a calm, organised, and focused approach to studying.

The application provides students with a centralised platform for managing their academic activities while encouraging productivity through study sessions, progress tracking, and achievements.

## 🎯 Project Objectives

- Provide a centralised platform for managing academic activities.
- Help students organise modules and assignments.
- Allow students to create and manage study notes.
- Encourage effective time management through a study timer.
- Track academic progress and achievements.
- Provide secure user authentication.
- Support Google Single Sign-On (SSO) through Firebase Authentication.
- Enable communication between the Android application and a backend REST API.

## ✨ Key Features

| Feature | Description |
|---|---|
| User Registration | Allows students to create accounts. |
| User Authentication | Supports secure email and password login. |
| Google Single Sign-On (SSO) | Allows users to sign in using their existing Google accounts through Firebase Authentication. |
| Dashboard | Provides a centralised overview of student activities. |
| Modules | Organise and manage academic modules. |
| Assignments | Create, view, and manage assignments. |
| Study Notes | Create and manage study notes. |
| Zen Timer | Use a study timer to support focused learning. |
| Achievements | Track study progress and accomplishments. |
| Settings | Manage application preferences. |
| REST API Integration | Communicate with the backend service. |
| Offline Support | Support selected local data operations and synchronisation. |
| Push Notifications | Support notifications for relevant application updates. |

## 🛠️ Technologies Used

### Android Development
- Kotlin
- Android Studio
- Jetpack Compose
- Android SDK
- Gradle

### Backend Development
- Java
- Spring Boot
- RESTful API
- JSON

### Database and Data Management
- Room Database
- MySQL
- DataStore

### Authentication and Cloud Services
- Firebase Authentication
- Google Sign-In
- Firebase Cloud Messaging

### Additional Tools and Technologies
- Git
- GitHub
- Cloudflare Tunnel (development API connectivity)

## 🔐 Google Single Sign-On (SSO)

Zensa implements Google Single Sign-On (SSO) using Firebase Authentication.

This feature allows students to access the application using their existing Google accounts instead of creating a separate Zensa password.

### Authentication Process

1. The student opens the Zensa login screen.
2. The student selects the Google Sign-In option.
3. Google authenticates the user's account.
4. Firebase Authentication processes the authentication result.
5. Once authentication is successful, the student can access the Zensa application.

This integration provides a convenient authentication experience while reducing the need for users to manage another set of login credentials.

## 🏗️ System Architecture

Zensa follows a client-server architecture.

```text
+-----------------------------+
|       Zensa Android App     |
|        (Kotlin/Compose)     |
+--------------+--------------+
               |
               | HTTPS / JSON
               |
+--------------v--------------+
|       Spring Boot API       |
|       REST Endpoints        |
+--------------+--------------+
               |
               |
+--------------v--------------+
|        MySQL Database       |
+-----------------------------+

       Authentication
               |
               v
+-----------------------------+
|     Google Sign-In          |
+--------------+--------------+
               |
               v
+-----------------------------+
|     Firebase Authentication|
+-----------------------------+
```

The Android application communicates with the backend through REST API endpoints. Firebase Authentication handles Google Sign-In, while local storage supports selected application functionality and data management.

## 🎨 Application Design

Zensa uses a modern and student-friendly interface designed around simplicity, accessibility, and productivity.

**Colour palette:**
- Deep Purple
- Soft Lavender
- Baby Pink
- White

The interface uses rounded cards, clear navigation, and organised layouts to create a calm study environment.

## 📱 Application Screens

The application includes the following screens:

1. Splash Screen
2. Login Screen
3. Registration Screen
4. Home Dashboard
5. Modules
6. Assignments
7. Assignment Details
8. Study Notes
9. Zen Timer
10. Settings
11. Achievements

## ⚙️ Installation and Setup

### Requirements

- Android Studio
- Android SDK
- JDK compatible with the project's Gradle configuration
- Git
- Android device or emulator

### Steps

1. Clone the repository:

```bash
git clone https://github.com/memeladakalo/Zensa-Mobile-App.git
```

2. Open Android Studio.
3. Select **Open** and choose the cloned project folder.
4. Allow Gradle to synchronise.
5. Configure Firebase using the appropriate Firebase project configuration.
6. Configure the backend API URL for your development environment.
7. Start the Spring Boot backend if backend connectivity is required.
8. Connect an Android device or start an emulator.
9. Click **Run** in Android Studio.

**Note:** Replace the repository URL if you choose a different GitHub repository name. Backend environment variables, Firebase configuration, and database credentials must be configured separately. Never commit private credentials, service account files, or secret keys.

## 🔒 Security

Zensa incorporates security considerations including:

- Firebase Authentication for Google Sign-In.
- Secure user authentication.
- Password protection for email and password accounts.
- HTTPS communication with the backend API.
- Controlled access to application functionality.
- Appropriate handling of sensitive configuration values.

## 🚀 Future Improvements

- Improve offline synchronisation.
- Expand study analytics and reporting.
- Introduce additional gamification features.
- Improve notification personalisation.
- Enhance accessibility and user experience.
- Expand automated testing.

## 👩‍💻 Developer

**Thembela Memela**

Final-Year Diploma in Information Technology (Software Development) Student  
IIE Rosebank International University College

GitHub: [@memeladakalo](https://github.com/memeladakalo)

## 📚 Project Purpose

Zensa is an academic software development project focused on applying Android development, database management, API integration, authentication, software engineering, and user interface design principles to solve a practical student productivity problem.

---

**Zensa – Calm, Focused Studying.**
