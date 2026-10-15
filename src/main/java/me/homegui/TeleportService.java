package me.homegui;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Xử lý đếm ngược trước khi dịch chuyển và hồi chiêu sau khi dịch chuyển. */
public final class TeleportService {

    private static final class Pending {
        final int blockX;
        final int blockZ;
        volatile ScheduledTask task;

        Pending(int blockX, int blockZ) {
            this.blockX = blockX;
            this.blockZ = blockZ;
        }
    }

    private final HomeGuiPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public TeleportService(HomeGuiPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasPending() {
        return !pending.isEmpty();
    }

    public long cooldownLeftSeconds(UUID id) {
        Long until = cooldowns.get(id);
        if (until == null) {
            return 0;
        }
        long left = until - System.currentTimeMillis();
        if (left <= 0) {
            cooldowns.remove(id);
            return 0;
        }
        return (left + 999) / 1000;
    }

    /** Bắt đầu đếm ngược để dịch chuyển người chơi về home có chỉ số index (0-4). */
    public void start(Player player, int index) {
        UUID id = player.getUniqueId();

        if (pending.containsKey(id)) {
            player.sendMessage(HomeGuiPlugin.text("&cBạn đang chờ dịch chuyển rồi!"));
            return;
        }
        long cooldown = cooldownLeftSeconds(id);
        if (cooldown > 0) {
            player.sendMessage(HomeGuiPlugin.text("&cBạn phải chờ &f" + cooldown + "s &cnữa mới dịch chuyển được."));
            return;
        }
        Home home = plugin.homes().get(id, index);
        if (home == null) {
            player.sendMessage(HomeGuiPlugin.text("&cHome " + (index + 1) + " chưa được đặt."));
            return;
        }

        int delay = plugin.delaySeconds();
        if (delay <= 0) {
            teleport(player, home, index);
            return;
        }

        Location here = player.getLocation();
        Pending pd = new Pending(here.getBlockX(), here.getBlockZ());
        if (pending.putIfAbsent(id, pd) != null) {
            return;
        }
        player.sendMessage(HomeGuiPlugin.text("&eSẽ dịch chuyển về &fHome " + (index + 1) + " &esau &f" + delay
                + " giây&e. Hãy đứng yên!"));

        AtomicInteger remaining = new AtomicInteger(delay);
        pd.task = player.getScheduler().runAtFixedRate(plugin, task -> {
            if (pending.get(id) != pd) { // đã bị huỷ
                task.cancel();
                return;
            }
            int left = remaining.decrementAndGet();
            if (left <= 0) {
                task.cancel();
                pending.remove(id, pd);
                teleport(player, home, index);
            } else {
                player.sendActionBar(HomeGuiPlugin.text("&eDịch chuyển sau &f" + left + "s &7- đứng yên!"));
            }
        }, () -> pending.remove(id, pd), 20L, 20L);
    }

    /** Huỷ đếm ngược nếu người chơi di chuyển khỏi vị trí ban đầu. */
    public void cancelIfMoved(Player player, Location to) {
        UUID id = player.getUniqueId();
        Pending pd = pending.get(id);
        if (pd == null) {
            return;
        }
        if (to.getBlockX() != pd.blockX || to.getBlockZ() != pd.blockZ) {
            if (pending.remove(id, pd)) {
                ScheduledTask task = pd.task;
                if (task != null) {
                    task.cancel();
                }
                player.sendMessage(HomeGuiPlugin.text("&cĐã huỷ dịch chuyển vì bạn di chuyển!"));
            }
        }
    }

    private void teleport(Player player, Home home, int index) {
        Location location = home.toLocation();
        if (location == null) {
            player.sendMessage(HomeGuiPlugin.text("&cThế giới của Home " + (index + 1) + " không còn tồn tại."));
            return;
        }
        UUID id = player.getUniqueId();
        player.teleportAsync(location, PlayerTeleportEvent.TeleportCause.PLUGIN).thenAccept(success -> {
            if (success) {
                int cd = plugin.cooldownSeconds();
                if (cd > 0) {
                    cooldowns.put(id, System.currentTimeMillis() + cd * 1000L);
                }
            }
            player.getScheduler().run(plugin, t -> player.sendMessage(HomeGuiPlugin.text(success
                    ? "&aĐã dịch chuyển về &fHome " + (index + 1)
                    : "&cDịch chuyển thất bại.")), null);
        });
    }
}
