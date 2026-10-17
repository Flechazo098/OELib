plugins {
    id("module-loader")
    id("net.fabricmc.fabric-loom")
}

val mcVersion: String = property("minecraft_version") as String

extra["mavenDependencyWhitelist"] = listOf("com.flechazo:optics-java", "io.smallrye.classfile:jdk-classfile-backport")
dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")

    api("io.smallrye.classfile:jdk-classfile-backport:26")
    api("com.flechazo:optics-java:1.0.8-beta") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "it.unimi.dsi", module = "fastutil")
    }
    include("io.smallrye.classfile:jdk-classfile-backport:26")
    include("com.flechazo:optics-java:1.0.8-beta")
}

loom {
    val aw = project(":modules:misc:misc-common").file("src/main/resources/oelib-misc.accesswidener")
    if (aw.exists()) {
        accessWidenerPath = aw
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client - OELibMisc"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server - OELibMisc"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}
