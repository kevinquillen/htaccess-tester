plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.25"
    id("info.solidsoft.pitest") version "1.15.0"
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")

    testImplementation("com.charleskorn.kaml:kaml:0.57.0")
}

tasks.test {
    useJUnitPlatform()
}

pitest {
    targetClasses.set(listOf("com.github.kevinquillen.htaccess.engine.*"))
    targetTests.set(listOf("com.github.kevinquillen.htaccess.engine.*"))
    threads.set(4)
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)
    mutationThreshold.set(50)
    coverageThreshold.set(50)
    junit5PluginVersion.set("1.2.1")
    excludedClasses.set(listOf(
        "com.github.kevinquillen.htaccess.engine.model.*Dto",
        "com.github.kevinquillen.htaccess.engine.model.*Fixture"
    ))
}
