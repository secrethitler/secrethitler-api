package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Collin Alpert
 */
@TableName("role")
public class Role extends BaseEntity {

	private String name;

	public Role(String name) {
		this.name = name;
	}

	public Role() {
	}

	public String getName() {
		return name;
	}
}
