import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.language.jvm.tasks.ProcessResources

// Convention for the loader projects (neoforge): they pull the sources and
// resources of the common project into their own compilation instead of only depending
// on its binary, so a single jar ships both the shared and the loader specific code.
plugins {
    id("multiloader-common")
}

val modId = providers.gradleProperty("mod_id").get()

configurations {
    create("commonJava") {
        isCanBeResolved = true
    }
    create("commonResources") {
        isCanBeResolved = true
    }
}

dependencies {
    compileOnly(project(":common")) {
        capabilities {
            requireCapability("$group:$modId")
        }
    }
    add("commonJava", project(mapOf("path" to ":common", "configuration" to "commonJava")))
    add("commonResources", project(mapOf("path" to ":common", "configuration" to "commonResources")))
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(configurations.named("commonJava"))
    source(configurations.named("commonJava"))
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(configurations.named("commonResources"))
    from(configurations.named("commonResources"))
}

tasks.named<Javadoc>("javadoc") {
    dependsOn(configurations.named("commonJava"))
    source(configurations.named("commonJava"))
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(configurations.named("commonJava"))
    from(configurations.named("commonJava"))
    dependsOn(configurations.named("commonResources"))
    from(configurations.named("commonResources"))
}
