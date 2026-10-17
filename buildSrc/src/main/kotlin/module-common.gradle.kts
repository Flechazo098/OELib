import cc.sighs.gradle.configureMavenDependencies
import cc.sighs.gradle.ModuleDependenciesExtension

plugins {
    `java-library`
    `maven-publish`
}

ModuleDependenciesExtension.init(project)
extensions.add("moduleDependencies", ModuleDependenciesExtension(project))

val modId: String = extra["mod_id"] as String
val modName: String = extra["mod_name"] as String

// Common boilerplate deps for all module types
project.dependencies.add("compileOnly", "org.spongepowered:mixin:0.8.5")
project.dependencies.add("compileOnly", "io.github.llamalad7:mixinextras-common:0.3.5")
project.dependencies.add("annotationProcessor", "io.github.llamalad7:mixinextras-common:0.3.5")
project.dependencies.add("compileOnly", "org.jetbrains:annotations:24.1.0")
val modAuthor: String = property("mod_author") as String
val mcVersion: String = property("minecraft_version") as String
val mcVersionRange: String = property("minecraft_version_range") as String
val javaVersion: String = property("java_version") as String
val fabricVersion: String = property("fabric_version") as String
val fabricLoaderVersion: String = property("fabric_loader_version") as String
val forge_version: String by project
val forge_loader_version_range: String by project
val licenseVal: String = property("license") as String
val creditsVal: String = findProperty("credits") as String? ?: ""

base {
    archivesName = "${project.name}-${mcVersion}"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
//    withJavadocJar()
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven {
                name = "Sponge"
                url = uri("https://repo.spongepowered.org/repository/maven-public")
            }
        }
        filter { includeGroupAndSubgroups("org.spongepowered") }
    }
    exclusiveContent {
        forRepositories(
            maven {
                name = "ParchmentMC"
                url = uri("https://maven.parchmentmc.org/")
            },
            maven {
                name = "NeoForge"
                url = uri("https://maven.neoforged.net/releases")
            }
        )
        filter { includeGroup("org.parchmentmc.data") }
    }
    maven {
        name = "BlameJared"
        url = uri("https://maven.blamejared.com")
    }

    maven {
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com/")
    }
    maven {
        url = uri("https://maven.sighs.cc/repository/maven-releases/")
    }
    maven {
        url = uri("https://maven.sighs.cc/repository/maven-snapshots/")
    }
}

listOf("apiElements", "runtimeElements", "sourcesElements").forEach { variant ->
    configurations.getByName(variant).outgoing {
        capability("${project.group}:${project.name}:${project.version}")
        capability("${project.group}:${base.archivesName.get()}:${project.version}")
    }
}

tasks.withType<org.gradle.api.publish.tasks.GenerateModuleMetadata>().configureEach {
    enabled = false
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE.txt")) {
        rename { "${it}_${modName}" }
    }
}

tasks.named<Jar>("jar") {
    from(rootProject.file("LICENSE.txt")) {
        rename { "${it}_${modName}" }
    }
    manifest {
        attributes(
            mapOf(
                "Specification-Title" to modName,
                "Specification-Vendor" to modAuthor,
                "Specification-Version" to project.version,
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to modAuthor,
                "Built-On-Minecraft" to mcVersion
            )
        )
    }
}

tasks.named<ProcessResources>("processResources") {
    val expandProps = mapOf(
        "version" to project.version,
        "group" to project.group,
        "minecraft_version" to mcVersion,
        "minecraft_version_range" to mcVersionRange,
        "fabric_version" to fabricVersion,
        "fabric_loader_version" to fabricLoaderVersion,
        "mod_name" to modName,
        "mod_author" to modAuthor,
        "mod_id" to modId,
        "license" to licenseVal,
        "description" to (project.description ?: ""),
        "forge_version" to forge_version,
        "forge_loader_version_range" to forge_loader_version_range,
        "credits" to creditsVal,
        "java_version" to javaVersion
    )

    val jsonExpandProps = expandProps.mapValues { (_, value) ->
        if (value is String) value.replace("\n", "\\\\n") else value
    }

    filesMatching(listOf("META-INF/mods.toml")) {
        expand(expandProps)
    }

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "*.mixins.json")) {
        expand(jsonExpandProps)
    }

    inputs.properties(expandProps)
}

// Maven dependency whitelist for POM filtering
// Subprojects override via extra["mavenDependencyWhitelist"].
extra["mavenDependencyWhitelist"] = emptyList<String>()

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
            pom.withXml {
                project.configureMavenDependencies(asNode())
            }
        }
    }
    repositories {
        val localMavenUrl = providers.environmentVariable("local_maven_url").orNull
        if (!localMavenUrl.isNullOrBlank()) {
            maven {
                url = uri(localMavenUrl)
            }
        }

        val modVersion = project.version.toString().ifBlank { "unknown" }
        val isSnapshot = modVersion.contains("snapshot", ignoreCase = true)
        val publishUrl = if (isSnapshot) {
            "https://maven.sighs.cc/repository/maven-snapshots/"
        } else {
            "https://maven.sighs.cc/repository/maven-releases/"
        }

        maven {
            name = "remoteRepo"
            url = uri(publishUrl)
            credentials {
                username = providers.environmentVariable("SIGHS_PUBLISH_USER").orNull
                password = providers.environmentVariable("SIGHS_PUBLISH_PASSWORD").orNull
            }
        }
    }
}
