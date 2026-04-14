package dev.zaen.betterGunGame.listener;

import dev.zaen.betterGunGame.BetterGunGame;
import dev.zaen.betterGunGame.game.GameState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;

public class ProtectionListener implements Listener {

    private final BetterGunGame plugin;

    public ProtectionListener(BetterGunGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isInGame(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isInGame(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (isInGame(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (isInGame(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isInGame(player)) return;

        // Only manage the player's own inventory view — other GUIs (triumph-gui, ItemEditorGui) handle themselves
        InventoryType viewType = event.getView().getType();
        if (viewType != InventoryType.CRAFTING && viewType != InventoryType.PLAYER) return;

        // Block any interaction with the compass slot (hotbar slot 8)
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv != null && clickedInv.equals(player.getInventory()) && event.getSlot() == 8) {
            event.setCancelled(true);
            return;
        }

        // Block if the cursor is carrying the compass (prevent placing it elsewhere)
        if (event.getCursor() != null && event.getCursor().getType() == Material.RECOVERY_COMPASS) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (isInGame(event.getPlayer())) event.setCancelled(true);
    }

    private boolean isInGame(Player player) {
        return plugin.getGameManager().getState() == GameState.INGAME
                && plugin.getGameManager().getArenaManager().isInGame(player.getUniqueId());
    }
}
