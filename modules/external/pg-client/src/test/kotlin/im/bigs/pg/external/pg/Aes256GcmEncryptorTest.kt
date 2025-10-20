package im.bigs.pg.external.pg

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class Aes256GcmEncryptorTest {

    private val aes256GcmEncryptor = Aes256GcmEncryptor()

    @Test
    fun `AES256 GCM 암호화 성공`() {
        val plainJson = mapOf(
            "cardNumber" to "1111-1111-1111-1111",
            "birthDate" to "19900101",
            "expiry" to "1227",
            "password" to "12",
            "amount" to 10000
        )

        val apiKey = "11111111-1111-4111-8111-111111111111"
        val ivBase64Url = "AAAAAAAAAAAAAAA"

        val encrypted = aes256GcmEncryptor.encryptToBase64Url(plainJson,apiKey,ivBase64Url)

        assertNotNull(encrypted)
        println("암호화 결과(Base64URL) = $encrypted")
    }
}