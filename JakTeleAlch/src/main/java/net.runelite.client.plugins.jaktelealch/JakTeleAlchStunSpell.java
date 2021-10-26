package net.runelite.client.plugins.jaktelealch;

import javax.annotation.Nullable;

public enum JakTeleAlchStunSpell {
	CONFUSE("Confuse"),
	WEAKEN("Weaken"),
	CURSE("Curse"),
	VULNERABILITY("Vulnerability"),
	ENFEEBLE("Enfeeble"),
	STUN("Stun");

	private final String name;

	JakTeleAlchStunSpell(String name) { this.name = name; }

	@Nullable
	String getName()
	{
		return this.name;
	}
}