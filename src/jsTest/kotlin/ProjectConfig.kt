import io.kotest.core.config.AbstractProjectConfig

class ProjectConfig : AbstractProjectConfig() {
	override suspend fun beforeProject() {
		println("HELLO")
	}
}
