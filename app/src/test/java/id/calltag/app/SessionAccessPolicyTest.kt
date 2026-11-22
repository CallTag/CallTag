package id.calltag.app

import id.calltag.app.data.preferences.SessionAccessPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAccessPolicyTest {
    @Test fun legacyOrStaleTokensDoNotTurnGuestIntoAccount() {
        assertFalse(SessionAccessPolicy.isLoggedIn(explicitLogin = false, accessToken = "legacy-token"))
        assertFalse(SessionAccessPolicy.isLoggedIn(explicitLogin = true, accessToken = ""))
        assertTrue(SessionAccessPolicy.isLoggedIn(explicitLogin = true, accessToken = "access-token"))
    }

    @Test fun outboxRequiresCurrentExplicitOwnerEvenAfterAnotherLogin() {
        assertFalse(SessionAccessPolicy.canAccessOwnedReport("", "account-a"))
        assertFalse(SessionAccessPolicy.canAccessOwnedReport("account-b", "account-a"))
        assertFalse(SessionAccessPolicy.canAccessOwnedReport("account-b", ""))
        assertTrue(SessionAccessPolicy.canAccessOwnedReport("account-a", "account-a"))
    }
}
