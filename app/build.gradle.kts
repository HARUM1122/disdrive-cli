plugins {
    alias(libs.plugins.kotlin.jvm)
    kotlin("kapt") version "1.9.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.guava)

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    implementation("net.java.dev.jna:jna:5.10.0")
    implementation("com.google.dagger:dagger:2.55")
    kapt("com.google.dagger:dagger-compiler:2.55")

    implementation("org.xerial:sqlite-jdbc:3.43.2.0")
    implementation("org.slf4j:slf4j-nop:2.0.7")
    implementation("org.json:json:20200518")
}


java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "AppKt"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "AppKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}