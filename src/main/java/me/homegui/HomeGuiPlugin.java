package me.homegui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HomeGuiPlugin extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private HomeManager homes;
    private TeleportService teleports;

    private int delaySeconds;
    private int cooldownSeconds;
    private boolean cancelOnMove;

    /** Chuyển chuỗi dùng mã màu '&' thành Component (tắt chữ nghiêng mặc định của lore). */
    public static Component text(String legacy) {
        return LEGACY.deserialize(legacy).decoration(TextDecoration.ITALIC, false);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        delaySeconds = Math.max(0, getConfig().getInt("delay-seconds", 5));
        cooldownSeconds = Math.max(0, getConfig().getInt("cooldown-seconds", 60));
        cancelOnMove = getConfig().getBoolean("cancel-on-move", true);

        homes = new HomeManager(this);
        homes.load();
        teleports = new TeleportService(this);

        getServer().getPluginManager().registerEvents(new HomeListener(this), this);
        PluginCommand command = getCommand("home");
        if (command != null) {
            command.setExecutor(new HomeCommand(this));
        }
    }

    @Override
    public void onDisable() {
        if (homes != null) {
            homes.saveNow();
        }
    }

    /** Home 1 ai cũng dùng được; Home 2-5 cần quyền homegui.home.N (cấp bằng LuckPerms). */
    public boolean canUse(Player player, int index) {
        return index == 0 || player.hasPermission("homegui.home." + (index + 1));
    }

    public HomeManager homes() {
        return homes;
    }

    public TeleportService teleports() {
        return teleports;
    }

    public int delaySeconds() {
        return delaySeconds;
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public boolean cancelOnMove() {
        return cancelOnMove;
    }
}
