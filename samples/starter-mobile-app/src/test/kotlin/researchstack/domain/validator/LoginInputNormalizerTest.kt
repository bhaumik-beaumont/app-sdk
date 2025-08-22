package researchstack.domain.validator

import kotlin.test.Test
import kotlin.test.assertEquals

class LoginInputNormalizerTest {
    @Test
    fun `normalize adds gmail domain when missing`() {
        val result = LoginInputNormalizer.normalize("username")
        assertEquals("username@gmail.com", result)
    }

    @Test
    fun `normalize leaves existing email unchanged`() {
        val result = LoginInputNormalizer.normalize("user@example.com")
        assertEquals("user@example.com", result)
    }
}
