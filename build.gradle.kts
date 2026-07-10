// Root build script intentionally left without a `plugins {}` block: each module
// declares (and versions) the plugins it needs itself. This keeps `:matching`
// buildable and testable on machines that don't have an Android SDK installed,
// since Gradle would otherwise try to resolve/configure the Android Gradle
// Plugin declared here even for tasks that only touch the pure-Kotlin module.
