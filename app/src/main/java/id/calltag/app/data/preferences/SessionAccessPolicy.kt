package id.calltag.app.data.preferences

/** Token lama tidak memberi hak akun tanpa pilihan masuk yang eksplisit. */
object SessionAccessPolicy {
    fun isLoggedIn(explicitLogin: Boolean, accessToken: String): Boolean =
        explicitLogin && accessToken.isNotBlank()

    fun canAccessOwnedReport(currentOwner: String, reportOwner: String): Boolean =
        currentOwner.isNotBlank() && reportOwner.isNotBlank() && currentOwner == reportOwner
}
