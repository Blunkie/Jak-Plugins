package net.runelite.client.plugins.JakLootHop;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.iutils.iUtils;
import net.runelite.client.plugins.JakLootHop.JakLootHopConfig;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
	name = "OliDropper",
	description = "Automatically drops specified items when inventory is full.",
	tags = {"oli", "oiuyo", "drop", "dropper", "auto", "skilling"}
)
@Slf4j
public class JakLootHopPlugin extends Plugin {
	@Inject
	private JakLootHopConfig config;

	@Inject
	private Client client;

	@Inject
	private InventoryUtils inventory;

	public List<String> targetString;

	public List<Integer> targetID;

	public JakLootHopPlugin() {
	}

	@Provides
	JakLootHopConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(JakLootHopConfig.class);
	}

	@Override
	protected void startUp() {

	}

	@Override
	protected void shutDown() {

	}

	@Subscribe
	private void onGameTick(GameTick event) {
		if (!startLootHop)
		{
			return;
		}
		else {
			targetString = Arrays.asList(config.targetIDs().split(","));
			targetID = itemList.stream().map(Integer::parseInt).collect(Collectors.toList());
		}
		if (config)
		{
			inventory.dropItems(itemIDList, true, config.sleepMin(), config.sleepMax());
		}
	}
}
