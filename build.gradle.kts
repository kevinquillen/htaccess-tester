plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.2.1"  // Note: 2.10.5 available but requires Gradle config changes
}

group = "com.github.kevinquillen"
version = "1.0.1"

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

    // HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.github.kevinquillen.htaccess-tester"
        name = "Htaccess Tester"
        version = project.version.toString()
        description = """
            Test .htaccess rewrite rules against a remote evaluation service.

            Features:
            <ul>
                <li>Test .htaccess rules against any URL</li>
                <li>Support for custom server variables</li>
                <li>Read rules directly from open .htaccess files</li>
                <li>Save and reload test cases per project</li>
                <li>Filter and analyze rule evaluation results</li>
            </ul>

            <b>Note:</b> Requires internet access for remote rule evaluation.
        """.trimIndent()

        vendor {
            name = "Kevin Quillen"
            url = "https://github.com/kevinquillen"
        }

        changeNotes = """
            <h3>1.0.0</h3>
            <ul>
                <li>Initial release</li>
                <li>Test .htaccess rewrite rules against htaccess.madewithlove.com API</li>
                <li>Support for custom server variables</li>
                <li>Read rules from open .htaccess files in editor</li>
                <li>Save and load test cases per project</li>
                <li>Filter trace results (all, failed, reached, met)</li>
                <li>Copy summary and view raw API response</li>
                <li>Automatic retry for transient server errors</li>
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
