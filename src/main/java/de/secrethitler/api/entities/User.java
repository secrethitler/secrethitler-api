package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Collin Alpert
 */
@TableName("user")
public class User extends BaseEntity {

	private String userName;

	public User(String userName) {
		this.userName = userName;
	}

	public User() {
	}

	public String getUserName() {
		return userName;
	}
}
