package me.TyAlternative.dungeonBuilderPlugin.command;

import me.TyAlternative.dungeonBuilderPlugin.DungeonBuilderPlugin;
import me.TyAlternative.dungeonBuilderPlugin.manager.RoomManager;
import me.TyAlternative.dungeonBuilderPlugin.manager.DungeonManager;
import me.TyAlternative.dungeonBuilderPlugin.model.Dungeon;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.maven.model.Plugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DungeonCommand implements CommandExecutor, TabCompleter {

    private final DungeonBuilderPlugin plugin;
    private final DungeonManager dungeonManager;
    private final RoomManager roomManager;
    private final Map<UUID, String> pendingDeletions;

    public DungeonCommand(DungeonBuilderPlugin plugin) {
        this.plugin = plugin;
        this.dungeonManager = plugin.getDungeonManager();
        this.roomManager = plugin.getRoomManager();
        this.pendingDeletions = new HashMap<>();
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof  Player player)) {
            sender.sendMessage("§cCette commande ne peut être exécutée que par un joueur!");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage("§cVous devez être OP pour utiliser cette commande!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "delete" -> handleDelete(player, args);
            case "confirm" -> handleConfirm(player);
            case "confirmroom" -> handleConfirmRoom(player);
            case "tp" -> handleTeleport(player, args);
            case "list" -> handleList(player);
            case "info" -> handleInfo(player, args);
            default -> {
                sendHelp(player);
                yield true;
            }
        };



    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /dungeon create <nom>");
            return true;
        }

        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (dungeonManager.getDungeon(name) != null) {
            player.sendMessage("§c[Dungeon] Un donjon avec ce nom existe déjà!");
            return true;
        }

        player.sendMessage("§e[Dungeon] Création du donjon en cours...");

        Dungeon dungeon = dungeonManager.createDungeon(name);

        if (dungeon == null) {
            player.sendMessage("§c[Dungeon] Erreur lors de la création du donjon!");
            return true;
        }

        player.sendMessage("§a[Dungeon] Donjon §f" + name + " §acréé avec succès!");
        player.sendMessage("§7Utilisez §f/dungeon tp " + name + " §7pour vous y téléporter");

        return true;
    }


    private boolean handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /dungeon delete <nom>");
            return true;
        }

        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Dungeon dungeon = dungeonManager.getDungeon(name);
        if (dungeon == null) {
            player.sendMessage("§c[Dungeon] Donjon introuvable!");
            return true;
        }

        // Enregistrer la suppression en attente
        pendingDeletions.put(player.getUniqueId(), name);

        // Message de confirmation avec click event
        Component message = Component.text("§cÊtes-vous sûr de supprimer [" + name + "] ? ")
                .append(Component.text("[CONFIRMER]")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/dungeon confirm")));

        player.sendMessage(message);

        return true;
    }

    private boolean handleConfirm(Player player) {
        String dungeonName = pendingDeletions.remove(player.getUniqueId());

        if (dungeonName == null) {
            player.sendMessage("§c[Dungeon] Aucune suppression en attente!");
            return true;
        }


        Dungeon dungeon = dungeonManager.getDungeon(dungeonName);
        if (dungeon == null) {
            player.sendMessage("§c[Dungeon] Donjon introuvable!");
            return true;
        }

        player.sendMessage("§e[Dungeon] Suppression du donjon en cours...");

        // Supprimer les salles du donjon
        roomManager.deleteDungeonRooms(dungeonName);

        // Supprimer le donjon
        boolean success = dungeonManager.deleteDungeon(dungeonName);

        if (success) {
            player.sendMessage("§a[Dungeon] Donjon §f" + dungeonName + " §asupprimé avec succès!");
        } else {
            player.sendMessage("§c[Dungeon] Erreur lors de la suppression du donjon!");
        }

        return true;
    }

    private boolean handleConfirmRoom(Player player) {
        String roomName = plugin.getEditModeManager().getPendingRoomDeletion(player);

        if (roomName == null) {
            player.sendMessage("§c[Dungeon] Aucune suppression de salle en attente!");
            return true;
        }

        // Trouver le donjon du monde actuel
        World world = player.getWorld();
        String dungeonName = roomManager.getDungeonNameByWorld(world);

        if (dungeonName == null) {
            player.sendMessage("§c[Dungeon] Monde de donjon introuvable!");
            return true;
        }

        // Supprimer la salle
        boolean success = roomManager.deleteRoom(dungeonName, roomName);

        if (success) {
            player.sendMessage("§a[Dungeon] Salle §f" + roomName + " §asupprimée avec succès!");

            // Clear le focus si c'était la salle focus
            plugin.getEditModeManager().clearFocusedRoom(player);
        } else {
            player.sendMessage("§c[Dungeon] Erreur lors de la suppression de la salle!");
        }

        return true;
    }


    private boolean handleTeleport(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /dungeon tp <nom>");
            return true;
        }

        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Dungeon dungeon = dungeonManager.getDungeon(name);
        if (dungeon == null) {
            player.sendMessage("§c[Dungeon] Donjon introuvable!");
            return true;
        }

        World world = dungeon.getWorld();
        if (world == null) {
            player.sendMessage("§c[Dungeon] Monde du donjon introuvable!");
            return true;
        }

        Location spawn = new Location(world, 0.5, 101, 0.5);
        spawn.setYaw(0);
        spawn.setPitch(0);

        player.teleport(spawn);
        player.sendMessage("§a[Dungeon] Téléportation vers §f" + name);

        return true;


    }


    private boolean handleList(Player player) {
        Collection<Dungeon> dungeons = dungeonManager.getAllDungeons();

        if (dungeons.isEmpty()) {
            player.sendMessage("§e[Dungeon] Aucun donjon créé");
            return true;
        }

        player.sendMessage("§6§l========== Liste des Donjons ==========");
        for (Dungeon dungeon : dungeons) {
            int roomCount = roomManager.getRoomCount(dungeon.getName());
            player.sendMessage("§e • §f" + dungeon.getName() + " §7(" + roomCount + " salle" + (roomCount <= 1? "": "s") + ")");
        }
        player.sendMessage("§6§l======================================");

        return true;
    }


    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /dungeon info <nom>");
            return true;
        }

        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Dungeon dungeon = dungeonManager.getDungeon(name);
        if (dungeon == null) {
            player.sendMessage("§c[Dungeon] Donjon introuvable!");
            return true;
        }

        int roomCount = roomManager.getRoomCount(dungeon.getName());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String creationDate = dateFormat.format(new Date(dungeon.getCreationDate()));

        player.sendMessage("§6§l========== Informations Donjon ==========");
        player.sendMessage("§e Nom: §f" + dungeon.getName());
        player.sendMessage("§e Monde: §f" + dungeon.getWorldName());
        player.sendMessage("§e Salles: §f" + roomCount);
        player.sendMessage("§e Création: §f" + creationDate);
        player.sendMessage("§6§l========================================");

        return true;
    }


    private void sendHelp(Player player) {
        player.sendMessage("§6§l========== Commandes Donjon ==========");
        player.sendMessage("§e/dungeon create <nom> §7- Créer un donjon");
        player.sendMessage("§e/dungeon delete <nom> §7- Supprimer un donjon");
        player.sendMessage("§e/dungeon tp <nom> §7- Se téléporter à un donjon");
        player.sendMessage("§e/dungeon list §7- Liste des donjons");
        player.sendMessage("§e/dungeon info <nom> §7- Infos sur un donjon");
        player.sendMessage("§6§l======================================");
    }


    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {

        if (args.length == 1) {
            return Stream.of("create", "delete","tp","list","info")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("info") )) {
            return dungeonManager.getAllDungeons().stream()
                    .map(Dungeon::getName)
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
