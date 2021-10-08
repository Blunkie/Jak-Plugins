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
package net.runelite.client.plugins.jaktelealch;

import net.runelite.client.config.*;

@ConfigGroup("JakTeleAlch")

public interface JakTeleAlchConfig extends Config
{
	@ConfigItem(
			keyName = "type",
			name = "Teleport Spell",
			description = "Type of teleport being used",
			position = 1
	)
	default JakTeleAlchLoc type() {
		return JakTeleAlchLoc.LUMBRIDGE;
	}

	@ConfigItem(
			keyName = "alchItemID",
			name = "Alch Item ID",
			description = "ID of item to be alched",
			position = 4
	)
	default int alchItemID() { return 420; }

	@ConfigItem(
			keyName = "enableUI",
			name = "Enable UI",
			description = "Enable if using karambwanji as food for cat during fight.",
			position = 4
	)
	default boolean enableUI()
	{
		return true;
	}

	@ConfigItem(
			keyName = "startTeleAlch",
			name = "Start/Stop",
			description = "Start/stop button",
			position = 5
	)
	default Button startTeleAlch() {
		return new Button();
	}

	@ConfigSection(
			keyName = "delayConfig",
			name = "Sleep Delay Configuration",
			description = "Configure how the bot handles sleep delays",
			closedByDefault = true,
			position = 11
	)
	String alchDelayConfig = "alchDelayConfig";

	@Range(
			min = 0,
			max = 550
	)
	@ConfigItem(
			keyName = "sleepMin",
			name = "Sleep Min",
			description = "",
			position = 12,
			section = "alchDelayConfig"
	)
	default int sleepMin() {
		return 50;
	}

	@Range(
			min = 0,
			max = 550
	)
	@ConfigItem(
			keyName = "sleepMax",
			name = "Sleep Max",
			description = "",
			position = 13,
			section = "alchDelayConfig"
	)
	default int sleepMax() {
		return 250;
	}

	@Range(
			min = 0,
			max = 550
	)
	@ConfigItem(
			keyName = "sleepTarget",
			name = "Sleep Target",
			description = "",
			position = 14,
			section = "alchDelayConfig"
	)
	default int sleepTarget() {
		return 175;
	}

	@Range(
			min = 0,
			max = 225
	)
	@ConfigItem(
			keyName = "sleepDeviation",
			name = "Sleep Deviation",
			description = "",
			position = 15,
			section = "alchDelayConfig"
	)
	default int sleepDeviation() {
		return 45;
	}

	@ConfigItem(
			keyName = "sleepWeightedDistribution",
			name = "Sleep Weighted Distribution",
			description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
			position = 16,
			section = "alchDelayConfig"
	)
	default boolean sleepWeightedDistribution() {
		return false;
	}
}