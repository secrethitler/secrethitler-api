package de.secrethitler.api.services;

import com.github.collinalpert.java2db.services.BaseService;
import de.secrethitler.api.entities.Game;

/**
 * @author Collin Alpert
 */
public class GameService extends BaseService<Game> {

	public long getCreatorIdByChannelName(String channelName) {
		return getSingle(x -> x.getChannelName() == channelName).project(Game::getCreatorId).first().orElse(0L);
	}
}
