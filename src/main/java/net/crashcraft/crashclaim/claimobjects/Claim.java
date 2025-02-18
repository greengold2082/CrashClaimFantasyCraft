package net.crashcraft.crashclaim.claimobjects;

import net.crashcraft.crashclaim.data.MathUtils;
import net.crashcraft.crashclaim.permissions.PermissionRoute;
import net.crashcraft.crashclaim.permissions.PermissionRouter;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.*;
import java.util.function.Predicate;

public class Claim extends BaseClaim {
    private boolean toSave;

    private ArrayList<SubClaim> subClaims;
    private UUID owner;

    private HashMap<UUID, Contribution> contribution;

    public Claim(int id, int upperCornerX, int upperCornerZ, int lowerCornerX, int lowerCornerZ, UUID world, PermissionGroup perms, UUID owner) {
        super(id, upperCornerX, upperCornerZ, lowerCornerX, lowerCornerZ, world, perms);
        this.toSave = false;
        this.subClaims = new ArrayList<>();
        this.owner = owner;
        this.contribution = new HashMap<>();
    }

    public boolean hasGlobalPermission(PermissionRoute route){
        return route.getPerm(getPerms().getGlobalPermissionSet()) == PermState.ENABLED;
    }

    public boolean hasPermission(UUID uuid, Location location, PermissionRoute route){
        return getActivePermission(uuid, location, route) == PermState.ENABLED;
    }

    public boolean hasPermission(UUID uuid, Location location, Material material){
        return getActivePermission(uuid, location, material) == PermState.ENABLED;
    }

    public boolean hasPermission(Location location, PermissionRoute route){
        return getActivePermission(location, route) == PermState.ENABLED;
    }

    public boolean hasPermission(Location location, Material material){
        return getActivePermission(location, material) == PermState.ENABLED;
    }

    public SubClaim getSubClaim(int x, int z){
        for (SubClaim subClaim : subClaims) {
            if (MathUtils.iskPointCollide(subClaim.getMinX(), subClaim.getMinZ(), subClaim.getMaxX(),
                    subClaim.getMaxZ(), x, z)) {
                return subClaim;
            }
        }
        return null;
    }

    private int getActivePermission(Location location, PermissionRoute route){
        if (location != null && subClaims.size() > 0){
            SubClaim subClaim = getSubClaim(location.getBlockX(), location.getBlockZ());
            if (subClaim != null){
                return PermissionRouter.getLayeredPermission(this, subClaim, route);
            }
            return route.getPerm(getPerms().getGlobalPermissionSet());
        } else {
            return PermissionRouter.getLayeredPermission(this, null, route);
        }
    }

    private int getActivePermission(Location location, Material material){
        if (location != null && subClaims.size() > 0){
            SubClaim subClaim = getSubClaim(location.getBlockX(), location.getBlockZ());
            if (subClaim != null){
                return PermissionRouter.getLayeredPermission(this, subClaim, material);
            }
            return PermissionRoute.CONTAINERS.getPerm(getPerms().getGlobalPermissionSet());
        } else {
            return PermissionRouter.getLayeredPermission(this, null, material);
        }
    }

    private int getActivePermission(UUID uuid, Location location, PermissionRoute route){   //should only be executed with a location inside the claim
        if (uuid.equals(owner))
            return PermState.ENABLED;

        if (location != null && subClaims.size() > 0){
            SubClaim subClaim = getSubClaim(location.getBlockX(), location.getBlockZ());
            if (subClaim != null){
                return PermissionRouter.getLayeredPermission(this, subClaim, uuid, route);
            } else {
                return PermissionRouter.getLayeredPermission(this, null, uuid, route);
            }
        } else {
            return PermissionRouter.getLayeredPermission(this, null, uuid, route);
        }
    }

    private int getActivePermission(UUID uuid, Location location, Material material){   //should only be executed with a location inside the claim
        if (uuid.equals(owner))
            return PermState.ENABLED;

        if (location != null && subClaims.size() > 0){
            SubClaim subClaim = getSubClaim(location.getBlockX(), location.getBlockZ());
            if (subClaim != null){
                return PermissionRouter.getLayeredContainer(this, subClaim, uuid, material);
            } else {
                return PermissionRouter.getLayeredContainer(this, null, uuid, material);
            }
        } else {
            return PermissionRouter.getLayeredContainer(this, null, uuid, material);
        }
    }

    public void addContribution(UUID player, int area, double price){
        int areaAdder = contribution.get(player) != null ? contribution.get(player).getArea() : 0;
        double priceAdder = contribution.get(player) != null ? contribution.get(player).getPrice() : 0;
        double newPrice = Math.round((priceAdder + price) * 100) / 100;
        contribution.put(player, new Contribution(areaAdder + area, newPrice));
    }

    /**
     *
     * @param player
     * @param area
     * @return returns the price truly paid by the player corresponding to the area deducted
     */
    public double deductContribution(UUID player, int area) {
        int contributedArea = contribution.get(player).getArea();
        double contributedPrice = contribution.get(player).getPrice();
        double deductedPrice = 0;
        double newPrice = 0;

        if (area >= contributedArea) {
            removeContribution(player);
            return contributedPrice;
        }

        deductedPrice = (double) (area * contributedPrice) / contributedArea;
        newPrice = Math.round((contributedPrice - deductedPrice) * 100) / 100;
        contribution.put(player, new Contribution(contributedArea - area, newPrice));

        return deductedPrice;
    }

    public void removeContribution(UUID player) {
        contribution.remove(player);
    }

    public int getContributionTotalArea() {
        int adderArea = 0;

        for (Map.Entry<UUID, Contribution> entry : contribution.entrySet()) {
            adderArea += entry.getValue().getArea();
        }
        return adderArea;
    }

    public Contribution getContribution(UUID player){
        return contribution.get(player);
    }

    public HashMap<UUID, Contribution> getContribution(){
        return contribution;
    }

    @Override
    public synchronized boolean isToSave() {
        return toSave;
    }

    @Override
    public synchronized void setToSave(boolean toSave) {
        this.toSave = toSave;
    }

    public void addSubClaim(SubClaim subClaim){
        subClaims.add(subClaim);
    }

    public void removeSubClaim(int id){
        Iterator<SubClaim> subClaimIterator = subClaims.iterator();
        while (subClaimIterator.hasNext()){
            SubClaim subClaim = subClaimIterator.next();

            if (subClaim.getId() == id){
                subClaimIterator.remove();
                return;
            }
        }
    }

    public ArrayList<SubClaim> getSubClaims(){
        return subClaims;
    }

    public UUID getOwner() {
        return owner;
    }

    //JSON needs this

    public void setSubClaims(ArrayList<SubClaim> subClaims) {
        this.subClaims = subClaims;
    }

    public void setContribution(HashMap<UUID, Contribution> contribution) {
        this.contribution = contribution;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

}
