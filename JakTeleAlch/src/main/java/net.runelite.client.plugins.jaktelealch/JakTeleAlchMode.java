package net.runelite.client.plugins.jaktelealch;

import javax.annotation.Nullable;

public enum JakTeleAlchMode {
	TELEPORT("Teleport"),
	TELEPORT_ALCH("Tele_Alch"),
	SPLASH_ALCH("Splash_Alch"),
	STUN_ALCH("Stun_Alch");

	private final String name;

	JakTeleAlchMode(String name) { this.name = name; }

	@Nullable
	String getName()
	{
		return this.name;
	}
}