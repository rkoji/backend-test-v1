package im.bigs.pg.api.payment

import im.bigs.pg.domain.payment.PaymentStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentApiTest(
    @Autowired val restTemplate: TestRestTemplate,
) {

    @Test
    fun `결제 생성 후 조회시 통계 일치`() {
        // given
        val createRequest = mapOf(
            "partnerId" to 1,
            "amount" to 10000,
            "cardBin" to "123456",
            "cardLast4" to "4242",
            "productName" to "테스트상품"
        )

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }

        // when - 결제 생성
        val createResponse = restTemplate.postForEntity(
            "/api/v1/payments",
            HttpEntity(createRequest, headers),
            Map::class.java
        )

        // then - 결제 생성 결과 확인
        assertEquals(200, createResponse.statusCode.value())
        val created = createResponse.body!!
        assertEquals(10000, created["amount"])
        assertEquals(0.0235, (created["appliedFeeRate"] as Number).toDouble(), 1e-6)
        assertEquals(235, (created["feeAmount"] as Number).toInt())
        assertEquals(9765, (created["netAmount"] as Number).toInt())
        assertEquals("4242", created["cardLast4"])
        assertEquals("APPROVED", created["status"])
        assertTrue(!created.containsKey("cardBin"), "민감정보 cardBin은 응답에 포함되면 안 됩니다.")

        // when - 결제 내역 조회
        val queryResponse = restTemplate.exchange(
            "/api/v1/payments?partnerId=1&status=${PaymentStatus.APPROVED}&limit=10",
            HttpMethod.GET,
            HttpEntity(null, headers),
            Map::class.java
        )

        // then - 통계 값 검증
        assertEquals(200, queryResponse.statusCode.value())
        val body = queryResponse.body!!
        val summary = body["summary"] as Map<*, *>
        val items = body["items"] as List<*>

        assertTrue(items.isNotEmpty())
        assertEquals(items.size, (summary["count"] as Number).toInt())
        assertEquals(
            items.sumOf { (it as Map<*, *>)["amount"] as Int },
            (summary["totalAmount"] as Number).toInt()
        )
        assertEquals(
            items.sumOf { (it as Map<*, *>)["netAmount"] as Int },
            (summary["totalNetAmount"] as Number).toInt()
        )
    }
}