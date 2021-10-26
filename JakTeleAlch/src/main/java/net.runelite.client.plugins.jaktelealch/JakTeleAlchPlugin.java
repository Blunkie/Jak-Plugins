/*
 * Copyright (c) 2018, SomeoneWithAnInternetConnection
 * Copyright (c) 2018, oplosthee <https://github.com/oplosthee>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.jaktelealch;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.queries.NPCQuery;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.iutils.CalculationUtils;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.iutils.PlayerUtils;
import net.runelite.client.plugins.iutils.iUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Set;


@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
		name = "Jak Magic Trainer",
		enabledByDefault = false,
		description = "Automatically trains magic using popular methods",
		tags = {"jak", "auto", "magic", "tele", "alch", "stun"}
)
@Slf4j
public class JakTeleAlchPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private JakTeleAlchConfig config;

	@Inject
	private JakTeleAlchOverlay overlay;

	@Inject
	private iUtils utils;

	@Inject
	private PlayerUtils playerUtils;

	@Inject
	private InventoryUtils inventory;

	@Inject
	private CalculationUtils calc;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private Notifier notifier;

	@Getter(AccessLevel.PACKAGE)
	private StringBuilder status = new StringBuilder();

	@Getter(AccessLevel.PACKAGE)
	int gainedMagicEXP;

	private int timeout = 0;
	private int tickLength;
	private int lastXpDropTick;
	int initialMagicEXP;
	long sleepLength;
	boolean startTeleAlch;
	private final Set<Integer> FIRE_RUNE_ITEMS = Set.of(ItemID.FIRE_BATTLESTAFF, ItemID.STAFF_OF_FIRE, ItemID.MYSTIC_FIRE_STAFF, ItemID.SMOKE_BATTLESTAFF, ItemID.MYSTIC_SMOKE_STAFF, ItemID.TOME_OF_FIRE);
	private final Set<Integer> WATER_RUNE_ITEMS = Set.of(ItemID.WATER_BATTLESTAFF, ItemID.STAFF_OF_WATER, ItemID.MYSTIC_WATER_STAFF, ItemID.MUD_BATTLESTAFF, ItemID.MIST_BATTLESTAFF, ItemID.TOME_OF_WATER);
	NPC target;
	Instant botTimer;
	MenuEntry teleport;
	MenuEntry stun;
	MenuEntry alch;
	WidgetInfo teleInfo;
	WidgetInfo stunInfo;

	@Provides
	JakTeleAlchConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(JakTeleAlchConfig.class);
	}

	@Override
	protected void startUp() {

	}

	@Override
	protected void shutDown() {
		resetVals();
	}


	private void resetVals() {
		timeout = 0;
		botTimer = null;
		startTeleAlch = false;
		target = null;
		overlayManager.remove(overlay);
		gainedMagicEXP = 0;
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
		if (!configButtonClicked.getGroup().equalsIgnoreCase("JakTeleAlch")) {
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startTeleAlch")) {
			if (!startTeleAlch) {
				startTeleAlch = true;
				botTimer = Instant.now();
				overlayManager.add(overlay);
				getCorrectTeleport();
				getCorrectStun();
				initialMagicEXP = client.getSkillExperience(Skill.MAGIC);
				if (config.mode().equals(JakTeleAlchMode.SPLASH_ALCH)) {
					target = (NPC) client.getLocalPlayer().getInteracting();
					log.info("Splash target: " + target.getName());
				}
				timeout = 0;
			} else {
				resetVals();
			}
		}
	}

	private long sleepDelay() {
		sleepLength = calc.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
		return calc.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
	}

	private int tickDelay() {
		tickLength = (int) calc.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
		return tickLength;
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		if (!startTeleAlch) {
			return;
		}

		final Skill skill = statChanged.getSkill();
		final int xp = statChanged.getXp();
		int tick = client.getTickCount();
		int currentXP = client.getSkillExperience(Skill.MAGIC);

		if (skill == Skill.MAGIC && currentXP != initialMagicEXP && tick != lastXpDropTick && config.enableUI())
		{
			lastXpDropTick = tick;
			gainedMagicEXP = currentXP - initialMagicEXP;
		}
	}

	@Subscribe
	private void onGraphicChanged(GraphicChanged event) {
		if(!startTeleAlch) {
			return;
		}
		if (event.getActor() != client.getLocalPlayer()) {
			return;
		}
		if (event.getActor().getGraphic() == 113) { //Checks for alch animation before next action
			 if (config.mode().equals(JakTeleAlchMode.TELEPORT_ALCH)) {
			 	utils.oneClickCastSpell(teleInfo, teleport, sleepDelay());
			 }

			 if (config.mode().equals(JakTeleAlchMode.STUN_ALCH)) {
			 	log.info("Casting debuff spell");
			 	NPC npc = new NPCQuery().idEquals(config.NpcID()).result(client).nearestTo(client.getLocalPlayer());
			 	stun = new MenuEntry("Cast", "", npc.getIndex(), MenuAction.SPELL_CAST_ON_NPC.getId(), 0, 0,false);
			 	utils.oneClickCastSpell(stunInfo, stun, npc.getConvexHull().getBounds(), sleepDelay());
			 	timeout = 1 + tickDelay();
			 }
		}
	}

	@Subscribe
	private void onGameTick(GameTick event) {

		if (!startTeleAlch) {
			return;
		}

		if (timeout > 0) {
			timeout--;
		}

		switch (config.mode()) {
			case TELEPORT:
				if (!hasLawRunes()) {
					notifyShutdown();
				} else if (timeout == 0) {
					utils.oneClickCastSpell(teleInfo, teleport, sleepDelay());
					timeout = 5 + tickDelay();
				}
				break;

			case TELEPORT_ALCH:
				if (!hasFireRunes() || !hasNatureRunes() || !hasLawRunes() || !inventory.containsItem(config.alchItemID())) {
					notifyShutdown();
					return;
				}
				// Checks for idle animation before alching... idle animation ensures that player finished previous spellcast.
				if (timeout == 0 && client.getLocalPlayer().getAnimation() == -1) {
					castHighAlch();
					timeout = 5;
				}
				break;

			case STUN_ALCH:
				if (!hasFireRunes() || !hasNatureRunes()) {
					notifyShutdown();
					return;
				}

				if (timeout == 0 && client.getLocalPlayer().getGraphic() != 113) { //ensure high alch is not already being cast
					log.info("Casting High Alch - Stun Alch");
					castHighAlch();
					timeout = tickDelay();
				}
				break;

			case SPLASH_ALCH:
				if (target.getInteracting() != client.getLocalPlayer()) {
					utils.sendGameMessage("Target not interacting with player, possibly crashed - shutting down");
					notifyShutdown();
					return;
				}
				if (timeout == 0 && client.getLocalPlayer().getInteracting() != target) {
					utils.doNpcActionMsTime(target, MenuAction.NPC_SECOND_OPTION.getId(), sleepDelay());
					timeout = 1 + tickDelay();
				}
				//Checks for spell cast animation
				if (client.getLocalPlayer().getAnimation() == 1162) {
					castHighAlch();
					timeout = tickDelay();
				}
				break;
		}
	}

	private void castHighAlch() {
		WidgetItem alchable = inventory.getWidgetItem(config.alchItemID());
		alch = new MenuEntry("Cast", "", alchable.getId(), MenuAction.ITEM_USE_ON_WIDGET.getId(), alchable.getIndex(), 9764864, true);
		utils.oneClickCastSpell(WidgetInfo.SPELL_HIGH_LEVEL_ALCHEMY, alch, alchable.getCanvasBounds().getBounds(), sleepDelay());
	 }

	private void getCorrectTeleport() {
		switch (config.teleport()) {
			case VARROCK: teleport = new MenuEntry("Cast", "<col=00ff00>Varrock Teleport</col>", 1, MenuAction.CC_OP.getId(), -1, 14286868, false);
				teleInfo = WidgetInfo.SPELL_VARROCK_TELEPORT;
				break;
			case LUMBRIDGE: teleport = new MenuEntry("Cast", "<col=00ff00>Lumbridge Teleport</col>", 1, MenuAction.CC_OP.getId(), -1, 14286871, false);
				teleInfo = WidgetInfo.SPELL_LUMBRIDGE_TELEPORT;
				break;
			case FALADOR: teleport = new MenuEntry("Cast", "<col=00ff00>Falador Teleport</col>", 1, MenuAction.CC_OP.getId(), -1, 14286874, false);
				teleInfo = WidgetInfo.SPELL_FALADOR_TELEPORT;
				break;
			case CAMELOT: teleport = new MenuEntry("", "<col=00ff00>Camelot Teleport</col>", 2, MenuAction.CC_OP.getId(), -1, 14286879, false);
				teleInfo = WidgetInfo.SPELL_CAMELOT_TELEPORT;
				break;
			case ARDOUGNE: teleport = new MenuEntry("Cast", "<col=00ff00>Ardougne Teleport</col>", 1, MenuAction.CC_OP.getId(), -1, 14286885, false);
				teleInfo = WidgetInfo.SPELL_ARDOUGNE_TELEPORT;
				break;
			case WATCHTOWER: teleport = new MenuEntry("Cast", "<col=00ff00>Watchtower Teleport</col>", 1, MenuAction.CC_OP.getId(), -1, 14286890, false);
				teleInfo = WidgetInfo.SPELL_WATCHTOWER_TELEPORT;
				break;
			case TROLLHEIM: teleport = new MenuEntry("Cast", "<col=00ff00>Trollheim Teleport</col>", 1, MenuAction.CC_OP.getId(), -1, 14286897, false);
				teleInfo = WidgetInfo.SPELL_TROLLHEIM_TELEPORT;
				break;
			case KOUREND: teleport = new MenuEntry("Cast", "<col=00ff00>Kourend Castle Teleport</col>", 1, MenuAction.CC_OP.getId(), -1, 14286905, false);
				teleInfo = WidgetInfo.SPELL_TELEPORT_TO_KOUREND;
				break;
		}
	}

	private void getCorrectStun() {
		NPC npc = new NPCQuery().idEquals(config.NpcID()).result(client).nearestTo(client.getLocalPlayer());
		switch (config.stun()) {
			case CONFUSE:
				stunInfo = WidgetInfo.SPELL_CONFUSE;
				break;
			case WEAKEN:
				stunInfo = WidgetInfo.SPELL_WEAKEN;
				break;
			case CURSE:
				stunInfo = WidgetInfo.SPELL_CURSE;
				break;
			case VULNERABILITY:
				stunInfo = WidgetInfo.SPELL_VULNERABILITY;
				break;
			case ENFEEBLE:
				stunInfo = WidgetInfo.SPELL_ENFEEBLE;
				break;
			case STUN:
				stunInfo = WidgetInfo.SPELL_STUN;
				break;
		}
	}

	private boolean hasWaterRunes() { return inventory.containsItem(ItemID.WATER_RUNE) || inventory.containsItem(ItemID.MIST_RUNE) || inventory.containsItem(ItemID.MUD_RUNE) || inventory.containsItem(ItemID.STEAM_RUNE)
			|| inventory.runePouchContains(ItemID.WATER_RUNE) || inventory.runePouchContains(ItemID.MIST_RUNE) || inventory.runePouchContains(ItemID.MUD_RUNE) || inventory.runePouchContains(ItemID.STEAM_RUNE)  || playerUtils.isItemEquipped(WATER_RUNE_ITEMS); }
	private boolean hasFireRunes() { return inventory.containsItem(ItemID.FIRE_RUNE) || inventory.containsItem(ItemID.LAVA_RUNE) || inventory.containsItem(ItemID.SMOKE_RUNE)
			|| inventory.runePouchContains(ItemID.FIRE_RUNE) || inventory.runePouchContains(ItemID.LAVA_RUNE) || inventory.runePouchContains(ItemID.SMOKE_RUNE) || playerUtils.isItemEquipped(FIRE_RUNE_ITEMS); }
	private boolean hasLawRunes() { return inventory.containsItem(ItemID.LAW_RUNE) || inventory.runePouchContains(ItemID.LAW_RUNE); }
	private boolean hasNatureRunes() { return inventory.containsItem(ItemID.NATURE_RUNE) || inventory.runePouchContains(ItemID.NATURE_RUNE); }

	private void notifyShutdown() {
		notifier.notify("Missing required items - shutting down");
		utils.sendGameMessage("Missing required items - shutting down");
		this.shutDown();
	}

}