package fr.efreicraft.ludos.games.rush;

import fr.efreicraft.ludos.core.games.interfaces.Game;
import fr.efreicraft.ludos.core.games.interfaces.GamePlugin;

public class Main extends GamePlugin {

    public Main() {
        super();
    }

    @Override
    protected Class<? extends Game> getGameClass() {
        return LudosGame.class;
    }
}
