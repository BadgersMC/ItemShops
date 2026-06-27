package dev.enthusia.itemshops.listener;

import dev.enthusia.itemshops.ItemShopsPlugin;
import dev.enthusia.itemshops.manager.ShopManager;
import dev.enthusia.itemshops.model.Shop;
import dev.enthusia.itemshops.util.Pos;
import dev.enthusia.itemshops.util.Texts;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;


public final class ContainerAccessListener implements Listener {
    private final ItemShopsPlugin plugin;
    private final ShopManager mgr;

    public ContainerAccessListener(ItemShopsPlugin plugin, ShopManager mgr){
        this.plugin = plugin;
        this.mgr = mgr;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        if (!(b.getState() instanceof Container)) return;

        Pos uni = mgr.unifyContainerPos(b);
        if (uni == null) return;
        List<Shop> shops = mgr.shopsOn(uni);
        if (shops.isEmpty()) return;

        Player p = e.getPlayer();
        if (canOpen(p, shops, p.hasPermission("itemshops.open.others"))) return;

        e.setCancelled(true);
        p.sendMessage(Texts.msg(plugin.messages(), "errors.not-owner"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        Inventory inv = e.getInventory();
        InventoryHolder holder = inv.getHolder();
        Block block = null;

        if (holder instanceof Container c) {
            block = c.getBlock();
        } else if (inv instanceof DoubleChestInventory dci) {
            block = ((Container) dci.getLeftSide().getHolder()).getBlock();
        }

        if (block == null) return;

        Pos uni = mgr.unifyContainerPos(block);
        if (uni == null) return;
        List<Shop> shops = mgr.shopsOn(uni);
        if (shops.isEmpty()) return;

        if (!(e.getPlayer() instanceof Player p)) return;
        if (canOpen(p, shops, p.hasPermission("itemshops.open.others"))) return;

        e.setCancelled(true);
        p.sendMessage(Texts.msg(plugin.messages(), "errors.not-owner"));
    }

    private boolean canOpen(Player player, List<Shop> onContainer, boolean hasBypass) {
        if (hasBypass) return true;
        for (Shop s : onContainer) {
            if (s.owner().equals(player.getUniqueId())) return true;
            if (s.trusted().contains(player.getUniqueId())) return true;
            Location loc = s.container().toLocation();
            if (loc != null && plugin.guildShops().isGuildShop(loc)
                    && plugin.guildShops().canAccessGuildShop(player, loc)) {
                return true;
            }
        }
        return false;
    }
}