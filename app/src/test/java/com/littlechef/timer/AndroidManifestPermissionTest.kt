package com.littlechef.timer

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AndroidManifestPermissionTest {

    @Test
    fun manifestDeclaresRequiredExactAlarmPermissions() {
        val manifestFile = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
            File("../app/src/main/AndroidManifest.xml"),
        ).firstOrNull { it.exists() } ?: error("AndroidManifest.xml not found")

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile)
        val permissions = doc.getElementsByTagName("uses-permission")

        val permissionNames = mutableListOf<String>()
        for (i in 0 until permissions.length) {
            val node = permissions.item(i)
            val name = node.attributes.getNamedItem("android:name")?.nodeValue
            if (name != null) {
                permissionNames.add(name)
            }
        }

        assertTrue(
            "USE_EXACT_ALARM must be declared for Android 13+ exact alarms",
            permissionNames.contains("android.permission.USE_EXACT_ALARM"),
        )
        assertTrue(
            "SCHEDULE_EXACT_ALARM must be declared for Android 12 compatibility",
            permissionNames.contains("android.permission.SCHEDULE_EXACT_ALARM"),
        )
        assertTrue(
            "POST_NOTIFICATIONS must be declared for Android 13+ ringing notifications",
            permissionNames.contains("android.permission.POST_NOTIFICATIONS"),
        )
    }
}
