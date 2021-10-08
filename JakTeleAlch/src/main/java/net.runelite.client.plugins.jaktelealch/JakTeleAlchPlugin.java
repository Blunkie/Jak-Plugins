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
		name = "Jak Tele Alch",
		enabledByDefault = false,
		description = "Automatically tele alchs",
		tags = {"jak", "auto", "bot", "cat", "rat", "spice"}
)
@Slf4j
public class JakTeleAlchPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	protected Chatbox chatbox;

	@Inject
	private JakTeleAlchConfig config;

	@Inject
	private JakTeleAlchOverlay overlay;

	@Inject
	private iUtils utils;

	@Inject
	private KeyboardUtils keyboard;

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

	int timeout = 0;
	int tickLength;
	long sleepLength;
	boolean startTeleAlch;

	Instant botTimer;
	MenuEntry teleport;
	MenuEntry alch;
	WidgetInfo spellInfo;

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
		overlayManager.remove(overlay);
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


/*	@Subscribe
	private void onAnimationChanged(AnimationChanged event) {
		if(!startTeleAlch) {
			return;
		}
		if (event.getActor() != client.getLocalPlayer()) {
			return;
		}
		if (event.getActor().getGraphic() == 713) { //Checks for alch animation before teleporting
			utils.oneClickCastSpell(spellInfo, teleport, teleSleepDelay());

		}
	}*/

	@Subscribe
	private void onGraphicChanged(GraphicChanged event) {
		if(!startTeleAlch) {
			return;
		}
		if (event.getActor() != client.getLocalPlayer()) {
			return;
		}
		if (event.getActor().getGraphic() == 113) { //Checks for alch animation before teleporting
			utils.oneClickCastSpell(spellInfo, teleport, sleepDelay());
		}
	}

	@Subscribe
	private void onGameTick(GameTick event) {

		if (!startTeleAlch) {
			return;
		}

		if (!inventory.containsItem(ItemID.FIRE_RUNE) || !inventory.containsItem(ItemID.NATURE_RUNE) || !inventory.containsItem(config.alchItemID())) {
			notifyShutdown();
		}

		if (timeout > 0) {
			timeout--;
		}

		if (timeout == 0 && client.getLocalPlayer().getAnimation() == -1) { //Checks for idle animation before alching.
			castHighAlch();
			timeout = 5;
		}
	}

	private void castHighAlch() {
		WidgetItem alchable = inventory.getWidgetItem(config.alchItemID());
		alch = new MenuEntry("Cast", "", alchable.getId(), MenuAction.ITEM_USE_ON_WIDGET.getId(), alchable.getIndex(), 9764864, true);
		utils.oneClickCastSpell(WidgetInfo.SPELL_HIGH_LEVEL_ALCHEMY, alch, alchable.getCanvasBounds().getBounds(), sleepDelay());
	 }

	private void getCorrectTeleport() {
		switch (config.type()) {
			case VARROCK: teleport = new MenuEntry("Cast", "<col=00ff00>Varrock Teleport</col>", 1, MenuAction.CC_OP.getId(), -1, 14286868, false);
				spellInfo = WidgetInfo.SPELL_VARROCK_TELEPORT;
				break;
			case LUMBRIDGE: teleport = new MenuEntry("Cast", "<col=00ff00>Lumbridge Teleport</col>", 1, MenuAction.CC_OP.getId(), -1, 14286871, false);
				spellInfo = WidgetInfo.SPELL_LUMBRIDGE_TELEPORT;
				break;
			case FALADOR: teleport = new MenuEntry("Cast", "<col=00ff00>Falador Teleport</col>", 1, MenuAction.CC_OP.getId(), -1, 14286874, false);
				spellInfo = WidgetInfo.SPELL_FALADOR_TELEPORT;
				break;
			case CAMELOT: teleport = new MenuEntry("Seers'", "<col=00ff00>Camelot Teleport</col>", 2, MenuAction.CC_OP.getId(), -1, 14286879, false);
				spellInfo = WidgetInfo.SPELL_CAMELOT_TELEPORT;
				break;
			case ARDOUGNE: teleport = new MenuEntry("Cast", "<col=00ff00>Ardougne Teleport</col>", 1, MenuAction.CC_OP.getId(), -1, 14286885, false);
				spellInfo = WidgetInfo.SPELL_ARDOUGNE_TELEPORT;
				break;
			case WATCHTOWER: teleport = new MenuEntry("Cast", "<col=00ff00>Watchtower Teleport</col>", 1, MenuAction.CC_OP.getId(), -1, 14286890, false);
				spellInfo = WidgetInfo.SPELL_WATCHTOWER_TELEPORT;
				break;
			case TROLLHEIM: teleport = new MenuEntry("Cast", "<col=00ff00>Trollheim Teleport</col>", 1, MenuAction.CC_OP.getId(), -1, 14286897, false);
				spellInfo = WidgetInfo.SPELL_TROLLHEIM_TELEPORT;
				break;
			case KOUREND: teleport = new MenuEntry("Cast", "<col=00ff00>Kourend Castle Teleport</col>", 1, MenuAction.CC_OP.getId(), -1, 14286905, false);
				spellInfo = WidgetInfo.SPELL_TELEPORT_TO_KOUREND;
				break;
		}
	}

	private void notifyShutdown() {
		notifier.notify("Out of Runes or Alchables");
		this.shutDown();
	}

}