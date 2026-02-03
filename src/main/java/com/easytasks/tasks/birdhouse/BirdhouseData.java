package com.easytasks.tasks.birdhouse;

import net.runelite.api.Client;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Static game data for birdhouse runs. Plan §2.4, §9.
 *
 * Each birdhouse patch has a unique object ID and a unique VarPlayer for state.
 * VarPlayer value: 0 = unoccupied, 19 = occupied but empty, 21 = occupied and full (harvestable).
 */
public final class BirdhouseData
{
	private BirdhouseData() {}

	public static final List<Integer> SEED_IDS = Collections.unmodifiableList(Arrays.asList(
		ItemID.BARLEY_SEED, ItemID.HAMMERSTONE_HOP_SEED, ItemID.JUTE_SEED,
		ItemID.YANILLIAN_HOP_SEED, ItemID.KRANDORIAN_HOP_SEED, ItemID.WILDBLOOD_HOP_SEED
	));

	public static final List<Integer> BIRDHOUSE_IDS = Collections.unmodifiableList(Arrays.asList(
		ItemID.BIRDHOUSE_NORMAL, ItemID.BIRDHOUSE_OAK, ItemID.BIRDHOUSE_WILLOW, ItemID.BIRDHOUSE_TEAK,
		ItemID.BIRDHOUSE_MAPLE, ItemID.BIRDHOUSE_MAHOGANY, ItemID.BIRDHOUSE_YEW,
		ItemID.BIRDHOUSE_MAGIC, ItemID.BIRDHOUSE_REDWOOD
	));

	public static final List<Integer> LOG_IDS = Collections.unmodifiableList(Arrays.asList(
		ItemID.LOGS, ItemID.OAK_LOGS, ItemID.WILLOW_LOGS, ItemID.TEAK_LOGS,
		ItemID.MAPLE_LOGS, ItemID.MAHOGANY_LOGS, ItemID.YEW_LOGS,
		ItemID.MAGIC_LOGS, ItemID.REDWOOD_LOGS
	));

	/** Item ID for Clockwork (used when building birdhouses from logs). */
	public static final int CLOCKWORK_ITEM_ID = 8792;

	/** Resolves tier index to birdhouse item ID; returns Oak (index 1) if out of range. */
	public static int getBirdhouseId(int tierIndex)
	{
		if (tierIndex < 0 || tierIndex >= BIRDHOUSE_IDS.size()) return BIRDHOUSE_IDS.get(1);
		return BIRDHOUSE_IDS.get(tierIndex);
	}

	/** Resolves tier index to log item ID; returns Oak logs (index 1) if out of range. */
	public static int getLogId(int tierIndex)
	{
		if (tierIndex < 0 || tierIndex >= LOG_IDS.size()) return LOG_IDS.get(1);
		return LOG_IDS.get(tierIndex);
	}

	/** Resolves seed index to seed item ID; returns Barley (index 0) if out of range. */
	public static int getSeedId(int seedIndex)
	{
		if (seedIndex < 0 || seedIndex >= SEED_IDS.size()) return SEED_IDS.get(0);
		return SEED_IDS.get(seedIndex);
	}

	public static final List<Integer> MUSHROOM_TELEPORT_OBJECT_IDS = Collections.unmodifiableList(Arrays.asList(30922, 30923, 30924, 30925));

	/** Mushroom tree object (30924) in Mushroom Forest — highlight when next step is Verdant Valley so player uses it to get there. */
	public static final int MUSHROOM_TREE_TO_VALLEY_OBJECT_ID = 30924;

	/** Object ID per patch (unique per location). Order: valley north, valley south, forest, swamp. */
	public static final int OBJECT_ID_VALLEY_NORTH = 30567;
	public static final int OBJECT_ID_VALLEY_SOUTH = 30568;
	public static final int OBJECT_ID_MUSHROOM_FOREST = 30565;
	public static final int OBJECT_ID_TAR_SWAMP = 30566;

	/** VarPlayer value: unoccupied. */
	public static final int VARPLAYER_VALUE_UNOCCUPIED = 0;
	/** VarPlayer value: occupied but empty (no seeds). */
	public static final int VARPLAYER_VALUE_OCCUPIED_EMPTY = 19;
	/** VarPlayer value: occupied and full of seeds (harvestable). */
	public static final int VARPLAYER_VALUE_OCCUPIED_FULL = 21;

	public static final WorldPoint VERDANT_VALLEY_NORTH = new WorldPoint(3768, 3761, 0);
	public static final WorldPoint VERDANT_VALLEY_SOUTH = new WorldPoint(3763, 3755, 0);
	public static final WorldPoint MUSHROOM_FOREST = new WorldPoint(3677, 3882, 0);
	public static final WorldPoint TAR_SWAMP = new WorldPoint(3679, 3815, 0);

	/** Plan §5.3: Only render overlays when player is on Fossil Island (approximate bounding box). */
	public static boolean isOnFossilIsland(Client client)
	{
		if (client == null || client.getLocalPlayer() == null) return false;
		WorldPoint wp = client.getLocalPlayer().getWorldLocation();
		return wp.getX() >= 3600 && wp.getX() <= 3900
			&& wp.getY() >= 3700 && wp.getY() <= 3900
			&& wp.getPlane() == 0;
	}
}
