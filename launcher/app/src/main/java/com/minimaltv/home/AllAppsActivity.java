package com.minimaltv.home;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Every installed app in one grid, A to Z. Back returns to the home screen.
 * In pick mode (EXTRA_PICK) choosing an app returns its package name instead of starting it, and apps
 * listed in EXTRA_EXCLUDE (already on the home screen) are left out.
 */
public class AllAppsActivity extends Activity {

    static final String EXTRA_PICK = "pick";
    static final String EXTRA_EXCLUDE = "exclude";
    static final String EXTRA_PACKAGE = "package";

    private GridView grid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_apps);
        final boolean pick = getIntent().getBooleanExtra(EXTRA_PICK, false);
        final String[] excluded = getIntent().getStringArrayExtra(EXTRA_EXCLUDE);
        final Set<String> exclude = new HashSet<>();
        if (excluded != null) exclude.addAll(Arrays.asList(excluded));
        if (pick) ((TextView) findViewById(R.id.title)).setText(R.string.add_to_home);

        grid = findViewById(R.id.grid);
        grid.setOnItemClickListener((parent, view, position, id) -> {
            Apps.Entry entry = (Apps.Entry) parent.getItemAtPosition(position);
            if (pick) {
                setResult(RESULT_OK, new Intent().putExtra(EXTRA_PACKAGE, entry.pkg));
                finish();
            } else {
                Apps.start(this, entry.launch);
            }
        });

        // Loading labels and icons is the slow part; keep it off the main thread so the screen opens at once.
        final PackageManager pm = getPackageManager();
        final String self = getPackageName();
        new Thread(() -> {
            final List<Apps.Entry> entries = Apps.list(pm, self);
            entries.removeIf(e -> exclude.contains(e.pkg));
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                grid.setAdapter(new EntryAdapter(entries));
                grid.requestFocus();
            });
        }).start();
    }

    private final class EntryAdapter extends BaseAdapter {
        private final List<Apps.Entry> entries;

        EntryAdapter(List<Apps.Entry> entries) {
            this.entries = entries;
        }

        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public Object getItem(int position) {
            return entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View cell = convertView != null
                    ? convertView
                    : getLayoutInflater().inflate(R.layout.item_app, parent, false);
            Apps.Entry entry = entries.get(position);
            ((ImageView) cell.findViewById(R.id.icon)).setImageDrawable(entry.icon);
            ((TextView) cell.findViewById(R.id.label)).setText(entry.label);
            return cell;
        }
    }
}
