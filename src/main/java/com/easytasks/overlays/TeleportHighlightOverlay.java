package com.easytasks.overlays;

import com.easytasks.BirdhouseConfig;
import com.easytasks.tasks.TaskManager;
import com.easytasks.tasks.TaskStep;
import com.easytasks.tasks.birdhouse.BirdhouseData;
import com.easytasks.tasks.birdhouse.BirdhouseLocation;
import com.easytasks.tasks.birdhouse.BirdhouseTask;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TeleportHighlightOverlay extends Overlay
{
	private final Client client;
	private final BirdhouseConfig config;
	private final TaskManager taskManager;

	@Inject
	public TeleportHighlightOverlay(Client client, BirdhouseConfig config, TaskManager taskManager)
	{
		this.client = client;
		this.config = config;
		this.taskManager = taskManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.highlightTeleports()) return null;
		if (!BirdhouseData.isOnFossilIsland(client)) return null;
		Optional<BirdhouseTask> opt = taskManager.getTaskByType(BirdhouseTask.class);
		if (opt.isEmpty()) return null;
		BirdhouseTask task = opt.get();
		if (!task.isActive()) return null;
		TaskStep step = task.getCurrentStep();
		if (step == null) return null;
		List<Integer> teleportIds = getTeleportIdsForNextStep(task, step);
		if (teleportIds.isEmpty()) return null;
		Color color = config.teleportColor();
		net.runelite.api.Scene scene = client.getScene();
		int plane = client.getPlane();
		for (int x = 0; x < 104; x++)
		{
			for (int y = 0; y < 104; y++)
			{
				net.runelite.api.Tile tile = scene.getTiles()[plane][x][y];
				if (tile == null) continue;
				for (TileObject obj : tile.getGameObjects())
				{
					if (obj == null) continue;
					if (!teleportIds.contains(obj.getId())) continue;
					Shape hull = obj.getClickbox();
					if (hull == null)
					{
						LocalPoint lp = obj.getLocalLocation();
						hull = Perspective.getCanvasTilePoly(client, lp);
					}
					if (hull != null)
					{
						OverlayUtil.renderPolygon(graphics, hull, color, new BasicStroke(config.highlightThickness()));
						return null;
					}
				}
			}
		}
		return null;
	}

	private List<Integer> getTeleportIdsForNextStep(BirdhouseTask task, TaskStep step)
	{
		WorldPoint targetWp = step.getLocation();
		List<BirdhouseLocation> ordered = task.getLocationsInRouteOrder();
		int targetIndex = -1;
		BirdhouseLocation targetLoc = null;
		for (int i = 0; i < ordered.size(); i++)
		{
			if (ordered.get(i).getWorldPoint().equals(targetWp))
			{
				targetIndex = i;
				targetLoc = ordered.get(i);
				break;
			}
		}
		if (targetLoc == null) return List.of();
		List<Integer> ids = new ArrayList<>(targetLoc.getTeleportObjectIds());
		// Only highlight mushroom tree when moving between (Tar Swamp or Forest) and (Verdant Valley N/S)
		BirdhouseLocation fromLoc = targetIndex > 0 ? ordered.get(targetIndex - 1) : null;
		if (fromLoc != null && isMushroomTreeNeeded(fromLoc, targetLoc))
			ids.add(BirdhouseData.MUSHROOM_TREE_TO_VALLEY_OBJECT_ID);
		return ids;
	}

	private static boolean isMushroomTreeNeeded(BirdhouseLocation from, BirdhouseLocation to)
	{
		boolean fromValley = isVerdantValley(from.getWorldPoint());
		boolean toValley = isVerdantValley(to.getWorldPoint());
		boolean fromForestOrSwamp = isMushroomForestOrTarSwamp(from.getWorldPoint());
		boolean toForestOrSwamp = isMushroomForestOrTarSwamp(to.getWorldPoint());
		return (fromValley && toForestOrSwamp) || (fromForestOrSwamp && toValley);
	}

	private static boolean isVerdantValley(WorldPoint wp)
	{
		return wp.equals(BirdhouseData.VERDANT_VALLEY_NORTH) || wp.equals(BirdhouseData.VERDANT_VALLEY_SOUTH);
	}

	private static boolean isMushroomForestOrTarSwamp(WorldPoint wp)
	{
		return wp.equals(BirdhouseData.MUSHROOM_FOREST) || wp.equals(BirdhouseData.TAR_SWAMP);
	}
}
