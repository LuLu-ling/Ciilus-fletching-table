plugins {
    alias(libs.plugins.shadow)
}

architectury {
    platformSetupLoomIde()
    fabric()
}

val common = configurations.create("common")
val shadowBundle = configurations.create("shadowBundle")

configurations {
    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
    getByName("developmentFabric").extendsFrom(common)
}

dependencies {
    modImplementation(rootProject.libs.fabric.loader)
    modImplementation(rootProject.libs.fabric.api)
    modImplementation(rootProject.libs.architectury.fabric)

    modImplementation(rootProject.libs.cloth.config.fabric) {
        exclude(group = "net.fabricmc.fabric-api")
    }
    modCompileOnly(rootProject.libs.modmenu)

    common(
        project(
            path = ":common",
            configuration = "namedElements"
        )
    ) { isTransitive = false }
    shadowBundle(
        project(
            path = ":common",
            configuration = "transformProductionFabric"
        )
    ) { isTransitive = false }

    compileOnly(rootProject.libs.jspecify)
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
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
