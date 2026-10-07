import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    // https://projects.neoforged.net/neoforged/moddevgradle
    id("net.neoforged.moddev") version "2.0.148" apply false
    // https://github.com/diffplug/spotless
    id("com.diffplug.spotless") version "8.10.3" apply false
    // ./gradlew dependencyUpdates - reports outdated plugins and dependencies
    id("com.github.ben-manes.versions") version "0.54.0"
}

// Checkstyle and PMD are wired into every module by the multiloader-common convention
// plugin; spotless is third party, so it is applied from here.
val licenseHeader = rootProject.file("gradle/spotless/license-header.txt")

subprojects {
    apply(plugin = "com.diffplug.spotless")

    val spotless = extensions.getByType(SpotlessExtension::class.java)
    spotless.java {
        target("src/**/*.java")
        // Generated data is written by the data run, never hand edited
        targetExclude("src/generated/**")
        licenseHeaderFile(licenseHeader, "package")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
