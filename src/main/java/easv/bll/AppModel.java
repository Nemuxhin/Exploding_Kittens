package easv.bll;

import easv.be.AuditLog;
import easv.be.ScanProfile;
import easv.be.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * BLL — The MVC Model of the application.
 *
 * WHAT IS THE MODEL? (for the exam):
 * ───────────────────────────────────
 * In MVC, the Model holds the application data and NOTIFIES the View
 * when data changes. In JavaFX, we do this with ObservableList.
 *
 * When we add/remove items from an ObservableList, any UI component
 * that is bound to it (like a TableView or ListView) updates AUTOMATICALLY.
 *
 * ARCHITECTURE:
 *
 *   ┌─────────────────────────────────────────────┐
 *   │  VIEW (FXML)                                │
 *   │  Displays data, listens for changes         │
 *   │           ↑ auto-updates                    │
 *   │  ─────────┼────────────────────────         │
 *   │  MODEL (AppModel) ← ObservableList          │
 *   │  Holds data, notifies View                  │
 *   │           ↑ calls                           │
 *   │  ─────────┼────────────────────────         │
 *   │  BLL (AdminManager)                         │
 *   │  Business logic, validation                 │
 *   │           ↑ calls                           │
 *   │  ─────────┼────────────────────────         │
 *   │  DAL (UserDAO, MetadataDAO...)              │
 *   │  Raw database access                        │
 *   └─────────────────────────────────────────────┘
 *
 * USAGE IN CONTROLLER:
 *   // The controller gets data from the model
 *   ObservableList<User> users = appModel.getUsers();
 *   tableView.setItems(users);  // auto-updates when users change!
 *
 *   // The controller asks the model to change data
 *   appModel.createUser(input);  // list updates automatically
 */
public class AppModel {

    // ── Observable data — Views bind to these and auto-update ────────
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObservableList<ScanProfile> profiles = FXCollections.observableArrayList();
    private final ObservableList<AuditLog> auditLogs = FXCollections.observableArrayList();

    // ── BLL dependency — all logic is delegated here ────────────────
    private final AdminManager adminManager;

    /** Default constructor — creates its own AdminManager. */
    public AppModel() {
        this(new AdminManager());
    }

    /** Test constructor — allows injecting a custom AdminManager. */
    public AppModel(AdminManager adminManager) {
        this.adminManager = adminManager;
        refresh();
    }

    // ── Observable getters — bind these to your UI components ───────

    /** Returns the observable list of users. Bind this to a TableView. */
    public ObservableList<User> getUsers() { return users; }

    /** Returns the observable list of scan profiles. */
    public ObservableList<ScanProfile> getProfiles() { return profiles; }

    /** Returns the observable list of audit logs. */
    public ObservableList<AuditLog> getAuditLogs() { return auditLogs; }

    // ── Data operations — modify data through here ──────────────────

    /** Reloads all data from the BLL into the observable lists. */
    public void refresh() {
        users.setAll(adminManager.getUsers());
        profiles.setAll(adminManager.getProfiles());
        auditLogs.setAll(adminManager.getAuditLogs());
    }

    /** Creates a new user and adds it to the observable list. */
    public User createUser(AdminManager.UserInput input) {
        User created = adminManager.createUser(input);
        users.setAll(adminManager.getUsers());  // refresh to get sorted list
        return created;
    }

    /** Updates a user and refreshes the observable list. */
    public User updateUser(int userId, AdminManager.UserInput input) {
        User updated = adminManager.updateUser(userId, input);
        users.setAll(adminManager.getUsers());
        return updated;
    }

    /** Deletes a user and removes it from the observable list. */
    public void deleteUser(int userId) {
        adminManager.deleteUser(userId);
        users.setAll(adminManager.getUsers());
    }

    /** Returns the dashboard summary statistics. */
    public AdminManager.DashboardSummary getDashboardSummary() {
        return adminManager.getDashboardSummary();
    }

    /** Returns the underlying AdminManager for advanced operations. */
    public AdminManager getAdminManager() {
        return adminManager;
    }
}
