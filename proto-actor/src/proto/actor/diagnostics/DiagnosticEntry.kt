package proto.actor.diagnostics

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class DiagnosticEntry(
    val module: String,
    val message: String?,
    @Contextual val data: Any?
)
