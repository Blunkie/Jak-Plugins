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
package net.runelite.client.plugins.JakClickToFlick;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.iutils.*;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
        name = "Jak Click-to-Flick",
        enabledByDefault = false,
        description = "Automatically flicks your quick-prayers",
        tags = {"jak", "auto", "bot", "prayers", "flick"}
)
@Slf4j
public class JakClickToFlickPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private JakClickToFlickConfig config;

    @Inject
    private JakClickToFlickOverlay overlay;

    @Inject
    private KeyManager keyManager;

    @Inject
    private Random random;

    @Inject
    OverlayManager overlayManager;

    Instant botTimer;
    MenuEntry targetMenu;
    MenuEntry entry;
    private ScheduledExecutorService executor;
    double val;
    long sleepLength;
    boolean startFlick;
    private Rectangle bounds;

    @Provides
    JakClickToFlickConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(JakClickToFlickConfig.class);
    }

    @Override
    protected void startUp() {
        executor = Executors.newSingleThreadScheduledExecutor();
        keyManager.registerKeyListener(flick);
    }

    @Override
    protected void shutDown() {
        executor.shutdown();
        keyManager.unregisterKeyListener(flick);
        resetVals();
    }

    private void resetVals() {
        botTimer = null;
        startFlick = false;
        overlayManager.remove(overlay);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            keyManager.unregisterKeyListener(flick);
            return;
        }
        keyManager.registerKeyListener(flick);
    }

    private final HotkeyListener flick = new HotkeyListener(() -> config.toggleKey())
    {
        @Override
        public void hotkeyPressed()
        {
            log.info("Key pressed");
            if (!startFlick) {
                startFlick = true;
                targetMenu = null;
                botTimer = Instant.now();
                overlayManager.add(overlay);
            } else {
                resetVals();
            }
        }
    };

    public int delayTime() {
        int delay;
        do {
            double val = random.nextGaussian() * config.sleepDeviation() + config.sleepTarget();
            delay = (int) Math.round(val);
        } while (delay <= config.sleepMin() || delay >= config.sleepMax());
        return delay;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!startFlick) {
            return;
        }
        delayTime();
        boolean quickPrayer = client.getVar(Varbits.QUICK_PRAYER) == 1;
        executor.submit(() -> {
            if (quickPrayer) {
                entry = new MenuEntry("prayer toggle", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775, false);
                click();
                try {
                    Thread.sleep(delayTime());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            entry = new MenuEntry("prayer toggle", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775, false);
            click();
        });
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event){
        if(entry!=null){
            event.setMenuEntry(entry);
            entry=null;
        }
    }

    public void click() {
        assert !client.isClientThread();
        mouseEvent(MouseEvent.MOUSE_PRESSED);
        mouseEvent(MouseEvent.MOUSE_RELEASED);
        mouseEvent(MouseEvent.MOUSE_CLICKED);
    }

    private void mouseEvent(int id) {
        Point point = new Point(0,0);
        MouseEvent e = new MouseEvent(
                client.getCanvas(), id,
                System.currentTimeMillis(),
                0, point.getX(), point.getY(),
                1, false, 1
        );
        client.getCanvas().dispatchEvent(e);
    }
}