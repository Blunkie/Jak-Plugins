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
package net.runelite.client.plugins.jakspicecollector;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.queries.GameObjectQuery;
import net.runelite.api.queries.NPCQuery;
import net.runelite.api.queries.WallObjectQuery;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.config.PluginSearch;
import net.runelite.client.plugins.crowdsourcing.dialogue.DialogueOptionsData;
import net.runelite.client.plugins.iutils.*;
import net.runelite.client.plugins.iutils.ui.Chatbox;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;

import static java.awt.event.KeyEvent.*;


@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
		name = "Jak Spice Collector",
		enabledByDefault = false,
		description = "Automatically fights behemoth hell rats for spice, uses raw karambwanji for heal",
		tags = {"jak", "auto", "bot", "cat", "rat", "spice"}
)
@Slf4j
public class JakSpiceCollectorPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private JakSpiceCollectorConfig config;

	@Inject
	private iUtils utils;

	@Inject
	private NPCUtils npc;

	@Inject
	private KeyboardUtils keyboard;

	@Inject
	private PlayerUtils player;

	@Inject
	protected Chatbox chatbox;

	@Inject
	private InventoryUtils inventory;

	@Inject
	private CalculationUtils calc;

	@Inject
	private MenuUtils menu;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private JakSpiceCollectorOverlay overlay;

	@Inject
	private Notifier notifier;

	@Inject
	private PluginManager pluginManager;

	@Getter(AccessLevel.PACKAGE)
	private StringBuilder status = new StringBuilder();

	private Rectangle bounds;
	private int opportunityEatHP;
	int timeout = 0;
	int tickLength;
	int nextHealHp;
	long sleepLength;
	boolean startSpice;

	JakSpiceCollectorPlugin plugin;
	Instant botTimer;
	WorldPoint huntingLocation;
	NPC targetNPC;
	MenuEntry targetMenu;

	@Provides
	JakSpiceCollectorConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(JakSpiceCollectorConfig.class);
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
		startSpice = false;
		overlayManager.remove(overlay);
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
		if (!configButtonClicked.getGroup().equalsIgnoreCase("JakSpiceCollector")) {
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startSpice")) {
			if (!startSpice && inBasement()) {
				startSpice = true;
				targetMenu = null;
				botTimer = Instant.now();
				overlayManager.add(overlay);
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
		log.debug("tick delay for {} ticks", tickLength);
		return tickLength;
	}

	@Subscribe
	private void onGameTick(GameTick event)
	{
		NPC behemothRat = new NPCQuery().idEquals(NpcID.HELLRAT_BEHEMOTH).result(client).nearestTo(client.getLocalPlayer());
		NPC basementRats = new NPCQuery().idEquals(NpcID.HELLRAT).result(client).nearestTo(client.getLocalPlayer());
		NPC cat = new NPCQuery().nameContains("cat").result(client).nearestTo(client.getLocalPlayer());
		WallObject curtain = new WallObjectQuery().idEquals(539).result(client).nearestTo(client.getLocalPlayer());
		Widget insertCat = client.getWidget(14352385);
		Widget clickContinue = client.getWidget(12648449); //Widget for click here to continue to drop/insert cat
		Widget clickContinue1 = client.getWidget(15007746); //Widget for click here to continue to start fight

		if (!startSpice) {
			return;
		}

		if (timeout > 0)
		{
			timeout--;
		}

		if (timeout == 0) {
			if (basementRats != null) { //hell rats in basement are not present in combat instance

				//shutdown plugin and picks up cat when only 1 open inventory spot left.
				if (inventory.getEmptySlots() == 1) {
					if (cat != null) {
						utils.sendGameMessage("CAT2");
						retrieveCat();
						timeout = 2 + tickDelay();
					}
					notifyShutdown();
					timeout = 2;
					return;
				}

				//Interacts with the curtain and performs action to start fight.
				updateStatus("Starting Fight");
				if (timeout == 0 && clickContinue == null && insertCat == null) {
					MenuEntry enterCurtain = new MenuEntry("Enter", "", //Target option and Target value
							curtain.getId(), //Target ID
							MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), //opcode for interaction
							curtain.getLocalLocation().getSceneX(), curtain.getLocalLocation().getSceneY(), false); //location
					utils.doInvokeMsTime(enterCurtain, sleepLength);
				}
				if (clickContinue != null) {
					keyboard.pressKey(VK_SPACE); //uses spacebar to continue dialogue
					timeout = tickDelay();
				}
				if (insertCat != null) {
					log.info("Cat");
					MenuEntry insert = new MenuEntry("Continue", "", 0, MenuAction.WIDGET_TYPE_6.getId(), 1, 14352385, false);
					utils.doInvokeMsTime(insert, sleepLength);
					timeout = 2 + tickDelay();
				}
			}

			if (basementRats == null) {
				updateStatus("In Combat");
				if (cat.getHealthRatio() != -1 && calculateHealth(cat) <= nextHealHp && timeout == 0) {
					updateStatus("Healing Cat");
					useKaramOnCurtain();
					getNextHealHP();
					timeout = 2 + tickDelay();
					return;
				} else
				if (clickContinue1 != null) {
					updateStatus("In Combat");
					getNextHealHP();
					keyboard.pressKey(VK_SPACE); //uses spacebar to continue dialogue
					timeout = 1 + tickDelay();
				}
			}
		}
	}


	private boolean inBasement()
	{
		return ArrayUtils.contains(client.getMapRegions(), 12442); //checks region ID of player and compares to basement region ID
	}

	private void notifyShutdown() {
		notifier.notify("Full Inventory - Spice Collector Shutdown");
		this.shutDown();
	}

	private int calculateHealth(NPC target)
	{
		// Based on OpponentInfoOverlay HP calculation & taken from the default slayer plugin
		if (target == null || target.getName() == null)
		{
			return -1;
		}

		final int healthScale = target.getHealthScale();
		final int healthRatio = target.getHealthRatio();
		final Integer maxHealth = 6;

		if (healthRatio < 0 || healthScale <= 0 || maxHealth == null)
		{
			return -1;
		}

		return (int)((maxHealth * healthRatio / healthScale) + 0.5f);
	}

	private void retrieveCat() {
		NPC cat = new NPCQuery().nameContains("cat").result(client).nearestTo(client.getLocalPlayer());
		MenuEntry entry = new MenuEntry("Pick-up", "", //Target option and Target value
				cat.getIndex(), //Target Index
				MenuAction.NPC_FIRST_OPTION.getId(), //opcode for interaction
				0, 0, false); //location
		utils.doInvokeMsTime(entry, sleepDelay());
		//  utils.doModifiedInvokeMsTime(entry, cat.getId(), cat.getIndex(), MenuAction.NPC_FIRST_OPTION.getId(), sleepLength);
	}

	private void useKaramOnCurtain() {
		WallObject curtain = new WallObjectQuery().idEquals(539).result(client).nearestTo(client.getLocalPlayer());
		WidgetItem karambwjani = inventory.getWidgetItem(ItemID.RAW_KARAMBWANJI); //Gets raw karambwanji
		MenuEntry entry = new MenuEntry("", "", //Target option and Target value
				curtain.getId(), //Target ID
				MenuAction.ITEM_USE_ON_GAME_OBJECT.getId(), //opcode for interaction
				curtain.getLocalLocation().getSceneX(), curtain.getLocalLocation().getSceneY(), false); //location

		utils.doModifiedInvokeMsTime(entry, karambwjani.getId(), karambwjani.getIndex(),
				MenuAction.ITEM_USE_ON_GAME_OBJECT.getId(), sleepDelay());
	}

	private void getNextHealHP() {
		nextHealHp = calc.getRandomIntBetweenRange(3, 5);
	}

	public void updateStatus(String newStatus){
		int statusTotalCharCount = status.length();
		status.replace(0, statusTotalCharCount, newStatus);
	}
}