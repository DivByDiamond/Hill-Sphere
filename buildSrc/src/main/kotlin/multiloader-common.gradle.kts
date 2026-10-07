import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.Pmd
import org.gradle.language.jvm.tasks.ProcessResources

// Convention shared by every module: publishing, jar metadata, resource expansion
// and the static analysis tool chain (checkstyle + pmd).
plugins {
    id("java-library")
    id("maven-publish")
    id("checkstyle")
    id("pmd")
}

val modId = providers.gradleProperty("mod_id").get()
val modName = providers.gradleProperty("mod_name").get()
val modAuthor = providers.gradleProperty("mod_author").get()
val javaVersion = providers.gradleProperty("java_version").get().toInt()
val minecraftVersion = providers.gradleProperty("minecraft_version").get()

base {
    archivesName.set("$modId-${project.name}-$minecraftVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    // https://docs.gradle.org/current/userguide/declaring_repositories.html#declaring_content_exclusively_found_in_one_repository
    exclusiveContent {
        forRepository {
            maven {
                name = "Sponge"
                url = uri("https://repo.spongepowered.org/repository/maven-public")
            }
        }
        filter { includeGroupAndSubgroups("org.spongepowered") }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "ParchmentMC"
                url = uri("https://maven.parchmentmc.org/")
            }
        }
        filter { includeGroup("org.parchmentmc.data") }
    }
    maven {
        name = "NeoForge"
        url = uri("https://maven.neoforged.net/releases")
    }
    maven {
        name = "BlameJared"
        url = uri("https://maven.blamejared.com")
    }
}

// Declare capabilities on the outgoing configurations, so loader projects can pick the
// common project without clashing with the per-loader artifacts.
// https://docs.gradle.org/current/userguide/component_capabilities.html#sec:declaring-additional-capabilities-for-a-local-component
listOf("apiElements", "runtimeElements", "sourcesElements", "javadocElements").forEach { variant ->
    configurations.named(variant) {
        outgoing {
            capability("$group:${base.archivesName.get()}:${project.version}")
            capability("$group:$modId-${project.name}-$minecraftVersion:${project.version}")
            capability("$group:$modId:${project.version}")
        }
    }
    publishing.publications.configureEach {
        if (this is MavenPublication) {
            suppressPomMetadataWarningsFor(variant)
        }
    }
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_$modName" }
    }
}

tasks.named<Jar>("jar") {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_$modName" }
    }

    manifest {
        attributes(
            mapOf(
                "Specification-Title" to modName,
                "Specification-Vendor" to modAuthor,
                "Specification-Version" to archiveVersion.get(),
                "Implementation-Title" to project.name,
                "Implementation-Version" to archiveVersion.get(),
                "Implementation-Vendor" to modAuthor,
                "Built-On-Minecraft" to minecraftVersion
            )
        )
    }
}

tasks.named<ProcessResources>("processResources") {
    val expandProps = mapOf<String, Any>(
        "version" to project.version,
        "group" to project.group,
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to providers.gradleProperty("minecraft_version_range").get(),
        "mod_name" to modName,
        "mod_author" to modAuthor,
        "mod_id" to modId,
        "license" to providers.gradleProperty("license").get(),
        "description" to providers.gradleProperty("description").get(),
        "neoforge_version" to providers.gradleProperty("neoforge_version").get(),
        "neoforge_loader_version_range" to providers.gradleProperty("neoforge_loader_version_range").get(),
        "credits" to providers.gradleProperty("credits").get(),
        "java_version" to javaVersion
    )

    filesMatching(listOf("pack.mcmeta", "META-INF/neoforge.mods.toml", "*.mixins.json")) {
        expand(expandProps)
    }
    inputs.properties(expandProps)
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
    repositories {
        // Publishing is only usable when the environment variable is set.
        val localMavenUrl = System.getenv("local_maven_url")
        if (!localMavenUrl.isNullOrBlank()) {
            maven {
                url = uri(localMavenUrl)
            }
        }
    }
}

checkstyle {
    toolVersion = "14.3.0"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    configDirectory.set(rootProject.file("config/checkstyle"))
    configProperties = mapOf(
        "checkstyle.suppressions.file" to rootProject.file("config/checkstyle/suppressions.xml").absolutePath
    )
    // Style noise is reported, only real defects break the build.
    maxWarnings = Int.MAX_VALUE
    maxErrors = 0
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

pmd {
    toolVersion = "7.28.0"
    ruleSets = emptyList()
    ruleSetFiles = files(rootProject.file("config/pmd/ruleset.xml"))
    setConsoleOutput(true)
    // Report only the rules that catch real defects; anything nicer is a hint, not a build break.
    rulesMinimumPriority.set(2)
}

tasks.withType<Pmd>().configureEach {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
    exclude("**/generated/**")
}
