package org.example.onlinepossystem.customer.api;

public record AccountAccess(int level) {
    public AccountAccess {
        if (level < 0 || level > 4) {
            throw new IllegalArgumentException("Access level must be between 0 and 4.");
        }
    }

    public boolean isAdmin() {
        return level >= 1 && level <= 3;
    }

    public boolean isSuperAdmin() {
        return level == 3;
    }

    public Integer branchId() {
        return level == 1 || level == 2 ? level : null;
    }

    public boolean canAccessBranch(Integer requestedBranchId) {
        return isSuperAdmin() || (branchId() != null && branchId().equals(requestedBranchId));
    }
}
