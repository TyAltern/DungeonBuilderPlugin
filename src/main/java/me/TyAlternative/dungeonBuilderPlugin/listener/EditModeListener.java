package me.TyAlternative.dungeonBuilderPlugin.listener;

import me.TyAlternative.dungeonBuilderPlugin.DungeonBuilderPlugin;
import me.TyAlternative.dungeonBuilderPlugin.manager.DungeonManager;
import me.TyAlternative.dungeonBuilderPlugin.manager.EditModeManager;
import me.TyAlternative.dungeonBuilderPlugin.manager.RoomManager;
import me.TyAlternative.dungeonBuilderPlugin.model.Room;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class EditModeListener implements Listener {

    private final DungeonBuilderPlugin plugin;
    private final EditModeManager editManager;
    private final RoomManager roomManager;
    private final DungeonManager dungeonManager;

    public EditModeListener(DungeonBuilderPlugin plugin) {
        this.plugin = plugin;
        this.editManager = plugin.getEditModeManager();
        this.roomManager = plugin.getRoomManager();
        this.dungeonManager = plugin.getDungeonManager();
    }

    @EventHandler
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        // Vérifier si le joueur est OP
        if (!player.isOp()) {
            return;
        }

        // Toggle le mode Edit
        event.setCancelled(true);
        editManager.toggleEditMode(player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!editManager.isInEditMode(player)) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        // Vérifier que c'est bien notre item New Room
        if (item.getType() != Material.NETHER_STAR) return;
//        if (!item.getItemMeta().displayName().contains(Component.text("New Room"))) return;


        Location clickedLocation = event.getClickedBlock() != null ?
                event.getClickedBlock().getLocation() : null;

        if (clickedLocation == null) return;

        DungeonBuilderPlugin.getInstance().getLogger().info("click");

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Premier coin
            event.setCancelled(true);
            editManager.setFirstCorner(player, clickedLocation);

        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Deuxième coin
            event.setCancelled(true);

            if (!editManager.hasFirstCorner(player)) {
                player.sendMessage("§c[Dungeon] Sélectionnez d'abord le premier coin!");
                return;
            }

            editManager.setSecondCorner(player, clickedLocation);
        }
    }


    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (!editManager.isInEditMode(player)) return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (!item.hasItemMeta() || !item.getItemMeta().hasCustomName()) return;

        if (item.getType() == Material.NETHER_STAR) {
            event.setCancelled(true);
            handleNewRoomDrop(player);
        }
        // Gestion de la Barrière (Delete Room)
        else if (item.getType() == Material.BARRIER) {
            event.setCancelled(true);
            handleDeleteRoomDrop(player);
        }

    }

    private void handleNewRoomDrop(Player player) {

        // Vérifier qu'on a les deux coins
        Location first = editManager.getFirstCorner(player);
        Location second = editManager.getSecondCorner(player);

        if (first == null || second == null) {
            player.sendMessage("§c[Dungeon] Sélectionnez les deux coins avant d'enregistrer!");
            return;
        }


        // Trouver le donjon du monde actuel
        World world = player.getWorld();
        String dungeonName = dungeonManager.getDungeonNameByWorld(world);

        if (dungeonName == null) {
            player.sendMessage("§c[Dungeon] Vous devez être dans un monde de donjon!");
            player.sendMessage("§7Créez un donjon avec §f/dungeon create <nom>");
            return;
        }

        // Créer la salle
        String roomName = roomManager.createRoom(dungeonName, first, second);

        if (roomName == null) {
            player.sendMessage("§c[Dungeon] Impossible de créer la salle!");
            player.sendMessage("§c- Vérifiez la taille (max 64x64x64)");
            player.sendMessage("§c- Vérifiez qu'il n'y a pas de chevauchement");
            return;
        }

        player.sendMessage("§a[Dungeon] Salle créée: §f" + roomName);

        // Focus automatique sur la nouvelle salle
        editManager.setFocusedRoom(player, roomManager.getRoom(dungeonName, roomName));

        // Clear la sélection
        editManager.clearSelection(player);
    }

    private void handleDeleteRoomDrop(Player player) {
        Room focusedRoom = editManager.getFocusedRoom(player);

        if (focusedRoom == null) {
            player.sendMessage("§c[Dungeon] Aucune salle focus! Cassez un bloc d'or pour focus une salle.");
            return;
        }

        // Enregistrer la suppression en attente
        editManager.setPendingRoomDeletion(player, focusedRoom.getName());

        // Message de confirmation avec click event
        Component message = Component.text("§cÊtes-vous sûr de supprimer [" + focusedRoom.getName() + "] ? ")
                .append(Component.text("[CONFIRMER]")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/dungeon confirmroom")));

        player.sendMessage(message);

    }



    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Désactiver le mode Edit à la déconnexion
        if (editManager.isInEditMode(player)) {
            player.getInventory().clear();
            editManager.toggleEditMode(player);
        }
    }
}
