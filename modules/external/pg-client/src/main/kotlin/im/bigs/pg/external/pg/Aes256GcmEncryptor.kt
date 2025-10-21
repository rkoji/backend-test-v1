package im.bigs.pg.external.pg

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class Aes256GcmEncryptor {

    private val objectMapper = ObjectMapper()

    fun encryptToBase64Url(
        plainJson: Map<String, Any>,
        apiKey: String,
        ivBase64Url: String
    ): String {
        try {
            val key = MessageDigest.getInstance("SHA-256").digest(apiKey.toByteArray(StandardCharsets.UTF_8))

            val iv = Base64.getUrlDecoder().decode(ivBase64Url)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(128, iv)
            )

            val plainText = objectMapper.writeValueAsBytes(plainJson)
            val ciphertextWithTag = cipher.doFinal(plainText)

            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(ciphertextWithTag)
        } catch (e: Exception) {
            throw IllegalStateException("암호화 실패", e)
        }
    }
}
