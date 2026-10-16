package com.yourco.spend.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class BackupRulesTest {

    private val resXmlDir = File("src/main/res/xml")

    @Test
    fun backupRulesXml_containsRequiredIncludesAndExcludes() {
        val backupFile = File(resXmlDir, "backup_rules.xml")
        assertTrue("backup_rules.xml should exist", backupFile.exists())

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(backupFile)
        val root = doc.documentElement

        assertEquals("full-backup-content", root.tagName)

        val includes = root.getElementsByTagName("include")
        val excludes = root.getElementsByTagName("exclude")

        var hasDatabase = false
        var hasDatastore = false
        for (i in 0 until includes.length) {
            val el = includes.item(i) as Element
            val domain = el.getAttribute("domain")
            val path = el.getAttribute("path")
            if (domain == "database") hasDatabase = true
            if (domain == "file" && path.contains("preferences_pb")) hasDatastore = true
        }

        assertTrue("backup_rules.xml must include database domain", hasDatabase)
        assertTrue("backup_rules.xml must include DataStore preferences file", hasDatastore)

        var includesCache = false
        for (i in 0 until includes.length) {
            val el = includes.item(i) as Element
            val domain = el.getAttribute("domain")
            val path = el.getAttribute("path")
            if (domain == "cache" || path.contains("cache")) includesCache = true
        }

        assertTrue("backup_rules.xml must not include cache", !includesCache)
    }

    @Test
    fun dataExtractionRulesXml_containsRequiredIncludesAndExcludes() {
        val rulesFile = File(resXmlDir, "data_extraction_rules.xml")
        assertTrue("data_extraction_rules.xml should exist", rulesFile.exists())

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(rulesFile)
        val root = doc.documentElement

        assertEquals("data-extraction-rules", root.tagName)

        val cloudBackup = root.getElementsByTagName("cloud-backup")
        val deviceTransfer = root.getElementsByTagName("device-transfer")

        assertTrue("Must contain cloud-backup element", cloudBackup.length > 0)
        assertTrue("Must contain device-transfer element", deviceTransfer.length > 0)

        val cloudEl = cloudBackup.item(0) as Element
        val cloudIncludes = cloudEl.getElementsByTagName("include")
        val cloudExcludes = cloudEl.getElementsByTagName("exclude")

        var hasDb = false
        var hasDatastore = false
        for (i in 0 until cloudIncludes.length) {
            val el = cloudIncludes.item(i) as Element
            val domain = el.getAttribute("domain")
            val path = el.getAttribute("path")
            if (domain == "database") hasDb = true
            if (domain == "file" && path.contains("preferences_pb")) hasDatastore = true
        }
        assertTrue("cloud-backup must include database", hasDb)
        assertTrue("cloud-backup must include DataStore file", hasDatastore)

        var includesCache = false
        for (i in 0 until cloudIncludes.length) {
            val el = cloudIncludes.item(i) as Element
            val domain = el.getAttribute("domain")
            val path = el.getAttribute("path")
            if (domain == "cache" || path.contains("cache")) includesCache = true
        }
        assertTrue("cloud-backup must not include cache", !includesCache)
    }

    @Test
    fun localesConfigXml_containsEnAndZhTw() {
        val localesFile = File(resXmlDir, "locales_config.xml")
        assertTrue("locales_config.xml should exist", localesFile.exists())

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(localesFile)
        val root = doc.documentElement

        assertEquals("locale-config", root.tagName)
        val locales = root.getElementsByTagName("locale")

        val localeNames = mutableListOf<String>()
        for (i in 0 until locales.length) {
            val el = locales.item(i) as Element
            val name = el.getAttribute("android:name")
            localeNames.add(name)
        }

        assertTrue("locales_config.xml must contain en", localeNames.contains("en"))
        assertTrue("locales_config.xml must contain zh-TW", localeNames.contains("zh-TW"))
    }
}
