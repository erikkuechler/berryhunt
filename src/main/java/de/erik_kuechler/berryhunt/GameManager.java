package de.erik_kuechler.berryhunt;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.logging.Logger;

public class GameManager {

    private JavaPlugin plugin;
    private GameStartCountdownTask gameStartCountdownTask;
    private PlayerManager playerManager;

    public int duration = 0;
    public GameState gameState = GameState.EMPTY;

    /**
     * Initializes a new instance of the GameManager class.
     * @param plugin The main JavaPlugin instance.
     */
    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Sets the game state to the specified state.
     * @param gameState The new GameState to set.
     */
    public void setGameState(GameState gameState) {
        if (this.gameState == gameState) return;

        int x1 = plugin.getConfig().getInt("pos1.x");
        int z1 = plugin.getConfig().getInt("pos1.z");
        World world1 = Bukkit.getWorld(plugin.getConfig().getString("pos1.world"));
        Location location1 = new Location(world1, x1, 0, z1);
        int x2 = plugin.getConfig().getInt("pos2.x");
        int z2 = plugin.getConfig().getInt("pos2.z");
        World world2 = Bukkit.getWorld(plugin.getConfig().getString("pos2.world"));
        Location location2 = new Location(world2, x2, 0, z2);
        BerryBushManager bushManager = new BerryBushManager(location1, location2);
        double density = plugin.getConfig().getDouble("settings.density");

        this.gameState = gameState;
        switch(gameState) {
            case EMPTY:
                Logger.getLogger("berryhunt").info("The game is empty.");
                break;
            case STARTING:
                Logger.getLogger("berryhunt").info("The game is starting.");
                this.gameStartCountdownTask = new GameStartCountdownTask(this, playerManager, plugin.getConfig().getInt("settings.countdown"));
                this.gameStartCountdownTask.runTaskTimer(plugin, 0, 20);
                break;
            case ACTIVE:
                if (this.gameStartCountdownTask != null) this.gameStartCountdownTask.cancel();
                Logger.getLogger("berryhunt").info("The game is active.");
                // Game
                bushManager.placeSweetBerryBushes(density);

                duration = plugin.getConfig().getInt("settings.duration");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (duration == 0) {
                            // Here the command is executed
                            setGameState(GameState.WON);
                            cancel();
                            return;
                        }
                        duration--;
                        playerManager.showScoreboardToPlayers(duration);
                    }
                }.runTaskTimer(plugin, 0, 20);

                // Game end
                break;
            case WON:
                Logger.getLogger("berryhunt").info("The game has ended.");
                bushManager.removeSweetBerryBushes();
                playerManager.evaluation();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Set berry count to 0 for all players
                        playerManager.resetBeerenstand();
                        // What's next?
                        if (playerManager.getJoinedPlayers().isEmpty()) {
                            setGameState(GameState.EMPTY);
                        } else {
                            // tp all spawn
                            playerManager.teleportAllToSpawn();
                            // It's starting again!
                            setGameState(GameState.STARTING);
                        }
                    }
                }.runTaskLater(plugin, 5 * 20);
                break;
        }
    }

    /**
     * Sets the PlayerManager for managing players in the game.
     * @param playerManager
     */
    public void setPlayerManager(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

}
