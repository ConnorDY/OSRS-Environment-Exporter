package utils

object PackageMetadata {
    private fun getPackageInfo(f: (Package) -> String?): String? =
        try {
            javaClass.`package`?.let(f)
        } catch (e: Exception) {
            null
        }

    val NAME: String by lazy {
        getPackageInfo { it.implementationTitle } ?: "OSRS Environment Exporter"
    }

    val VERSION: String by lazy {
        getPackageInfo { it.implementationVersion } ?: ""
    }
}
