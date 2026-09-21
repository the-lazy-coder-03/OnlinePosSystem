package org.example.onlinepossystem.customer.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountAccessTest {

    @Test
    void mapsAllFiveLevelsToTheirExpectedScope() {
        assertThat(new AccountAccess(0).isAdmin()).isFalse();
        assertThat(new AccountAccess(1).branchId()).isEqualTo(1);
        assertThat(new AccountAccess(2).branchId()).isEqualTo(2);
        assertThat(new AccountAccess(3).isSuperAdmin()).isTrue();
        assertThat(new AccountAccess(3).canAccessBranch(1)).isTrue();
        assertThat(new AccountAccess(3).canAccessBranch(2)).isTrue();
        assertThat(new AccountAccess(4).isAdmin()).isFalse();
        assertThat(new AccountAccess(4).canAccessBranch(1)).isFalse();
    }

    @Test
    void rejectsValuesOutsideTheDatabaseConstraint() {
        assertThatThrownBy(() -> new AccountAccess(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AccountAccess(5)).isInstanceOf(IllegalArgumentException.class);
    }
}
