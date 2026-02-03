package com.easytasks.tasks;

import net.runelite.api.coords.WorldPoint;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

/**
 * Abstract contract for task management. Tasks represent a sequence of steps
 * (e.g., birdhouse run) with progress tracking and required items.
 */
public interface Task
{
	void start();
	void stop();
	boolean isActive();
	boolean isComplete();
	int getProgress();
	List<RequiredItem> getRequiredItems();
	List<? extends TaskLocation> getLocations();
	TaskStep getCurrentStep();
	int getCompletedSteps();
	int getTotalSteps();

	String getName();
	String getDescription();
	BufferedImage getIcon();

	interface RequiredItem
	{
		int getItemId();
		int getQuantity();
		/** IDs that satisfy this requirement (e.g. any birdhouse). Default: single getItemId(). */
		default List<Integer> getItemIds() { return Collections.singletonList(getItemId()); }
	}
}
