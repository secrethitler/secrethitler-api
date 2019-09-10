package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.DefaultIfNull;
import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

import java.time.LocalDateTime;

/**
 * @author Collin Alpert
 */
@TableName("applicationLog")
public class ApplicationLog extends BaseEntity {

	@DefaultIfNull
	private LocalDateTime timeOfException;

	private String exceptionMessage;

	public ApplicationLog(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}

	public LocalDateTime getTimeOfException() {
		return timeOfException;
	}

	public String getExceptionMessage() {
		return exceptionMessage;
	}
}
