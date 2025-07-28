package researchstack.domain.validator

object PasswordValidator {
    private val digitRegex = Regex(".*\\d.*")
    private val specialRegex = Regex(".*[^A-Za-z0-9].*")

    fun validate(password: String): ValidationResult {
        if (password.isBlank()) {
            return ValidationResult(false, "Password is required")
        }
        if (password.length < 12) {
            return ValidationResult(false, "Password must be at least 12 characters")
        }
        if (!digitRegex.containsMatchIn(password)) {
            return ValidationResult(false, "Password must contain at least one digit")
        }
        if (!specialRegex.containsMatchIn(password)) {
            return ValidationResult(false, "Password must contain at least one special character")
        }
        return ValidationResult(true)
    }
}
