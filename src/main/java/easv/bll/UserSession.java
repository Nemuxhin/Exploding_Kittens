package easv.bll;

import easv.be.User;

/**
 * This class keeps track of the currently logged-in user.
 * Later features can ask this class "who is using the system right now?"
 */
public final class UserSession {

    private static User currentUser;

    private UserSession() {
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean hasCurrentUser() {
        return currentUser != null;
    }

    public static void clearCurrentUser() {
        currentUser = null;
    }
}
