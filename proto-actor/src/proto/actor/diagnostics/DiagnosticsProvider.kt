package proto.actor.diagnostics

interface DiagnosticsProvider {
    suspend fun getDiagnostics(): List<DiagnosticEntry>
}

