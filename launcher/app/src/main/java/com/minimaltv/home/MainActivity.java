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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
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
 * Home screen: a random wallpaper, greeting, clock and date centred near the top, three frosted pills top right,
 * and a dock with the pinned apps plus All apps, the focused app's name above the row, VPN status and Add app
 * below it. Pinned apps start as the list in config.xml; the Add app pill pins more, and holding OK on a tile
 * removes it. The look follows docs/design/mockups/d-hybrid.html: every piece of chrome belongs to the TOP or
 * the DOCK region and takes that region's colour pair, light frost + dark ink on a pale photo or the reverse
 * on a dark one, as sampled at build time into assets/wallpapers/themes.json.
 */
public class MainActivity extends Activity {

    private static final float FOCUS_SCALE = 1.08f;
    private static final float PILL_FOCUS_SCALE = 1.06f;
    private static final int REQUEST_PICK = 1;
    private static final String PREFS = "dock";
    private static final String KEY_APPS = "apps";

    private static final ViewOutlineProvider ROUNDED = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            float radius = 9 * view.getResources().getDisplayMetrics().density;
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
        }
    };

    /** One colour pair: what a region's chrome is drawn with. */
    private static final class Pair {
        final int ink, tint, tintFocus, edge, dotOff, dotOn;

        Pair(Resources r, boolean dark) {
            ink = r.getColor(dark ? R.color.dark_ink : R.color.light_ink, null);
            tint = r.getColor(dark ? R.color.dark_tint : R.color.light_tint, null);
            tintFocus = r.getColor(dark ? R.color.dark_tint_focus : R.color.light_tint_focus, null);
            edge = r.getColor(dark ? R.color.dark_edge : R.color.light_edge, null);
            dotOff = r.getColor(dark ? R.color.dark_dot_off : R.color.light_dot_off, null);
            dotOn = r.getColor(dark ? R.color.dark_dot_on : R.color.light_dot_on, null);
        }
    }

    private TextView greeting;
    private TextView network;
    private TextView vpnStatus;
    private TextView appName;
    private LinearLayout dock;
    private ImageView wallpaperView;
    private Bitmap blurred;
    private Pair top, pills, dockPair;
    private int screenW, screenH;
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
        screenW = getResources().getDisplayMetrics().widthPixels;
        screenH = getResources().getDisplayMetrics().heightPixels;
        greeting = findViewById(R.id.greeting);
        network = findViewById(R.id.btnNetwork);
        vpnStatus = findViewById(R.id.vpnStatus);
        appName = findViewById(R.id.appName);
        dock = findViewById(R.id.dock);
        wallpaperView = findViewById(R.id.wallpaper);

        findViewById(R.id.btnInputs).setOnClickListener(this::showInputs);
        network.setOnClickListener(v -> open(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        findViewById(R.id.btnSettings).setOnClickListener(v -> open(new Intent(Settings.ACTION_SETTINGS)));
        findViewById(R.id.btnAdd).setOnClickListener(v -> startPicker());

        // Until the photo is decoded, everything is drawn with the light pair on black: readable, and only
        // for a moment. The wallpaper thread then applies the real theme.
        applyTheme(false, false, false);
        buildDock();

        // The square panel gets rounded corners: pure black outside a 48px-radius rectangle, over everything.
        FrameLayout root = findViewById(R.id.root);
        root.addView(new CornerMask(this, 24 * density),
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        new Thread(() -> {
            final Wallpapers.Loaded loaded = Wallpapers.pickAndLoad(this);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || loaded == null) return;
                blurred = loaded.blurred;
                wallpaperView.setImageBitmap(loaded.sharp);
                applyTheme(loaded.theme.topDark, loaded.theme.pillsDark, loaded.theme.dockDark);
                buildDock();
                if (getCurrentFocus() == null) focusFirstTile();
            });
        }).start();
    }

    // ---- look -------------------------------------------------------------------------------------

    /** Colours every piece of chrome from its region's pair and repaints the two scrims. */
    private void applyTheme(boolean topDark, boolean pillsDark, boolean dockDark) {
        Resources r = getResources();
        top = new Pair(r, topDark);
        pills = new Pair(r, pillsDark);
        dockPair = new Pair(r, dockDark);

        int ink = top.ink;
        greeting.setTextColor(ink);
        ((TextView) findViewById(R.id.clock)).setTextColor(ink);
        ((TextView) findViewById(R.id.date)).setTextColor(ink);

        for (int id : new int[]{R.id.btnInputs, R.id.btnNetwork, R.id.btnSettings}) {
            dressPill(findViewById(id), pills);
        }
        dressPill(findViewById(R.id.btnAdd), dockPair);
        appName.setTextColor(dockPair.ink);
        vpnStatus.setTextColor(dockPair.ink);
        drawVpn(vpnHandle != 0);

        // Scrims: a radial lift behind the clock block, a linear lift under the dock, each in its region's tint.
        int topTint = top.tint & 0x00FFFFFF;
        GradientDrawable radial = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x57000000 | topTint, 0x00000000 | topTint});
        radial.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        radial.setGradientCenter(0.5f, 0.36f);
        radial.setGradientRadius(screenW * 0.42f);
        findViewById(R.id.scrimTop).setBackground(radial);

        // The lift under the dock eases in over its top 55% so it never shows as a fold, and sits a little
        // stronger on a dark pair, whose light ink needs it.
        findViewById(R.id.scrimDock).setBackground(
                new LiftDrawable(dockPair.tint, dockDark ? 0x5C : 0x30, 0.65f));
    }

    /** Frost under a pill, its ink, and its focus state (fills brighter, scales a touch, one soft shadow). */
    private void dressPill(TextView pill, Pair pair) {
        FrostDrawable frost = new FrostDrawable(blurred, screenW, screenH, 14 * density, pair.tint, pair.edge, density);
        pill.setBackground(frost);
        pill.setTextColor(pair.ink);
        pill.setCompoundDrawableTintList(ColorStateList.valueOf(pair.ink));
        pill.setOutlineProvider(null);
        trackPosition(pill, frost);
        pill.setOnFocusChangeListener((v, focused) -> {
            frost.setFill(focused ? pair.tintFocus : pair.tint);
            v.animate().scaleX(focused ? PILL_FOCUS_SCALE : 1f).scaleY(focused ? PILL_FOCUS_SCALE : 1f)
                    .translationZ(focused ? 14 * density : 0f).setDuration(120).start();
        });
        if (pill.isFocused()) frost.setFill(pair.tintFocus);
    }

    /** Keeps a frost drawable pointed at the patch of blurred photo under its view. */
    private void trackPosition(View view, FrostDrawable frost) {
        ViewTreeObserver.OnPreDrawListener listener = () -> {
            int[] xy = new int[2];
            view.getLocationOnScreen(xy);
            frost.setScreenOffset(xy[0], xy[1]);
            return true;
        };
        view.getViewTreeObserver().addOnPreDrawListener(listener);
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
        all.setTag(R.id.label, getString(R.string.all_apps));
        all.setOnClickListener(v -> startActivity(new Intent(this, AllAppsActivity.class)));
        dock.addView(all);
        // The window picks this tile when it first takes focus, so the first dock app is selected on start.
        dock.getChildAt(0).setFocusedByDefault(true);
        View focused = getCurrentFocus();
        appName.setText(focused != null && focused.getParent() == dock ? String.valueOf(focused.getTag(R.id.label)) : "");
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
        tile.setTag(R.id.label, label);
        Drawable banner = info.loadBanner(pm);
        if (banner != null) {
            ImageView image = tile.findViewById(R.id.image);
            image.setImageDrawable(banner);
            // The hairline keeps a white banner visible on a pale photo (the mockup's inset 1px at 8%).
            GradientDrawable hair = new GradientDrawable();
            hair.setCornerRadius(9 * density);
            hair.setStroke(Math.max(1, (int) density), 0x14221C17);
            tile.setForeground(hair);
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
        int count = pinned().size() + 1;   // plus All apps
        if (count > 3) {
            float scale = Math.max(0.72f, 3f / count);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) tile.getLayoutParams();
            lp.width = Math.round(lp.width * scale);
            lp.height = Math.round(lp.height * scale);
            lp.setMarginEnd(Math.round(lp.getMarginEnd() * scale));
            tile.setLayoutParams(lp);
        }
        ImageView image = tile.findViewById(R.id.image);
        image.setOutlineProvider(ROUNDED);
        image.setClipToOutline(true);
        // tvOS lift: the focused tile grows a little and gets one soft shadow, and its name appears above the row.
        tile.setOutlineProvider(ROUNDED);
        tile.setOnFocusChangeListener((v, focused) -> {
            v.animate()
                    .scaleX(focused ? FOCUS_SCALE : 1f)
                    .scaleY(focused ? FOCUS_SCALE : 1f)
                    .translationZ(focused ? 18 * density : 0f)
                    .setDuration(140)
                    .start();
            if (focused) appName.setText(String.valueOf(v.getTag(R.id.label)));
            else if (appName.getText().toString().equals(String.valueOf(v.getTag(R.id.label)))) appName.setText("");
        });
        return tile;
    }

    /** Frosted tile with a centred icon and a name underneath, for All apps and apps that have no banner. */
    private void showPlain(View tile, Drawable icon, String label) {
        ImageView image = tile.findViewById(R.id.image);
        int tint = (dockPair.tint & 0x00FFFFFF) | 0xA3000000;   // a touch more opaque than a pill
        FrostDrawable frost = new FrostDrawable(blurred, screenW, screenH, 9 * density, tint, dockPair.edge, density);
        image.setBackground(frost);
        trackPosition(image, frost);
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        // Padding in proportion to the tile, so the glyph stays the same relative size when tiles shrink.
        ViewGroup.LayoutParams tl = tile.getLayoutParams();
        float k = tl.width / (192f * density);
        image.setPadding(Math.round(dp(70) * k), Math.round(dp(18) * k), Math.round(dp(70) * k), Math.round(dp(40) * k));
        image.setImageDrawable(icon);
        image.setImageTintList(ColorStateList.valueOf(dockPair.ink));
        TextView text = tile.findViewById(R.id.label);
        text.setText(label);
        text.setTextColor(dockPair.ink);
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
        String label = ethernet ? getString(R.string.ethernet) : wifi ? wifiName() : getString(R.string.offline);
        // Long network names ("Home FRITZ!Box 7590") would be cut off: keep the first word.
        if (label.length() > 14 && label.contains(" ")) label = label.substring(0, label.indexOf(' '));
        network.setText(label);
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
        if (dockPair == null) return;
        vpnStatus.setTextColor(dockPair.ink);
        vpnStatus.setCompoundDrawableTintList(ColorStateList.valueOf(on ? dockPair.dotOn : dockPair.dotOff));
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
        FrostDrawable panel = new FrostDrawable(blurred, screenW, screenH, 14 * density, pills.tint, pills.edge, density);
        rows.setBackground(panel);
        trackPosition(rows, panel);
        rows.setPadding(dp(6), dp(6), dp(6), dp(6));
        rows.setElevation(12 * density);
        PopupWindow popup = new PopupWindow(rows, dp(220), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));   // needed for Back to dismiss
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(0);
        for (TvInputInfo info : inputs) {
            TextView row = (TextView) getLayoutInflater().inflate(R.layout.input_row, rows, false);
            String label = String.valueOf(info.loadLabel(this));
            boolean hdmi = info.getType() == TvInputInfo.TYPE_HDMI;
            row.setText(hdmi && !label.toUpperCase(Locale.ROOT).contains("HDMI") ? "HDMI " + label : label);
            row.setTextColor(pills.ink);
            GradientDrawable focusFill = new GradientDrawable();
            focusFill.setCornerRadius(10 * density);
            focusFill.setColor(pills.tintFocus);
            row.setOnFocusChangeListener((v, focused) -> v.setBackground(focused ? focusFill : null));
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
