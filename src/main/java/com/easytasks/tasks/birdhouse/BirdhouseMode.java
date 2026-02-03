package com.easytasks.tasks.birdhouse;

import com.easytasks.tasks.Task;
import net.runelite.api.gameval.ItemID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum BirdhouseMode
{
	BIRDHOUSES,
	LOGS;

	/**
	 * Builds the required item list for this mode.
	 * BIRDHOUSES: any birdhouse (4), any valid seed (40), hammer, chisel.
	 * LOGS: any log (4), any valid seed (40), hammer, chisel.
	 */
	public List<Task.RequiredItem> getRequiredItems()
	{
		List<Task.RequiredItem> items = new ArrayList<>();
		if (this == BIRDHOUSES)
			items.add(new RequiredItemImpl(BirdhouseData.BIRDHOUSE_IDS, 4));
		else
			items.add(new RequiredItemImpl(BirdhouseData.LOG_IDS, 4));
		items.add(new RequiredItemImpl(BirdhouseData.SEED_IDS, 40));
		items.add(new RequiredItemImpl(ItemID.HAMMER, 1));
		items.add(new RequiredItemImpl(ItemID.CHISEL, 1));
		return Collections.unmodifiableList(items);
	}

	private static final class RequiredItemImpl implements Task.RequiredItem
	{
		private final List<Integer> itemIds;
		private final int quantity;
		RequiredItemImpl(int itemId, int quantity) { this.itemIds = Collections.singletonList(itemId); this.quantity = quantity; }
		RequiredItemImpl(List<Integer> itemIds, int quantity) { this.itemIds = List.copyOf(itemIds); this.quantity = quantity; }
		@Override public int getItemId() { return itemIds.get(0); }
		@Override public int getQuantity() { return quantity; }
		@Override public List<Integer> getItemIds() { return itemIds; }
	}
}
