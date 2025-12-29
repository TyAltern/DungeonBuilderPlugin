package me.TyAlternative.dungeonBuilderPlugin;

import me.TyAlternative.dungeonBuilderPlugin.command.DungeonCommand;
import me.TyAlternative.dungeonBuilderPlugin.listener.EditModeListener;
import me.TyAlternative.dungeonBuilderPlugin.listener.RoomInteractionListener;
import me.TyAlternative.dungeonBuilderPlugin.manager.DungeonManager;
import me.TyAlternative.dungeonBuilderPlugin.manager.EditModeManager;
import me.TyAlternative.dungeonBuilderPlugin.manager.RoomManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class DungeonBuilderPlugin extends JavaPlugin {

    private static DungeonBuilderPlugin instance;
    private DungeonManager dungeonManager;
    private RoomManager roomManager;
    private EditModeManager editModeManager;


    @Override
    public void onEnable() {
        instance = this;


        // Initialisation des managers
        this.roomManager = new RoomManager(this);
        this.dungeonManager = new DungeonManager(this);
        this.editModeManager = new EditModeManager();


        // Chargement des donjons
        dungeonManager.loadDungeons();

        // Chargement des salles pour chaque donjon
        dungeonManager.getAllDungeons().forEach(dungeon ->
                roomManager.loadRooms(dungeon.getName())
        );

        // Enregistrement des listeners
        getServer().getPluginManager().registerEvents(new EditModeListener(this), this);
        getServer().getPluginManager().registerEvents(new RoomInteractionListener(this), this);

        // Enregistrement des commandes
        DungeonCommand dungeonCommand = new DungeonCommand(this);
        getCommand("dungeon").setExecutor(dungeonCommand);
        getCommand("dungeon").setTabCompleter(dungeonCommand);

        getLogger().info("DungeonBuilder activé avec succès!");
    }

    @Override
    public void onDisable() {
        // Sauvegarde des salles pour chaque donjon
        dungeonManager.getAllDungeons().forEach(dungeon ->
                roomManager.saveRooms(dungeon.getName())
        );

        // Nettoyage des visualisations
        editModeManager.clearAllVisualizations();


        getLogger().info("DungeonBuilder désactivé!");
    }


    public static DungeonBuilderPlugin getInstance() {
        return instance;
    }

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public EditModeManager getEditModeManager() {
        return editModeManager;
    }
}
