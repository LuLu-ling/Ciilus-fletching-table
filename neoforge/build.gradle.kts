plugins {
    alias(libs.plugins.shadow)
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

val common = configurations.create("common")
val shadowBundle = configurations.create("shadowBundle")

configurations {
    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
    getByName("developmentNeoForge").extendsFrom(common)
}

dependencies {
    "neoForge"(rootProject.libs.neoforge)
    modImplementation(rootProject.libs.architectury.neoforge)

    modImplementation(rootProject.libs.cloth.config.neoforge)

    common(
        project(
            path = ":common",
            configuration = "namedElements"
        )
    ) { isTransitive = false }
    shadowBundle(
        project(
            path = ":common",
            configuration = "transformProductionNeoForge"
        )
    ) { isTransitive = false }

    compileOnly(rootProject.libs.jspecify)
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.get().archiveFile)
    dependsOn(tasks.shadowJar)
    archiveClassifier.set(null as String?)
}

tasks.jar {
    archiveClassifier.set("dev")
}
