package net.runelite.client.plugins.jakspicecollector;

import javax.annotation.Nullable;

public enum JakSpiceCollectorCatType {
	WILY("Wily cat"),
	NORMAL("Cat");


	private final String name;

	JakSpiceCollectorCatType(String name)
	{
		this.name = name;
	}

	@Nullable
	String getName()
	{
		return this.name;
	}
}