package me.TyAlternative.dungeonBuilderPlugin.manager;


import me.TyAlternative.dungeonBuilderPlugin.DungeonBuilderPlugin;
import me.TyAlternative.dungeonBuilderPlugin.model.Room;
import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.*;
import java.util.*;

public class RoomManager {

    private final DungeonBuilderPlugin plugin;
    private final Map<String, Map<String, Room>> roomsByDungeon;
    private final Map<String, Integer> roomCountersByDungeon;

    public RoomManager(DungeonBuilderPlugin plugin) {
        this.plugin = plugin;
        this.roomsByDungeon = new HashMap<>();
        this.roomCountersByDungeon = new HashMap<>();
    }

    public String createRoom(String dungeonName, Location corner1, Location corner2) {
        if (!roomsByDungeon.containsKey(dungeonName.toLowerCase())) {
            roomsByDungeon.put(dungeonName.toLowerCase(), new HashMap<>());
            roomCountersByDungeon.put(dungeonName.toLowerCase(), 0);
        }

        Map<String, Room> rooms = roomsByDungeon.get(dungeonName.toLowerCase());
        int counter = roomCountersByDungeon.get(dungeonName.toLowerCase());

        String roomName = "Room_" + (++counter);
        Room room = new Room(roomName, corner1, corner2);

        if (!room.isValidSize()) return null; // Taille invalide

        for (Room existingRoom : rooms.values()) {
            if (room.overlaps(existingRoom)) return null; // Chevauchement détecté
        }

        rooms.put(roomName, room);
        roomCountersByDungeon.put(dungeonName.toLowerCase(), counter);

        // Placer le block d'or
        Location goldLoc = room.getGoldBlockLocation();
        goldLoc.getBlock().setType(Material.GOLD_BLOCK);

        // Sauvegarder
        saveRooms(dungeonName);

        return roomName;
    }

    public Room getRoom(String dungeonName, String roomName) {
        Map<String, Room> rooms = roomsByDungeon.get(dungeonName.toLowerCase());
        if (rooms == null) {
            return null;
        }
        return rooms.get(roomName);
    }

    public Room getRoomByGoldBlock(String dungeonName, Location location) {
        Map<String, Room> rooms = roomsByDungeon.get(dungeonName.toLowerCase());
        if (rooms == null) return null;

        for (Room room : rooms.values()) {
            Location goldLoc = room.getGoldBlockLocation();
            if (goldLoc.getBlockX() == location.getBlockX() &&
                    goldLoc.getBlockY() == location.getBlockY() &&
                    goldLoc.getBlockZ() == location.getBlockZ() &&
                    goldLoc.getWorld().equals(location.getWorld())) {
                return room;
            }
        }
        return null; // Not found
    }

    public boolean deleteRoom(String dungeonName, String roomName) {
        Map<String, Room> rooms = roomsByDungeon.get(dungeonName.toLowerCase());
        if (rooms == null) return false;

        Room room = rooms.remove(roomName);
        if (room == null) return false;

        Location goldLoc = room.getGoldBlockLocation();
        if (goldLoc.getWorld() != null) goldLoc.getBlock().setType(Material.AIR);

        // Sauvegarder
        saveRooms(dungeonName);

        return true;
    }

    public void removeTagFromAllRooms(String dungeonName, String tagName) {
        Map<String, Room> rooms = roomsByDungeon.get(dungeonName.toLowerCase());
        if (rooms == null) return;

        for (Room room : rooms.values()) {
            room.removeTag(tagName);
        }
    }


    public Collection<Room> getRooms(String dungeonName) {
        Map<String, Room> rooms = roomsByDungeon.get(dungeonName.toLowerCase());
        if (rooms == null) return Collections.emptyList();
        return rooms.values();
    }

    public int getRoomCount(String dungeonName) {
        Map<String, Room> rooms = roomsByDungeon.get(dungeonName.toLowerCase());
        return rooms != null ? rooms.size() : 0;
    }

    public String getDungeonNameByWorld(World world) {
        for (Map.Entry<String, Map<String, Room>> entry : roomsByDungeon.entrySet()) {
            for (Room room : entry.getValue().values()) {
                if (room.getWorld().equals(world)) return entry.getKey();
            }
        }
        return null;
    }


    public void saveRooms(String dungeonName) {
        try {
            File dungeonFolder = plugin.getDungeonManager().getDungeonFolder(dungeonName);
            if (!dungeonFolder.exists()) {
                dungeonFolder.mkdirs();
            }

            File roomsFile = new File(dungeonFolder, "rooms.json");

            Map<String, Room> rooms = roomsByDungeon.get(dungeonName.toLowerCase());
            if (rooms == null || rooms.isEmpty()) {
                if (roomsFile.exists()) {
                    roomsFile.delete();
                }
                return;
            }

            JsonArray jsonArray = new JsonArray();

            for (Room room : rooms.values()) {
                JsonObject roomObj= new JsonObject();
                roomObj.addProperty("name", room.getName());
                roomObj.addProperty("world", room.getWorldUUID().toString());
                roomObj.addProperty("iconMaterial", room.getIconMaterial().name());

                JsonArray tagsArray = new JsonArray();
                for (String tag : room.getTags()) {
                    tagsArray.add(tag);
                }
                roomObj.add("tags", tagsArray);

                roomObj.addProperty("weight", room.getWeight());
                roomObj.addProperty("minOccurrence", room.getMinOccurrence());
                roomObj.addProperty("maxOccurrence", room.getMaxOccurrence());
                roomObj.addProperty("minDistance", room.getMinDistance());
                roomObj.addProperty("maxDistance", room.getMaxDistance());

                JsonObject corner1Obj = new JsonObject();
                corner1Obj.addProperty("x", room.getCorner1().getX());
                corner1Obj.addProperty("y", room.getCorner1().getY());
                corner1Obj.addProperty("z", room.getCorner1().getZ());
                roomObj.add("corner1", corner1Obj);

                JsonObject corner2Obj = new JsonObject();
                corner2Obj.addProperty("x", room.getCorner2().getX());
                corner2Obj.addProperty("y", room.getCorner2().getY());
                corner2Obj.addProperty("z", room.getCorner2().getZ());
                roomObj.add("corner2", corner2Obj);

                jsonArray.add(roomObj);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(roomsFile)) {
                gson.toJson(jsonArray, writer);
            }
            
            plugin.getLogger().info("Sauvegarde de " + rooms.size() + " salle(s)");
            
        } catch (IOException exception) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde des salles pour " + dungeonName + ": " + exception.getMessage());
        }
    }


    public void loadRooms(String dungeonName) {

        File dungeonFolder = plugin.getDungeonManager().getDungeonFolder(dungeonName);
        File roomsFile = new File(dungeonFolder, "rooms.json");

        if (!roomsFile.exists()) {
            return;
        }

        try {
            Gson gson = new Gson();
            try (FileReader reader = new FileReader(roomsFile)) {
                JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

                Map<String, Room> rooms = new HashMap<>();
                int maxCounter = 0;

                for (JsonElement element : jsonArray) {
                    JsonObject roomObj = element.getAsJsonObject();

                    String name = roomObj.get("name").getAsString();
                    UUID worldUUID = UUID.fromString(roomObj.get("world").getAsString());
                    World world = Bukkit.getWorld(worldUUID);

                    if (world == null) {
                        plugin.getLogger().warning("Monde introuvable pour la salle " + name);
                        continue;
                    }

                    JsonObject corner1Obj = roomObj.getAsJsonObject("corner1");
                    JsonObject corner2Obj = roomObj.getAsJsonObject("corner2");

                    Location corner1 = new Location(world,
                            corner1Obj.get("x").getAsDouble(),
                            corner1Obj.get("y").getAsDouble(),
                            corner1Obj.get("z").getAsDouble());

                    Location corner2 = new Location(world,
                            corner2Obj.get("x").getAsDouble(),
                            corner2Obj.get("y").getAsDouble(),
                            corner2Obj.get("z").getAsDouble());

                    Room room = new Room(name, corner1, corner2);


                    if (roomObj.has("iconMaterial")) {
                        Material iconMat = Material.valueOf(roomObj.get("iconMaterial").getAsString());
                        room.setIconMaterial(iconMat);
                    }

                    if (roomObj.has("tags")) {
                        JsonArray tagsArray = roomObj.getAsJsonArray("tags");
                        for (JsonElement tagElement : tagsArray) {
                            room.addTag(tagElement.getAsString());
                        }
                    }

                    if (roomObj.has("weight")) {
                        room.setWeight(roomObj.get("weight").getAsInt());
                    }
                    if (roomObj.has("minOccurrence")) {
                        room.setMinOccurrence(roomObj.get("minOccurrence").getAsInt());
                    }
                    if (roomObj.has("maxOccurrence")) {
                        room.setMaxOccurrence(roomObj.get("maxOccurrence").getAsInt());
                    }
                    if (roomObj.has("minDistance")) {
                        room.setMinDistance(roomObj.get("minDistance").getAsInt());
                    }
                    if (roomObj.has("maxDistance")) {
                        room.setMaxDistance(roomObj.get("maxDistance").getAsInt());
                    }

                    rooms.put(name, room);

                    // Restaurer le bloc d'or
                    Location goldLoc = room.getGoldBlockLocation();
                    goldLoc.getBlock().setType(room.getIconMaterial());

                    try {
                        int num = Integer.parseInt(name.substring(5)); // "Room_X"
                        if (num >= maxCounter) {
                            maxCounter = num;
                        }
                    } catch (NumberFormatException ignored) {}
                }

                roomsByDungeon.put(dungeonName.toLowerCase(), rooms);
                roomCountersByDungeon.put(dungeonName.toLowerCase(), maxCounter);

                plugin.getLogger().info("Chargement de " + rooms.size() + " salle(s) pour le donjon " + dungeonName);

            }
        } catch (IOException exception) {
            plugin.getLogger().severe("Erreur lors du chargement des salles pour " + dungeonName + ": " + exception.getMessage());
        }

    }

    public void deleteDungeonRooms(String dungeonName) {
        roomsByDungeon.remove(dungeonName.toLowerCase());
        roomCountersByDungeon.remove(dungeonName.toLowerCase());
    }

}
