# Examples

One folder per TV model, each with a `debloat.txt` listing the packages that are safe to disable
on that firmware, grouped and commented. These are starting points: comment out anything you use.

Most lists come from [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv). Where a note mentions launcher
options or key services (such as `HOME_KEY_SERVICE`), it refers to that project's launcher, which this one does not have.

| Folder | TV | OS | Notes |
|---|---|---|---|
| `benq-e55-735/` | BenQ E55-735 (Changhong AI PONT, DVB96BQ) | Google TV 14 | Homwee store/help, Anoki ACR, Google TV recommendations; wireless-debugging pairing, setupwraith, deep-standby notes |
| `chiq-ai-pont/` | CHiQ AI PONT (DVB93CQ) | Android TV 11 | Anoki ACR, FAST channels, AI PONT, Homwee services |
| `google-chromecast-gtv-4k/` | Chromecast with Google TV 4K (sabrina) | Google TV 14 | Google telemetry and ad services, ambient mode, launcherx + setupwraith; wireless-debugging pairing, YouTube button remap |
| `hisense-smarttv-4k-ffm/` | Hisense SmartTV 4K FFM (huangshan, MediaTek) | Android TV 10 | ACR (evservice), VIDAA preload, store/hotel mode, OTA; smartsense must stay enabled |
| `insignia-ns55f301na25/` | Insignia 55" F30 4K Fire TV (NS-55F301NA25, AFTDCT31, duckie) | Fire OS 7 / Android 9 | Amazon ACR telemetry, Fire TV ads/shopping, Alexa, OTA updates; AirPlay and TIF HDMI inputs kept |
| `nokia-vestel-android-tv-11/` | Nokia-branded TV built by Vestel (yeoksam, MediaTek/MStar, 1.4 GB RAM) | Android TV 11 | Stock launcher recommendations, Google feedback agent, partner auto-installs, demo mode, Vestel reporter; voice and phone remote off by choice, Chromecast kept so casting works. Measured memory gain, and what to leave alone (remote keys, backlight, TV stack, Bluetooth) |
| `nvidia-shield-tv-pro-2019/` | NVIDIA SHIELD TV Pro 2019 (P2897, mdarcy) | Android TV 11 | NVIDIA telemetry, GameStream leftovers, sponsored rows, partner auto-installs |
| `oneplus-tv-dubai/` | OnePlus TV (D00, Dubai) | Google TV 14 | HeyTap/OnePlus extras, OnePlus Connect, Smart Home |
| `philips-smart-4k-google-tv/` | Philips Smart 4K TV (Skyworth, eleonas, MediaTek), India | Google TV 14 | Skyworth recommendations, after-sale/activation trackers, iCast; Chromecast, Assistant off by choice; wireless-debugging pairing, setupwraith |
| `rca-ai-pont/` | RCA AI PONT SA (Changhong, redwood, ISDB92RC; also sold as realme 40-inch) | Android 14 | Homwee store and installers, Anoki ACR, FAST channels; 1 GB |
| `sanyo-atv-r2/` | SANYO ATV R2 (SCBC, Realtek RTD285O) | Android TV 9 | S-Channels, E-SHOW demo, Vewd, TV Booster, SANYO OTA |
| `sony-bravia-4k-ur2/` | Sony BRAVIA 4K UR2 (BRAVIA_UR2_4K, MediaTek) | Android TV 10 | Samba TV, Sony promos/Select, demo mode; Assistant, Chromecast, unused streaming apps off by choice |
| `sony-xbr-x900e/` | Sony BRAVIA XBR-65X900E (2017, BRAVIA_ATV2) | Android TV 9 | Samba TV ACR, Sony Select, My BRAVIA promos, Discover, device agents; Chromecast, DLNA, mirroring off by choice |
| `stellamore-t950s/` | Stellamore T950S projector (Droidlogic be30a3, Amlogic txhd2) | Android 13 | Zeasn App Store and stock launcher both bundle Google Ads (doubleclick); Zeasn device portal/OTA, Stellamore factory/test/guide; 1 GB; keystone correction must stay enabled |
| `tcl-g10-google-tv/` | TCL 55" 4K Google TV, 2023 (G10, G10_4K_US_NF) | Google TV 12 | TCL TV+, TCL Live, store demo, smartlink installer |
| `tcl-65c725/` | TCL 65C725 (Smart TV Pro, BeyondTV4) | Android TV 11 | TCL analytics/promos, Google recommendation rows; voice off, OTA off by choice |
| `tcl-ak30a5/` | TCL ak30a5 (China, Amlogic) | Android TV 9 | No Google services, TCL boot ad + analytics, no TvInputManager, installs via USB only; debloat only, third-party launchers cannot be installed on this firmware |
| `tcl-q10g-pro/` | TCL 55Q10G Pro (China, tcl_mt5879_cn, MediaTek MT9952) | Android TV 11 | No Google services, TCL boot ad + analytics/BI, OTA and account services; ADB opens from Device info + UP/DOWN/LEFT/RIGHT, not the build number; debloat only, stock launcher cannot be replaced (signature check at install time) |
| `tcl-beyondtv/` | TCL BeyondTV (India) | Android TV 9 | TCL analytics, content feed, TCL app store, Google recommendation rows; Jio/Airtel/Hotstar apps untouched |
| `tcl-qm6k-google-tv/` | TCL QM6K Pro, 2025 (G08, MediaTek tcl9618) | Google TV 14 | Samba TV ACR (ttvs), TCL TV+, TCL Live, store/hotel demo; setupwraith must go for a custom launcher (also AU14, #23) |
| `tcl-g05-google-tv/` | TCL Smart TV Pro (G05, G05_4K_US, MediaTek MT5889) | Google TV 11 | TCL Channel, waterfall, ttvs BI, TGuard; Quick Panel off by choice; setupwraith returned after a reboot, launcher half unverified |
| `toshiba-fire-tv-aftha004/` | Toshiba Fire TV (AFTHA004, hazel) | Fire OS 7 / Android 9 | Amazon ACR, metrics, shopping, tutorials, Miracast sink; streaming preloads off by choice; debloat only |
| `xgimi-horizon-s-pro/` | XGIMI Horizon S Pro projector (XK03H, xgimigalileo, MediaTek) | Android TV 11 | First projector: keystone/autofocus/HDMI player kept, factory and burn-in tools, bug-report uploader, Google preloads; HDMI needs `input_action`; voice, Chromecast off by choice |
| `xiaomi-mi-box-s/` | Xiaomi Mi Box S (MIBOX4, oneday), streaming box | Android TV 9 | PatchWall, Mi analytics, Alphonso ad tracking, MiLink; voice, casting, phone remote off by choice |
| `xiaomi-mitv-mooq0/` | Xiaomi Mi TV (MiTV-MOOQ0, croods) | Android TV 10 | PatchWall, Mi analytics, MiChannel, TV Manager, Alexa bridge; factory test app also claims HOME; notes from #81 |
| `xiaomi-mi-tv-4a/` | Xiaomi Mi TV 4A (MiTV-MOOQ1) | Android TV 10 | PatchWall, Mi analytics, SmartShare, MiLink |
| `xiaomi-mitv-aesp0/` | Xiaomi MiTV-AESP0 stick (aquaman, 1 GB) | Android TV 9 | No inputs; Home key sends ALL_APPS, needs `HOME_KEY_SERVICE=1`; Chromecast kept |
| `xiaomi-mitv-axso0/` | Xiaomi MiTV-AXSO0 (dangal; Mi TV 4C Pro 2019 / 4A Pro) | Android TV 9 | PatchWall, Glance ads, Mi TV+, Sensara, Mi analytics, MiLink; voice, Chromecast, unused streaming apps off by choice |
| `xiaomi-mitv-moor2/` | Xiaomi MiTV-MOOR2 (venom, MediaTek) | Google TV 11 | Mi ad row and PatchWall, Mi analytics, MiLink, store demo; setup wizard off so a third-party launcher can own Home |
| `xiaomi-mitv-mssp3/` | Xiaomi MiTV-MSSP3 (machuca, MediaTek) | Android TV 11 | PatchWall, Mi analytics, Netflix observer; Chromecast, screensavers, unused streaming apps off by choice |
| `xiaomi-tv-a-pro-2026/` | Xiaomi TV A Pro 75" 2026 (MiTV-MZTU0, river, MediaTek) | Google TV 14 | PatchWall/Mi Home tab, Mi analytics, TV Manager, updaters; setup wizard off, launcherx crash-loop notes |

To add your TV, open a pull request with a new folder here. The quickest way to get a list is to
run the prompt in [PROMPT.md](../PROMPT.md) and copy the packages your assistant disabled.
