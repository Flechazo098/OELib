plugins {
    id("module-loader")
    id("net.fabricmc.fabric-loom")
}

val mcVersion: String = property("minecraft_version") as String

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
}

loom {
    val aw = project(":modules:network:network-common").file("src/main/resources/oelib-network.accesswidener")
    if (aw.exists()) {
        accessWidenerPath = aw
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client - OELibNetwork"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server - OELibNetwork"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}
