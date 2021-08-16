plugins {
	kotlin("multiplatform") version "1.5.21"
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
	targets {
		js(IR) {
			nodejs { }
			browser {
				webpackTask {
					output.libraryTarget = "umd"
				}
				testTask { useMocha() }
			}
		}
	}
	sourceSets {
		val jsMain by getting {

		}
		val jsTest by getting {
			dependencies {
				implementation("io.kotest:kotest-assertions-core:5.0.0.376-SNAPSHOT")
				implementation("io.kotest:kotest-framework-api:5.0.0.376-SNAPSHOT")
				implementation("io.kotest:kotest-framework-engine:5.0.0.376-SNAPSHOT")
			}
		}
	}
}
