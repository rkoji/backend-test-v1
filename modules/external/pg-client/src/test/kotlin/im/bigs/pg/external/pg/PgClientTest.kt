package im.bigs.pg.external.pg

import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.external.pg.dto.PgApproveErrorResponse
import im.bigs.pg.external.pg.dto.PgApproveResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime

class PgClientTest {

    private val restTemplate = mockk<RestTemplate>()
    private val objectMapper = ObjectMapper().apply {
        findAndRegisterModules()
    }
    private val aes256GcmEncryptor = mockk<Aes256GcmEncryptor>()

    private val pgClient = PgClient(restTemplate, objectMapper, aes256GcmEncryptor).apply {
        // 리플렉션을 이용해서 private lateinit var 필드 값 수동 설정
        val baseUrlField = PgClient::class.java.getDeclaredField("baseUrl").apply { isAccessible = true }
        baseUrlField.set(this, "http://localhost:8080")

        val apiKeyField = PgClient::class.java.getDeclaredField("apiKey").apply { isAccessible = true }
        apiKeyField.set(this, "11111111-1111-4111-8111-111111111111")

        val ivField = PgClient::class.java.getDeclaredField("iv").apply { isAccessible = true }
        ivField.set(this, "AAAAAAAAAAAAAAA")
    }

    private val request = PgApproveRequest(
        partnerId = 1L,
        amount = 10000.toBigDecimal(),
        cardBin = "123456",
        cardLast4 = "4242",
        productName = "테스트상품"
    )

    @Test
    fun `PG 승인 요청 성공`() {
        every { aes256GcmEncryptor.encryptToBase64Url(any(), any(), any()) } returns "encryptedData"

        val body = PgApproveResponse(
            approvalCode = "10080728",
            approvedAt = LocalDateTime.parse("2025-10-08T03:31:34.181568"),
            maskedCardLast4 = "1111",
            amount = 10000,
            status = "APPROVED"
        )
        every { restTemplate.exchange(any<String>(), any(), any(), String::class.java) } returns
            ResponseEntity(objectMapper.writeValueAsString(body), HttpStatus.OK)

        val result = pgClient.approve(request)

        assertEquals("10080728", result.approvalCode)
        assertEquals(PaymentStatus.APPROVED, result.status)
        verify { aes256GcmEncryptor.encryptToBase64Url(any(), any(), any()) }
    }

    @Test
    fun `PG 승인 실패 - 한도 초과`() {
        val error = PgApproveErrorResponse(1002, "INSUFFICIENT_LIMIT", "한도가 초과되었습니다.", "ref-id")
        every { aes256GcmEncryptor.encryptToBase64Url(any(), any(), any()) } returns "encryptedData"
        every { restTemplate.exchange(any<String>(), any(), any(), String::class.java) } returns
            ResponseEntity(objectMapper.writeValueAsString(error), HttpStatus.UNPROCESSABLE_ENTITY)

        val ex = assertThrows(IllegalStateException::class.java) {
            pgClient.approve(request)
        }
        assertTrue(ex.message!!.contains("INSUFFICIENT_LIMIT"))
    }

    @Test
    fun `PG 인증 실패 - 잘못된 API-KEY`() {
        every { aes256GcmEncryptor.encryptToBase64Url(any(), any(), any()) } returns "encryptedData"
        every { restTemplate.exchange(any<String>(), any(), any(), String::class.java) } returns
            ResponseEntity("{}", HttpStatus.UNAUTHORIZED)

        val ex = assertThrows(IllegalStateException::class.java) {
            pgClient.approve(request)
        }
        assertTrue(ex.message!!.contains("PG 인증 실패"))
    }
}
