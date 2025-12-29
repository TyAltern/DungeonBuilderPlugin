package me.TyAlternative.dungeonBuilderPlugin.listener;


import me.TyAlternative.dungeonBuilderPlugin.DungeonBuilderPlugin;
import me.TyAlternative.dungeonBuilderPlugin.manager.DungeonManager;
import me.TyAlternative.dungeonBuilderPlugin.manager.EditModeManager;
import me.TyAlternative.dungeonBuilderPlugin.manager.RoomManager;
import me.TyAlternative.dungeonBuilderPlugin.model.Room;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class RoomInteractionListener implements Listener {

    private final DungeonBuilderPlugin plugin;
    private final EditModeManager editManager;
    private final DungeonManager dungeonManager;
    private final RoomManager roomManager;

    public RoomInteractionListener(DungeonBuilderPlugin plugin) {
        this.plugin = plugin;
        this.editManager = plugin.getEditModeManager();
        this.dungeonManager = plugin.getDungeonManager();
        this.roomManager = plugin.getRoomManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.GOLD_BLOCK) {
            return;
        }

        Location blockLoc = event.getBlock().getLocation();

        // Trouver le donjon du monde actuel
        String dungeonName = findDungeonByWorld(blockLoc.getWorld());
        if (dungeonName == null) {
            return;
        }

        Room room = roomManager.getRoomByGoldBlock(dungeonName, blockLoc);

        if (room == null) {
            return; // Ce n'est pas un bloc d'or de salle
        }

        // Annuler la destruction
        event.setCancelled(true);

        Player player = event.getPlayer();

        // Si le joueur est en mode Edit, focus sur la salle
        if (editManager.isInEditMode(player)) {
            editManager.setFocusedRoom(player, room);
        } else {
            player.sendMessage("§c[Dungeon] Activez le mode Edit pour interagir avec les salles!");
        }
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getClickedBlock() == null ||
                event.getClickedBlock().getType() != Material.GOLD_BLOCK) {
            return;
        }

        Location blockLoc = event.getClickedBlock().getLocation();
        // Trouver le donjon du monde actuel
        String dungeonName = findDungeonByWorld(blockLoc.getWorld());
        if (dungeonName == null) {
            return;
        }
        Room room = roomManager.getRoomByGoldBlock(dungeonName, blockLoc);

        if (room == null) {
            return; // Ce n'est pas un bloc d'or de salle
        }

        Player player = event.getPlayer();

        if (!editManager.isInEditMode(player)) {
            event.setCancelled(true);
            player.sendMessage("§c[Dungeon] Activez le mode Edit pour voir les infos de la salle!");
            return;
        }

        event.setCancelled(true);

        // Afficher les informations de la salle
        player.sendMessage("§6§l========== Informations Salle ==========");
        player.sendMessage("§e Nom: §f" + room.getName());
        player.sendMessage("§e Position:");
        player.sendMessage("  §7Coin 1: §f" + formatLocation(room.getCorner1()));
        player.sendMessage("  §7Coin 2: §f" + formatLocation(room.getCorner2()));
        player.sendMessage("§e Dimensions: §f" +
                room.getSizeX() + "x" + room.getSizeY() + "x" + room.getSizeZ());
        player.sendMessage("§6§l=====================================");

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Protéger les blocs d'or des explosions
        event.blockList().removeIf(block -> {
            if (block.getType() != Material.GOLD_BLOCK) {
                return false;
            }

            Location blockLoc = block.getLocation();
            String dungeonName = findDungeonByWorld(blockLoc.getWorld());
            if (dungeonName == null) {
                return false;
            }

            return roomManager.getRoomByGoldBlock(dungeonName, blockLoc) != null;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        // Protéger les blocs d'or des explosions (TNT, etc.)
        event.blockList().removeIf(block -> {
            if (block.getType() != Material.GOLD_BLOCK) {
                return false;
            }

            Location blockLoc = block.getLocation();
            String dungeonName = findDungeonByWorld(blockLoc.getWorld());
            if (dungeonName == null) {
                return false;
            }

            return roomManager.getRoomByGoldBlock(dungeonName, blockLoc) != null;
        });
    }


    private String findDungeonByWorld(World world) {
        return dungeonManager.getDungeonNameByWorld(world);
    }

    private String formatLocation(Location loc) {
        return String.format("(%d, %d, %d)",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
