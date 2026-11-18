pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LeitorDeMesaTCG"
include(":app")
include(":core")
include(":rules-mtg")
include(":rules-ygo")
include(":ai")
include(":data")
include(":vision")
