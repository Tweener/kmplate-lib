import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * @author Vivien Mahe
 * @since 27/11/2024
 */

abstract class RenameProjectTask : DefaultTask() {

    companion object {
        private const val PROJECT_NAME_TASK_PARAM = "projectName"
        private const val PACKAGE_NAME_TASK_PARAM = "packageName"
        private const val DRY_RUN_TASK_PARAM = "dryRun"

        private const val ACTUAL_PROJECT_NAME = "Kmplate_Library"
        private const val ACTUAL_MODULE_NAME = "changehere"
        private const val ACTUAL_PROJECT_CONFIGURATION_OBJECT = "MyProject" // For ProjectConfiguration.kt
        private const val ACTUAL_PACKAGE_NAME_CONFIGURATION_OBJECT = "com.tweener.changehere" // For ProjectConfiguration.kt

        private const val BUILD_DIR = "build"

        private const val GITHUB_WORKFLOW_BUILD_FILENAME = "buildRelease.yml"
        private const val GITHUB_WORKFLOW_NOTIFY_FILENAME = "notify.yml"
        private const val LIBRARY_BUILD_GRADLE_FILENAME = "build.gradle.kts"
        private const val SETTINGS_FILENAME = "settings.gradle.kts"
        private const val PROJECT_CONFIG_FILENAME = "ProjectConfiguration.kt"
        private const val THIS_TASK_FILENAME = "RenameProjectTask.kt"
    }

    init {
        group = "custom"
        description =
            "Renames the project's directories and updates references in files. Usage: ./gradlew renameProject -$PROJECT_NAME_TASK_PARAM=MyLibrary -$PACKAGE_NAME_TASK_PARAM=org.example.mylibrary"
    }

    @TaskAction
    fun renameProject() {
        // Get projectName property and validate it
        val projectName = project.findProperty(PROJECT_NAME_TASK_PARAM)?.toString()
            ?: throw IllegalArgumentException("You must pass the '$PROJECT_NAME_TASK_PARAM' property. Example: ./gradlew renameProject -P$PROJECT_NAME_TASK_PARAM=MyLibrary")

        require(projectName.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            "Invalid project name: $projectName. Only alphanumeric characters and underscores are allowed."
        }

        val moduleName = projectName.lowercase()

        // Get packageName property and validate it
        val packageName = project.findProperty(PACKAGE_NAME_TASK_PARAM)?.toString()
            ?: throw IllegalArgumentException("You must pass the '$PACKAGE_NAME_TASK_PARAM' property. Example: ./gradlew renameProject -P$PACKAGE_NAME_TASK_PARAM=org.example.mylibrary")

        require(packageName.matches(Regex("^[a-zA-Z][a-zA-Z0-9_.]+$"))) {
            "Invalid package name: $packageName. It must be a valid Java package name."
        }

        // Get dryRun property
        val dryRun = project.hasProperty(DRY_RUN_TASK_PARAM)

        println("Starting project rename task...")
        println("Target project name: $projectName")
        println("Derived module name: $moduleName")
        println("Target package name: $packageName")
        println(if (dryRun) "Dry run enabled. No changes will be applied." else "Applying changes...")

        // Step 1: Delete existing moduleName directory if it exists
        deleteExistingModuleDirectory(moduleName, dryRun)

        // Step 2: Rename directories
        val renamedDirectories = mutableListOf<String>()
        renameDirectories(moduleName = moduleName, packageName = packageName, dryRun = dryRun, renamedDirectories = renamedDirectories)

        // Step 3: Update files
        val updatedFiles = mutableListOf<String>()
        replaceWordsInFiles(projectName = projectName, moduleName = moduleName, packageName = packageName, dryRun = dryRun, updatedFiles = updatedFiles)

        // Step 4: Print summary
        printSummary(renamedDirectories = renamedDirectories, updatedFiles = updatedFiles, dryRun = dryRun)
    }

    private fun deleteExistingModuleDirectory(moduleName: String, dryRun: Boolean) {
        println("\n--- Deleting existing module directory ---")

        val moduleDir = File(project.projectDir, moduleName)

        if (moduleDir.exists()) {
            if (dryRun) {
                println(
                    "\n------ Dry run: Directory to be deleted: ${moduleDir.absolutePath}"
                )
            } else {
                try {
                    moduleDir.deleteRecursively()
                    println("Deleted existing directory: ${moduleDir.absolutePath}")
                } catch (e: Exception) {
                    println("Error deleting directory: ${moduleDir.absolutePath}. Reason: ${e.message}")
                }
            }
        } else {
            println("No existing directory to delete: ${moduleDir.absolutePath}")
        }
    }

    private fun replaceWordsInFiles(projectName: String, moduleName: String, packageName: String, dryRun: Boolean, updatedFiles: MutableList<String>) {
        println("\n--- Updating words in files ---")

        // Find all .kt files and specified configuration files
        val specificFilesToUpdate = listOf(GITHUB_WORKFLOW_BUILD_FILENAME, GITHUB_WORKFLOW_NOTIFY_FILENAME, SETTINGS_FILENAME, PROJECT_CONFIG_FILENAME)

        val filesToUpdate = project.projectDir
            .walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.name in specificFilesToUpdate || it.path.endsWith(LIBRARY_BUILD_GRADLE_FILENAME)) }
            .filter { it.isProjectTaskFile().not() } // Exclude this file
            .toList()

        filesToUpdate.forEach { file ->
            if (!file.exists()) {
                println("Warning: File not found: ${file.absolutePath}")
                return@forEach
            }

            try {
                val updatedContent = file
                    .readText(StandardCharsets.UTF_8)
                    .replace(ACTUAL_PROJECT_CONFIGURATION_OBJECT, projectName) // Replace "MyProject"
                    .replace(ACTUAL_PACKAGE_NAME_CONFIGURATION_OBJECT, packageName)  // Replace "com.tweener.changehere"
                    .replace(ACTUAL_PROJECT_NAME, projectName) // Replace root project name
                    .replace(ACTUAL_MODULE_NAME, moduleName) // Replace module name
                    // Replace package and import statements
                    .replace("package $ACTUAL_PACKAGE_NAME_CONFIGURATION_OBJECT", "package $packageName")
                    .replace("import $ACTUAL_PACKAGE_NAME_CONFIGURATION_OBJECT", "import $packageName")

                if (dryRun) {
                    println("\n------ Dry run: File changes for ${file.absolutePath}:")
                    println(updatedContent)
                    updatedFiles.add("${file.absolutePath} (dry run)")
                } else {
                    file.writeText(updatedContent, StandardCharsets.UTF_8)
                    println("\nUpdated file: ${file.absolutePath}")
                    updatedFiles.add(file.absolutePath)
                }
            } catch (e: Exception) {
                println("Error updating file: ${file.absolutePath}. Reason: ${e.message}")
            }
        }
    }

    private fun renameDirectories(moduleName: String, packageName: String, dryRun: Boolean, renamedDirectories: MutableList<String>) {
        println("\n--- Renaming directories ---")

        // Convert packageName to directory path (e.g., "org.example.mylibrary" -> "org/example/mylibrary")
        val packagePath = packageName.replace(".", File.separator)

        fun processAndRenameDirectory(baseDir: File, oldHierarchy: String, newHierarchy: String) {
            baseDir.walkTopDown()
                .filter { file -> file.isDirectory && file.path.contains(oldHierarchy) }
                .forEach { oldDir ->
                    // Replace only the segment starting from `oldHierarchy`
                    val updatedPath = oldDir.path.replace(oldHierarchy, newHierarchy)
                    val newDirPath = File(updatedPath)

                    // Ensure the full path to the new directory exists
                    if (!newDirPath.parentFile.exists()) {
                        if (dryRun) {
                            println("Dry run: Would create parent directories: ${newDirPath.parentFile.absolutePath}")
                        } else {
                            println("Creating parent directories: ${newDirPath.parentFile.absolutePath}")
                            newDirPath.parentFile.mkdirs()
                        }
                    }

                    // Create the target directory
                    if (!newDirPath.exists()) {
                        if (dryRun) {
                            println("Dry run: Would create directory: ${newDirPath.absolutePath}")
                        } else {
                            println("Creating directory: ${newDirPath.absolutePath}")
                            newDirPath.mkdirs()
                        }
                    }

                    // Move the files and subdirectories
                    if (dryRun) {
                        println("Dry run: Would move contents of: ${oldDir.absolutePath} -> ${newDirPath.absolutePath}")
                        renamedDirectories.add("${oldDir.absolutePath} -> ${newDirPath.absolutePath} (dry run)")
                    } else {
                        println("Moving contents of: ${oldDir.absolutePath} -> ${newDirPath.absolutePath}")
                        oldDir.listFiles()?.forEach { file ->
                            val targetFile = File(newDirPath, file.name)
                            if (file.renameTo(targetFile)) {
                                println("Moved: ${file.absolutePath} -> ${targetFile.absolutePath}")
                            } else {
                                println("Failed to move: ${file.absolutePath}")
                            }
                        }

                        // Clean up old directory if empty
                        if (oldDir.delete()) {
                            println("Deleted old directory: ${oldDir.absolutePath}")
                        }
                    }
                }
        }

        fun deleteEmptyDirectoriesRecursively(directory: File) {
            directory.walkBottomUp().forEach { subDir ->
                if (subDir.isDirectory && subDir.listFiles()?.all { !it.isFile } == true) {
                    if (dryRun) {
                        println("Dry run: Would delete empty directory: ${subDir.absolutePath}")
                    } else {
                        println("Deleting empty directory: ${subDir.absolutePath}")
                        if (!subDir.delete()) {
                            println("Failed to delete directory: ${subDir.absolutePath}")
                        }
                    }
                }
            }
        }

        // Step 1: Rename the root "changehere" directory to "mylibrary"
        project.projectDir.listFiles { file -> file.isDirectory }?.forEach { dir ->
            if (dir.name == ACTUAL_MODULE_NAME) {
                val newDir = File(dir.parentFile, moduleName)
                if (dryRun) {
                    println("Dry run: Root directory renamed: ${dir.absolutePath} -> ${newDir.absolutePath}")
                    renamedDirectories.add("${dir.absolutePath} -> ${newDir.absolutePath} (dry run)")
                } else {
                    if (dir.renameTo(newDir)) {
                        println("Renamed root directory: ${dir.absolutePath} -> ${newDir.absolutePath}")
                        renamedDirectories.add("${dir.absolutePath} -> ${newDir.absolutePath}")
                    } else {
                        println("Failed to rename root directory: ${dir.absolutePath}. Check if it's in use or locked.")
                    }
                }
            }
        }

        // Step 2: Rename subdirectories recursively inside "sample" and "mylibrary"
        val oldHierarchy = "com${File.separator}tweener${File.separator}changehere"
        project.projectDir.listFiles { file -> file.isDirectory }?.forEach { baseDir ->
            when (baseDir.name) {
                "sample", moduleName -> {
                    println("Processing subdirectories in '${baseDir.name}': ${baseDir.absolutePath}")
                    processAndRenameDirectory(baseDir, oldHierarchy, packagePath)
                }
            }
        }

        // Step 3: Delete all empty directories recursively in "sample" and "mylibrary"
        project.projectDir.listFiles { file -> file.isDirectory }?.forEach { baseDir ->
            when (baseDir.name) {
                "sample", moduleName -> {
                    println("Deleting empty directories inside '${baseDir.name}'")
                    deleteEmptyDirectoriesRecursively(baseDir)
                }
            }
        }
    }

    private fun printSummary(renamedDirectories: List<String>, updatedFiles: List<String>, dryRun: Boolean) {
        println("\n--- Summary (${if (dryRun) "Dry Run" else "Actual Run"}) ---")

        println("Renamed directories:")
        if (renamedDirectories.isEmpty()) {
            println("None")
        } else {
            renamedDirectories.forEach { println(it) }
        }

        println("\nUpdated files:")
        if (updatedFiles.isEmpty()) {
            println("None")
        } else {
            updatedFiles.forEach {
                if (it.contains("(error:")) {
                    println("Error: $it")
                } else {
                    println(it)
                }
            }
        }

        println("----------------")
        println(if (dryRun) "Dry run completed successfully!" else "Task completed successfully!")
    }

    private fun File.isInsideBuildDirectory(): Boolean {
        var current: File? = this
        while (current != null) {
            if (current.name == BUILD_DIR) return true
            current = current.parentFile
        }
        return false
    }

    private fun File.isProjectTaskFile(): Boolean =
        this.name == THIS_TASK_FILENAME && this.toPath().startsWith(File(project.projectDir, "buildSrc").toPath())
}
