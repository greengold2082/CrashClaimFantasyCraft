package net.crashcraft.crashclaim.config;

import net.crashcraft.crashclaim.visualize.api.VisualColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;

public class GlobalConfig extends BaseConfig{
    public static String locale;
    public static String paymentProvider;
    public static boolean checkUpdates;
    public static boolean skipNaturalMobGrief;

    private static void loadGeneral(){
        locale = getString("language", "en_US");
        paymentProvider = getString("payment-provider", "default");
        checkUpdates = getBoolean("check-updates", true);
        skipNaturalMobGrief = getBoolean("skip-natural-mob-grief", false);
    }

    public static HashMap<String, GroupSettings> groupSettings;

    private static void loadGroups(){
        groupSettings = new HashMap<>();

        final String baseKey = "group-settings";
        ConfigurationSection section = config.getConfigurationSection(baseKey);
        Set<String> keys;
        if (section == null){
            keys = new HashSet<>();
        } else {
            keys = section.getKeys(false);
        }

        keys.add("default"); // Always need a default group.

        for (String groupName : keys){
            groupSettings.put(groupName, new GroupSettings(
                    getInt(baseKey + "." + groupName + ".max-claims", -1)
            ));
        }
    }

    public static boolean use_money_per_world_per_blocks_ranges;
    public static HashMap<String, TreeMap<Integer, Double>> money_per_world_per_blocks_ranges;

    //TODO test si worlds existent avec bukkit, try catch, ajouter la config dans le fichier config ressource du plugin

    // Processing config : world: 100:10;625:0.8;2550:0.5;10000:0.1;22500:0.06
    private static void moneyPerWorldPerBlocksRanges() {

        final String baseKey = "money-per-world-per-blocks-ranges";
        use_money_per_world_per_blocks_ranges = getBoolean(baseKey + ".use", false);
        money_per_world_per_blocks_ranges = new HashMap<>();

        if (! use_money_per_world_per_blocks_ranges) return;

        ConfigurationSection worldsSection = config.getConfigurationSection(baseKey + ".worlds");
        Set<String> worldsKeys;
        if (worldsSection == null) {
            use_money_per_world_per_blocks_ranges = false;
            logError("Error in config.yml, expected worlds not found. Turn off money_per_world_per_blocks_ranges");
            return;
        } else {
            worldsKeys = worldsSection.getKeys(false);
        }


        for (String worldName : worldsKeys){
            String worldData = getString(baseKey + ".worlds." + worldName, null);
            if (worldData == null) {
                use_money_per_world_per_blocks_ranges = false;
                logError("Error in config.yml, invalid value in key : " + worldName + ". Turn off money_per_world_per_blocks_ranges");
                return;
            }

            TreeMap<Integer, Double> rangesMap = new TreeMap<Integer, Double>();
            String[] arrRanges = worldData.split(";");

            for (String range : arrRanges) {
                String[] arrRange = range.split(":");
                rangesMap.put( Integer.valueOf(arrRange[0]), Double.valueOf(arrRange[1]));
            }
            money_per_world_per_blocks_ranges.put(worldName, rangesMap);
        }
    }

    /*try {
        for (int x = 0; x < arr.length; x++) {
            arr[x] = Integer.valueOf(value[x]);
        }
    } catch (NumberFormatException e) {
        logError("Invalid number format for, " + path);
    }*/

    public static String visual_type;
    public static boolean visual_use_highest_block;
    public static HashMap<UUID, Material> visual_menu_items;

    public static int visual_alert_fade_in;
    public static int visual_alert_duration;
    public static int visual_alert_fade_out;

    private static void loadVisual(){
        visual_type = getString("visualization.visual-type", "glow");

        visual_menu_items = new HashMap<>();

        for (World world : Bukkit.getWorlds()){
            visual_menu_items.put(world.getUID(), Material.getMaterial(getString("visualization.claim-items." + world.getName(), Material.OAK_FENCE.name())));
        }

        visual_use_highest_block = getBoolean("visualization.visual-use-highest-block", false);
        visual_alert_fade_in = getInt("visualization.alert.fade-in", 10);
        visual_alert_duration = getInt("visualization.alert.duration", 1);
        visual_alert_fade_out = getInt("visualization.alert.fade-out", 10);

        setVisualBlockColor(VisualColor.GOLD, Material.ORANGE_CONCRETE);
        setVisualBlockColor(VisualColor.RED, Material.RED_CONCRETE);
        setVisualBlockColor(VisualColor.GREEN, Material.LIME_CONCRETE);
        setVisualBlockColor(VisualColor.YELLOW, Material.GREEN_CONCRETE);
        setVisualBlockColor(VisualColor.WHITE, Material.WHITE_CONCRETE);
    }

    private static void setVisualBlockColor(VisualColor color, Material defaultMaterial){
        String key = "visualization.visual-colors." + color.name();
        Material material = Material.getMaterial(getString(key, defaultMaterial.name()));

        if (material != null) {
            color.setMaterial(material);
            return;
        }

        log("Invalid material for " + key + ", loading default value");
        color.setMaterial(defaultMaterial);
    }

    public static boolean useCommandInsteadOfEdgeEject;
    public static String claimEjectCommand;
    public static boolean allowPlayerClaimTeleporting;

    private static void loadEject(){
        useCommandInsteadOfEdgeEject = getBoolean("eject.useCommandInstead", false);
        claimEjectCommand = getString("eject.command", "home");
        allowPlayerClaimTeleporting = getBoolean("allow-player-teleport-claim", false);
    }

    public static HashMap<PlayerTeleportEvent.TeleportCause, Integer> teleportCause;

    private static void loadTeleport(){
        // 0 | NONE  - diable, 1 | BLOCK - enable check with blocking, 2 | RELOCATE - enable check with relocating
        teleportCause = new HashMap<>();
        for (PlayerTeleportEvent.TeleportCause cause : PlayerTeleportEvent.TeleportCause.values()){
            if (cause.equals(PlayerTeleportEvent.TeleportCause.UNKNOWN)){
                continue;
            }

            String value = getString("events.teleport." + cause.name(), "block");

            switch (value.toLowerCase()){
                case "none":
                    teleportCause.put(cause, 0);
                case "block":
                    teleportCause.put(cause, 1);
                case "relocate":
                    teleportCause.put(cause, 2);
                default:
                    //Bad value default to good one
                    teleportCause.put(cause, 1);
            }
        }
    }

    public static double money_per_block;
    public static ArrayList<UUID> disabled_worlds;
    public static String forcedVersionString;
    public static boolean blockPvPInsideClaims;
    public static boolean checkEntryExitWhileFlying;
    public static String claimNetherPermissionName;

    private static void miscValues(){
        money_per_block = getDouble("money-per-block", 0.01);
        disabled_worlds = new ArrayList<>();
        for (String s : getStringList("disabled-worlds", Collections.emptyList())){
            World world = Bukkit.getWorld(s);
            if (world == null){
                logError("World name was invalid or the world was not loaded into memory");
                continue;
            }

            disabled_worlds.add(world.getUID());
        }

        forcedVersionString = config.getString("use-this-version-instead");
        blockPvPInsideClaims = getBoolean("block-pvp-inside-claims", false);
        checkEntryExitWhileFlying = config.getBoolean("check-entry-and-exit-while-flying", false);
        claimNetherPermissionName = config.getString("claim-nether-permission-name", null);
    }

    public static boolean bypassModeBypassesMoney;

    private static void onBypass(){
        bypassModeBypassesMoney = getBoolean("bypass-mode-bypasses-payment", false);
    }

    public static boolean useStatistics;

    private static void onStats(){
        useStatistics = getBoolean("statistics", true);
    }
}
