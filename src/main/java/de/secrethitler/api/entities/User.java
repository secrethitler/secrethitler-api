package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Collin Alpert
 */
@TableName("user")
public class User extends BaseEntity {

	private String channelname;
	private String username;

	public String getChannelname() {
		return channelname;
	}

	public void setChannelname(String channelname) {
		this.channelname = channelname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
