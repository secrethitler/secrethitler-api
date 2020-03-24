package de.secrethitler.api.services;

import com.github.collinalpert.java2db.queries.OrderTypes;
import com.github.collinalpert.java2db.services.AsyncBaseService;
import de.secrethitler.api.entities.Round;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Vladislav Denzel
 */
@Service
public class RoundService extends AsyncBaseService<Round> {

	public Optional<Round> getCurrentRound(long gameId) {
		return getMultiple(x -> x.getGameId() == gameId).orderBy(OrderTypes.DESCENDING, Round::getSequenceNumber).limit(1).first();
	}
}
