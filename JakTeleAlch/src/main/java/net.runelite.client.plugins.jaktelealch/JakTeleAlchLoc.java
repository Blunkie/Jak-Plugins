package net.runelite.client.plugins.jaktelealch;

import javax.annotation.Nullable;

public enum JakTeleAlchLoc {
	LUMBRIDGE("Lumbridge"),
	VARROCK("Varrock"),
	FALADOR("Falador"),
	CAMELOT("Camelot"),
	ARDOUGNE("Ardougne"),
	WATCHTOWER("Watchtower"),
	TROLLHEIM("Trollheim"),
	KOUREND("Kourend");

	private final String name;

	JakTeleAlchLoc(String name) { this.name = name; }

	@Nullable
	String getName()
	{
		return this.name;
	}
}