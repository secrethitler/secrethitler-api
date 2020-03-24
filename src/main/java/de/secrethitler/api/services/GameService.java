package de.secrethitler.api.services;

import com.github.collinalpert.java2db.services.AsyncBaseService;
import de.secrethitler.api.entities.Game;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Collin Alpert
 */
@Service
public class GameService extends AsyncBaseService<Game> {

	public long getCreatorIdByChannelName(String channelName) {
		return getSingle(x -> x.getChannelName() == channelName).project(Game::getCreatorId).first().orElse(0L);
	}

	public Optional<Long> getIdByChannelName(String channelName) {
		return getSingle(x -> x.getChannelName() == channelName).project(Game::getId).first();
	}

	public Optional<Game> getByChannelName(String channelName) {
		return getSingle(x -> x.getChannelName() == channelName).first();
	}
}
