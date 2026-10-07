pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven {
                    name = "Sponge"
                    url = uri("https://repo.spongepowered.org/repository/maven-public")
                }
            }
            filter {
                includeGroupAndSubgroups("org.spongepowered")
            }
        }
        exclusiveContent {
            forRepository {
                maven {
                    name = "NeoForge"
                    url = uri("https://maven.neoforged.net/releases")
                }
            }
            filter {
                includeGroupAndSubgroups("net.neoforged")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Must match the folder name, or IntelliJ may complain (https://youtrack.jetbrains.com/issue/IDEA-317606)
rootProject.name = "hillsphere"

include("common")
include("neoforge")
