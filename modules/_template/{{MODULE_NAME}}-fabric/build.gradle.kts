plugins {
    id("module-loader")
    id("net.fabricmc.fabric-loom")
}

val mcVersion: String = property("minecraft_version") as String

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
}

loom {
    val aw = project(":modules:{{MODULE_NAME}}:{{MODULE_NAME}}-common").file("src/main/resources/{{FULL_MOD_ID}}.accesswidener")
    if (aw.exists()) {
        accessWidenerPath = aw
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client - {{CLASS_PREFIX}}"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server - {{CLASS_PREFIX}}"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}
