package com.easytasks;

import com.easytasks.tasks.birdhouse.BirdhouseMode;
import net.runelite.client.config.*;

import java.awt.Color;

@ConfigGroup("birdhouse")
public interface BirdhouseConfig extends Config
{
	@ConfigItem(keyName = "birdhouseMode", name = "Birdhouse mode", description = "Item loadout mode: complete birdhouses or logs + seeds")
	default BirdhouseMode birdhouseMode() { return BirdhouseMode.BIRDHOUSES; }

	@ConfigItem(keyName = "highlightBirdhouses", name = "Highlight birdhouses", description = "Toggle birdhouse object highlights")
	default boolean highlightBirdhouses() { return true; }

	@ConfigItem(keyName = "highlightTeleports", name = "Highlight teleports", description = "Toggle teleport object highlights")
	default boolean highlightTeleports() { return true; }

	@ConfigItem(keyName = "showItemTracking", name = "Show item tracking", description = "Toggle item tracking infobox")
	default boolean showItemTracking() { return true; }

	@ConfigItem(keyName = "showStepGuidance", name = "Show step guidance", description = "Toggle step guidance infobox")
	default boolean showStepGuidance() { return true; }

	@ConfigItem(keyName = "emptySlotColor", name = "Empty slot color", description = "Highlight color for empty ground slots")
	default Color emptySlotColor() { return Color.RED; }

	@ConfigItem(keyName = "emptyBirdhouseColor", name = "Empty birdhouse color", description = "Highlight color for placed but empty birdhouses")
	default Color emptyBirdhouseColor() { return Color.YELLOW; }

	@ConfigItem(keyName = "occupiedBirdhouseColor", name = "Occupied birdhouse color", description = "Highlight color for occupied birdhouses")
	default Color occupiedBirdhouseColor() { return Color.GREEN; }

	@ConfigItem(keyName = "teleportColor", name = "Teleport color", description = "Highlight color for navigation teleports")
	default Color teleportColor() { return Color.CYAN; }

	@ConfigItem(keyName = "highlightThickness", name = "Highlight thickness", description = "Outline thickness for highlights (1-5)")
	@Range(min = 1, max = 5)
	default int highlightThickness() { return 2; }
}
