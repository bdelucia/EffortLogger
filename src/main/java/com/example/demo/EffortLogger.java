package com.example.demo;

import javafx.application.Application;
import java.sql.*;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class EffortLogger extends Application {
    String loggedInName;
    private TextField taskNameField;
    private ComboBox<String> taskTypeComboBox;
    private ComboBox<Integer> effortPointsComboBox;
    private Label timerLabel;
    private static final String databaseUrl = "jdbc:sqlite:Effortdb.db";
    private Scene loginScene;
    private Instant startTime;
    private Instant endTime;
public class User {
        private int id;
        private String username;
        private String password;
        public User(int id, String username, String password) {
            this.id = id;
            this.username = username;
            this.password = password;
        }
        public int getId() {
            return id;
        }
        public String getUsername() {
            return username;
        }
        public String getPassword() {
            return password;
        }
    }
    public class Log {
        private int id;
        private String taskName;
        private String taskType;
        private double duration;
        private int effortPoints;
        public Log(int id, String taskName, String taskType, double duration, int effortPoints) {
            this.id = id;
            this.taskName = taskName;
            this.taskType = taskType;
            this.duration = duration;
            this.effortPoints = effortPoints;
        }
        public int getId() {
            return id;
        }
        public String getTaskName() {
            return taskName;
        }
        public String getTaskType() {
            return taskType;
        }
        public double getDuration() {
            return duration;
        }
        public int getEffortPoints() {
            return effortPoints;
        }
    }

    static final String createUsersTable = "CREATE TABLE IF NOT EXISTS users "
            + "(id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "username TEXT NOT NULL, "
            + "password TEXT NOT NULL)";

    static final String createLogsTable = "CREATE TABLE IF NOT EXISTS logs "
            + "(id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "taskName TEXT NOT NULL, "
            + "taskType TEXT NOT NULL, "
            + "duration REAL NOT NULL, "
            + "effortPoints INTEGER NOT NULL)";

    public static void createUser(Connection conn, String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Error: username and password cannot be empty.");
            return;
        }

        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            System.out.println("User created successfully.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void printTable(Connection conn, String query, String tableName) throws SQLException {
        try {
            // Execute query to count rows in table
            String countQuery = "SELECT COUNT(*) AS count FROM " + tableName;
            Statement countStmt = conn.createStatement();
            ResultSet countRs = countStmt.executeQuery(countQuery);
            int rowCount = countRs.getInt("count");

            if (rowCount > 0) {
                // Execute query to retrieve all rows from table
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);

                // Print column headers
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(rsmd.getColumnName(i) + "\t");
                }
                System.out.println();

                // Print rows
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        System.out.print(rs.getString(i) + "\t");
                    }
                    System.out.println();
                }
            } else {
                System.out.println("No rows found in " + tableName + ".");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        conn.close();
    }
    public static void clearTable(Connection conn, String tableName) {
        try {
            // Execute delete statement to clear table
            String deleteQuery = "DELETE FROM " + tableName;
            Statement stmt = conn.createStatement();
            int numRowsDeleted = stmt.executeUpdate(deleteQuery);
            System.out.println(numRowsDeleted + " rows deleted from " + tableName + ".");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // AYDEN - added global variables
    private Timeline timeline;
    private int secondsElapsed;
    private Set<String> elements = new HashSet<>();
    @Override
    //AYDEN Helped start stuff
    public void start(Stage primaryStage) {

        // Login form
        GridPane loginPane = createLoginForm();
        loginScene = new Scene(loginPane, 500, 300);

        // Main form
        VBox mainLayout = createMainLayout(primaryStage);
        Scene mainScene = new Scene(mainLayout, 400, 300);

        primaryStage.setTitle("Effort Logger");
        primaryStage.setScene(loginScene);
        primaryStage.show();

        // GEORGE - I added this to make the login form look better
        // Handle login
        Button loginButton = (Button) loginPane.lookup("#loginButton");
        loginButton.setOnAction(e -> {
            TextField usernameField = (TextField) loginPane.lookup("#usernameField");
            PasswordField passwordField = (PasswordField) loginPane.lookup("#passwordField");

            String username = usernameField.getText();
            String password = passwordField.getText();

            if (isValid(username, password)) {
                loggedInName = username;
                resetUI(taskNameField, taskTypeComboBox, effortPointsComboBox, timerLabel);
                primaryStage.setScene(mainScene);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login Error");
                alert.setHeaderText("Invalid username or password.");
                alert.showAndWait();
            }
        });


        /*
        // Delete user popup
        Button deleteUserButton = (Button) loginPane.lookup("#deleteUserButton");
        if (deleteUserButton != null) {
            deleteUserButton.setOnAction(e -> {
                try {
                    Connection conn = DriverManager.getConnection(databaseUrl);
                    createDeleteUserPopup(conn);
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            });
        }

         */
    }
    private void createDeleteUserPopup(Connection conn) {
        try {
            // Create popup window
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initOwner(timerLabel.getScene().getWindow());
            popup.setTitle("Delete User");

            // Create list view of users
            ObservableList<String> usersList = FXCollections.observableArrayList();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");
            while (rs.next()) {
                usersList.add(rs.getString("username"));
            }
            ListView<String> usersListView = new ListView<>(usersList);

            // Create delete button
            Button deleteButton = new Button("Delete");
            deleteButton.setOnAction(event -> {
                String selectedUser = usersListView.getSelectionModel().getSelectedItem();
                if (selectedUser != null) {
                    try {
                        PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE username = ?");
                        pstmt.setString(1, selectedUser);
                        pstmt.executeUpdate();
                        usersList.remove(selectedUser);
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("User Deleted");
                        alert.setHeaderText(null);
                        alert.setContentText("User " + selectedUser + " has been deleted.");
                        alert.showAndWait();
                    } catch (SQLException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText(null);
                        alert.setContentText("An error occurred while deleting the user.");
                        alert.showAndWait();
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("No User Selected");
                    alert.setHeaderText(null);
                    alert.setContentText("Please select a user to delete.");
                    alert.showAndWait();
                }
            });

            // Add list view and delete button to popup window
            VBox popupLayout = new VBox(10, usersListView, deleteButton);
            popupLayout.setPadding(new Insets(10));
            popup.setScene(new Scene(popupLayout));
            popup.showAndWait();
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("An error occurred while retrieving user data.");
            alert.showAndWait();
        }
    }

    //SERGIO CODE STARTS HERE
    private GridPane createLoginForm() {
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        Label usernameLabel = new Label("Username:");
        TextField usernameField = new TextField();
        usernameField.setId("usernameField");
        gridPane.add(usernameLabel, 0, 0);
        gridPane.add(usernameField, 1, 0);

        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        passwordField.setId("passwordField");
        gridPane.add(passwordLabel, 0, 1);
        gridPane.add(passwordField, 1, 1);

        Button loginButton = new Button("Login");
        loginButton.setId("loginButton");
        gridPane.add(loginButton, 1, 2);

        Button createUserButton = new Button("Create User");
        createUserButton.setOnAction(e -> {
            String newUsername = usernameField.getText();
            String newPassword = passwordField.getText();

            try (Connection conn = DriverManager.getConnection(databaseUrl)) {
                createUser(conn, newUsername, newPassword);
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        });
        gridPane.add(createUserButton, 1, 3);
        /*
        Button printUsersButton = new Button("Print Users Table");
        printUsersButton.setOnAction(e -> {
            try {
                Connection conn = DriverManager.getConnection(databaseUrl);
                printTable(conn, "SELECT * FROM users", "users");
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        });
        gridPane.add(printUsersButton, 1, 4);

        Button deleteUserButton = new Button("Delete User");
        deleteUserButton.setOnAction(e -> {
                    try {
                        Connection conn = DriverManager.getConnection(databaseUrl);
                        createDeleteUserPopup(conn);
                    } catch (SQLException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
        );
        gridPane.add(deleteUserButton, 1, 5);

         */
        return gridPane;
    }

    private boolean isValid(String username, String password) {
        String sql = "SELECT * FROM users";
        try (Connection conn = DriverManager.getConnection(databaseUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String dbUsername = rs.getString("username");
                String dbPassword = rs.getString("password");

                if (dbUsername.equals(username) && dbPassword.equals(password)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return false;
    }
    private VBox createMainLayout(Stage primaryStage) {
        // Task Name input field
        taskNameField = new TextField();
        taskNameField.setPromptText("Enter task name");

        // Dropdown menu for selecting task type
        // DRAKE CODE STARTS HERE (added a new button/DROPDOWNS)
        taskTypeComboBox = new ComboBox<>();
        taskTypeComboBox.setPromptText("Select task type");
        taskTypeComboBox.getItems().addAll("Effort", "Defect");

        taskTypeComboBox.setCellFactory(ComboBoxListCell.forListView(taskTypeComboBox.getItems()));
        taskTypeComboBox.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
            }
        });

        // Dropdown menu for selecting effort points
        effortPointsComboBox = new ComboBox<>();
        effortPointsComboBox.setPromptText("Select effort points");
        effortPointsComboBox.getItems().addAll(1, 2, 4, 8, 16, 32, 64);

        effortPointsComboBox.setCellFactory(ComboBoxListCell.forListView(effortPointsComboBox.getItems()));
        effortPointsComboBox.setButtonCell(new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? null : item.toString());
            }
        });


        // DRAKE CODE STARTS HERE (added a new button)
        Button averageDurationButton = new Button("Average Duration");
        averageDurationButton.setOnAction(e -> {
            int selectedEffortPoints = effortPointsComboBox.getSelectionModel().getSelectedItem();
            double averageDuration = getAverageDurationByEffortPoints(selectedEffortPoints);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Average Duration");
            alert.setHeaderText(null);
            alert.setContentText("Average duration for effort points " + selectedEffortPoints + " is: " + String.format("%.2f", averageDuration) + " seconds.");
            alert.showAndWait();
        });

        // BOBBY MEGA NICE TIMER CODE STARTS HERE
        // Timer
        Button startTimerButton = new Button("Start Timer");
        Button stopTimerButton = new Button("Stop Timer");
        timerLabel = new Label("00:00");
// BOBBY MEGA NICE TIMER CODE STARTS HERE
        startTimerButton.setOnAction(e -> {
            startTime = Instant.now();
            if (timeline != null) {
                timeline.stop();
            }
            startTimer();
        });
// BOBBY MEGA NICE TIMER CODE STARTS HERE
        stopTimerButton.setOnAction(e -> {
            endTime = Instant.now();
            if (timeline != null) {
                timeline.stop();
            }
        });

        Button deleteUserButton = new Button("Delete User");
        if (deleteUserButton != null) {
            deleteUserButton.setOnAction(e -> {
                try {
                    Connection conn = DriverManager.getConnection(databaseUrl);
                    createDeleteUserPopup(conn);
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            });
        }


        // Logout button
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> {
            primaryStage.setScene(loginScene);
        });
        //WALLY CODE STARTS HERE (added a button to send logs to the database)
        // Finish button
        Button finishButton = new Button("Finish");
        finishButton.setOnAction(e -> {
            String taskName = taskNameField.getText();
            String taskType = taskTypeComboBox.getSelectionModel().getSelectedItem();
            int effortPoints = effortPointsComboBox.getSelectionModel().getSelectedItem();
            long duration = getDurationInSeconds();

            if (taskName.isEmpty() || taskType == null || effortPoints == 0 || duration == 0) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("All fields are required");
                alert.showAndWait();
            } else {
                try (Connection conn = DriverManager.getConnection(databaseUrl);
                     PreparedStatement pstmt = conn.prepareStatement("INSERT INTO logs (taskName, taskType, duration, effortPoints) VALUES (?, ?, ?, ?)")) {
                    pstmt.setString(1, taskName);
                    pstmt.setString(2, taskType);
                    pstmt.setLong(3, duration);
                    pstmt.setInt(4, effortPoints);
                    pstmt.executeUpdate();

                    elements.add(taskName);
                    taskNameField.clear();
                    timerLabel.setText("00:00");
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        });

        //wally  - get logs from database
        // Get Logs button
        Button getLogsButton = new Button("Get Logs");
        getLogsButton.setOnAction(e -> {
            createLogsPopup();
        });



        HBox timerLayout = new HBox(10, startTimerButton, stopTimerButton, timerLabel);
        HBox buttonLayout = new HBox(10, averageDurationButton,finishButton, getLogsButton, logoutButton);
        HBox.setHgrow(finishButton, Priority.ALWAYS);
        finishButton.setMaxWidth(Double.MAX_VALUE);

        VBox mainLayout = new VBox(10, taskNameField, taskTypeComboBox, effortPointsComboBox, timerLayout, buttonLayout);
        mainLayout.setPadding(new Insets(10));

        // Making buttons and dropdown menu resizable with the window
        AnchorPane mainAnchorPane = new AnchorPane(mainLayout);
        AnchorPane.setTopAnchor(mainLayout, 0.0);
        AnchorPane.setBottomAnchor(mainLayout, 0.0);
        AnchorPane.setLeftAnchor(mainLayout, 0.0);
        AnchorPane.setRightAnchor(mainLayout, 0.0);

        return new VBox(mainAnchorPane);
    }
    private void resetUI(TextField taskNameField, ComboBox<String> taskTypeComboBox, ComboBox<Integer> effortPointsComboBox, Label timerLabel) {
        taskNameField.clear();
        taskTypeComboBox.getSelectionModel().clearSelection();
        effortPointsComboBox.getSelectionModel().clearSelection();
        timerLabel.setText("00:00");
    }
    public class LogEntry {
        private final SimpleStringProperty taskName;
        private final SimpleStringProperty taskType;
        private final SimpleDoubleProperty duration;
        private final SimpleIntegerProperty effortPoints;

        public LogEntry(String taskName, String taskType, double duration, int effortPoints) {
            this.taskName = new SimpleStringProperty(taskName);
            this.taskType = new SimpleStringProperty(taskType);
            this.duration = new SimpleDoubleProperty(duration);
            this.effortPoints = new SimpleIntegerProperty(effortPoints);
        }

        public String getTaskName() {
            return taskName.get();
        }

        public String getTaskType() {
            return taskType.get();
        }

        public double getDuration() {
            return duration.get();
        }

        public int getEffortPoints() {
            return effortPoints.get();
        }
    }
    private void createLogsPopup() {
        // Create popup window
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(timerLabel.getScene().getWindow());
        popup.setTitle("Logs");
        popup.setWidth(600);

        // Create filter dropdown
        ComboBox<String> filterComboBox = new ComboBox<>();
        filterComboBox.getItems().addAll("All", "Effort", "Defect");
        filterComboBox.setValue("All");

        // Create TableView
        TableView<LogEntry> logsTableView = new TableView<>();

        TableColumn<LogEntry, String> taskNameColumn = new TableColumn<>("Name");
        taskNameColumn.setCellValueFactory(new PropertyValueFactory<>("taskName"));
        taskNameColumn.setMinWidth(150);

        TableColumn<LogEntry, String> taskTypeColumn = new TableColumn<>("Type");
        taskTypeColumn.setCellValueFactory(new PropertyValueFactory<>("taskType"));
        taskTypeColumn.setMinWidth(150);

        TableColumn<LogEntry, Double> durationColumn = new TableColumn<>("Duration");
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));
        durationColumn.setMinWidth(150);

        TableColumn<LogEntry, Integer> effortPointsColumn = new TableColumn<>("Effort Points");
        effortPointsColumn.setCellValueFactory(new PropertyValueFactory<>("effortPoints"));
        effortPointsColumn.setMinWidth(150);

        logsTableView.getColumns().addAll(taskNameColumn, taskTypeColumn, durationColumn, effortPointsColumn);

        // Add filter functionality
        filterComboBox.setOnAction(e -> {
            String filter = filterComboBox.getSelectionModel().getSelectedItem();
            loadLogs(logsTableView, filter);
        });

        // Load logs
        loadLogs(logsTableView, "All");

        // Add filter dropdown and TableView to popup window
        VBox popupLayout = new VBox(10, filterComboBox, logsTableView);
        popupLayout.setPadding(new Insets(10));
        popup.setScene(new Scene(popupLayout));
        popup.showAndWait();
    }
    private void loadLogs(TableView<LogEntry> logsTableView, String filter) {
        try (Connection conn = DriverManager.getConnection(databaseUrl)) {
            ObservableList<LogEntry> logsList = FXCollections.observableArrayList();
            String query = "SELECT * FROM logs";

            if (!filter.equals("All")) {
                query += " WHERE taskType = ?";
            }

            PreparedStatement pstmt = conn.prepareStatement(query);

            if (!filter.equals("All")) {
                pstmt.setString(1, filter);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String taskName = rs.getString("taskName");
                String taskType = rs.getString("taskType");
                double duration = rs.getDouble("duration");
                int effortPoints = rs.getInt("effortPoints");

                LogEntry logEntry = new LogEntry(taskName, taskType, duration, effortPoints);
                logsList.add(logEntry);
            }

            logsTableView.setItems(logsList);
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }
    private double getAverageDurationByEffortPoints(int effortPoints) {
        String query = "SELECT AVG(duration) FROM logs WHERE effortPoints = ?";
        double averageDuration = 0;

        try (Connection conn = DriverManager.getConnection(databaseUrl);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, effortPoints);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                averageDuration = rs.getDouble(1);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return averageDuration;
    }

    private long getDurationInSeconds() {
        if (startTime != null && endTime != null) {
            return ChronoUnit.SECONDS.between(startTime, endTime);
        }
        return 0;
    }

    // BOBBY MEGA NICE TIMER CODE STARTS HERE
    private void startTimer () {
        secondsElapsed = 0;
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            updateTimerLabel();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
    // BOBBY MEGA NICE TIMER CODE STARTS HERE
    private void updateTimerLabel () {
        int minutes = secondsElapsed / 60;
        int seconds = secondsElapsed % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    public static void main (String[]args) throws SQLException {
        String databaseUrl = "jdbc:sqlite:Effortdb.db";
        Connection conn = DriverManager.getConnection(databaseUrl);
        Statement stmt = conn.createStatement();
        stmt.execute(createUsersTable);
        stmt.execute(createLogsTable);
        launch(args);
    }
}


/*
import java.sql.*;

public class EffortLogger {
    public class User {
        private int id;
        private String username;
        private String password;
        public User(int id, String username, String password) {
            this.id = id;
            this.username = username;
            this.password = password;
        }
        public int getId() {
            return id;
        }
        public String getUsername() {
            return username;
        }
        public String getPassword() {
            return password;
        }
    }
    public class Log {
        private int id;
        private String taskName;
        private String taskType;
        private double duration;
        private int effortPoints;
        public Log(int id, String taskName, String taskType, double duration, int effortPoints) {
            this.id = id;
            this.taskName = taskName;
            this.taskType = taskType;
            this.duration = duration;
            this.effortPoints = effortPoints;
        }
        public int getId() {
            return id;
        }
        public String getTaskName() {
            return taskName;
        }
        public String getTaskType() {
            return taskType;
        }
        public double getDuration() {
            return duration;
        }
        public int getEffortPoints() {
            return effortPoints;
        }
    }

    static final String createUsersTable = "CREATE TABLE IF NOT EXISTS users "
            + "(id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "username TEXT NOT NULL, "
            + "password TEXT NOT NULL)";

    static final String createLogsTable = "CREATE TABLE IF NOT EXISTS logs "
            + "(id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "taskName TEXT NOT NULL, "
            + "taskType TEXT NOT NULL, "
            + "duration REAL NOT NULL, "
            + "effortPoints INTEGER NOT NULL)";

    public static void createUser(Connection conn, String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            System.out.println("User created successfully.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void printTable(Connection conn, String query, String tableName) {
        try {
            // Execute query to count rows in table
            String countQuery = "SELECT COUNT(*) AS count FROM " + tableName;
            Statement countStmt = conn.createStatement();
            ResultSet countRs = countStmt.executeQuery(countQuery);
            int rowCount = countRs.getInt("count");

            if (rowCount > 0) {
                // Execute query to retrieve all rows from table
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);

                // Print column headers
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(rsmd.getColumnName(i) + "\t");
                }
                System.out.println();

                // Print rows
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        System.out.print(rs.getString(i) + "\t");
                    }
                    System.out.println();
                }
            } else {
                System.out.println("No rows found in " + tableName + ".");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public static void clearTable(Connection conn, String tableName) {
        try {
            // Execute delete statement to clear table
            String deleteQuery = "DELETE FROM " + tableName;
            Statement stmt = conn.createStatement();
            int numRowsDeleted = stmt.executeUpdate(deleteQuery);
            System.out.println(numRowsDeleted + " rows deleted from " + tableName + ".");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public static void main(String[] args) {
        Connection conn = null;
        try {
            // db parameters
            String url = "jdbc:sqlite:C:\\Users\\ezman\\OneDrive\\Desktop\\EffortLoggerDatabase\\Effortdb.db";
            // create a connection to the database
            conn = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite has been established.");
            Statement stmt = conn.createStatement();
            stmt.execute(createUsersTable);
            stmt.execute(createLogsTable);

            // Create a new user
            createUser(conn, "wally", "password");

            // Create a new log
            String sql = "INSERT INTO logs (taskName, taskType, duration, effortPoints) VALUES ('test', 'effort', 20, 2)";
            stmt.executeUpdate(sql);
            System.out.println("Log created successfully.");

            printTable(conn, "SELECT * FROM logs", "logs");
            printTable(conn, "SELECT * FROM users", "users");

            //clearTable(conn, "logs");
            //clearTable(conn, "users");

            //printTable(conn, "SELECT * FROM logs", "logs");
            //printTable(conn, "SELECT * FROM users", "users");

            //button here
            createUser(conn, "drake", "password");
            printTable(conn, "SELECT * FROM users", "users");



       } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}

 */