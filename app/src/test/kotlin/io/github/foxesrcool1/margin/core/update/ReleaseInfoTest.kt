package io.github.foxesrcool1.margin.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun `a tag and a version name give the same version`() {
        assertEquals(AppVersion(0, 1, 2), AppVersion.parse("v0.1.2"))
        assertEquals(AppVersion(0, 1, 2), AppVersion.parse("0.1.2"))
        assertEquals(AppVersion(0, 1, 2), AppVersion.parse("0.1.2-viwoods"))
        assertEquals(AppVersion(12, 0, 99), AppVersion.parse(" v12.0.99 "))
    }

    @Test
    fun `text that is not a version gives null`() {
        listOf("", "v1", "1.2", "latest", "v1.2.x", "1.2.3.4", "v1.100.0", "v1.0.100", "nightly-1.2.3").forEach {
            assertNull("'$it' was taken as a version", AppVersion.parse(it))
        }
    }

    @Test
    fun `the code is the sum the build file uses`() {
        assertEquals(100, AppVersion(0, 1, 0).code)
        assertEquals(10_203, AppVersion(1, 2, 3).code)
    }

    @Test
    fun `versions sort by number and not by text`() {
        assertTrue(AppVersion(0, 10, 0) > AppVersion(0, 9, 9))
        assertTrue(AppVersion(1, 0, 0) > AppVersion(0, 99, 99))
        assertEquals(0, AppVersion(0, 1, 0).compareTo(AppVersion.parse("v0.1.0-generic")!!))
    }
}

class ReleaseParserTest {

    @Test
    fun `the list GitHub sends is read`() {
        val releases = ReleaseParser.parseList(UpdateTestData.releasesJson())

        // GitHub's order is kept. Picking the newest is the job of UpdateAssets.
        assertEquals(listOf("v0.1.1", "v0.2.0"), releases.map { it.tag })
        val newest = releases.first { it.tag == "v0.2.0" }
        assertEquals(AppVersion(0, 2, 0), newest.version)
        assertEquals("Margin v0.2.0", newest.title)
        assertEquals("## 0.2.0\n\n- A fix.", newest.notes)
        assertEquals(false, newest.prerelease)
        assertEquals(4, newest.assets.size)

        val asset = newest.assets.first { it.name == "margin-v0.2.0-viwoods.apk" }
        assertEquals(1000L, asset.sizeBytes)
        assertEquals("https://api.github.com/repos/o/r/releases/assets/11", asset.apiUrl)
        assertEquals("a".repeat(64), asset.sha256)
    }

    @Test
    fun `a draft and a tag that is not a version are left out`() {
        val tags = ReleaseParser.parseList(UpdateTestData.releasesJson()).map { it.tag }
        assertTrue("draft" !in tags.joinToString())
        assertTrue("nightly" !in tags)
    }

    @Test
    fun `a file that is still uploading is left out`() {
        val old = ReleaseParser.parseList(UpdateTestData.releasesJson()).first { it.tag == "v0.1.1" }
        assertEquals(listOf("margin-v0.1.1-viwoods-debug.apk"), old.assets.map { it.name })
    }

    @Test
    fun `a digest that is not sha256 is ignored`() {
        val json = """[{"tag_name":"v1.0.0","draft":false,"assets":[
            {"name":"a-viwoods.apk","size":5,"url":"https://x/1","state":"uploaded","digest":"md5:abc"}]}]"""
        assertNull(ReleaseParser.parseList(json).single().assets.single().sha256)
    }

    @Test
    fun `an answer that is not a list is refused`() {
        assertThrows(IllegalArgumentException::class.java) { ReleaseParser.parseList("""{"message":"Not Found"}""") }
        assertThrows(IllegalArgumentException::class.java) { ReleaseParser.parseList("<html>") }
    }
}

class UpdateAssetsTest {

    private val releases = ReleaseParser.parseList(UpdateTestData.releasesJson())

    @Test
    fun `each build takes only its own kind of file`() {
        val newest = releases.first { it.tag == "v0.2.0" }
        assertEquals("margin-v0.2.0-viwoods.apk", UpdateAssets.pick(newest, "viwoods", debug = false)?.name)
        assertEquals("margin-v0.2.0-viwoods-debug.apk", UpdateAssets.pick(newest, "viwoods", debug = true)?.name)
        assertEquals("margin-v0.2.0-generic.apk", UpdateAssets.pick(newest, "generic", debug = false)?.name)
        assertNull(UpdateAssets.pick(newest, "other", debug = false))
    }

    @Test
    fun `the newest version wins, whatever order the list is in`() {
        assertEquals("v0.2.0", UpdateAssets.newestFor(releases, "viwoods", debug = true)?.first?.tag)
        assertEquals("v0.2.0", UpdateAssets.newestFor(releases.reversed(), "viwoods", debug = true)?.first?.tag)
    }

    @Test
    fun `a release build leaves a pre-release alone and a debug build takes it`() {
        val json = """[{"tag_name":"v0.3.0","draft":false,"prerelease":true,"assets":[
            {"name":"e-v0.3.0-viwoods.apk","size":5,"url":"https://x/1","state":"uploaded"},
            {"name":"e-v0.3.0-viwoods-debug.apk","size":5,"url":"https://x/2","state":"uploaded"}]}]"""
        val list = ReleaseParser.parseList(json)
        assertNull(UpdateAssets.newestFor(list, "viwoods", debug = false))
        assertEquals("e-v0.3.0-viwoods-debug.apk", UpdateAssets.newestFor(list, "viwoods", debug = true)?.second?.name)
    }

    @Test
    fun `a normal release with debug files only is for debug builds, with no pre-release mark to say so`() {
        // What the Release workflow makes while there is no release key.
        val json = """[{"tag_name":"v0.1.0","draft":false,"prerelease":false,"assets":[
            {"name":"margin-v0.1.0-viwoods-debug.apk","size":5,"url":"https://x/1","state":"uploaded"},
            {"name":"margin-v0.1.0-generic-debug.apk","size":5,"url":"https://x/2","state":"uploaded"}]}]"""
        val list = ReleaseParser.parseList(json)

        assertNull(UpdateAssets.newestFor(list, "viwoods", debug = false))
        assertEquals(
            "margin-v0.1.0-viwoods-debug.apk",
            UpdateAssets.newestFor(list, "viwoods", debug = true)?.second?.name,
        )
    }

    @Test
    fun `a release with no file for this build is skipped for the one before it`() {
        val json = """[
            {"tag_name":"v0.4.0","draft":false,"assets":[
                {"name":"e-v0.4.0-generic.apk","size":5,"url":"https://x/1","state":"uploaded"}]},
            {"tag_name":"v0.3.0","draft":false,"assets":[
                {"name":"e-v0.3.0-viwoods.apk","size":5,"url":"https://x/2","state":"uploaded"}]}]"""
        val found = UpdateAssets.newestFor(ReleaseParser.parseList(json), "viwoods", debug = false)
        assertEquals("v0.3.0", found?.first?.tag)
    }
}

/** A cut-down copy of what `GET /repos/<owner>/<name>/releases` really sends. */
object UpdateTestData {

    fun releasesJson(debugSha: String = "b".repeat(64), debugSize: Long = 2000): String = """
        [
          {"tag_name":"v9.9.9-draft","name":"draft","draft":true,"prerelease":false,"body":"","assets":[]},
          {"tag_name":"nightly","name":"nightly","draft":false,"prerelease":false,"body":"","assets":[]},
          {
            "tag_name":"v0.1.1","name":"","draft":false,"prerelease":true,"body":"old",
            "assets":[
              {"name":"margin-v0.1.1-viwoods-debug.apk","size":10,"state":"uploaded",
               "url":"https://api.github.com/repos/o/r/releases/assets/1","digest":null},
              {"name":"margin-v0.1.1-generic-debug.apk","size":0,"state":"starter",
               "url":"https://api.github.com/repos/o/r/releases/assets/2"}
            ]
          },
          {
            "tag_name":"v0.2.0","name":"Margin v0.2.0","draft":false,"prerelease":false,
            "body":"## 0.2.0\n\n- A fix.",
            "assets":[
              {"name":"margin-v0.2.0-viwoods.apk","size":1000,"state":"uploaded",
               "url":"https://api.github.com/repos/o/r/releases/assets/11",
               "digest":"sha256:${"A".repeat(64)}"},
              {"name":"margin-v0.2.0-generic.apk","size":1000,"state":"uploaded",
               "url":"https://api.github.com/repos/o/r/releases/assets/12","digest":"sha256:${"c".repeat(64)}"},
              {"name":"margin-v0.2.0-viwoods-debug.apk","size":$debugSize,"state":"uploaded",
               "url":"https://api.github.com/repos/o/r/releases/assets/13","digest":"sha256:$debugSha"},
              {"name":"SHA256SUMS.txt","size":300,"state":"uploaded",
               "url":"https://api.github.com/repos/o/r/releases/assets/14"}
            ]
          }
        ]
    """.trimIndent()
}
