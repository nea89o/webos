plugins {
	kotlin("js") version "1.5.21"
	id("io.kotest.multiplatform") version "5.0.0.3"
	id("com.bnorm.power.kotlin-power-assert") version "0.10.0"
}

repositories {
	mavenCentral()
	maven("https://oss.sonatype.org/content/repositories/snapshots")
}

val kotestVersion: String by project

configure<com.bnorm.power.PowerAssertGradleExtension> {
	functions = listOf("kotlin.assert", "kotlin.test.assertTrue", "kotlin.test.assertFalse", "kotlin.test.assertEquals")
}
kotlin {
	js(IR) {
		binaries.executable()
		useCommonJs()
		browser {
			testTask { useMocha() }
		}
	}
	sourceSets {
		val main by getting {

		}
		val test by getting {
			dependencies {
				implementation("io.kotest:kotest-assertions-core:5.0.0.376-SNAPSHOT")
				implementation("io.kotest:kotest-framework-api:5.0.0.376-SNAPSHOT")
				implementation("io.kotest:kotest-framework-engine:5.0.0.376-SNAPSHOT")
			}
		}
	}
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>() {
	kotlinOptions {
		moduleKind = "commonjs"
		sourceMap = true
		sourceMapEmbedSources = "always"
		freeCompilerArgs += "-Xopt-in=kotlin.contracts.ExperimentalContracts"

	}
}
