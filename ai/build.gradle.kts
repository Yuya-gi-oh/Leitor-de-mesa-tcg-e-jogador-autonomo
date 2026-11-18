plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
    testImplementation(project(":core"))
    testImplementation(project(":rules-mtg"))
    testImplementation(libs.junit)
}
