package me.TyAlternative.dungeonBuilderPlugin.manager;

import me.TyAlternative.dungeonBuilderPlugin.DungeonBuilderPlugin;
import me.TyAlternative.dungeonBuilderPlugin.model.RoomTag;
import com.google.gson.*;
import org.bukkit.Material;

import java.io.*;
import java.util.*;

public class TagManager {

    private final DungeonBuilderPlugin plugin;
    private final Map<String, Map<String, RoomTag>> tagsByDungeon; // dungeonName -> (tagName -> RoomTag)

    private TagManager(DungeonBuilderPlugin plugin) {
        this.plugin = plugin;
        this.tagsByDungeon = new HashMap<>();
    }

    // Initialiser les tags par défaut pour un nouveau donjon
    public void initializeDefaultTags(String dungeonName) {
        Map<String, RoomTag> tags = new HashMap<>();

        // Tags "Start" (non supprimable)
        tags.put("Start", new RoomTag("Start", Material.GREEN_WOOL, false));

        // Tags supprimables
        tags.put("Boss", new RoomTag("Boss", Material.WITHER_SKELETON_SKULL, true));
        tags.put("Treasure", new RoomTag("Treasure", Material.CHEST, true));

        tagsByDungeon.put(dungeonName.toLowerCase(), tags);
        saveTags(dungeonName);
    }

    public RoomTag createTag(String dungeonName, String tagName, Material texture) {
        Map<String, RoomTag> tags = tagsByDungeon.get(dungeonName.toLowerCase());
        if (tags == null) {
            tags = new HashMap<>();
            tagsByDungeon.put(dungeonName.toLowerCase(), tags);
        }

        if (tags.containsKey(tagName)) return null; // Tag existe déjà

        RoomTag tag = new RoomTag(tagName, texture);
        tags.put(tagName, tag);
        saveTags(dungeonName);

        return tag;
    }

    public boolean deleteTag(String dungeonName, String tagName) {
        Map<String, RoomTag> tags = tagsByDungeon.get(dungeonName.toLowerCase());
        if (tags == null) return false;

        RoomTag tag = tags.get(tagName);
        if (tag == null || !tag.isDeletable()) return false;

        tags.remove(tagName);

        // Retirer ce tag de toutes les salles du donjon
        plugin.getRoomManager().removeTagFromAllRooms(dungeonName, tagName);

        saveTags(dungeonName);
        return true;
    }

    public RoomTag getTag(String dungeonName, String tagName) {
        Map<String, RoomTag> tags = tagsByDungeon.get(dungeonName.toLowerCase());
        if (tags == null) {
            return null;
        }
        return tags.get(tagName);
    }

    public Collection<RoomTag> getAllTags(String dungeonName) {
        Map<String, RoomTag> tags = tagsByDungeon.get(dungeonName.toLowerCase());
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags.values();
    }

    public void saveTags(String dungeonName) {
        try {
            File dungeonFolder = plugin.getDungeonManager().getDungeonFolder(dungeonName);
            if (!dungeonFolder.exists()) {
                dungeonFolder.mkdirs();
            }

            File tagsFile = new File(dungeonFolder, "tags.json");

            Map<String, RoomTag> tags = tagsByDungeon.get(dungeonName.toLowerCase());
            if (tags == null || tags.isEmpty()) {
                if (tagsFile.exists()) {
                    tagsFile.delete();
                }
                return;
            }

            JsonArray jsonArray = new JsonArray();

            for (RoomTag tag : tags.values()) {
                JsonObject tagObj = new JsonObject();
                tagObj.addProperty("name", tag.getName());
                tagObj.addProperty("texture", tag.getTexture().name());
                tagObj.addProperty("deletable", tag.isDeletable());
                jsonArray.add(tagObj);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(tagsFile)) {
                gson.toJson(jsonArray, writer);
            }

        } catch (IOException exception) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde des tags pour " + dungeonName + ": " + exception.getMessage());
        }
    }


    public void loadTags(String dungeonName) {
        File dungeonFolder = plugin.getDungeonManager().getDungeonFolder(dungeonName);
        File tagsFile = new File(dungeonFolder, "tags.json");

        if (!tagsFile.exists()) {
            // Initialiser les tags par défaut
            initializeDefaultTags(dungeonName);
            return;
        }

        try {
            Gson gson = new Gson();
            try (FileReader reader = new FileReader(tagsFile)) {
                JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

                Map<String, RoomTag> tags = new HashMap<>();

                for (JsonElement element : jsonArray) {
                    JsonObject tagObj = element.getAsJsonObject();

                    String name = tagObj.get("name").getAsString();
                    Material texture = Material.valueOf(tagObj.get("texture").getAsString());
                    boolean deletable = tagObj.get("deletable").getAsBoolean();

                    RoomTag tag = new RoomTag(name, texture, deletable);
                    tags.put(name, tag);
                }

                tagsByDungeon.put(dungeonName.toLowerCase(), tags);

                plugin.getLogger().info("Chargement de " + tags.size() + " tag(s) pour le donjon " + dungeonName);
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors du chargement des tags pour " + dungeonName + ": " + e.getMessage());
            // En cas d'erreur, initialiser les tags par défaut
            initializeDefaultTags(dungeonName);
        }
    }

    public void deleteDungeonTags(String dungeonName) {
        tagsByDungeon.remove(dungeonName.toLowerCase());
    }
}
