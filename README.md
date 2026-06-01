Installation Guide
==================
This appendix explains how the project can be setup and ran by the user the system is submitted as an IntelliJ project, as required by the exam specifications.
Project Setup
-------------
The application is built with JavaFX. Maven is used for dependency management, and Microsoft SQL Server is used as the database.
The main application class is:
easv.gui.MainApp
Before running the application the user needs the following:
- IntelliJ IDEA
- JDK 17 or newer
- Access to the school network or VPN
- Internet access
The school network or VPN is needed because the application connects to the school's SQL Server database. Internet access is also needed because the scanning functionality fetches TIFF files from an WebLager student TIFF API.
Opening the Project in IntelliJ
-------------------------------
To open the project, start IntelliJ IDEA and choose File > Open.
Then select the Exploding_Kittens project folder.  IntelliJ should automatically detect the pom.xml file and import the project as a Maven project.
After the project has been opened, make sure that IntelliJ uses JDK 17 or newer. If the Maven dependencies are not loaded automatically, reload the Maven project from the Maven tool window.
Database Configuration
----------------------
The application connects to the school's MSSQL database. To configure the database connection, rename the premade database connection file called database.properties.placeholder to database.properties in the root of the project folder.
The file should contain everything needed all that will need to be changed is the username and password which are hashed out to secure the database. This information is just below.

exploding-kittens.db.user=CS2025b_e_17
exploding-kittens.db.password=CS2025bE17#23

Scanner API
-----------
The scanning feature uses the WebLager student TIFF API. The default API address is already configured in the application:
https://studentiffapi-production.up.railway.app/
The API is used to fetch TIFF files during the scanning workflow. If the API is unavailable, the scanning function may fail. However, the rest of the application can still be opened as long as the database connection works.
Running the Application
-----------------------
The easiest way to run the application is directly from IntelliJ.
First, open the Maven tool window and reload the Maven project. Then run the JavaFX Maven goal:
javafx:run
The application can also be started by creating an IntelliJ run configuration with this main class:
easv.gui.MainApp
If Maven is installed locally, the application can also be started from the terminal with this command:
mvn clean javafx:run
Login Information
-----------------
The default administrator account is:
Username: admin
Password: admin123
The default user account is:
Username: user
Password: user123
The administrator account can be used to access the admin portal, where the examiner can inspect users, profiles, review functionality, and activity logs.
The user account can be used to access the user portal, where the user can test the scanning workflow, review scanned pages, use shortcuts, and export TIFF files.
Testing the Project
-------------------
The project includes JUnit tests. They can be run from IntelliJ or from the Maven tool window by running the test goal.
If Maven is installed locally, the tests can also be run from the terminal with this command:
mvn test
Some parts of the application and some tests depend on the real database connection. Therefore, the database configuration should be completed before running the full project.
Basic Check After Installation
------------------------------
After logging in, the user can check the main parts of the system by opening both the admin portal and the user portal.
In the admin portal, the user will be able to manage users, manage profiles, review scans, and inspect activity logs.
In the user portal, the user can check the scanning workflow by starting a scan, fetching TIFF files, reviewing scanned pages, using rotation and shortcut functions, and exporting TIFF files when the scan is ready.
Troubleshooting
---------------
If the login does not work a likely reason is that the database.properties file is missing or that the database connection is not available. The user should check the database username, password, VPN connection, and access to the SQL Server address.
If the JavaFX application does not start, the user should check that the project is using JDK 17 or newer and that Maven dependencies have been downloaded correctly.
If scanning does not work, the user should check the internet connection and whether the WebLager student TIFF API is reachable.
