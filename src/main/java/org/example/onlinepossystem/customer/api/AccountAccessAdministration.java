package org.example.onlinepossystem.customer.api;

import java.util.List;

public interface AccountAccessAdministration {
    List<AccountAccessSummary> search(String query);

    void assignAccessLevel(Long customerId, int accessLevel);
}
