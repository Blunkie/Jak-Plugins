/*
 * Copyright (c) 2018, Andrew EP | ElPinche256 <https://github.com/ElPinche256>
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
package net.runelite.client.plugins.JakLootHop;

import net.runelite.client.config.*;

@ConfigGroup("JakLootHopConfig")

public interface JakLootHopConfig extends Config
{
    @ConfigSection(
            keyName = "pluginConfig",
            name = "Plugin Settings",
            description = "Key plugin settings",
            position = 1,
            closedByDefault = true
    )
    String pluginConfig = "pluginConfig";

    @ConfigItem(
            keyName = "enableUI",
            name = "Enable UI",
            description = "Enables UI.",
            position = 2,
            section = "pluginConfig"
    )
    default boolean enableUI()
    {
        return true;
    }

    @ConfigItem(
            keyName = "targetType",
            name = "Target Type",
            description = "Type of Target, use developer tools to get item, NPC, and object IDs.",
            position = 70,
            title = "skillerTitle"
    )
    default JakLootHopType type() {
        return JakLootHopType.ITEM  ;
    }

    @Range(
            min = 0,
            max = 550
    )
    @ConfigItem(
            keyName = "targetIDs",
            name = "Target IDs",
            description = "List of item IDs. Seperated by a comma.",
            position = 4,
            section = "pluginConfig"
    )
    default String targetIDs()
    {
        return null;
    }

    @ConfigTitle(
            keyName = "delayConfig",
            name = "Sleep Delay Configuration",
            description = "Configure how the bot handles sleep delays",
            position = 1
    )
    String delayConfig = "delayConfig";


    @Range(
            min = 0,
            max = 550
    )
    @ConfigItem(
            keyName = "sleepMin",
            name = "Sleep Min",
            description = "",
            position = 2,
            section = "delayConfig"
    )
    default int sleepMin()
    {
        return 60;
    }

    @Range(
            min = 0,
            max = 550
    )

    @ConfigItem(
            keyName = "sleepMax",
            name = "Sleep Max",
            description = "",
            position = 3,
            section = "delayConfig"
    )
    default int sleepMax()
    {
        return 350;
    }

}