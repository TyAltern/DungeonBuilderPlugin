package me.TyAlternative.dungeonBuilderPlugin.util;

import me.TyAlternative.dungeonBuilderPlugin.DungeonBuilderPlugin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class RoomVisualizer {

    private final Player player;
    private final Location corner1;
    private final Location corner2;
    private BukkitTask particleTask;

    public RoomVisualizer(Player player, Location corner1, Location corner2) {
        this.player = player;

        // Normaliser les coins
        double minX = Math.min(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());

        double maxX = Math.max(corner1.getX(), corner2.getX());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        this.corner1 = new Location(corner1.getWorld(), minX, minY, minZ);
        this.corner2 = new Location(corner1.getWorld(), maxX, maxY, maxZ);
    }


    public void show() {
        if (particleTask != null) {
            particleTask.cancel();
        }

        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                drawEdges();
            }
        }.runTaskTimer(DungeonBuilderPlugin.getInstance(), 0L, 5L); // Toutes les 5 ticks (0.25s)
    }

    public void hide() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }

    private void drawEdges() {
        double minX = corner1.getX();
        double minY = corner1.getY();
        double minZ = corner1.getZ();
        double maxX = corner2.getX() + 1;
        double maxY = corner2.getY() + 1;
        double maxZ = corner2.getZ() + 1;

        // Les 12 arêtes du cuboid
        // Arêtes inférieures (y = minY)
        drawLine(minX, minY, minZ, maxX, minY, minZ); // Avant
        drawLine(minX, minY, maxZ, maxX, minY, maxZ); // Arrière
        drawLine(minX, minY, minZ, minX, minY, maxZ); // Gauche
        drawLine(maxX, minY, minZ, maxX, minY, maxZ); // Droite

        // Arêtes supérieures (y = maxY)
        drawLine(minX, maxY, minZ, maxX, maxY, minZ); // Avant
        drawLine(minX, maxY, maxZ, maxX, maxY, maxZ); // Arrière
        drawLine(minX, maxY, minZ, minX, maxY, maxZ); // Gauche
        drawLine(maxX, maxY, minZ, maxX, maxY, maxZ); // Droite

        // Arêtes verticales
        drawLine(minX, minY, minZ, minX, maxY, minZ); // Coin avant-gauche
        drawLine(maxX, minY, minZ, maxX, maxY, minZ); // Coin avant-droit
        drawLine(minX, minY, maxZ, minX, maxY, maxZ); // Coin arrière-gauche
        drawLine(maxX, minY, maxZ, maxX, maxY, maxZ); // Coin arrière-droit
    }

    private void drawLine(double x1, double y1, double z1, double x2, double y2, double z2) {

        double distance = Math.sqrt(
                Math.pow(x2 - x1, 2) +
                Math.pow(y2 - y1, 2) +
                Math.pow(z2 - z1, 2)
        );

        double stepSize = 0.25;
        int steps = (int) (distance / stepSize);

        if (steps == 0) steps = 1;

        double dx = (x2 - x1) / steps;
        double dy = (y2 - y1) / steps;
        double dz = (z2 - z1) / steps;

        for (int i = 0; i <= steps ; i++) {
            double x = x1 + (dx * i);
            double y = y1 + (dy * i);
            double z = z1 + (dz * i);

            Location particleLoc = new Location(corner1.getWorld(), x, y, z);

            player.spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    1,
                    0, 0, 0,
                    0,
                    new Particle.DustOptions(Color.WHITE, 1.5f)
            );
        }


    }

}
