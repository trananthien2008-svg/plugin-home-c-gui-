package me.homegui;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

public final class HomeListener implements Listener {

    private final HomeGuiPlugin plugin;

    public HomeListener(HomeGuiPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HomeMenus.Holder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getInventory()) {
            return;
        }

        int slot = event.getSlot();
        if (holder.type() == HomeMenus.Type.MAIN) {
            handleMain(player, holder, slot, event.isLeftClick(), event.isRightClick());
        } else {
            handleConfirm(player, holder, slot);
        }
    }

    private void handleMain(Player player, HomeMenus.Holder holder, int slot, boolean left, boolean right) {
        UUID id = player.getUniqueId();

        int wool = HomeMenus.woolIndex(slot);
        if (wool >= 0) {
            int n = wool + 1;
            if (!plugin.canUse(player, wool)) {
                player.sendMessage(HomeGuiPlugin.text("&cBạn chưa có quyền mở khoá Home " + n + "."));
                return;
            }
            Home home = plugin.homes().get(id, wool);
            if (left) {
                if (home == null) {
                    plugin.homes().set(id, wool, Home.of(player.getLocation()));
                    player.sendMessage(HomeGuiPlugin.text("&aĐã đặt &fHome " + n + " &atại vị trí của bạn."));
                    HomeMenus.fillMain(plugin, player, holder.getInventory());
                } else {
                    player.sendMessage(HomeGuiPlugin.text("&eHome " + n + " đã được đặt. Chuột phải để xoá trước khi đặt lại."));
                }
            } else if (right && home != null) {
                // mở GUI xác nhận ở tick sau để tránh đổi inventory ngay trong event click
                player.getScheduler().run(plugin, t -> HomeMenus.openConfirm(plugin, player, wool), null);
            }
            return;
        }

        int bed = HomeMenus.bedIndex(slot);
        if (bed >= 0 && left) {
            if (!plugin.canUse(player, bed) || plugin.homes().get(id, bed) == null) {
                player.sendMessage(HomeGuiPlugin.text("&cHome " + (bed + 1) + " chưa được đặt."));
                return;
            }
            player.closeInventory();
            plugin.teleports().start(player, bed);
        }
    }

    private void handleConfirm(Player player, HomeMenus.Holder holder, int slot) {
        if (slot == HomeMenus.CONFIRM_SLOT) {
            plugin.homes().remove(player.getUniqueId(), holder.index());
            player.sendMessage(HomeGuiPlugin.text("&aĐã xoá &fHome " + (holder.index() + 1) + "&a."));
            player.getScheduler().run(plugin, t -> HomeMenus.openMain(plugin, player), null);
        } else if (slot == HomeMenus.CANCEL_SLOT) {
            player.getScheduler().run(plugin, t -> HomeMenus.openMain(plugin, player), null);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof HomeMenus.Holder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.cancelOnMove() || !plugin.teleports().hasPending()) {
            return;
        }
        Location to = event.getTo();
        if (to != null) {
            plugin.teleports().cancelIfMoved(event.getPlayer(), to);
        }
    }
}
