package net.crashcraft.crashclaim.migration.adapters;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.crashcraft.crashclaim.CrashClaim;
import net.crashcraft.crashclaim.claimobjects.PermState;
import net.crashcraft.crashclaim.claimobjects.permission.PlayerPermissionSet;
import net.crashcraft.crashclaim.data.ClaimResponse;
import net.crashcraft.crashclaim.migration.MigrationAdapter;
import net.crashcraft.crashclaim.migration.MigrationManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;



public class WorldGuardAdaptor implements MigrationAdapter {
    private final WorldGuardPlugin worldGuard;

    private BukkitTask task;
    private int pos;
    private ProtectedRegion region;

    public WorldGuardAdaptor(){
        worldGuard = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
    }

    @Override
    public String checkRequirements(MigrationManager manager) {
        manager.getLogger().info("--------------------- checkRequirements ");
        if (worldGuard == null){
            return "WorldGuard was not located at runtime. Is it installed?";
        }

        boolean isRegionFound = false;
        for (World world : Bukkit.getWorlds()) {
            RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
            if (rm.getRegions().size() > 0) {
                isRegionFound = true;
                break;
            }
        }
        if (!isRegionFound) return "WorldGuard does not report any loaded claims to migrate";

        return null;
    }

    @Override
    public CompletableFuture<String> migrate(MigrationManager manager) {
        final Logger logger = manager.getLogger();
        //final DataStore dataStore = worldGuard.dataStore;
        Integer claimCount = 0;

        manager.getLogger().info("--------------------- migrate ");

        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
            claimCount += regionManager.getRegions().size();
        }
        logger.info("Found " + claimCount + " Worldguard claims");



        CompletableFuture<String> finishedFuture = new CompletableFuture<>();
        Map<String, ProtectedRegion> claims = new HashMap<>();
        Integer i = 0;
        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));

            Collection<ProtectedRegion> regions = regionManager.getRegions().values();
            for (ProtectedRegion region : regions) {

                // We keep only regions beginning by "rgui-". That excludes "__global__" and "sys_" regions
                if (! region.getId().substring(0, 5).equalsIgnoreCase("rgui-")) {
                    logger.warning("Skipping Monde : " + world.getName() + " | Claim : " + region.getId());
                    continue;
                }

                claims.put(world.getName() + "@@@" + i, region);
                i++;
                //logger.warning(world.getName() + " " + region.getId());
            }
        }
        logger.info("Found " + i + " player's claims, migrating...");


        task = Bukkit.getScheduler().runTaskTimer(CrashClaim.getPlugin(), () -> {


            int nextPos = Math.min(pos + 20, claims.size());
            for (int x = pos; x < nextPos; x++){

                // Parsing HashMap
                String worldNameUnique = (String) claims.keySet().toArray()[x];
                ProtectedRegion claim = claims.get(worldNameUnique);

                String worldName = worldNameUnique.split("@@@")[0];
                //logger.info("Monde : " + worldName + " | Claim : " + claim.getId());


                Location locationGreaterBoundaryCorner = new Location (
                        Bukkit.getWorld(worldName),
                        claim.getMaximumPoint().getBlockX(),
                        claim.getMaximumPoint().getBlockY(),
                        claim.getMaximumPoint().getBlockZ()
                );

                Location locationLesserBoundaryCorner = new Location (
                        Bukkit.getWorld(worldName),
                        claim.getMinimumPoint().getBlockX(),
                        claim.getMinimumPoint().getBlockY(),
                        claim.getMinimumPoint().getBlockZ()
                );

                UUID ownerUUID = null;
                for (UUID uuid : claim.getOwners().getUniqueIds()) {
                    ownerUUID = uuid;
                }
                ClaimResponse claimResponse = manager.getManager().createClaim(locationGreaterBoundaryCorner, locationLesserBoundaryCorner, ownerUUID);


                if (!claimResponse.isStatus()){
                    logger.severe("A claim has failed to be created due to [" + claimResponse.getError().name() + "] | " + claim.getId());
                    continue;
                }


                // claim messages / name
                Flag<String> flagGreetingMessageChat = Flags.GREET_MESSAGE;
                String greetingMessageChat = claim.getFlag(flagGreetingMessageChat);
                if (greetingMessageChat != null) claimResponse.getClaim().setEntryMessage(greetingMessageChat);

                Flag<String> flagExitMessageChat = Flags.FAREWELL_MESSAGE;
                String exitMessageChat = claim.getFlag(flagExitMessageChat);
                if (exitMessageChat != null) claimResponse.getClaim().setExitMessage(exitMessageChat);

                claimResponse.getClaim().setName(claim.getId());


                // claim spawn coords
                if (claim.getFlag(Flags.TELE_LOC) != null) {
                    Location teleportLocation = new Location (
                            Bukkit.getWorld(worldName),
                            claim.getFlag(Flags.TELE_LOC).getBlockX(),
                            claim.getFlag(Flags.TELE_LOC).getBlockY(),
                            claim.getFlag(Flags.TELE_LOC).getBlockZ(),
                            claim.getFlag(Flags.TELE_LOC).getYaw(),
                            claim.getFlag(Flags.TELE_LOC).getPitch()
                    );
                    claimResponse.getClaim().setTeleportLocation(String.valueOf(teleportLocation.serialize()));
                }



                // Permissions
                net.crashcraft.crashclaim.claimobjects.Claim cClaim = (net.crashcraft.crashclaim.claimobjects.Claim) claimResponse.getClaim();
                addMembers(manager, claim, cClaim);



                logger.info("Successfully migrated claim Monde : " + worldName + " | Claim : " + claim.getId());
            }



            pos = nextPos + 1;

            if (nextPos >= claims.size()){
                finishedFuture.complete(null);
                task.cancel();
            }
        }, 1L, 1L);

        return finishedFuture;
    }


    private void addMembers(MigrationManager manager, ProtectedRegion claimWorldguard, net.crashcraft.crashclaim.claimobjects.BaseClaim cClaim) {

        Set<UUID> membersUUIDs = claimWorldguard.getMembers().getUniqueIds();

        for (UUID uuid : membersUUIDs) {

            PlayerPermissionSet permissionSet = cClaim.getPerms().getPlayerPermissionSet(uuid);

            permissionSet.setBuild(PermState.ENABLED);
            permissionSet.setInteractions(PermState.ENABLED);
            permissionSet.setEntities(PermState.ENABLED);

            for (Material container : manager.getManager().getPermissionSetup().getTrackedContainers()) {
                permissionSet.setContainer(container, PermState.ENABLED);
            }

            cClaim.getPerms().setPlayerPermissionSet(
                    uuid, permissionSet
            );
        }
    }



    @Override
    public String getIdentifier() {
        return "WorldGuard";
    }
}
