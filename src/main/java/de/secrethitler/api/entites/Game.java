package de.secrethitler.api.entites;

import com.github.collinalpert.java2db.annotations.ForeignKeyEntity;
import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Collin Alpert
 */
@TableName("game")
public class Game extends BaseEntity {

	private int creatorid;

	@ForeignKeyEntity("creatorid")
	private User creator;
}
