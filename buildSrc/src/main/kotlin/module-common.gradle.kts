import cc.sighs.gradle.configureMavenDependencies
import cc.sighs.gradle.ModuleDependenciesExtension
import cc.sighs.gradle.configureStandardArtifactMetadata

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
val modAuthor: String = property("mod_author") as String
val mcVersion: String = property("minecraft_version") as String
val mcVersionRange: String = property("minecraft_version_range") as String
val javaVersion: String = property("java_version") as String
val fabricVersion: String = property("fabric_version") as String
val fabricLoaderVersion: String = property("fabric_loader_version") as String
val neoforge_version: String by project
val neoforge_loader_version_range: String by project
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
        // Project dependencies select the project-name capability while external
        // consumers select the published Maven coordinate. Since artifactId adds
        // the Minecraft version, both capabilities must be present.
        capability("${project.group}:${project.name}:${project.version}")
        capability("${project.group}:${base.archivesName.get()}:${project.version}")
    }
}

// The published dependency model is intentionally authored in the Maven POM
// below. Loom and ModDev add build-only dependencies to the Java variants that
// must not leak to consumers, and pom.withXml cannot also rewrite .module files.
tasks.withType<org.gradle.api.publish.tasks.GenerateModuleMetadata>().configureEach {
    enabled = false
}

configureStandardArtifactMetadata(
    modId = modId,
    modName = modName,
    modAuthor = modAuthor,
    mcVersion = mcVersion,
    mcVersionRange = mcVersionRange,
    fabricVersion = fabricVersion,
    fabricLoaderVersion = fabricLoaderVersion,
    license = licenseVal,
    neoforgeVersion = neoforge_version,
    neoforgeLoaderVersionRange = neoforge_loader_version_range,
    credits = creditsVal,
    javaVersion = javaVersion
)

// Maven dependency whitelist for POM filtering. Empty means that no external
// dependencies are published; explicit moduleDependencies are added separately.
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
