package me.TyAlternative.dungeonBuilderPlugin.manager;

import me.TyAlternative.dungeonBuilderPlugin.DungeonBuilderPlugin;
import me.TyAlternative.dungeonBuilderPlugin.model.Dungeon;
import me.TyAlternative.dungeonBuilderPlugin.world.VoidWorldGenerator;

import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;

public class DungeonManager {

    private final DungeonBuilderPlugin plugin;
    private final Map<String, Dungeon> dungeons;
    private final File dungeonsFolder;

    public DungeonManager(DungeonBuilderPlugin plugin) {
        this.plugin = plugin;
        this.dungeons = new HashMap<>();
        this.dungeonsFolder = new File(plugin.getDataFolder(), "dungeons");

        if (!dungeonsFolder.exists()) {
            dungeonsFolder.mkdirs();
        }
    }


    public Dungeon createDungeon(String name) {
        if (dungeons.containsKey(name.toLowerCase())) return null;

        // Créer le monde VOID avec le générateur dont je suis PUTAIN DE FIER !
        String worldName = "dungeon_" + name.toLowerCase().replace(" ", "_");

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generator(new VoidWorldGenerator());
        creator.generateStructures(false);

        World world = creator.createWorld();;

        if (world == null) return null;

        // Config du monde
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setSpawnLocation(0,101,0);
        world.setAutoSave(true);

        // Désactiver les règles de jeu indésirables
        world.setGameRule(GameRule.SPAWN_RADIUS, 0);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setTime(6000); // Midi

        Dungeon dungeon = new Dungeon(name, world.getUID());
        dungeons.put(name.toLowerCase(), dungeon);

        saveDungeon(dungeon);

        return dungeon;
    }

    public boolean deleteDungeon(String name) {
        Dungeon dungeon = getDungeon(name);
        if (dungeon == null) return false;

        // Suppr le monde
        World world = dungeon.getWorld();
        if (world != null) {
            // Téléportation de tous les joueurs hors du monde
            Location spawn = Bukkit.getWorlds().getFirst().getSpawnLocation();
            for (Player player : world.getPlayers()) {
                player.teleport(spawn);
            }

            Bukkit.unloadWorld(world, false);

            deleteWorldFolder(world.getWorldFolder());
        }

        // Suppr le dossier du donjon
        File dungeonFolder = getDungeonFolder(name);
        deleteFolder(dungeonFolder);

        dungeons.remove(name.toLowerCase());

        return true;
    }


    public Dungeon getDungeon(String name) {
        return dungeons.get(name.toLowerCase());
    }

    public Collection<Dungeon> getAllDungeons(){
        return dungeons.values();
    }
    public String getDungeonNameByWorld(World world) {
        return plugin.getDungeonManager().getAllDungeons()
                .stream()
                .filter(dungeon -> dungeon.getWorld() != null && dungeon.getWorld().equals(world))
                .findFirst()
                .map(Dungeon::getName)
                .orElse(null);
    }

    public File getDungeonFolder(String dungeonName) {
        return new File(dungeonsFolder, dungeonName.toLowerCase().replace(" ", "_"));
    }

    public void saveDungeon(Dungeon dungeon) {
        try {
            File dungeonFolder = getDungeonFolder(dungeon.getName());
            if (!dungeonFolder.exists()) {
                dungeonFolder.mkdirs();
            }

            File infoFile = new File(dungeonFolder, "info.json");

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", dungeon.getName());
            jsonObject.addProperty("worldUUID", dungeon.getWorldUUID().toString());
            jsonObject.addProperty("creationDate", dungeon.getCreationDate());

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(infoFile)) {
                gson.toJson(jsonObject, writer);
            }


        } catch (IOException exception) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde du donjon " + dungeon.getName() + ": " + exception.getMessage());
        }
    }

    public void loadDungeons() {
        if (!dungeonsFolder.exists()) return;

        File[] dungeonFolders = dungeonsFolder.listFiles(File::isDirectory);
        if (dungeonFolders == null) return;

        for (File dungeonFolder : dungeonFolders) {
            File infoFile = new File(dungeonFolder, "info.json");
            if (!infoFile.exists()) continue;

            try {
                Gson gson = new Gson();
                try (FileReader reader = new FileReader(infoFile)) {
                    JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

                    String name = jsonObject.get("name").getAsString();
                    UUID worldUUID = UUID.fromString(jsonObject.get("worldUUID").getAsString());
                    long creationDate = jsonObject.get("creationDate").getAsLong();

                    // Vérifier que le monde existe
                    World world = Bukkit.getWorld(worldUUID);
                    if (world == null) {
                        // Charger le monde
                        String worldName = "dungeon_" + name.toLowerCase().replace(" ", "_");
                        WorldCreator creator = new WorldCreator(worldName);
                        creator.environment(World.Environment.NORMAL);
                        creator.generator(new VoidWorldGenerator());
                        world = creator.createWorld();

                        if (world == null) {
                            plugin.getLogger().warning("Impossible de charger le monde pour le donjon: " + name);
                            continue;
                        }
                    }

                    Dungeon dungeon = new Dungeon(name, worldUUID, creationDate);
                    dungeons.put(name.toLowerCase(), dungeon);

                    plugin.getLogger().info("Donjon chargé: " + name);

                }
            } catch (IOException exception) {
                plugin.getLogger().severe("Erreur lors du chargement du donjon dans " + dungeonFolder.getName() + ": " + exception.getMessage());
            }
        }

        plugin.getLogger().info("Chargement de " + dungeons.size() + " donjon(s)");
    }

    private void deleteWorldFolder(File folder) {
        deleteFolder(folder);
    }


    private void deleteFolder(File folder) {
        if (!folder.exists()) return;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) deleteFolder(file);
                else file.delete();
            }
        }
        folder.delete();
    }

}
