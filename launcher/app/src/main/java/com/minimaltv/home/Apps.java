package com.minimaltv.home;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Finding and starting apps. */
final class Apps {

    static final class Entry {
        final String pkg;
        final String label;
        final Drawable icon;
        final Intent launch;

        Entry(String pkg, String label, Drawable icon, Intent launch) {
            this.pkg = pkg;
            this.label = label;
            this.icon = icon;
            this.launch = launch;
        }
    }

    private Apps() {
    }

    /** Launch intent for a package: its leanback entry first, then any launcher entry. Null when not installed. */
    static Intent launchIntent(PackageManager pm, String pkg) {
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        return i != null ? i : pm.getLaunchIntentForPackage(pkg);
    }

    static void start(Context context, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(context, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
    }

    /** Every app with a TV or phone launcher entry except this one, A to Z. Loads icons: call off the main thread. */
    static List<Entry> list(PackageManager pm, String self) {
        Map<String, Entry> byPackage = new LinkedHashMap<>();
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
                ActivityInfo ai = ri.activityInfo;
                if (ai.packageName.equals(self) || byPackage.containsKey(ai.packageName)) continue;
                Intent launch = new Intent(Intent.ACTION_MAIN).addCategory(category)
                        .setComponent(new ComponentName(ai.packageName, ai.name));
                byPackage.put(ai.packageName,
                        new Entry(ai.packageName, String.valueOf(ri.loadLabel(pm)), ri.loadIcon(pm), launch));
            }
        }
        List<Entry> entries = new ArrayList<>(byPackage.values());
        Collections.sort(entries, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return entries;
    }
}
