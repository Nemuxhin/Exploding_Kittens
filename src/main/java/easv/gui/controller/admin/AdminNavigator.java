package easv.gui.controller.admin;

interface AdminNavigator {

    static AdminNavigator none() {
        return NoOpAdminNavigator.INSTANCE;
    }

    void showDashboard();

    void showUsers();

    void showProfiles();

    void showAssignments();

    void showMetadataTemplates();

    void showMetadataReview();

    void showActivity();

    enum NoOpAdminNavigator implements AdminNavigator {
        INSTANCE;

        @Override
        public void showDashboard() {
        }

        @Override
        public void showUsers() {
        }

        @Override
        public void showProfiles() {
        }

        @Override
        public void showAssignments() {
        }

        @Override
        public void showMetadataTemplates() {
        }

        @Override
        public void showMetadataReview() {
        }

        @Override
        public void showActivity() {
        }
    }
}
