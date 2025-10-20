package im.bigs.pg.external.pg

import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.external.pg.dto.PgApproveErrorResponse
import im.bigs.pg.external.pg.dto.PgApproveResponse
import im.bigs.pg.external.pg.dto.PgEncryptedRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class PgClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val aes256GcmEncryptor: Aes256GcmEncryptor
) : PgClientOutPort {

    @Value("\${pg.base-url}")
    private lateinit var baseUrl: String

    @Value("\${pg.api-key}")
    private lateinit var apiKey: String

    @Value("\${pg.iv}")
    private lateinit var iv: String

    @Autowired(required = false)
    constructor(
        restTemplate: RestTemplate,
        objectMapper: ObjectMapper,
        aes256GcmEncryptor: Aes256GcmEncryptor,
        baseUrl: String,
        apiKey: String,
        iv: String
    ) : this(restTemplate, objectMapper, aes256GcmEncryptor) {
        this.baseUrl = baseUrl
        this.apiKey = apiKey
        this.iv = iv
    }

    override fun supports(partnerId: Long): Boolean = partnerId % 2L == 1L

    override fun approve(request: PgApproveRequest): PgApproveResult {
        try {
            val plainJson = mapOf(
                "cardNumber" to "1111-1111-1111-1111",
                "birthDate" to "19900101",
                "expiry" to "1227",
                "password" to "12",
                "amount" to request.amount.toInt()
            )

            val enc = aes256GcmEncryptor.encryptToBase64Url(plainJson, apiKey, iv)

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("API-KEY", apiKey)
            }

            val entity = HttpEntity(PgEncryptedRequest(enc), headers)
            val url = "$baseUrl/api/v1/pay/credit-card"

            val response = restTemplate.exchange(url, HttpMethod.POST, entity, String::class.java)

            return when (response.statusCode) {
                HttpStatus.OK -> {
                    val body = objectMapper.readValue(response.body, PgApproveResponse::class.java)
                    PgApproveResult(
                        approvalCode = body.approvalCode,
                        approvedAt = body.approvedAt,
                        status = PaymentStatus.valueOf(body.status)
                    )
                }

                HttpStatus.UNPROCESSABLE_ENTITY -> {
                    val error = objectMapper.readValue(response.body, PgApproveErrorResponse::class.java)
                    throw IllegalStateException("PG 승인 실패(${error.errorCode}): ${error.message}")
                }

                HttpStatus.UNAUTHORIZED -> {
                    throw IllegalStateException("PG 인증 실패: 잘못된 또는 누락된 API KEY")
                }

                else -> {
                    throw IllegalStateException("PG 알 수 없는 오류: ${response.statusCode}")
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("PG 승인 중 오류 발생", e)
        }
    }
}