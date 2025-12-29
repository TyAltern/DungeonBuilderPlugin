package me.TyAlternative.dungeonBuilderPlugin.model;

import org.bukkit.Material;

public class RoomTag {

    private final String name;
    private Material texture;
    private final boolean deletable;

    // Constructeur pour tags custom
    public RoomTag(String name, Material texture) {
        this.name = name;
        this.texture = texture;
        this.deletable = true;
    }

    // Constructeur pour tags par défaut
    public RoomTag(String name, Material texture, boolean deletable) {
        this.name = name;
        this.texture = texture;
        this.deletable = deletable;
    }

    public String getName() {
        return name;
    }

    public Material getTexture() {
        return texture;
    }

    public void setTexture(Material texture) {
        this.texture = texture;
    }

    public boolean isDeletable() {
        return deletable;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RoomTag)) return false;
        RoomTag other = (RoomTag) obj;
        return name.equals(other.getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
