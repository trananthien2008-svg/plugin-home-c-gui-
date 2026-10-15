package me.homegui;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/** Một điểm home đã lưu. */
public record Home(String world, double x, double y, double z, float yaw, float pitch) {

    public static Home of(Location l) {
        return new Home(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    /** @return Location, hoặc null nếu world không còn tồn tại. */
    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }
}
