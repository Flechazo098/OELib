plugins {
    id("module-loader")
    id("net.fabricmc.fabric-loom")
}

val mcVersion: String = property("minecraft_version") as String

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
}

loom {
    val aw = project(":modules:registry:registry-common").file("src/main/resources/oelib-registry.accesswidener")
    if (aw.exists()) {
        accessWidenerPath = aw
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client - OELibRegistry"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server - OELibRegistry"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}
