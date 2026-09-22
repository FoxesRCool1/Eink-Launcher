package io.github.foxesrcool1.margin.core.update

import io.github.foxesrcool1.margin.core.json.Json
import io.github.foxesrcool1.margin.core.json.JsonArray
import io.github.foxesrcool1.margin.core.json.JsonObject

/** One file of a GitHub Release. */
data class ReleaseAsset(
    val name: String,
    val sizeBytes: Long,
    /** The API address of the file. It works for a private repository too, which the browser address does not. */
    val apiUrl: String,
    /** Lower case hex, or null when GitHub gave none. */
    val sha256: String?,
)

/** One GitHub Release whose tag is a version. */
data class ReleaseInfo(
    val tag: String,
    val version: AppVersion,
    val title: String,
    val notes: String,
    val prerelease: Boolean,
    val assets: List<ReleaseAsset>,
)

/** Reads the answer of `GET /repos/<owner>/<name>/releases`. */
object ReleaseParser {

    /**
     * Drafts are left out, and so is any release whose tag is not a version,
     * because this app could not tell whether it is newer. Throws when the
     * text is not the list GitHub sends.
     */
    fun parseList(json: String): List<ReleaseInfo> {
        val root = Json.parse(json) as? JsonArray
            ?: throw IllegalArgumentException("The answer is not a list of releases")
        return root.objects().mapNotNull(::parseRelease)
    }

    private fun parseRelease(release: JsonObject): ReleaseInfo? {
        if (release.boolean("draft") == true) return null
        val tag = release.string("tag_name") ?: return null
        val version = AppVersion.parse(tag) ?: return null
        return ReleaseInfo(
            tag = tag,
            version = version,
            title = release.string("name")?.takeIf { it.isNotBlank() } ?: tag,
            notes = release.string("body").orEmpty(),
            prerelease = release.boolean("prerelease") == true,
            assets = release.array("assets")?.objects().orEmpty().mapNotNull(::parseAsset),
        )
    }

    private fun parseAsset(asset: JsonObject): ReleaseAsset? {
        // A file that is still uploading has a size of zero and cannot be fetched yet.
        if (asset.string("state") != null && asset.string("state") != "uploaded") return null
        val name = asset.string("name") ?: return null
        val url = asset.string("url") ?: return null
        return ReleaseAsset(
            name = name,
            sizeBytes = asset.long("size") ?: 0L,
            apiUrl = url,
            sha256 = asset.string("digest")
                ?.takeIf { it.startsWith("sha256:") }
                ?.removePrefix("sha256:")
                ?.lowercase()
                ?.takeIf { it.length == 64 },
        )
    }
}

/** Which file of a release belongs to which build of the app. */
object UpdateAssets {

    /**
     * The Release workflow names the files `<name>-<tag>-<flavour>.apk` and
     * `<name>-<tag>-<flavour>-debug.apk`. A debug build and a release build
     * have different signing keys, and one cannot install over the other, so
     * each build only ever takes its own kind.
     */
    fun pick(release: ReleaseInfo, flavor: String, debug: Boolean): ReleaseAsset? {
        val ending = if (debug) "-$flavor-debug.apk" else "-$flavor.apk"
        return release.assets.firstOrNull { it.name.endsWith(ending, ignoreCase = true) }
    }

    /**
     * The newest release this build can take, or null.
     *
     * What keeps the wrong file away from the wrong build is [pick]: a
     * release with no file for this build is passed over for the one before
     * it. The Release workflow makes normal releases only. "Pre-release" is
     * left for a release the owner marks by hand on GitHub, to try something
     * out: a debug build takes it, a release build leaves it alone.
     */
    fun newestFor(releases: List<ReleaseInfo>, flavor: String, debug: Boolean): Pair<ReleaseInfo, ReleaseAsset>? =
        releases
            .filter { debug || !it.prerelease }
            .sortedByDescending { it.version }
            .firstNotNullOfOrNull { release -> pick(release, flavor, debug)?.let { release to it } }
}
