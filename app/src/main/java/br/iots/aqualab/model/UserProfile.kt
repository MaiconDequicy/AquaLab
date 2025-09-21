package br.iots.aqualab.model

enum class UserRole
{
    COMMON,
    RESEARCHER
}

enum class RequestStatus
{
    PENDING,
    ACCEPTED,
    REJECTED
}

data class UserProfile(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val role: UserRole = UserRole.COMMON,
    val requestedRole: UserRole? = null,
    val roleRequestStatus: RequestStatus? = null
)
