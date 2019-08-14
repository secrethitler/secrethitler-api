package de.secrethitler.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Collin Alpert
 */
@ConfigurationProperties(prefix = "pusher")
public class PusherConfiguration {

	private String appId;
	private String appKey;
	private String appSecret;
	private String cluster;

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public String getAppSecret() {
		return appSecret;
	}

	public void setAppSecret(String appSecret) {
		this.appSecret = appSecret;
	}

	public String getCluster() {
		return cluster;
	}

	public void setCluster(String cluster) {
		this.cluster = cluster;
	}
}
