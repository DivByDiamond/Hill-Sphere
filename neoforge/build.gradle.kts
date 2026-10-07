plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev")
}

val modId = providers.gradleProperty("mod_id").get()
val neoforgeVersion = providers.gradleProperty("neoforge_version").get()
val parchmentMinecraft = providers.gradleProperty("parchment_minecraft").get()
val parchmentVersion = providers.gradleProperty("parchment_version").get()

neoForge {
    version = neoforgeVersion
    // Automatically enable neoforge AccessTransformers if the file exists
    val accessTransformer = project(":common").file("src/main/resources/META-INF/accesstransformer.cfg")
    if (accessTransformer.exists()) {
        accessTransformers.from(accessTransformer)
    }
    parchment {
        minecraftVersion.set(parchmentMinecraft)
        mappingsVersion.set(parchmentVersion)
    }
    runs {
        configureEach {
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            ideName = "NeoForge ${name.replaceFirstChar { it.uppercase() }} (${project.path})"
        }
        create("client") {
            client()
        }
        create("data") {
            data()

            // DataGen is run with "./gradlew :neoforge:runData" in the terminal.
            // Specify the modid for data generation, where to output the resulting resource,
            // and where to look for existing resources.
            programArguments.addAll(
                listOf(
                    "--mod", modId,
                    "--all",
                    "--output", file("src/generated/resources/").absolutePath,
                    "--existing", file("src/main/resources/").absolutePath
                )
            )
        }
        create("server") {
            server()
        }
    }
    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

repositories {
    maven("https://maven.createmod.net")
    maven("https://maven.ithundxr.dev/snapshots") // Registrate
    maven("https://maven.ryanhcode.dev/releases") // Sable
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
}

val mc = providers.gradleProperty("minecraft_version").get()
val prop = { name: String -> providers.gradleProperty(name).get() }

dependencies {
    implementation("com.simibubi.create:create-$mc:${prop("create_version")}") { isTransitive = false }
    implementation("net.createmod.ponder:ponder-neoforge:${prop("ponder_version")}+mc$mc")
    compileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-$mc:${prop("flywheel_version")}")
    runtimeOnly("dev.engine-room.flywheel:flywheel-neoforge-$mc:${prop("flywheel_version")}")
    implementation("com.tterrag.registrate:Registrate:${prop("registrate_version")}")
    implementation("dev.ryanhcode.sable:sable-neoforge-$mc:${prop("sable_version")}") {
        exclude(group = "foundry.veil")
        exclude(group = "com.tterrag.registrate")
    }
    implementation("maven.modrinth:aero_cam_sync:${prop("camera_sync_version")}")
}

sourceSets["main"].resources.srcDir("src/generated/resources")
