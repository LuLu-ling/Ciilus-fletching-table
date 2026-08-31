architectury {
    common("fabric", "neoforge")
}

dependencies {
    // Architectury API
    modImplementation(rootProject.libs.architectury.common)

    // Cloth Config
    modImplementation(rootProject.libs.cloth.config.fabric) {
        exclude(group = "net.fabricmc.fabric-api")
    }

    // JSpecify
    compileOnly(rootProject.libs.jspecify)

    // Tests
    testImplementation(rootProject.libs.junit.jupiter.api)
    testRuntimeOnly(rootProject.libs.junit.jupiter.engine)
    testRuntimeOnly(rootProject.libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
