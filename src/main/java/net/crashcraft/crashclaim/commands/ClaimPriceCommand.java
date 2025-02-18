package net.crashcraft.crashclaim.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import net.crashcraft.crashclaim.config.GlobalConfig;
import net.crashcraft.crashclaim.localization.Localization;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.TreeMap;

@CommandAlias("claimprice")
@CommandPermission("crashclaim.user.claimprice")
public class ClaimPriceCommand extends BaseCommand {

    @Default
    public void onClaimPrice(Player player){

        if (GlobalConfig.use_money_per_world_per_blocks_ranges) {

            String stringUpTo = Localization.RAW_TRANSLATION__COMMAND_CLAIM_PRICE__1.getRawMessage();
            String stringBlocks = Localization.RAW_TRANSLATION__COMMAND_CLAIM_PRICE__2.getRawMessage();
            String stringSeparator = Localization.RAW_TRANSLATION__COMMAND_CLAIM_PRICE__3.getRawMessage();
            String currencySymbol = Localization.RAW_TRANSLATION__CURRENCY_SYMBOL.getRawMessage();

            // Iterating over the worlds
            for (Map.Entry<String, TreeMap<Integer, Double>> entry : GlobalConfig.money_per_world_per_blocks_ranges.entrySet()) {
                String world = entry.getKey();
                TreeMap<Integer, Double> blocksRanges = entry.getValue();

                // Iterating over the ranges of prices for each world
                String pricesString = "";
                for (Map.Entry<Integer, Double> blockEntry : blocksRanges.entrySet()) {
                    int blockRange = blockEntry.getKey();
                    double price = blockEntry.getValue();
                    String priceString = ((int) price == price) ? Integer.toString((int) price) : Double.toString(price);

                    if (! pricesString.equals("")) pricesString += stringSeparator;
                    pricesString += stringUpTo + " " + blockRange + " " + stringBlocks + " = " + priceString + " " + currencySymbol;
                }
                player.spigot().sendMessage(Localization.CLAIM_INFO__PRICE_PER_WORLD_RANGES.getMessage(player,
                        "world", world,
                        "prices", pricesString));
            }
        } else {
            player.spigot().sendMessage(Localization.CLAIM_INFO__PRICE_PER_BLOCK.getMessage(player,"price",  String.valueOf(GlobalConfig.money_per_block)));
        }
    }
}
