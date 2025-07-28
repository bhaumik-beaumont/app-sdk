package researchstack.domain.validator

/** Validates email input. */
object EmailValidator {
    // Very small regex to check a basic email format
    private val pattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    fun validate(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult(false, "Email is required")
        }
        if (!pattern.matches(email)) {
            return ValidationResult(false, "Invalid email address")
        }
        return ValidationResult(true)
    }
}
