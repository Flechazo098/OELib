plugins {
    id("module-loader")
    id("net.fabricmc.fabric-loom")
}

val mcVersion: String = property("minecraft_version") as String

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
}

loom {
    val aw = project(":modules:compat:compat-common").file("src/main/resources/oelib-compat.accesswidener")
    if (aw.exists()) {
        accessWidenerPath = aw
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client - OELibCompat"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server - OELibCompat"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}

// Compatibility artifacts are embedded in the main mod only.
tasks.withType<org.gradle.api.publish.maven.tasks.AbstractPublishToMaven>().configureEach {
    enabled = false
}
