package im.bigs.pg.domain.calculation

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class FeeCalculatorTest {

    @Test
    fun `비율 수수료만 적용`() {
        // given
        val amount = BigDecimal(10000)
        val rate = BigDecimal("0.0235")

        // when
        val (fee, net) = FeeCalculator.calculateFee(amount, rate)

        // then
        assertEquals(BigDecimal(235),fee)
        assertEquals(BigDecimal(9765),net)
    }

    @Test
    fun `비율 수수료와 고정 수수료를 함께 적용`(){
        // given
        val amount = BigDecimal(10000)
        val rate = BigDecimal("0.0235")
        val fixed = BigDecimal(300)

        // when
        val (fee, net) = FeeCalculator.calculateFee(amount, rate, fixed)

        // then
        assertEquals(BigDecimal(535),fee)
        assertEquals(BigDecimal(9465),net)
    }

    @Test
    fun `0원일 경우 정산금도 0`(){
        // given
        val amount = BigDecimal.ZERO
        val rate = BigDecimal("0.0235")

        // when
        val (fee, net) = FeeCalculator.calculateFee(amount, rate)

        // then
        assertEquals(BigDecimal.ZERO, fee)
        assertEquals(BigDecimal.ZERO, net)

    }

    @Test
    fun `음수 금액이면 예외 발생`() {
        val amount = BigDecimal(-1000)
        val rate = BigDecimal("0.02")

        val ex = assertThrows(IllegalArgumentException::class.java) {
            FeeCalculator.calculateFee(amount, rate)
        }
        assertTrue(ex.message!!.contains("amount must be"))
    }

    @Test
    fun `음수 수수료율일경우 예외 발생`() {
        val amount = BigDecimal(1000)
        val rate = BigDecimal(-0.02)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            FeeCalculator.calculateFee(amount, rate)
        }
        assertTrue(ex.message!!.contains("rate must be"))
    }
}