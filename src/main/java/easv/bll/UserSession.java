package easv.bll;

import easv.be.User;

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
