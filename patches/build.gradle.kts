group = "suchi.morphe"

patches {
    about {
        name = "Suchi Patches"
        description = "Patches for use with Morphe"
        source = "git@github.com:MangoTornado/morphe-patches-suchi.git"
        author = "MangoTornado"
        contact = "na"
        website = "https://github.com/MangoTornado/morphe-patches-suchi"
        license = "GPLv3"
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}
