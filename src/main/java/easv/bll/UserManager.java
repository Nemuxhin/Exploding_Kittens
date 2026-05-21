package easv.bll;

import easv.be.User;

public class UserManager {

    public record AccountInput(
            String name,
            String username,
            String email,
            String newPassword
    ) {}

    private AdminManager adminManager;

    private AdminManager getAdminManager() {
        if (adminManager == null) {
            // Load the heavy admin data only when account settings really need it.
            adminManager = new AdminManager();
        }

        return adminManager;
    }

    public User getCurrentAccount() {
        User sessionUser = UserSession.getCurrentUser();

        if (sessionUser == null) {
            return null;
        }

        return getAdminManager().getUsers().stream()
                .filter(user -> user.getId() == sessionUser.getId())
                .findFirst()
                .orElse(sessionUser);
    }

    public User updateCurrentAccount(AccountInput input) {
        User account = getCurrentAccount();

        if (account == null) {
            throw new IllegalStateException("Could not find the current account.");
        }

        User updatedUser = getAdminManager().updateUser(
                account.getId(),
                new AdminManager.UserInput(
                        input.name(),
                        input.username(),
                        input.email(),
                        account.getRole(),
                        account.getStatus(),
                        account.getAssignedProfiles(),
                        input.newPassword()
                )
        );

        UserSession.setCurrentUser(updatedUser);
        return updatedUser;
    }
}
