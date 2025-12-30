plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.2.1"  // Note: 2.10.5 available but requires Gradle config changes
}

group = "com.github.kevinquillen"
version = "1.1.0"

kotlin {
    jvmToolchain(17)
}

// Prevent Kotlin stdlib version conflicts with IntelliJ Platform
configurations.all {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1")

        bundledPlugin("com.intellij.java")

        pluginVerifier()
        zipSigner()

        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    // Offline htaccess engine
    implementation(project(":htaccess-engine"))

    // JSON serialization (for raw output formatting)
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.github.kevinquillen.htaccess-tester"
        name = "Htaccess Tester"
        version = project.version.toString()
        description = """
            Test .htaccess rewrite rules with instant offline evaluation.

            Features:
            <ul>
                <li>Test .htaccess rules against any URL</li>
                <li>Offline evaluation - no internet required</li>
                <li>Support for custom server variables</li>
                <li>Read rules directly from open .htaccess files</li>
                <li>Save and reload test cases per project</li>
                <li>Filter and analyze rule evaluation results</li>
            </ul>
        """.trimIndent()

        vendor {
            name = "Kevin Quillen"
            url = "https://github.com/kevinquillen"
        }

        changeNotes = """
            <h3>1.1.0</h3>
            <ul>
                <li>Offline evaluation - no internet connection required</li>
                <li>Improved trace table with separate Rule and Response columns</li>
            </ul>
            <h3>1.0.0</h3>
            <ul>
                <li>Initial release</li>
                <li>Support for custom server variables</li>
                <li>Read rules from open .htaccess files in editor</li>
                <li>Save and load test cases per project</li>
                <li>Filter trace results (all, failed, reached, met)</li>
                <li>Copy summary and view raw output</li>
            </ul>
        """.trimIndent()

        ideaVersion {
            sinceBuild = "241"
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            ide(org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.IntellijIdeaCommunity, "2024.1")
            ide(org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.IntellijIdeaCommunity, "2024.2")
            ide(org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.IntellijIdeaCommunity, "2024.3")
            ide(org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.IntellijIdeaCommunity, "2025.1")
        }
    }
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
}
