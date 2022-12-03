package fr.efreicraft.ludos.core;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import fr.efreicraft.ludos.core.games.GameManager;
import fr.efreicraft.ludos.core.players.Player;
import fr.efreicraft.ludos.core.utils.ActionBarUtils;
import fr.efreicraft.ludos.core.utils.NBTUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.UUID;

/**
 * Evenements de Core.
 *
 * @author Antoine B. {@literal <antoine@jiveoff.fr>}
 */
public class EventListener implements Listener {

    /**
     * Evenement de login d'un joueur.
     */
    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if(Core.get().getGameManager().getStatus() == GameManager.GameStatus.WAITING
                && Core.get().getGameManager().getCurrentGame() != null
                && Core.get().getPlayerManager().getNumberOfPlayingPlayers() >= Core.get().getGameManager().getCurrentGame().getMetadata().rules().maxPlayers()) {
            event.disallow(
                    PlayerLoginEvent.Result.KICK_OTHER,
                    Component.text("La partie est déjà pleine !")
            );
        }
    }

    /**
     * Evenement de connexion d'un joueur.
     * @param event Evenement Bukkit
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSpawn(PlayerSpawnLocationEvent event) {
        if(Core.get().getGameManager().getStatus() == GameManager.GameStatus.WAITING) {
            event.setSpawnLocation(Core.get().getMapManager().getLobbyWorld().getSpawnLocation().add(-0.5, 0, -0.5));
        } else {
            event.setSpawnLocation(Core.get().getMapManager().getCurrentMap().getMiddleOfMap());
        }
    }

    /**
     * Evenement de spawn d'un joueur sur le serveur.
     * @param event Evenement Bukkit
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.joinMessage(null);
        Core.get().getPlayerManager().addPlayer(new Player(event.getPlayer()));
    }

    /**
     * Evenement de deconnexion d'un joueur du serveur.
     * @param event Evenement Bukkit
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
        Core.get().getPlayerManager().removePlayer(event.getPlayer());
    }

    /**
     * Vérifie si le joueur a changé de Block X & Z, pour laisser le saut disponible.
     * @param event Evenement {@link PlayerMoveEvent} Bukkit.
     * @return Vrai si le joueur a changé de Block X & Z, faux sinon.
     */
    private boolean hasMovedInBlockXAndBlockZ(@NotNull PlayerMoveEvent event) {
        return event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
    }

    /**
     * Evenement de deplacement d'un joueur.
     * @param event Evenement Bukkit
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if(
                hasMovedInBlockXAndBlockZ(event)
        ) {
            Player player = Core.get().getPlayerManager().getPlayer(event.getPlayer());
            if(player.getTeam().isPlayingTeam()) {
                if(Core.get().getGameManager().getStatus() == GameManager.GameStatus.STARTING) {
                    event.setCancelled(true);
                } else if(Core.get().getGameManager().getStatus() == GameManager.GameStatus.INGAME
                        && !Core.get().getMapManager().getCurrentMap().isLocationWithinTheMapXandZ(event.getTo())) {
                    event.setCancelled(true);
                    ActionBarUtils.sendActionBar(player, "&cVous ne pouvez pas sortir de la map !");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if(event.getEntity() instanceof org.bukkit.entity.Player
                && Core.get().getGameManager().getStatus() != GameManager.GameStatus.INGAME) {
            event.setCancelled(true);
            if(event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                event.getEntity().teleport(Core.get().getMapManager().getLobbyWorld().getSpawnLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = Core.get().getPlayerManager().getPlayer(event.getPlayer());
        if(player != null) {
            player.deathEvent(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = Core.get().getPlayerManager().getPlayer(event.getPlayer());
        if(player != null) {
            player.respawnEvent(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPostRespawn(PlayerPostRespawnEvent event) {
        Player player = Core.get().getPlayerManager().getPlayer(event.getPlayer());
        if(player != null) {
            player.postRespawnEvent();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        if(Core.get().getGameManager().getStatus() != GameManager.GameStatus.INGAME) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPortalEvent(PlayerPortalEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if(Core.get().getGameManager().getStatus() == GameManager.GameStatus.WAITING){
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = Core.get().getPlayerManager().getPlayer((org.bukkit.entity.Player) event.getWhoClicked());
        if(player != null && event.getCurrentItem() != null) {
            String nbtValue = NBTUtils.getNBT(event.getCurrentItem(), "menu_item_uuid");
            if(nbtValue != null) {
                event.setCancelled(true);
                player.getPlayerMenus().getMenuItemFromUUID(UUID.fromString(nbtValue)).getCallback().run(event);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = Core.get().getPlayerManager().getPlayer(event.getPlayer());
        if(player != null
                && event.getItem() != null
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            String nbtValue = NBTUtils.getNBT(event.getItem(), "menu_item_uuid");
            if(nbtValue != null) {
                event.setCancelled(true);
                player.getPlayerMenus().getMenuItemFromUUID(UUID.fromString(nbtValue)).getCallback().run(event);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItemEvent(PlayerDropItemEvent event) {
        if(Core.get().getGameManager().getStatus() != GameManager.GameStatus.INGAME){
            event.setCancelled(true);
        }
    }

}
