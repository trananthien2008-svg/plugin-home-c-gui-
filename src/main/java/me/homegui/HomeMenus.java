package me.homegui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Tạo các GUI: menu chính (36 ô) và menu xác nhận xoá (27 ô). */
public final class HomeMenus {

    // Ô số 12-16 và 21-25 (đếm từ 1) = chỉ số 11-15 và 20-24 (đếm từ 0)
    public static final int[] WOOL_SLOTS = {11, 12, 13, 14, 15};
    public static final int[] BED_SLOTS = {20, 21, 22, 23, 24};

    public static final int CONFIRM_SLOT = 11;
    public static final int INFO_SLOT = 13;
    public static final int CANCEL_SLOT = 15;

    private HomeMenus() {
    }

    public enum Type {MAIN, CONFIRM}

    public static final class Holder implements InventoryHolder {
        private final Type type;
        private final int index;
        private Inventory inventory;

        Holder(Type type, int index) {
            this.type = type;
            this.index = index;
        }

        public Type type() {
            return type;
        }

        /** Chỉ số home (0-4) đang được xác nhận xoá; -1 ở menu chính. */
        public int index() {
            return index;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public static int woolIndex(int slot) {
        for (int i = 0; i < WOOL_SLOTS.length; i++) {
            if (WOOL_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    public static int bedIndex(int slot) {
        for (int i = 0; i < BED_SLOTS.length; i++) {
            if (BED_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ menu chính

    public static void openMain(HomeGuiPlugin plugin, Player player) {
        Holder holder = new Holder(Type.MAIN, -1);
        Inventory inv = Bukkit.createInventory(holder, 36, HomeGuiPlugin.text("&8Home của bạn"));
        holder.inventory = inv;
        fillMain(plugin, player, inv);
        player.openInventory(inv);
    }

    /** Vẽ lại nội dung menu chính (dùng khi mở và sau khi đặt/xoá home). */
    public static void fillMain(HomeGuiPlugin plugin, Player player, Inventory inv) {
        UUID id = player.getUniqueId();
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        long cooldown = plugin.teleports().cooldownLeftSeconds(id);

        for (int i = 0; i < HomeManager.MAX_HOMES; i++) {
            int n = i + 1;
            Home home = plugin.homes().get(id, i);
            boolean unlocked = plugin.canUse(player, i);

            if (!unlocked) {
                inv.setItem(WOOL_SLOTS[i], item(Material.RED_WOOL, "&cHome " + n + " &8(Chưa mở khoá)",
                        "&7Bạn chưa có quyền dùng Home " + n + "."));
                inv.setItem(BED_SLOTS[i], item(Material.GRAY_BED, "&7Home " + n, "&cChưa mở khoá"));
            } else if (home == null) {
                inv.setItem(WOOL_SLOTS[i], item(Material.RED_WOOL, "&cHome " + n + " &7- Chưa đặt",
                        "&eChuột trái &7» Đặt home tại chỗ đang đứng"));
                inv.setItem(BED_SLOTS[i], item(Material.GRAY_BED, "&7Home " + n, "&cChưa có home để dịch chuyển"));
            } else {
                inv.setItem(WOOL_SLOTS[i], item(Material.LIME_WOOL, "&aHome " + n, describe(home),
                        " ", "&eChuột phải &7» Xoá home"));
                List<String> lore = new ArrayList<>();
                lore.add("&eChuột trái &7» Dịch chuyển về home");
                lore.add("&7Chờ &f" + plugin.delaySeconds() + "s &7(đứng yên)");
                if (cooldown > 0) {
                    lore.add("&cHồi chiêu: " + cooldown + "s");
                }
                inv.setItem(BED_SLOTS[i], item(Material.LIME_BED, "&aVề Home " + n, lore.toArray(new String[0])));
            }
        }
    }

    // ------------------------------------------------------------------ menu xác nhận

    public static void openConfirm(HomeGuiPlugin plugin, Player player, int index) {
        Home home = plugin.homes().get(player.getUniqueId(), index);
        if (home == null) {
            openMain(plugin, player);
            return;
        }
        Holder holder = new Holder(Type.CONFIRM, index);
        Inventory inv = Bukkit.createInventory(holder, 27, HomeGuiPlugin.text("&8Xoá Home " + (index + 1) + "?"));
        holder.inventory = inv;

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
        inv.setItem(CONFIRM_SLOT, item(Material.LIME_CONCRETE, "&a&lXÁC NHẬN XOÁ",
                "&7Xoá vĩnh viễn Home " + (index + 1)));
        inv.setItem(INFO_SLOT, item(Material.LIME_WOOL, "&aHome " + (index + 1), describe(home)));
        inv.setItem(CANCEL_SLOT, item(Material.RED_CONCRETE, "&c&lHUỶ", "&7Giữ lại home này"));
        player.openInventory(inv);
    }

    // ------------------------------------------------------------------ tiện ích

    private static String describe(Home h) {
        return "&7" + h.world() + " &8(&f" + Math.round(h.x()) + "&8, &f" + Math.round(h.y()) + "&8, &f"
                + Math.round(h.z()) + "&8)";
    }

    private static ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(HomeGuiPlugin.text(name));
        if (lore.length > 0) {
            List<Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(HomeGuiPlugin.text(line));
            }
            meta.lore(lines);
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
