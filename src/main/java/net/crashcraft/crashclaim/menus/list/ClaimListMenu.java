package net.crashcraft.crashclaim.menus.list;

import co.aikar.taskchain.TaskChain;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.whip.crashutils.menusystem.GUI;
import io.papermc.lib.PaperLib;
import net.crashcraft.crashclaim.CrashClaim;
import net.crashcraft.crashclaim.claimobjects.Claim;
import net.crashcraft.crashclaim.config.GlobalConfig;
import net.crashcraft.crashclaim.data.ClaimDataManager;
import net.crashcraft.crashclaim.localization.Localization;
import net.crashcraft.crashclaim.menus.ClaimMenu;
import net.crashcraft.crashclaim.permissions.PermissionHelper;
import net.crashcraft.crashclaim.permissions.PermissionRoute;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ClaimListMenu extends GUI {
    private final GUI previousMenu;

    private int page = 1;
    private final ArrayList<Claim> claims;
    private final HashMap<Integer, Claim> pageItemsDisplay;

    public ClaimListMenu(Player player, GUI previousMenu) {
        super(player, BaseComponent.toLegacyText(Localization.MENU__CLAIM_LIST__TITLE.getMessage(null)), 54);
        this.previousMenu = previousMenu;
        this.claims = new ArrayList<>();
        this.pageItemsDisplay = new HashMap<>();

        initialize();
    }

    @Override
    public void initialize() {
        claims.clear();

        TaskChain<?> chain = CrashClaim.newChain().async(() -> {
            try {
                ClaimDataManager manager = CrashClaim.getPlugin().getDataManager();
                claims.addAll(manager.getOwnedClaims(player.getUniqueId()));
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }).sync(this::loadItems);

        chain.setErrorHandler((ex, task) -> ex.printStackTrace());
        chain.execute();
    }

    @Override
    public void loadItems() {
        inv.clear();

        ArrayList<Claim> currentPageItems = getPageFromArray();
        pageItemsDisplay.clear();

        int slot = 10;
        for (Claim claim : currentPageItems) {
            while ((slot % 9) < 1 || (slot % 9) > 7) {
                slot++;
            }

            pageItemsDisplay.put(slot, claim);

            ItemStack descItem;

            World world = Bukkit.getWorld(claim.getWorld());

            if (world == null){
                continue; // Skip the claim if the world is not loaded
            }

            if (claim.getOwner().equals(getPlayer().getUniqueId())) {
                descItem = Localization.MENU__GENERAL__CLAIM_ITEM_NO_OWNER.getItem(player,
                        "name", claim.getName(),
                        "min_x", Integer.toString(claim.getMinX()),
                        "min_z", Integer.toString(claim.getMinZ()),
                        "max_x", Integer.toString(claim.getMaxX()),
                        "max_z", Integer.toString(claim.getMaxZ()),
                        "world", world.getName()
                );
            } else {
                descItem = Localization.MENU__GENERAL__CLAIM_ITEM.getItem(player,
                        "name", claim.getName(),
                        "min_x", Integer.toString(claim.getMinX()),
                        "min_z", Integer.toString(claim.getMinZ()),
                        "max_x", Integer.toString(claim.getMaxX()),
                        "max_z", Integer.toString(claim.getMaxZ()),
                        "world", world.getName(),
                        "owner", Bukkit.getOfflinePlayer(claim.getOwner()).getName()
                );
            }

            descItem.setType(GlobalConfig.visual_menu_items.getOrDefault(claim.getWorld(), Material.OAK_FENCE));

            inv.setItem(slot, descItem);

            slot++;
        }

        //Controls
        if (page > 1) {
            inv.setItem(48, Localization.MENU__GENERAL__PREVIOUS_BUTTON.getItem(player));
        }

        int maxPages = (int) Math.ceil((float) claims.size() / 21);

        inv.setItem(49, Localization.MENU__GENERAL__PAGE_DISPLAY.getItem(player,
                "page", Integer.toString(page),
                "page_total", Integer.toString(maxPages == 0 ? 1 : maxPages)
        ));

        if (claims.size() > page * 21) {
            inv.setItem(50, Localization.MENU__GENERAL__NEXT_BUTTON.getItem(player));
        }

        if (previousMenu != null) {
            inv.setItem(45, Localization.MENU__GENERAL__BACK_BUTTON.getItem(player));
        }
    }

    @Override
    public void onClose() {

    }

    @Override
    public void onClick(InventoryClickEvent e, String rawItemName) {
        switch (e.getSlot()) {
            case 48:
                if (page > 1) {
                    page--;
                    loadItems();
                }
                break;
            case 50:
                page++;
                loadItems();
                break;
            case 45:
                if (previousMenu == null) {
                    return;
                }
                previousMenu.open();
                break;
            default:
                Claim claim = pageItemsDisplay.get(e.getSlot());
                if (claim == null){
                    return;
                }

                if (GlobalConfig.allowPlayerClaimTeleporting && e.getClick().equals(ClickType.SHIFT_LEFT)){
                    if (player.hasPermission("crashclaim.user.teleport-own-claim")
                            && claim.getOwner().equals(player.getUniqueId())){
                        forceClose();
                        teleportPlayer(player, claim);
                        player.sendMessage(Localization.CLAIM_TELEPORT__TELEPORT_OWN.getMessage(player));
                        return;
                    } else if (player.hasPermission("crashclaim.user.teleport-claim-with-permission")
                            && PermissionHelper.getPermissionHelper().hasPermission(claim, player.getUniqueId(), PermissionRoute.TELEPORTATION)){
                        teleportPlayer(player, claim);
                        forceClose();
                        player.sendMessage(Localization.CLAIM_TELEPORT__TELEPORT_OTHER.getMessage(player));
                        return;
                    } else {
                        forceClose();
                        player.sendMessage(Localization.CLAIM_TELEPORT__TELEPORT_NO_PERMISSION.getMessage(player));
                        return;
                    }
                }
                new ClaimMenu(player, claim, this).open();
                break;
        }
    }

    private void teleportPlayer(Player player, Claim claim){
        int x = (int) Math.round(claim.getMaxX() - (double) (Math.abs(claim.getMaxX() - claim.getMinX()) / 2));
        int z = (int) Math.round(claim.getMaxZ() - (double) (Math.abs(claim.getMaxZ() - claim.getMinZ()) / 2));

        World world = Bukkit.getWorld(claim.getWorld());
        if (world == null){
            return;
        }

        Location location;

        if (claim.getTeleportLocation() != null) {
            Map<String, Object> map = new Gson().fromJson(claim.getTeleportLocation(), new TypeToken<Map<String, Object>>() {}.getType());

            double x2 = (double) map.get("x");
            double y2 = (double) map.get("y");
            double z2 = (double) map.get("z");
            float pitch = ((Double) map.get("pitch")).floatValue();
            float yaw = ((Double) map.get("yaw")).floatValue();

            location = new Location(world, x2, y2, z2, yaw, pitch);
        } else {
            location = new Location(world, x, world.getHighestBlockYAt(x, z) + 1, z);
        }

        // safe TP with Essentials
        Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials != null) {
            User user = essentials.getUser(player);
            final CompletableFuture<Boolean> future = new CompletableFuture<>();
            user.getAsyncTeleport().now(location, false, PlayerTeleportEvent.TeleportCause.PLUGIN, future);
        } else {
            PaperLib.teleportAsync(player, location);
        }
    }

    private ArrayList<Claim> getPageFromArray() {
        ArrayList<Claim> pageItems = new ArrayList<>();

        for (int x = 21 * (page - 1); x < 21 * page && x < claims.size(); x++) {
            Claim claim = claims.get(x);
            pageItems.add(claim);
        }

        return pageItems;
    }
}