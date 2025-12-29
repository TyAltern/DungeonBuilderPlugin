package me.TyAlternative.dungeonBuilderPlugin.manager;

import me.TyAlternative.dungeonBuilderPlugin.util.RoomVisualizer;
import me.TyAlternative.dungeonBuilderPlugin.model.Room;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EditModeManager {

    private final Set<UUID> playersInEditMode;
    private final Map<UUID, Location> firstCornerSelection;
    private final Map<UUID, Location> secondCornerSelection;
    private final Map<UUID, Room> focusedRooms;
    private final Map<UUID, RoomVisualizer> activeVisualizers;
    private final Map<UUID, String> pendingRoomDeletions;

    public EditModeManager() {
        this.playersInEditMode = new HashSet<>();
        this.firstCornerSelection = new HashMap<>();
        this.secondCornerSelection = new HashMap<>();
        this.focusedRooms = new HashMap<>();
        this.activeVisualizers = new HashMap<>();
        this.pendingRoomDeletions = new HashMap<>();
    }

    public boolean isInEditMode (Player player) {
        return playersInEditMode.contains(player.getUniqueId());
    }

    public void toggleEditMode(Player player) {
        UUID uuid = player.getUniqueId();

        if (playersInEditMode.contains(uuid)) {
            // Désactiver le mode Edit
            playersInEditMode.remove(uuid);
            firstCornerSelection.remove(uuid);

            // Nettoyer la visualisation
            clearVisualization(player);

            // Restaurer l'inventaire
            player.getInventory().clear();
            player.updateInventory();

            player.sendMessage("§c[Dungeon] Mode Edit désactivé");
        } else {
            // Activer le mode Edit
            playersInEditMode.add(uuid);

            // Donner les items
            setupEditInventory(player);

            player.sendMessage("§a[Dungeon] Mode Edit activé");
        }
    }

    private void setupEditInventory(Player player) {
        player.getInventory().clear();

        // Slot 0: New Room (Nether Star)
        ItemStack netherStar = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = netherStar.getItemMeta();
        meta.displayName(Component.text("§6§lNew Room"));
        meta.lore(Arrays.asList(
                Component.text("§7Clic gauche: Premier coin"),
                Component.text("§7Clic droit: Deuxième coin"),
                Component.text("§7Drop: Enregistrer la salle")
        ));
        netherStar.setItemMeta(meta);

        // Slot 1: Delete Room (Barrière)
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta barrierMeta = barrier.getItemMeta();
        barrierMeta.displayName(Component.text("§c§lDelete Room"));
        barrierMeta.lore(Arrays.asList(
                Component.text("§7Drop: Supprimer la salle focus")
        ));
        barrier.setItemMeta(barrierMeta);


        player.getInventory().setItem(0, netherStar);
        player.getInventory().setItem(1, barrier);
        player.updateInventory();
    }


    public void setFirstCorner(Player player, Location location) {
        firstCornerSelection.put(player.getUniqueId(), location);
        player.sendMessage("§a[Dungeon] Premier coin sélectionné: §f" +
                formatLocation(location));
    }

    public Location getFirstCorner(Player player) {
        return firstCornerSelection.get(player.getUniqueId());
    }

    public boolean hasFirstCorner(Player player) {
        return firstCornerSelection.containsKey(player.getUniqueId());
    }


    public void setSecondCorner(Player player, Location location) {
        Location first = getFirstCorner(player);
        if (first == null) {
            player.sendMessage("§c[Dungeon] Sélectionnez d'abord le premier coin!");
            return;
        }

        secondCornerSelection.put(player.getUniqueId(), location);
        player.sendMessage("§a[Dungeon] Deuxième coin sélectionné: §f" +
                formatLocation(location));
        player.sendMessage("§e[Dungeon] Drop la Nether Star pour enregistrer la salle!");

        // Visualiser la sélection
        visualizeSelection(player, first, location);
    }

    public Location getSecondCorner(Player player) {
        return secondCornerSelection.get(player.getUniqueId());
    }

    public boolean hasSecondCorner(Player player) {
        return secondCornerSelection.containsKey(player.getUniqueId());
    }

    public void clearSelection(Player player) {
        firstCornerSelection.remove(player.getUniqueId());
        secondCornerSelection.remove(player.getUniqueId());
        clearVisualization(player);
    }

    public void setFocusedRoom(Player player, Room room) {
        UUID uuid = player.getUniqueId();

        // Retirer le focus précédent
        if (focusedRooms.containsKey(uuid)) clearVisualization(player);

        focusedRooms.put(uuid, room);

        // Visualiser la nouvelle salle
        if (room != null && isInEditMode(player)) {
            visualizeRoom(player, room);
            player.sendMessage("§a[Dungeon] Focus sur: §f" + room.getName());
        }
    }

    public Room getFocusedRoom(Player player) {
        return focusedRooms.get(player.getUniqueId());
    }

    public void clearFocusedRoom(Player player) {
        focusedRooms.remove(player.getUniqueId());
        clearVisualization(player);
    }

    private void visualizeSelection(Player player, Location corner1, Location corner2) {
        clearVisualization(player);
        RoomVisualizer visualizer = new RoomVisualizer(player, corner1, corner2);
        visualizer.show();
        activeVisualizers.put(player.getUniqueId(), visualizer);
    }

    private void visualizeRoom(Player player, Room room) {
        clearVisualization(player);
        RoomVisualizer visualizer = new RoomVisualizer(player, room.getCorner1(), room.getCorner2());
        visualizer.show();
        activeVisualizers.put(player.getUniqueId(), visualizer);
    }

    private void clearVisualization(Player player) {
        UUID uuid = player.getUniqueId();
        RoomVisualizer visualizer = activeVisualizers.get(uuid);
        if (visualizer == null) return;
        visualizer.hide();
        activeVisualizers.remove(uuid);
    }

    public void clearAllVisualizations() {
        for (RoomVisualizer visualizer : activeVisualizers.values()) {
            visualizer.hide();
        }
        activeVisualizers.clear();
    }

    public void setPendingRoomDeletion(Player player, String roomName) {
        pendingRoomDeletions.put(player.getUniqueId(), roomName);
    }

    public String getPendingRoomDeletion(Player player) {
        return pendingRoomDeletions.remove(player.getUniqueId());
    }

    public boolean hasPendingRoomDeletion(Player player) {
        return pendingRoomDeletions.containsKey(player.getUniqueId());
    }




    private String formatLocation(Location loc) {
        return String.format("(%d, %d, %d)",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

}
