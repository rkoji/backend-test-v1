package im.bigs.pg.external.pg.dto

import java.time.LocalDateTime

data class PgApproveResponse (
    val approvalCode: String,
    val approvedAt: LocalDateTime,
    val maskedCardLast4: String,
    val amount: Int,
    val status: String
)