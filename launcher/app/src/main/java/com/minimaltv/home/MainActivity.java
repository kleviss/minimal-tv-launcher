package com.minimaltv.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Home screen: greeting, clock and date, three pills, and a dock with the pinned apps plus All apps.
 * Pinned apps start as the list in config.xml; the Add app pill pins more, and holding OK on a tile removes it.
 */
public class MainActivity extends Activity {

    private static final float FOCUS_SCALE = 1.08f;
    private static final int REQUEST_PICK = 1;
    private static final String PREFS = "dock";
    private static final String KEY_APPS = "apps";

    private static final ViewOutlineProvider ROUNDED = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            float radius = 14 * view.getResources().getDisplayMetrics().density;
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
        }
    };

    private TextView greeting;
    private TextView network;
    private TextView vpnStatus;
    private LinearLayout dock;
    private PopupWindow inputsPopup;
    private AlertDialog removeDialog;
    private boolean networkCallbackRegistered;
    private boolean focusPlaced;
    private float density;

    // VPN state: the network handle of the current VPN (0 when none) and the country found for it.
    private long vpnHandle;
    private String vpnCountry;
    private boolean vpnLookingUp;
    private long vpnLastLookup;

    /** The clock and date are TextClocks that tick on their own; this refreshes what they do not cover. */
    private final BroadcastReceiver minuteTick = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
        }
    };

    /** A VPN switching on or off changes the default network; this updates the header right away. */
    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            refresh();
        }

        @Override
        public void onLost(Network network) {
            refresh();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        density = getResources().getDisplayMetrics().density;
        greeting = findViewById(R.id.greeting);
        network = findViewById(R.id.btnNetwork);
        vpnStatus = findViewById(R.id.vpnStatus);
        dock = findViewById(R.id.dock);

        findViewById(R.id.btnInputs).setOnClickListener(this::showInputs);
        network.setOnClickListener(v -> open(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        findViewById(R.id.btnSettings).setOnClickListener(v -> open(new Intent(Settings.ACTION_SETTINGS)));
        findViewById(R.id.btnAdd).setOnClickListener(v -> startPicker());

        buildDock();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(minuteTick, new IntentFilter(Intent.ACTION_TIME_TICK));
        try {
            getSystemService(ConnectivityManager.class)
                    .registerDefaultNetworkCallback(networkCallback, new Handler(Looper.getMainLooper()));
            networkCallbackRegistered = true;
        } catch (RuntimeException e) {
            // The header still refreshes on resume and every minute.
        }
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(minuteTick);
        if (networkCallbackRegistered) {
            networkCallbackRegistered = false;
            try {
                getSystemService(ConnectivityManager.class).unregisterNetworkCallback(networkCallback);
            } catch (RuntimeException ignored) {
                // Already gone.
            }
        }
        if (inputsPopup != null) inputsPopup.dismiss();
        if (removeDialog != null) removeDialog.dismiss();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // The first time this window has focus (a cold start, also right after boot) begin on the first app,
        // whatever the system's own initial focus did.
        if (hasFocus && !focusPlaced) {
            focusPlaced = true;
            focusFirstTile();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed while the launcher is already running: start over on the first app.
        focusFirstTile();
    }

    private void focusFirstTile() {
        if (dock.getChildCount() > 0) dock.getChildAt(0).requestFocus();
    }

    @Override
    public void onBackPressed() {
        // Home screen: Back goes nowhere.
    }

    // ---- dock ------------------------------------------------------------------------------------

    private void buildDock() {
        dock.removeAllViews();
        PackageManager pm = getPackageManager();
        for (String pkg : pinned()) {
            try {
                addApp(pm, pkg);
            } catch (RuntimeException e) {
                // One broken app must not take the whole home screen down: leave its tile out.
            }
        }
        View all = newTile();
        showPlain(all, getDrawable(R.drawable.ic_apps), getString(R.string.all_apps));
        all.setOnClickListener(v -> startActivity(new Intent(this, AllAppsActivity.class)));
        dock.addView(all);
        // The window picks this tile when it first takes focus, so the first dock app is selected on start.
        dock.getChildAt(0).setFocusedByDefault(true);
    }

    private void addApp(PackageManager pm, String pkg) {
        Intent launch = Apps.launchIntent(pm, pkg);
        if (launch == null) return;
        ApplicationInfo info;
        try {
            info = pm.getApplicationInfo(pkg, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }
        String label = String.valueOf(info.loadLabel(pm));
        View tile = newTile();
        Drawable banner = info.loadBanner(pm);
        if (banner != null) {
            ((ImageView) tile.findViewById(R.id.image)).setImageDrawable(banner);
        } else {
            showPlain(tile, info.loadIcon(pm), label);
        }
        tile.setOnClickListener(v -> Apps.start(this, launch));
        tile.setOnLongClickListener(v -> {
            confirmRemove(pkg, label);
            return true;
        });
        dock.addView(tile);
    }

    // ---- pinned apps -----------------------------------------------------------------------------

    /** Package names on the dock, in order: what was saved on this TV, or the config.xml list before any change. */
    private List<String> pinned() {
        String saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_APPS, null);
        List<String> list = new ArrayList<>();
        if (saved == null) {
            Collections.addAll(list, getResources().getStringArray(R.array.dock_apps));
        } else if (!saved.isEmpty()) {
            Collections.addAll(list, saved.split(","));
        }
        return list;
    }

    private void savePinned(List<String> list) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_APPS, TextUtils.join(",", list)).apply();
    }

    /** Opens the app list in pick mode, without the apps that are already on the dock. */
    private void startPicker() {
        Intent picker = new Intent(this, AllAppsActivity.class)
                .putExtra(AllAppsActivity.EXTRA_PICK, true)
                .putExtra(AllAppsActivity.EXTRA_EXCLUDE, pinned().toArray(new String[0]));
        startActivityForResult(picker, REQUEST_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK || resultCode != RESULT_OK || data == null) return;
        String pkg = data.getStringExtra(AllAppsActivity.EXTRA_PACKAGE);
        if (pkg == null) return;
        List<String> list = pinned();
        if (!list.contains(pkg)) {
            list.add(pkg);
            savePinned(list);
        }
        buildDock();
        // The new app is the last tile before All apps: select it.
        dock.getChildAt(Math.max(0, dock.getChildCount() - 2)).requestFocus();
    }

    private void confirmRemove(String pkg, String label) {
        removeDialog = new AlertDialog.Builder(this)
                .setTitle(label)
                .setItems(new CharSequence[]{getString(R.string.remove_from_home)}, (dialog, which) -> {
                    List<String> list = pinned();
                    list.remove(pkg);
                    savePinned(list);
                    buildDock();
                    dock.getChildAt(0).requestFocus();
                })
                .show();
    }

    private View newTile() {
        View tile = getLayoutInflater().inflate(R.layout.tile, dock, false);
        ImageView image = tile.findViewById(R.id.image);
        image.setOutlineProvider(ROUNDED);
        image.setClipToOutline(true);
        // Plain scale on focus: cheap on a slow TV, and the white ring comes from the focused drawable state.
        tile.setOnFocusChangeListener((v, focused) -> v.animate()
                .scaleX(focused ? FOCUS_SCALE : 1f)
                .scaleY(focused ? FOCUS_SCALE : 1f)
                .setDuration(120)
                .start());
        return tile;
    }

    /** Dark tile with a centred icon and a name underneath, for apps that have no banner. */
    private void showPlain(View tile, Drawable icon, String label) {
        ImageView image = tile.findViewById(R.id.image);
        image.setBackgroundResource(R.drawable.tile_bg);
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        image.setPadding(dp(40), dp(20), dp(40), dp(44));
        image.setImageDrawable(icon);
        TextView text = tile.findViewById(R.id.label);
        text.setText(label);
        text.setVisibility(View.VISIBLE);
    }

    // ---- header ----------------------------------------------------------------------------------

    private void refresh() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int greetingRes;
        if (hour < 5) greetingRes = R.string.good_night;
        else if (hour < 12) greetingRes = R.string.good_morning;
        else if (hour < 17) greetingRes = R.string.good_afternoon;
        else if (hour < 22) greetingRes = R.string.good_evening;
        else greetingRes = R.string.good_night;
        greeting.setText(greetingRes);
        updateNetwork();
    }

    /** Reads the connections once: sets the network pill and the VPN chip. A VPN is a network of its own. */
    private void updateNetwork() {
        boolean wifi = false;
        boolean ethernet = false;
        Network vpn = null;
        try {
            ConnectivityManager cm = getSystemService(ConnectivityManager.class);
            for (Network n : cm.getAllNetworks()) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(n);
                if (caps == null) continue;
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    vpn = n;
                } else if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    wifi |= caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                    ethernet |= caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
                }
            }
        } catch (RuntimeException ignored) {
            // Show whatever was found so far.
        }
        network.setText(ethernet ? getString(R.string.ethernet) : wifi ? wifiName() : getString(R.string.offline));
        showVpn(vpn);
    }

    /** The Wi-Fi network name when it can be read, otherwise the plain label. */
    private String wifiName() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wm.getConnectionInfo();
            String ssid = info == null ? null : info.getSSID();
            if (ssid != null) {
                ssid = ssid.replace("\"", "").trim();
                if (!ssid.isEmpty() && !ssid.contains("unknown")) return ssid;
            }
        } catch (RuntimeException ignored) {
            // Fall through to the plain label.
        }
        return getString(R.string.wifi);
    }

    // ---- VPN -------------------------------------------------------------------------------------

    private void showVpn(Network vpn) {
        if (vpn == null) {
            vpnHandle = 0;
            vpnCountry = null;
            drawVpn(false);
            return;
        }
        long handle = vpn.getNetworkHandle();
        long now = SystemClock.elapsedRealtime();
        boolean newConnection = handle != vpnHandle;
        if (newConnection) {
            vpnHandle = handle;
            vpnCountry = null;
        }
        // One lookup per VPN connection, and a retry at most once a minute if no service answered.
        if (vpnCountry == null && (newConnection || (!vpnLookingUp && now - vpnLastLookup > 60_000))) {
            lookUpVpnCountry(handle, now);
        }
        drawVpn(true);
    }

    private void lookUpVpnCountry(long handle, long now) {
        vpnLookingUp = true;
        vpnLastLookup = now;
        VpnCountry.lookup(country -> runOnUiThread(() -> {
            if (handle != vpnHandle) return;   // the VPN changed while we were asking
            vpnLookingUp = false;
            vpnCountry = country;
            drawVpn(true);
        }));
    }

    private void drawVpn(boolean on) {
        String text;
        if (!on) text = getString(R.string.vpn_off);
        else if (vpnCountry != null) text = getString(R.string.vpn_country, vpnCountry);
        else text = getString(vpnLookingUp ? R.string.vpn_checking : R.string.vpn_on);
        vpnStatus.setText(text);
        vpnStatus.setTextColor(on ? 0xFFFFFFFF : 0x8CFFFFFF);
        vpnStatus.setCompoundDrawableTintList(ColorStateList.valueOf(on ? 0xFF4ADE80 : 0x59FFFFFF));
    }

    private void open(Intent intent) {
        Apps.start(this, intent);
    }

    /** Dropdown under the Inputs pill: the HDMI and AV inputs the TV reports. */
    private void showInputs(View anchor) {
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        List<TvInputInfo> inputs = new ArrayList<>();
        if (tim != null) {
            for (TvInputInfo info : tim.getTvInputList()) {
                if (info.isPassthroughInput()) inputs.add(info);
            }
        }
        if (inputs.isEmpty()) {
            Toast.makeText(this, R.string.no_inputs, Toast.LENGTH_SHORT).show();
            return;
        }
        // HDMI ports first, then AV and the rest, each group A to Z.
        Collections.sort(inputs, (a, b) -> {
            boolean ha = a.getType() == TvInputInfo.TYPE_HDMI;
            boolean hb = b.getType() == TvInputInfo.TYPE_HDMI;
            if (ha != hb) return ha ? -1 : 1;
            return String.valueOf(a.loadLabel(this)).compareToIgnoreCase(String.valueOf(b.loadLabel(this)));
        });

        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setBackgroundResource(R.drawable.popup_bg);
        rows.setPadding(dp(8), dp(8), dp(8), dp(8));
        PopupWindow popup = new PopupWindow(rows, dp(220), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));   // needed for Back to dismiss
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(0);
        for (TvInputInfo info : inputs) {
            TextView row = (TextView) getLayoutInflater().inflate(R.layout.input_row, rows, false);
            String label = String.valueOf(info.loadLabel(this));
            boolean hdmi = info.getType() == TvInputInfo.TYPE_HDMI;
            row.setText(hdmi && !label.toUpperCase(Locale.ROOT).contains("HDMI") ? "HDMI " + label : label);
            String id = info.getId();
            row.setOnClickListener(v -> {
                popup.dismiss();
                open(new Intent(Intent.ACTION_VIEW).setDataAndType(
                        TvContract.buildChannelUriForPassthroughInput(id), "vnd.android.cursor.item/channel"));
            });
            rows.addView(row);
        }
        inputsPopup = popup;
        popup.showAsDropDown(anchor, 0, dp(10), Gravity.START);
        rows.getChildAt(0).requestFocus();
    }

    private int dp(int value) {
        return (int) (value * density + 0.5f);
    }
}
