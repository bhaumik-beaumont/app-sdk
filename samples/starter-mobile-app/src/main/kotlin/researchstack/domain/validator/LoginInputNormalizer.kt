package researchstack.domain.validator

/** Normalizes login input by appending a default domain when missing. */
object LoginInputNormalizer {
    private const val DEFAULT_DOMAIN = "@gmail.com"

    /**
     * Returns [input] unchanged if it already contains a domain. Otherwise, appends
     * [DEFAULT_DOMAIN] to treat [input] as a username.
     */
    fun normalize(input: String): String {
        return if (input.contains('@')) input else input + DEFAULT_DOMAIN
    }
}
