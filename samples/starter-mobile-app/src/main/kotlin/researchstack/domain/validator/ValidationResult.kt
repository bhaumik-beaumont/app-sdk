package researchstack.domain.validator

data class ValidationResult(
    val successful: Boolean,
    val errorMessage: String? = null
)
