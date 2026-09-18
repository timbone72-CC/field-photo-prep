package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.XmlResourceParser;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;

import java.util.LinkedHashSet;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public final class BackupRulesInstrumentedTest {
    private static final Set<String> EXPECTED_DOMAINS = Set.of(
            "root",
            "file",
            "database",
            "sharedpref",
            "external",
            "device_root",
            "device_file",
            "device_database",
            "device_sharedpref");

    @Test
    public void manifestStillAllowsBackupFrameworkSoExplicitRulesCanGovernIt() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ApplicationInfo info = context.getPackageManager()
                .getApplicationInfo(context.getPackageName(), 0);
        assertTrue((info.flags & ApplicationInfo.FLAG_ALLOW_BACKUP) != 0);
    }

    @Test
    public void android12RulesExcludeAllAppOwnedDomainsFromCloudAndDeviceTransfer()
            throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals(
                EXPECTED_DOMAINS,
                exclusionsForSection(
                        context,
                        R.xml.data_extraction_rules,
                        "cloud-backup"));
        assertEquals(
                EXPECTED_DOMAINS,
                exclusionsForSection(
                        context,
                        R.xml.data_extraction_rules,
                        "device-transfer"));
    }

    @Test
    public void legacyRulesExcludeAllAppOwnedDomains() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals(
                EXPECTED_DOMAINS,
                exclusionsForSection(context, R.xml.full_backup_content, null));
    }

    private static Set<String> exclusionsForSection(
            Context context,
            int resourceId,
            String wantedSection) throws Exception {
        LinkedHashSet<String> domains = new LinkedHashSet<>();
        try (XmlResourceParser parser = context.getResources().getXml(resourceId)) {
            String activeSection = null;
            int event;
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if ("cloud-backup".equals(name) || "device-transfer".equals(name)) {
                        activeSection = name;
                        continue;
                    }
                    if (!"exclude".equals(name)) {
                        continue;
                    }
                    if (wantedSection != null && !wantedSection.equals(activeSection)) {
                        continue;
                    }
                    String domain = parser.getAttributeValue(null, "domain");
                    String path = parser.getAttributeValue(null, "path");
                    assertEquals("Every exclusion must cover its complete domain", ".", path);
                    domains.add(domain);
                } else if (event == XmlPullParser.END_TAG
                        && parser.getName().equals(activeSection)) {
                    activeSection = null;
                }
            }
        }
        return domains;
    }
}
