package com.easytasks.overlays;

import com.easytasks.BirdhouseConfig;
import com.easytasks.tasks.TaskManager;
import com.easytasks.tasks.birdhouse.BirdhouseData;
import com.easytasks.tasks.birdhouse.BirdhouseTask;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SeedHighlightOverlay extends WidgetItemOverlay
{
	private final BirdhouseConfig config;
	private final TaskManager taskManager;
	private final ItemManager itemManager;

	private static final Color USE_ON_BIRDHOUSE_COLOR = Color.PINK;

	@Inject
	public SeedHighlightOverlay(BirdhouseConfig config, TaskManager taskManager, ItemManager itemManager)
	{
		this.config = config;
		this.taskManager = taskManager;
		this.itemManager = itemManager;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!config.highlightBirdhouses()) return;
		var opt = taskManager.getTaskByType(BirdhouseTask.class);
		if (opt.isEmpty()) return;
		BirdhouseTask task = opt.get();
		if (!task.isActive() || !task.isCurrentStepFillWithSeeds()) return;
		// Highlight any valid birdhouse seed (all 6 types), not just the configured seed
		if (!BirdhouseData.SEED_IDS.contains(itemId)) return;
		Rectangle bounds = widgetItem.getCanvasBounds();
		BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), USE_ON_BIRDHOUSE_COLOR);
		if (outline != null)
			graphics.drawImage(outline, (int) bounds.getX(), (int) bounds.getY(), null);
		else
		{
			graphics.setColor(USE_ON_BIRDHOUSE_COLOR);
			graphics.setStroke(new BasicStroke(2));
			graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
		}
	}
}
