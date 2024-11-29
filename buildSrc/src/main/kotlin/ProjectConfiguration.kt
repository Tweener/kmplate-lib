import org.gradle.api.JavaVersion

/**
 * @author Vivien Mahe
 * @since 23/07/2022
 */

object ProjectConfiguration {

    object MyProject {
        const val packageName = "com.tweener.changehere"
        const val versionName = "1.0.0"
        const val namespace = "$packageName.android"
        const val compileSDK = 34
        const val minSDK = 24

        // TODO Change all the values in this block to your needs
        object Maven {
            const val name = "MyKMPLibrary"
            const val description = "All Tweener commons stuff for Kotlin Multiplatform"
            const val group = "io.github.tweener"
            const val packageUrl = "https://github.com/Tweener/kmp-common"
            const val gitUrl = "github.com:Tweener/kmp-common.git"

            object Developer {
                const val id = "Tweener"
                const val name = "Vivien Mahé"
                const val email = "vivien@tweener-labs.com"
            }
        }
    }

    object Compiler {
        val javaCompatibility = JavaVersion.VERSION_21
        val jvmTarget = javaCompatibility.toString()
    }
}
