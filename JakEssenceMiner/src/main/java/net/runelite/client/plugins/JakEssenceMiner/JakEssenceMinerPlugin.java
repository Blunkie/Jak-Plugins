package net.runelite.client.plugins.JakEssenceMiner;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.queries.GameObjectQuery;
import net.runelite.api.queries.NPCQuery;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.iutils.*;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.util.Set;


@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
        name = "Jak Essence Miner",
        enabledByDefault = false,
        description = "Automatically mines rune/pure essence",
        tags = {"jak", "auto", "bot", "prayers", "flick"}
)
@Slf4j
public class JakEssenceMinerPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private JakEssenceMinerConfig config;

    @Inject
    private JakEssenceMinerOverlay overlay;

    @Inject
    private iUtils utils;

    @Inject
    private PlayerUtils player;

    @Inject
    private InventoryUtils inventory;

    @Inject
    private BankUtils bank;

    @Inject
    private WalkUtils walk;

    @Inject
    private CalculationUtils calc;

    @Inject
    OverlayManager overlayManager;

    Instant botTimer;
    int timeout;
    int tickLength;
    long sleepLength;
    boolean startEssenceMiner;
    private static final int VARROCK_REGION = 12853;
    private static final int ESSENCE_MINE_REGION = 11595;
    private final Set<Integer> PORTALS = Set.of(34779, 34774, 34778, 34825, 34777, 34775);
    private final Set<Integer> ESSENCE = Set.of(ItemID.RUNE_ESSENCE, ItemID.PURE_ESSENCE);

    @Provides
    JakEssenceMinerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(JakEssenceMinerConfig.class);
    }

    @Override
    protected void startUp() {

    }

    @Override
    protected void shutDown() {
        resetVals();
    }

    private void resetVals() {
        overlayManager.remove(overlay);
        botTimer = null;
        startEssenceMiner = false;
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
    private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
        if (!configButtonClicked.getGroup().equalsIgnoreCase("JakEssenceMiner")) {
            return;
        }
        log.info("Start/Stop", configButtonClicked.getKey());
        if (configButtonClicked.getKey().equals("startEssenceMiner")) {
            if (!startEssenceMiner) {
                startEssenceMiner = true;
                if (config.enableUI()) {
                    overlayManager.add(overlay);
                    botTimer = Instant.now();
                }
            } else {
                resetVals();
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!startEssenceMiner) {
            return;
        }

        if (timeout > 0) {
            timeout--;
        }

        GameObject bankBooth = new GameObjectQuery().nameEquals("Bank booth").result(client).nearestTo(client.getLocalPlayer());
        GameObject essence = new GameObjectQuery().idEquals(34773).result(client).nearestTo(client.getLocalPlayer());
        GameObject portalObject = new GameObjectQuery().idEquals(PORTALS).result(client).nearestTo(client.getLocalPlayer());
        NPC portalNPC = new NPCQuery().nameContains("Portal").result(client).nearestTo(client.getLocalPlayer());
        NPC aubury = new NPCQuery().nameEquals("Aubury").result(client).nearestTo(client.getLocalPlayer());
        LocalPoint auburyShopTest = new LocalPoint(6848, 3401);
        WorldPoint auburyShop = new WorldPoint(3253, 3402, 0);

        if (isAtVarrock() && timeout == 0) {
            if ((!player.isInteracting() || !player.isMoving()) && inventory.isFull()) {
                if (!bank.isOpen()) {
                    utils.doGameObjectActionMsTime(bankBooth, MenuAction.GAME_OBJECT_SECOND_OPTION.getId(), sleepDelay());
                    timeout = 1 + tickDelay();
                } else if (bank.isOpen()) {
                    bank.depositAllOfItems(ESSENCE);
                }
            }

            if (!inventory.isFull() && client.getLocalPlayer().getGraphic() != 110) { //Teleport graphic
                if (aubury != null && client.getLocalPlayer().getInteracting() != aubury) {
                    utils.doNpcActionMsTime(aubury, MenuAction.NPC_FOURTH_OPTION.getId(), sleepDelay());
                    timeout = 1 + tickDelay();
                } else if (!client.getLocalPlayer().isMoving() && aubury == null) {
                    walk.sceneWalk(walk.getRandPoint(auburyShop, 3), 0, sleepDelay());
                    timeout = 14 + tickDelay();
                }
            }
        }

        if (isAtEssenceMine() && timeout == 0) {
            if (!player.isInteracting()) {
                if (!inventory.isFull()) {
                    utils.doGameObjectActionMsTime(essence, MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), sleepDelay());
                    timeout = 2 + tickDelay();
                }
                if (inventory.isFull() && client.getLocalPlayer().getGraphic() != 110) {
                    if (portalNPC != null) {
                        utils.doNpcActionMsTime(portalNPC, MenuAction.NPC_FIRST_OPTION.getId(), sleepDelay()); //Portal ID 34779 can be either NPC or gameObject depending on instance.
                    }
                    if (portalObject != null) {
                        utils.doGameObjectActionMsTime(portalObject, MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), sleepDelay());
                    }
                    timeout = 4 + tickDelay();
                }
            }
        }
    }

    private boolean isAtEssenceMine()
    {
        return ArrayUtils.contains(client.getMapRegions(), ESSENCE_MINE_REGION);
    }

    private boolean isAtVarrock()
    {
        return ArrayUtils.contains(client.getMapRegions(), VARROCK_REGION);
    }
}