package researchstack.util.version

object VersionComparator {
    fun compareVersions(first: String, second: String): Int {
        val firstParts = normalize(first)
        val secondParts = normalize(second)
        val maxLength = maxOf(firstParts.size, secondParts.size)
        for (index in 0 until maxLength) {
            val firstValue = firstParts.getOrElse(index) { 0 }
            val secondValue = secondParts.getOrElse(index) { 0 }
            if (firstValue != secondValue) {
                return firstValue.compareTo(secondValue)
            }
        }
        return 0
    }

    private fun normalize(version: String): List<Int> {
        return version
            .split('.', '-', '_')
            .mapNotNull { part ->
                DIGIT_PATTERN.find(part)?.value?.toIntOrNull()
            }
    }

    private val DIGIT_PATTERN = Regex("\\d+")
}
