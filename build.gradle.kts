plugins {
	kotlin("multiplatform") version "1.5.21"
	//id("io.kotest.multiplatform") version "5.0.0.3"
}

repositories {
	mavenCentral()
	maven("https://oss.sonatype.org/content/repositories/snapshots")
}

val kotestVersion: String by project

kotlin {
	targets {
		js(IR) {
			nodejs { }
			browser { }
		}
	}
	sourceSets {
		val jsMain by getting {

		}
		val jsTest by getting {
			dependencies {
				implementation("io.kotest:kotest-assertions-core:4.6.1")
				implementation("io.kotest:kotest-framework-api:4.6.1")
				implementation("io.kotest:kotest-framework-engine:4.6.1")
			}
		}
	}
}
