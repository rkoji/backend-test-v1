package im.bigs.pg.external.pg.dto

class PgApproveErrorResponse(
    val code: Int,
    val errorCode: String,
    val message: String,
    val referenceId: String?
)
