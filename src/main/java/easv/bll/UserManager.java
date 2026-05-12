package easv.bll;

import easv.be.User;

public class UserManager {

    public record AccountInput(
            String name,
            String username,
            String email,
            String newPassword
    ) {}

    private final AdminManager adminManager = new AdminManager();

    public User getCurrentAccount() {
        User sessionUser = UserSession.getCurrentUser();

        if (sessionUser == null) {
            return null;
        }

        return adminManager.getUsers().stream()
                .filter(user -> user.getId() == sessionUser.getId())
                .findFirst()
                .orElse(sessionUser);
    }

    public User updateCurrentAccount(AccountInput input) {
        User account = getCurrentAccount();

        if (account == null) {
            throw new IllegalStateException("Could not find the current account.");
        }

        User updatedUser = adminManager.updateUser(
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