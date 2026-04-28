# Programming 3 assignment

Coursework for programming 3 course.

The project implements a secure, multi-threaded Java HTTPS server. Main use for storing astronomical observation data. Code written by me found under src/main/java/com/o3/server and a couple modifications to pom.xml

Short summary of classes:

Server.java: HTTPS configuration / entry point

MessageDatabase.java: Handles JDBC/SQLite interactions

ObservationRecord.java: Data model for the astronomical data

UserAuthenticator.java: Manages secure user access.

RegistrationHandler.java: Manages account creation

CollectionsHandler.java: Manages logical grouping for observation records.

User.java: Data model for a user

**Language:** Java

**Database:** SQLite via Java Database Connectivity (JDBC) for persistent storage

**Networking:** com.sun.net.httpserver for handling traffic

**JSON Processing:** org.json for parsing and constructing API responses

