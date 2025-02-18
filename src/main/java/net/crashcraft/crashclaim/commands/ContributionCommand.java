package net.crashcraft.crashclaim.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import net.crashcraft.crashclaim.claimobjects.Claim;
import net.crashcraft.crashclaim.claimobjects.Contribution;
import net.crashcraft.crashclaim.data.ClaimDataManager;
import net.crashcraft.crashclaim.localization.Localization;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

@CommandAlias("claimcontribution")
@CommandPermission("crashclaim.user.claimcontribution")
public class ContributionCommand extends BaseCommand {
    private final ClaimDataManager manager;

    public ContributionCommand(ClaimDataManager manager){
        this.manager = manager;
    }

    @Default
    public void onDefault(Player player){

        Location location = player.getLocation();
        Claim claim = manager.getClaim(location.getBlockX(), location.getBlockZ(), location.getWorld().getUID());
        if (claim != null) {

            for (Map.Entry<UUID, Contribution> playerContribution : claim.getContribution().entrySet()) {

//                String contributorName = Bukkit.getPlayer(playerContribution.getKey()) == null ?
//                        Localization.COMMAND__CONTRIBUTION__UNKNOWN_PLAYER.getRawMessage() :
//                        Bukkit.getPlayer(playerContribution.getKey()).getName();
                String contributorName = Bukkit.getOfflinePlayer(playerContribution.getKey()).getName();


                String contributionString = contributorName +
                        " : Claimed blocks = " + playerContribution.getValue().getArea() +
                        " : Price = " + playerContribution.getValue().getPrice();

                player.spigot().sendMessage(Localization.COMMAND__CONTRIBUTION__SHOW_CONTRIBUTION.getMessage(player,
                        "contribution", contributionString
                ));
            }

        } else {
            player.spigot().sendMessage(Localization.COMMAND__CONTRIBUTION__NO_CLAIM.getMessage(player));
        }
    }
}
