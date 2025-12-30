plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.25"
}

group = "com.github.kevinquillen"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":htaccess-engine"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("com.charleskorn.kaml:kaml:0.57.0")
}

tasks.test {
    useJUnitPlatform()

    systemProperty("madewithlove.cli.enabled", System.getProperty("madewithlove.cli.enabled", "false"))
}
