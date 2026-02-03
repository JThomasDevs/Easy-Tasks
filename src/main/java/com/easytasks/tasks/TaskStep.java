package com.easytasks.tasks;

import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.function.BooleanSupplier;

public final class TaskStep
{
	private final String description;
	private final WorldPoint location;
	private final List<Integer> requiredItems;
	private final BooleanSupplier completionCondition;

	public TaskStep(String description, WorldPoint location, List<Integer> requiredItems, BooleanSupplier completionCondition)
	{
		this.description = description;
		this.location = location;
		this.requiredItems = requiredItems != null ? List.copyOf(requiredItems) : List.of();
		this.completionCondition = completionCondition != null ? completionCondition : () -> false;
	}

	public String getDescription() { return description; }
	public WorldPoint getLocation() { return location; }
	public List<Integer> getRequiredItems() { return requiredItems; }
	public boolean isComplete() { return completionCondition.getAsBoolean(); }
}
