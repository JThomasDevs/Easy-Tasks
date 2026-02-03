package com.easytasks.tasks;

import net.runelite.api.coords.WorldPoint;

import java.util.List;

public class TaskLocation
{
	private final String name;
	private final WorldPoint worldPoint;
	private final List<Integer> objectIds;
	private final List<Integer> teleportObjectIds;
	private boolean visited;
	private boolean completed;

	public TaskLocation(String name, WorldPoint worldPoint, List<Integer> objectIds, List<Integer> teleportObjectIds)
	{
		this.name = name;
		this.worldPoint = worldPoint;
		this.objectIds = objectIds != null ? List.copyOf(objectIds) : List.of();
		this.teleportObjectIds = teleportObjectIds != null ? List.copyOf(teleportObjectIds) : List.of();
	}

	public String getName() { return name; }
	public WorldPoint getWorldPoint() { return worldPoint; }
	public List<Integer> getObjectIds() { return objectIds; }
	public List<Integer> getTeleportObjectIds() { return teleportObjectIds; }
	public boolean isVisited() { return visited; }
	public void setVisited(boolean visited) { this.visited = visited; }
	public boolean isCompleted() { return completed; }
	public void setCompleted(boolean completed) { this.completed = completed; }
}
