plugins {
    id("module-loader")
    id("net.fabricmc.fabric-loom")
}

val mcVersion: String = property("minecraft_version") as String

extra["mavenDependencyWhitelist"] = listOf("org.mvel:mvel2")

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")

    api("org.mvel:mvel2:2.5.0.Final")
    include("org.mvel:mvel2:2.5.0.Final")
}

loom {
    val aw = project(":modules:data:data-common").file("src/main/resources/oelib-data.accesswidener")
    if (aw.exists()) {
        accessWidenerPath = aw
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client - OELibData"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server - OELibData"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}
