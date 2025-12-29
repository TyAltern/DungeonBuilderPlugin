package me.TyAlternative.dungeonBuilderPlugin.model;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.UUID;

public class Dungeon {

    private final String name;
    private final UUID worldUUID;
    private final long creationDate;

    public Dungeon(String name, UUID worldUUID) {
        this.name = name;
        this.worldUUID = worldUUID;
        this.creationDate = System.currentTimeMillis();
    }

    public Dungeon(String name, UUID worldUUID, long creationDate) {
        this.name = name;
        this.worldUUID = worldUUID;
        this.creationDate = creationDate;
    }

    public String getName() {
        return this.name;
    }

    public UUID getWorldUUID() {
        return this.worldUUID;
    }

    public World getWorld() {
        return Bukkit.getWorld(this.worldUUID);
    }

    public long getCreationDate() {
        return this.creationDate;
    }

    public String getWorldName() {
        return "dungeon_" + this.name.toLowerCase().replace(" ", "_");
    }
}
