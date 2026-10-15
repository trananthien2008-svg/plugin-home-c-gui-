package me.homegui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Lưu home trong bộ nhớ, ghi ra homes.yml bất đồng bộ (an toàn trên Folia). */
public final class HomeManager {

    public static final int MAX_HOMES = 5;

    private final HomeGuiPlugin plugin;
    private final File file;
    private final Map<UUID, Home[]> homes = new HashMap<>();

    private final Object fileLock = new Object();
    private long version;
    private long written;

    public HomeManager(HomeGuiPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
    }

    public synchronized void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("homes");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection player = root.getConfigurationSection(key);
            if (player == null) {
                continue;
            }
            Home[] arr = new Home[MAX_HOMES];
            for (int i = 0; i < MAX_HOMES; i++) {
                ConfigurationSection h = player.getConfigurationSection(String.valueOf(i));
                if (h == null || h.getString("world") == null) {
                    continue;
                }
                arr[i] = new Home(h.getString("world"), h.getDouble("x"), h.getDouble("y"), h.getDouble("z"),
                        (float) h.getDouble("yaw"), (float) h.getDouble("pitch"));
            }
            homes.put(id, arr);
        }
    }

    public synchronized Home get(UUID id, int index) {
        Home[] arr = homes.get(id);
        return arr == null ? null : arr[index];
    }

    public synchronized void set(UUID id, int index, Home home) {
        homes.computeIfAbsent(id, k -> new Home[MAX_HOMES])[index] = home;
        saveAsync();
    }

    public synchronized void remove(UUID id, int index) {
        Home[] arr = homes.get(id);
        if (arr != null) {
            arr[index] = null;
            saveAsync();
        }
    }

    /** Ghi file ngay trên thread hiện tại (dùng khi tắt plugin). */
    public void saveNow() {
        String data;
        long v;
        synchronized (this) {
            data = serialize();
            v = ++version;
        }
        write(data, v);
    }

    // đang giữ khoá của this
    private void saveAsync() {
        String data = serialize();
        long v = ++version;
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> write(data, v));
    }

    private String serialize() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Home[]> entry : homes.entrySet()) {
            Home[] arr = entry.getValue();
            for (int i = 0; i < arr.length; i++) {
                Home h = arr[i];
                if (h == null) {
                    continue;
                }
                String path = "homes." + entry.getKey() + "." + i;
                yaml.set(path + ".world", h.world());
                yaml.set(path + ".x", h.x());
                yaml.set(path + ".y", h.y());
                yaml.set(path + ".z", h.z());
                yaml.set(path + ".yaw", (double) h.yaw());
                yaml.set(path + ".pitch", (double) h.pitch());
            }
        }
        return yaml.saveToString();
    }

    private void write(String data, long v) {
        synchronized (fileLock) {
            if (v <= written) {
                return; // đã có bản mới hơn được ghi
            }
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                Files.writeString(file.toPath(), data, StandardCharsets.UTF_8);
                written = v;
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Không ghi được homes.yml", e);
            }
        }
    }
}
