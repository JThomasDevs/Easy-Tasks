package com.easytasks.infoboxes;

import com.easytasks.tasks.Task;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Tracks a single required item and auto-removes when gathered.
 * Multiple instances are created per task, one for each required item.
 * If the user drops items mid-run after gathering, infoboxes will reappear
 * (missing count updates from inventory); this is intentional.
 */
public class ItemTrackingInfoBox extends InfoBox
{
	private final Task task;
	private final Task.RequiredItem requiredItem;
	private final Client client;
	private final ItemManager itemManager;
	private int missingCount = 0;
	private String tooltipText = "";

	public ItemTrackingInfoBox(BufferedImage image, Plugin plugin, Task.RequiredItem requiredItem, Task task, Client client, ItemManager itemManager)
	{
		super(image, plugin);
		this.requiredItem = requiredItem;
		this.task = task;
		this.client = client;
		this.itemManager = itemManager;
		update();
	}

	/** Only the count still needed is shown on the infobox; no other text. */
	@Override
	public String getText()
	{
		if (missingCount > 0)
			return missingCount + "x";
		return "";
	}

	@Override
	public Color getTextColor()
	{
		return missingCount <= 0 ? Color.GREEN : Color.ORANGE;
	}

	@Override
	public boolean render()
	{
		return missingCount > 0 && task.isActive();
	}

	@Override
	public String getTooltip()
	{
		return tooltipText;
	}

	/** Called when inventory changes; recomputes missing count and tooltip for this item. */
	public void update()
	{
		if (task == null || !task.isActive())
		{
			missingCount = 0;
			tooltipText = "";
			return;
		}
		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		int have = countItemsInList(inv, requiredItem.getItemIds());
		missingCount = Math.max(0, requiredItem.getQuantity() - have);
		String itemName = itemManager.getItemComposition(requiredItem.getItemId()).getName();
		if (missingCount > 0)
			tooltipText = "Need " + missingCount + " more " + itemName;
		else
			tooltipText = itemName + ": Ready";
		setTooltip(tooltipText);
	}

	private static int countItemsInList(ItemContainer container, List<Integer> itemIds)
	{
		if (container == null || itemIds == null) return 0;
		int count = 0;
		for (Item item : container.getItems())
			if (itemIds.contains(item.getId())) count += item.getQuantity();
		return count;
	}
}
