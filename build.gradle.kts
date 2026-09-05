plugins {
    java
}

group = "dev.dusk.rankcolors"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("net.kyori:adventure-api:4.14.0")
    testImplementation("net.kyori:adventure-text-serializer-legacy:4.14.0")
    testImplementation("net.kyori:adventure-text-serializer-plain:4.14.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.88.1")
    testImplementation("net.bytebuddy:byte-buddy:1.18.12")
    testRuntimeOnly("me.clip:placeholderapi:2.11.6")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks.processResources {
    val resourceTokens = mapOf("version" to project.version.toString())
    inputs.properties(resourceTokens)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(resourceTokens)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("DuskRankColors")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    from("LICENSE") {
        into("META-INF")
    }
}
