plugins {
    kotlin("jvm") version libs.versions.kotlin
    id("maven-publish")
}

group = "io.effectivelabs"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    testImplementation(kotlin("test"))
    testImplementation(libs.mockito)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit)

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

publishing {
    repositories {
        maven {
            setUrl("https://maven.pkg.github.com/flashingpumpkin/kotlin-injector")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}