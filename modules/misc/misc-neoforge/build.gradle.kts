import cc.sighs.gradle.configureJarJarFilenameNormalization
import cc.sighs.gradle.registerGeneratedResources

plugins {
    id("module-loader")
    id("net.neoforged.moddev")
}

val modId: String = extra["mod_id"] as String
val neoVer: String = property("neoforge_version") as String

neoForge {

    val commonProject = project(":modules:misc:misc-common")
    val at = commonProject.file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }

    version = neoVer

    runs {
        create("client") { client() }
        create("data") {
            clientData()
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", project.file("src/generated/resources/").absolutePath,
                "--existing", project.file("src/main/resources/").absolutePath
            )
        }
        create("server") { server() }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

registerGeneratedResources()

extra["mavenDependencyWhitelist"] = emptyList<String>()
dependencies {
    compileOnlyApi("io.smallrye.classfile:jdk-classfile-backport:26")
    compileOnlyApi("com.flechazo:optics-java:1.0.8-beta-j21") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "it.unimi.dsi", module = "fastutil")
    }
    jarJar("io.smallrye.classfile:jdk-classfile-backport:26")
    jarJar("com.flechazo:optics-java:1.0.8-beta-j21") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "it.unimi.dsi", module = "fastutil")
    }
    runtimeOnly("io.smallrye.classfile:jdk-classfile-backport:26")
    runtimeOnly("com.flechazo:optics-java:1.0.8-beta-j21") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "it.unimi.dsi", module = "fastutil")
    }
}

configureJarJarFilenameNormalization()
