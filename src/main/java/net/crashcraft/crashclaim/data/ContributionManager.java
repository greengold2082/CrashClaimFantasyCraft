package net.crashcraft.crashclaim.data;

import com.sk89q.worldedit.math.MathUtils;
import net.crashcraft.crashclaim.CrashClaim;
import net.crashcraft.crashclaim.claimobjects.Claim;
import net.crashcraft.crashclaim.claimobjects.Contribution;
import net.crashcraft.crashclaim.config.GlobalConfig;
import net.crashcraft.crashclaim.localization.Localization;
import net.crashcraft.crashpayment.payment.TransactionType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ContributionManager {
    public static int getArea(int minCornerX, int minCornerZ, int maxCornerX, int maxCornerZ){
        return (((maxCornerX + 1) - minCornerX) * ((maxCornerZ + 1) - minCornerZ));
    }

    public static void addContribution(Claim claim, int minCornerX, int minCornerZ, int maxCornerX, int maxCornerZ, UUID player, double price){

        int area = getArea(minCornerX, minCornerZ, maxCornerX, maxCornerZ);
        int originalArea = getArea(claim.getMinX(), claim.getMinZ(), claim.getMaxX(), claim.getMaxZ());
        int remainingArea;

        int difference = area - originalArea;

        if (difference == 0)
            return;

        if (difference > 0){
            claim.addContribution(player, difference, price);  //add to contribution
        } else {

            remainingArea = Math.abs(difference);

            adjustContributions(claim, remainingArea); // adjusting contributions proportionally to all contributors

            /*
            // priority is given to the player who resized the claim, then the rest of contributors
            if (claim.getContribution().containsKey(player)) {
                int contributedArea = claim.getContribution(player).getArea();
                if (remainingArea >= contributedArea) { // full refund
                    remainingArea -= contributedArea;
                    refundPlayer(player, claim.getContribution(player).getPrice());
                    claim.removeContribution(player);
                } else { // partial refund
                    double priceToRefund = claim.deductContribution(player, remainingArea);
                    refundPlayer(player, priceToRefund);
                    remainingArea = 0;
                }
            }

            // In all cases, we adjust the remaining contribution to the other contributors + refund
            if (remainingArea > 0) {
                adjustContributions(claim, remainingArea);
            }
            */
        }
    }

    public static void adjustContributions(Claim claim, int areaToDeduct) {

        int totalContributedArea = claim.getContributionTotalArea();
        int totalDeductedArea = 0;
        HashMap<UUID, Integer> playersAreaToDeduct = new HashMap<UUID, Integer>();

        // calculation of the area to be deducted for each player in proportion to their contribution
        for (Map.Entry<UUID, Contribution> playerContribution : claim.getContribution().entrySet()) {
            double contributionPercent = (double) playerContribution.getValue().getArea() / totalContributedArea;
            int deductedArea = (int) Math.floor(areaToDeduct * contributionPercent);
            playersAreaToDeduct.put(playerContribution.getKey(), deductedArea);
            totalDeductedArea += deductedArea;
        }

        // dealing with rounding problems (adding the area on the first player who can handle it)
        int roundingDifference = areaToDeduct - totalDeductedArea;
        if (roundingDifference > 0) {
            for (Map.Entry<UUID, Integer> playerAreaToDeduct : playersAreaToDeduct.entrySet()) {
                int playerAreaWithRoundingDifference = playerAreaToDeduct.getValue() + roundingDifference;
                int playerAreaContribution = claim.getContribution(playerAreaToDeduct.getKey()).getArea();

                if (playerAreaContribution >= playerAreaWithRoundingDifference) {
                    playerAreaToDeduct.setValue(playerAreaWithRoundingDifference);
                    break;
                }
            }
        }

        // Adjusting and refunding all contributors
        for (Map.Entry<UUID, Integer> playerAreaToDeduct : playersAreaToDeduct.entrySet()) {
            double priceToRefund = claim.deductContribution(playerAreaToDeduct.getKey(), playerAreaToDeduct.getValue());
            refundPlayer(playerAreaToDeduct.getKey(), priceToRefund);
        }
    }

    public static void refundContributors(Claim claim){
        for (Map.Entry<UUID, Contribution> entry : claim.getContribution().entrySet()) {
            refundPlayer(entry.getKey(), entry.getValue().getPrice());
        }
    }

    private static void refundPlayer(UUID player, double price) {
        if (price == 0){
            return;
        }
        //round to 2 decimals
        price = Math.round(price * 100) / 100;

        CrashClaim.getPlugin().getPayment().makeTransaction(player, TransactionType.DEPOSIT, "Claim Refund", price, (transaction) -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(transaction.getOwner());
            if (offlinePlayer.isOnline()){
                Player p = offlinePlayer.getPlayer();
                if (p != null) {
                    p.spigot().sendMessage(Localization.CONTRIBUTION_REFUND.getMessage(p,
                            "amount", Integer.toString((int) Math.round(transaction.getAmount()))));
                }
            }
        });
    }
}
