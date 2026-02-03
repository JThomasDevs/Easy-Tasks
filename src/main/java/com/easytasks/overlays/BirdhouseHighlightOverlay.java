package com.easytasks.overlays;

import com.easytasks.BirdhouseConfig;
import com.easytasks.tasks.TaskManager;
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
import java.util.Optional;

public class BirdhouseHighlightOverlay extends Overlay
{
	private final Client client;
	private final BirdhouseConfig config;
	private final TaskManager taskManager;

	@Inject
	public BirdhouseHighlightOverlay(Client client, BirdhouseConfig config, TaskManager taskManager)
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
		if (!config.highlightBirdhouses()) return null;
		if (!BirdhouseData.isOnFossilIsland(client)) return null;
		Optional<BirdhouseTask> opt = taskManager.getTaskByType(BirdhouseTask.class);
		if (opt.isEmpty()) return null;
		BirdhouseTask task = opt.get();
		if (!task.isActive()) return null;
		net.runelite.api.Scene scene = client.getScene();
		int plane = client.getPlane();
		BirdhouseLocation currentStepLoc = task.getCurrentStepLocation();
		boolean fillWithSeeds = task.isCurrentStepFillWithSeeds();
		for (BirdhouseLocation loc : task.getLocationsInRouteOrder())
		{
			if (loc.isVisited()) continue;
			Color color;
			if (fillWithSeeds && loc == currentStepLoc)
				color = Color.PINK;
			else if (loc.isEmpty(client)) color = config.emptySlotColor();
			else if (loc.isPlaced(client)) color = config.emptyBirdhouseColor();
			else if (loc.isOccupied(client)) color = config.occupiedBirdhouseColor();
			else color = config.emptySlotColor();
			WorldPoint wp = loc.getWorldPoint();
			LocalPoint localPoint = LocalPoint.fromWorld(client, wp);
			if (localPoint == null) continue;
			Shape hull = null;
			net.runelite.api.Tile tile = scene.getTiles()[plane][localPoint.getSceneX()][localPoint.getSceneY()];
			if (tile != null)
			{
				for (TileObject obj : tile.getGameObjects())
				{
					if (obj == null) continue;
					Shape box = obj.getClickbox();
					if (box != null)
					{
						hull = box;
						break;
					}
				}
			}
			if (hull == null)
				hull = Perspective.getCanvasTilePoly(client, localPoint);
			if (hull != null)
				OverlayUtil.renderPolygon(graphics, hull, color, new BasicStroke(config.highlightThickness()));
		}
		return null;
	}
}
