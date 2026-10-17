plugins {
    id("module-loader")
    id("net.fabricmc.fabric-loom")
}

val mcVersion: String = property("minecraft_version") as String

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
}

loom {
    val aw = project(":modules:dev:dev-common").file("src/main/resources/oelib_dev.accesswidener")
    if (aw.exists()) {
        accessWidenerPath = aw
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client - Dev"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server - Dev"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}
