package me.TyAlternative.dungeonBuilderPlugin.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import java.util.*;

public class Room {

    private final String name;
    private final UUID worldUUID;
    private final Location corner1;
    private final Location corner2;
    private final Location goldBlockLocation;

    private Material iconMaterial;
    private final Set<String> tags;
    private int weight;
    private int minOccurrence;
    private int maxOccurrence;
    private int minDistance;
    private int maxDistance;

    public Room(String name, Location corner1, Location corner2) {
        this.name = name;
        this.worldUUID = corner1.getWorld().getUID();

        // Normaliser les coins (min/max)
        double minX = Math.min(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());

        double maxX = Math.max(corner1.getX(), corner2.getX());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        this.corner1 = new Location(corner1.getWorld(), minX, minY, minZ);
        this.corner2 = new Location(corner1.getWorld(), maxX, maxY, maxZ);

        // Position du bloc d'or: coin inférieur + (1,0,1)
        this.goldBlockLocation = new Location(
                corner1.getWorld(),
                minX - 1,
                minY,
                minZ - 1
        );

        this.iconMaterial = Material.GOLD_BLOCK;
        this.tags = new HashSet<>();
        this.weight = 10;
        this.minOccurrence = -1; // -1 = non défini
        this.maxOccurrence = -1;
        this.minDistance = -1;
        this.maxDistance = -1;

    }


    public String getName() {
        return name;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldUUID);
    }

    public UUID getWorldUUID() {
        return worldUUID;
    }

    public Location getCorner1() {
        return corner1.clone();
    }

    public Location getCorner2() {
        return corner2.clone();
    }

    public Location getGoldBlockLocation() {
        return goldBlockLocation.clone();
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public void setIconMaterial(Material material) {
        this.iconMaterial = material;
    }

    public Set<String> getTags() {
        return new HashSet<>(tags);
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public void clearTags() {
        tags.clear();
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = Math.max(0, weight);
    }

    public int getMinOccurrence() {
        return minOccurrence;
    }

    public void setMinOccurrence(int minOccurrence) {
        this.minOccurrence = minOccurrence;
    }

    public int getMaxOccurrence() {
        return maxOccurrence;
    }

    public void setMaxOccurrence(int maxOccurrence) {
        this.maxOccurrence = maxOccurrence;
    }

    public int getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(int minDistance) {
        this.minDistance = minDistance;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }


    public boolean contains(Location loc) {
        if (!loc.getWorld().getUID().equals(worldUUID)) return false;

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        return x >= corner1.getX() && x <= corner2.getX() &&
                y >= corner1.getY() && y <= corner2.getY() &&
                z >= corner1.getZ() && z <= corner2.getZ();
    }

    public boolean overlaps(Room other) {
        if (!this.worldUUID.equals(other.worldUUID)) return false;

        return !(corner2.getX() < other.corner1.getX() || corner1.getX() > other.corner2.getX() ||
                corner2.getY() < other.corner1.getY() || corner1.getY() > other.corner2.getY() ||
                corner2.getZ() < other.corner1.getZ() || corner1.getZ() > other.corner2.getZ());
    }

    public int getSizeX() {
        return (int) (corner2.getX() - corner1.getX() + 1);
    }

    public int getSizeY() {
        return (int) (corner2.getY() - corner1.getY() + 1);
    }

    public int getSizeZ() {
        return (int) (corner2.getZ() - corner1.getZ() + 1);
    }


    public boolean isValidSize() {
        return getSizeX() <= 64 && getSizeY() <= 64 && getSizeZ() <= 64 &&
                getSizeX() > 0 && getSizeY() > 0 && getSizeZ() > 0;
    }

}

