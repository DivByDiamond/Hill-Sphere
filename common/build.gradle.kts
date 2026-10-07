plugins {
    id("multiloader-common")
    id("net.neoforged.moddev")
}

neoForge {
    neoFormVersion = providers.gradleProperty("neo_form_version").get()
    // Automatically enable AccessTransformers if the file exists
    val accessTransformer = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (accessTransformer.exists()) {
        accessTransformers.from(accessTransformer)
    }
    parchment {
        minecraftVersion.set(providers.gradleProperty("parchment_minecraft").get())
        mappingsVersion.set(providers.gradleProperty("parchment_version").get())
    }
}

dependencies {
    compileOnly("org.spongepowered:mixin:0.8.7")
    // neoforge bundles mixinextras, so it is safe to use it in common
    compileOnly("io.github.llamalad7:mixinextras-common:0.4.1")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1")

    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

configurations {
    create("commonJava") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    create("commonResources") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
}

artifacts {
    add("commonJava", sourceSets["main"].java.sourceDirectories.singleFile)
    add("commonResources", sourceSets["main"].resources.sourceDirectories.singleFile)
}
