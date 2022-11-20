package fr.efreicraft.ludos.core.players.scoreboards;

import fr.efreicraft.ludos.core.Core;
import fr.efreicraft.ludos.core.players.Player;
import fr.efreicraft.ludos.core.utils.StringAnimationUtils;
import fr.efreicraft.ludos.core.utils.fastboard.FastBoard;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Gestionnaire de scoreboard propre au joueur.<br /><br />
 *
 * Cette classe ne permet pas une entière customisation du scoreboard pour ainsi standardiser les affichages à travers les jeux.
 *
 * @author Antoine B. {@literal <antoine@jiveoff.fr>}
 * @project EFREI-Minigames
 */
public class PlayerScoreboard {

    /**
     * Joueur auquel est lié le scoreboard.
     */
    private final Player player;

    /**
     * Instance {@link FastBoard} pour le contrôle du scoreboard.
     */
    private FastBoard board;

    /**
     * Tâche de mise à jour du scoreboard.
     */
    private final BukkitTask updateTask;

    /**
     * Titre du scoreboard.
     */
    private TextComponent title;

    /**
     * Map des fields du scoreboard.
     */
    private final Map<Integer, ScoreboardField> fields;

    /**
     * Animation du footer du scoreboard pour tous les joueurs en même temps.
     */
    private static final List<PlayerScoreboard> scoreboardsToAnimate = new ArrayList<>();

    /**
     * Indice dans l'animation du footer du scoreboard.
     */
    private static int animationIndex = 0;

    /**
     * String de l'animation du footer du scoreboard pour les updates par joueur.
     */
    private static String currentAnimationString = "";

    static {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                Core.get().getPlugin(),
                PlayerScoreboard::animateFooter,
                0,
                2
        );
    }

    /**
     * Constructeur du scoreboard. Celui-ci déclare un timer asynchrone pour la mise à jour tous les 2 ticks serveur.
     * @param player Joueur auquel est lié le scoreboard.
     */
    public PlayerScoreboard(Player player) {
        this.player = player;
        this.setVisibility(true);
        this.fields = new TreeMap<>();
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                Core.get().getPlugin(),
                this::updateScoreboard,
                0,
                10
        );
        scoreboardsToAnimate.add(this);
    }

    /**
     * Détruit le scoreboard et arrête la tâche de mise à jour.
     */
    public void unload() {
        this.updateTask.cancel();
        try {
            this.board.delete();
        } catch (RuntimeException ignored) {
            // Le scoreboard a été détruit si le joueur est déjà déconnecté.
        }
        scoreboardsToAnimate.remove(this);
    }

    /**
     * Met à jour le scoreboard pour le joueur.
     */
    private void updateScoreboard() {
        if(this.board.isDeleted()) return;
        this.board.updateTitle(LegacyComponentSerializer.legacySection().serialize(this.title));
        this.board.updateLines(this.getLines());
    }

    /**
     * Anime la dernière ligne du scoreboard.
     */
    private static void animateFooter() {
        if(scoreboardsToAnimate.isEmpty()) return;

        List<String> stringsAnim = StringAnimationUtils.generateColorStrings(
                Core.get().getPlugin().getConfig().getString("serverAddress"),
                ChatColor.GOLD,
                ChatColor.YELLOW,
                ChatColor.WHITE
        );
        if(animationIndex >= stringsAnim.size()) {
            animationIndex = 0;
        }

        currentAnimationString = stringsAnim.get(animationIndex++);

        for(PlayerScoreboard playerScoreboard : scoreboardsToAnimate) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if(playerScoreboard.board.getLines().isEmpty() || playerScoreboard.board.isDeleted()) return;
                    int indexToSet = playerScoreboard.board.getLines().size() - 1;
                    playerScoreboard.board.updateLine(indexToSet, currentAnimationString);
                }
            }.runTaskAsynchronously(Core.get().getPlugin());
        }
    }

    /**
     * Retourne les lignes du scoreboard et les formate pour l'affichage. Une animation est appliquée au footer du scoreboard.
     * @return Collection des lignes du scoreboard.
     */
    private Collection<String> getLines() {
        List<String> fieldsToShow = new ArrayList<>();
        fieldsToShow.add("§r");
        for(ScoreboardField field : this.fields.values()) {
            if(field.isOneLine()) {
                fieldsToShow.add(
                        LegacyComponentSerializer.legacySection().serialize(field.name())
                        + "§r "
                        + LegacyComponentSerializer.legacySection().serialize(field.value())
                );
            } else {
                fieldsToShow.add(LegacyComponentSerializer.legacySection().serialize(field.name().decoration(TextDecoration.BOLD, true)));
                fieldsToShow.add(LegacyComponentSerializer.legacySection().serialize(field.value()));
            }
            fieldsToShow.add("§r");
        }
        try {
            fieldsToShow.add("" + ChatColor.RESET + ChatColor.DARK_GRAY + InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        fieldsToShow.add(currentAnimationString);
        return fieldsToShow;
    }

    /**
     * Définit un champ du scoreboard. Maximum: 5 champs.
     * @param pos Position du champ.
     * @param field Champ à définir.
     * @see ScoreboardField
     */
    public void setField(int pos, ScoreboardField field) {
        this.fields.put(pos, field);
    }

    /**
     * Supprime tous les champs du scoreboard.
     */
    public void clearFields() {
        this.fields.clear();
    }

    /**
     * Supprime un champ à la position donnée du scoreboard.
     * @param pos Position du champ à supprimer.
     */
    public void removeField(int pos) {
        this.fields.remove(pos);
    }

    /**
     * Définit le composant texte de titre du scoreboard.
     * @param title Titre du scoreboard. Si celui-ci est coloré, les couleurs doivent être au format {@link LegacyComponentSerializer#legacyAmpersand()}.
     * @see PlayerScoreboard#setTitle
     */
    public void setTitle(String title) {
        this.title = LegacyComponentSerializer.legacyAmpersand().deserialize(title);
    }

    /**
     * Définit la visibilité du scoreboard. Cette méthode peut créer et détruire l'instance {@link FastBoard}.
     * @param visibility Visibilité du scoreboard.
     */
    public void setVisibility(boolean visibility) {
        if(visibility) {
            this.board = new FastBoard(this.player.entity());
        } else {
            try {
                this.board.delete();
            } catch (RuntimeException ignored) {
                // Le scoreboard a été détruit si le joueur est déjà déconnecté.
            }
        }
    }
}
