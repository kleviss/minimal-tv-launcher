package com.minimaltv.home;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * The wallpapers bundled in assets/wallpapers, prepared by docs/design/prepare_wallpapers.py: each photo as a
 * 1080p crop plus a blurred half-size twin, and themes.json saying which colour pair each screen region uses.
 * One is picked at random per launcher start. Nothing is computed on the TV beyond decoding two JPEGs.
 */
final class Wallpapers {

    static final class Theme {
        final String file;
        final boolean topDark;
        final boolean pillsDark;
        final boolean dockDark;

        Theme(String file, boolean topDark, boolean pillsDark, boolean dockDark) {
            this.file = file;
            this.topDark = topDark;
            this.pillsDark = pillsDark;
            this.dockDark = dockDark;
        }
    }

    static final class Loaded {
        final Theme theme;
        final Bitmap sharp;
        final Bitmap blurred;

        Loaded(Theme theme, Bitmap sharp, Bitmap blurred) {
            this.theme = theme;
            this.sharp = sharp;
            this.blurred = blurred;
        }
    }

    private static final String TAG = "Wallpapers";
    private static final String DIR = "wallpapers";

    private Wallpapers() {
    }

    /** Picks a wallpaper at random and decodes it. Null when the assets are missing or broken. Call off the main thread. */
    static Loaded pickAndLoad(Context context) {
        AssetManager assets = context.getAssets();
        Theme[] themes = readThemes(assets);
        if (themes.length == 0) return null;
        Theme theme = themes[new Random().nextInt(themes.length)];
        Bitmap sharp = decode(assets, theme.file);
        if (sharp == null) return null;
        Bitmap blurred = decode(assets, theme.file.replace(".jpg", "-blur.jpg"));
        return new Loaded(theme, sharp, blurred);
    }

    private static Theme[] readThemes(AssetManager assets) {
        try (InputStream in = assets.open(DIR + "/themes.json")) {
            byte[] buf = new byte[in.available()];
            int n = in.read(buf);
            JSONArray array = new JSONArray(new String(buf, 0, n, "UTF-8"));
            Theme[] themes = new Theme[array.length()];
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                themes[i] = new Theme(o.getString("file"),
                        "dark".equals(o.optString("top")),
                        "dark".equals(o.optString("pills")),
                        "dark".equals(o.optString("dock")));
            }
            return themes;
        } catch (Exception e) {
            Log.w(TAG, "themes.json unreadable", e);
            return new Theme[0];
        }
    }

    private static Bitmap decode(AssetManager assets, String name) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.RGB_565;   // no alpha needed; half the memory of ARGB on a 1.4 GB TV
        try (InputStream in = assets.open(DIR + "/" + name)) {
            return BitmapFactory.decodeStream(in, null, opts);
        } catch (IOException e) {
            Log.w(TAG, "cannot decode " + name, e);
            return null;
        }
    }
}
