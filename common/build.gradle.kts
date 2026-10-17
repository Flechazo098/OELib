import cc.sighs.gradle.configureCommonSourceArtifacts

plugins {
    id("multiloader-common")
    id("net.neoforged.moddev")
}

val NFMVersion = property("neo_form_version") as String

neoForge {
    neoFormVersion = NFMVersion

    val at = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from("src/main/resources/META-INF/accesstransformer.cfg")
    }
}

dependencies {
    compileOnly("org.spongepowered:mixin:0.8.5")
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation(files(sourceSets.main.get().compileClasspath))

    // Aggregate every module's common API. Consumers can depend on the single
    // oelib-common coordinate while javac still receives each module as a real
    // classpath entry (nested jars would not be visible to the compiler).
    @Suppress("UNCHECKED_CAST")
    val discoveredModules = rootProject.extra["discoveredModules"] as? Map<String, File> ?: emptyMap()
    for ((name, _) in discoveredModules) {
        val path = ":modules:$name:$name-common"
        try {
            api(project(path))
        } catch (_: UnknownProjectException) {
        }
    }
}

extra["mavenDependencyWhitelist"] = listOf(
    "cc.sighs.oelib",
    "cn.6tail",
    "de.marhali",
    "com.electronwill.night-config",
    "org.mvel",
    "io.smallrye.classfile"
)

configurations.testImplementation {
    extendsFrom(configurations.compileOnly.get())
}

configureCommonSourceArtifacts()

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

val jmhSourceSet = sourceSets.create("jmh") {
    java.srcDir("src/jmh/java")
    resources.srcDir("src/jmh/resources")
    compileClasspath += sourceSets.main.get().output + configurations.compileClasspath.get()
    runtimeClasspath += output + compileClasspath + configurations.runtimeClasspath.get()
}

configurations.named("jmhImplementation") {
    extendsFrom(configurations.implementation.get())
}
configurations.named("jmhRuntimeOnly") {
    extendsFrom(configurations.runtimeOnly.get())
}
dependencies {
    add("jmhImplementation", "org.openjdk.jmh:jmh-core:1.37")
    add("jmhAnnotationProcessor", "org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

fun registerJmhRun(name: String, descriptionText: String, defaults: List<String>) {
    tasks.register<JavaExec>(name) {
        group = "benchmark"
        description = descriptionText
        classpath = jmhSourceSet.runtimeClasspath
        mainClass = "org.openjdk.jmh.Main"
        dependsOn(tasks.named("jmhClasses"))
        args(defaults)
        providers.gradleProperty("jmhInclude").orNull?.let { args(it) }
        providers.gradleProperty("jmhArgs").orNull?.trim()
            ?.split(Regex("\\s+"))?.filter { it.isNotBlank() }?.let { args(it) }
    }
}
registerJmhRun("jmhQuick", "Run JMH benchmarks (quick defaults)",
    listOf("-wi", "2", "-i", "3", "-f", "1", "-w", "300ms", "-r", "300ms", "-tu", "ns"))
registerJmhRun("jmh", "Run JMH benchmarks (more stable defaults)",
    listOf("-wi", "5", "-i", "5", "-f", "2", "-w", "1s", "-r", "1s", "-tu", "ns"))
registerJmhRun("jmhGc", "Run JMH benchmarks with -prof gc (stable defaults)",
    listOf("-wi", "5", "-i", "5", "-f", "2", "-w", "1s", "-r", "1s", "-tu", "ns", "-prof", "gc"))
tasks.named<ProcessResources>("processJmhResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
