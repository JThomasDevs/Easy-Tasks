package com.easytasks.tasks.birdhouse;

import com.easytasks.BirdhouseConfig;
import com.easytasks.tasks.Task;
import com.easytasks.tasks.TaskLocation;
import com.easytasks.tasks.TaskManager;
import com.easytasks.tasks.TaskStep;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BirdhouseTask implements Task
{
	private final Client client;
	private final TaskManager taskManager;
	private final BirdhouseConfig config;

	private volatile boolean active;
	private BirdhouseMode mode;
	private int[] routeOrder;
	private List<BirdhouseLocation> locations;
	private List<TaskStep> steps;
	private int currentStepIndex;
	private Runnable onCompleteCallback;

	@Inject
	public BirdhouseTask(Client client, TaskManager taskManager, BirdhouseConfig config)
	{
		this.client = client;
		this.taskManager = taskManager;
		this.config = config;
		this.locations = createDefaultLocations();
		this.steps = new ArrayList<>();
		this.currentStepIndex = 0;
		this.routeOrder = new int[]{0, 1, 2, 3};
	}

	public void setMode(BirdhouseMode mode) { this.mode = mode; }

	public void setRouteOrder(int[] routeOrder)
	{
		if (routeOrder == null || routeOrder.length != 4) return;
		this.routeOrder = routeOrder.clone();
		rebuildStepsFromRoute();
	}

	public void setOnCompleteCallback(Runnable onCompleteCallback) { this.onCompleteCallback = onCompleteCallback; }

	/** Rebuilds the step list from current routeOrder and resets currentStepIndex to match completed count. */
	public void rebuildStepsFromRoute()
	{
		buildStepSequence();
		int completed = getCompletedSteps();
		currentStepIndex = Math.min(Math.max(0, completed), Math.max(0, steps.size() - 1));
		taskManager.notifyTaskStateChanged(this);
	}

	private static List<BirdhouseLocation> createDefaultLocations()
	{
		List<Integer> allTeleportIds = BirdhouseData.MUSHROOM_TELEPORT_OBJECT_IDS;
		List<BirdhouseLocation> list = new ArrayList<>();
		list.add(new BirdhouseLocation("Verdant Valley (North)", BirdhouseData.VERDANT_VALLEY_NORTH, Collections.singletonList(BirdhouseData.OBJECT_ID_VALLEY_NORTH), Collections.singletonList(allTeleportIds.get(0)), BirdhouseData.OBJECT_ID_VALLEY_NORTH));
		list.add(new BirdhouseLocation("Verdant Valley (South)", BirdhouseData.VERDANT_VALLEY_SOUTH, Collections.singletonList(BirdhouseData.OBJECT_ID_VALLEY_SOUTH), Collections.singletonList(allTeleportIds.get(1)), BirdhouseData.OBJECT_ID_VALLEY_SOUTH));
		list.add(new BirdhouseLocation("Mushroom Forest", BirdhouseData.MUSHROOM_FOREST, Collections.singletonList(BirdhouseData.OBJECT_ID_MUSHROOM_FOREST), Collections.singletonList(allTeleportIds.get(2)), BirdhouseData.OBJECT_ID_MUSHROOM_FOREST));
		list.add(new BirdhouseLocation("Tar Swamp", BirdhouseData.TAR_SWAMP, Collections.singletonList(BirdhouseData.OBJECT_ID_TAR_SWAMP), Collections.singletonList(allTeleportIds.get(3)), BirdhouseData.OBJECT_ID_TAR_SWAMP));
		return list;
	}

	@Override
	public void start()
	{
		active = true;
		if (mode == null) mode = BirdhouseMode.BIRDHOUSES;
		for (BirdhouseLocation loc : locations)
		{
			loc.setVisited(false);
			loc.setCompleted(false);
			loc.setFilledThisRun(false);
			loc.setHadNeedsFilling(false);
			loc.setLastVarplayerValue(loc.getStateValue(client));
		}
		buildStepSequence();
		currentStepIndex = 0;
		taskManager.notifyTaskStateChanged(this);
	}

	@Override
	public void stop()
	{
		active = false;
		steps.clear();
		currentStepIndex = 0;
		taskManager.notifyTaskStateChanged(this);
	}

	@Override
	public boolean isActive() { return active; }

	@Override
	public boolean isComplete()
	{
		if (!active || locations == null) return false;
		for (int i = 0; i < 4; i++)
		{
			BirdhouseLocation loc = getLocationByRouteIndex(i);
			if (loc == null || !loc.isVisited()) return false;
		}
		return true;
	}

	@Override
	public int getProgress()
	{
		int total = getTotalSteps();
		return total == 0 ? 0 : (getCompletedSteps() * 100) / total;
	}

	@Override
	public List<RequiredItem> getRequiredItems()
	{
		if (mode == null) return Collections.emptyList();
		return mode.getRequiredItems();
	}

	@Override
	public List<? extends TaskLocation> getLocations()
	{
		return locations == null ? Collections.emptyList() : locations;
	}

	public List<BirdhouseLocation> getLocationsInRouteOrder()
	{
		if (locations == null || routeOrder == null) return Collections.emptyList();
		List<BirdhouseLocation> ordered = new ArrayList<>(4);
		for (int i = 0; i < 4; i++)
		{
			int idx = routeOrder[i];
			if (idx >= 0 && idx < locations.size()) ordered.add(locations.get(idx));
		}
		return ordered;
	}

	@Override
	public TaskStep getCurrentStep()
	{
		if (steps == null || steps.isEmpty() || currentStepIndex < 0 || currentStepIndex >= steps.size()) return null;
		return steps.get(currentStepIndex);
	}

	@Override
	public int getCompletedSteps()
	{
		int count = 0;
		for (BirdhouseLocation loc : getLocationsInRouteOrder()) if (loc.isVisited()) count++;
		return count;
	}

	@Override
	public int getTotalSteps() { return 4; }

	@Override
	public String getName() { return "Birdhouse Run"; }

	@Override
	public String getDescription() { return "Assists with birdhouse runs on Fossil Island."; }

	@Override
	public BufferedImage getIcon() { return null; }

	public BirdhouseMode getMode() { return mode; }
	public int[] getRouteOrder() { return routeOrder == null ? new int[]{0, 1, 2, 3} : routeOrder.clone(); }

	/** The location for the current step (by route order). */
	public BirdhouseLocation getCurrentStepLocation()
	{
		return getLocationByRouteIndex(currentStepIndex);
	}

	/** True when the current step is to use seeds on the birdhouse (occupied but empty). */
	public boolean isCurrentStepFillWithSeeds()
	{
		BirdhouseLocation loc = getCurrentStepLocation();
		return loc != null && loc.getStateValue(client) == BirdhouseData.VARPLAYER_VALUE_OCCUPIED_EMPTY;
	}

	/**
	 * Returns the instruction for the current step. If the player is not near the current step's patch,
	 * returns "Go to &lt;location&gt;". Otherwise uses the patch state (varp) at that location:
	 * 21 = harvest, 0 = build, 19 = fill. When just filled (varp 21 + hadNeedsFilling), returns next step's instruction.
	 */
	public String getCurrentStepInstruction(WorldPoint playerWp)
	{
		if (!active || locations == null) return null;
		return getInstructionForStep(currentStepIndex, playerWp);
	}

	/** 1-based step number for display; matches the instruction (e.g. 2 when showing "Go to" next location after completing first). */
	public int getCurrentStepNumberForDisplay(WorldPoint playerWp)
	{
		if (!active || locations == null) return 1;
		BirdhouseLocation loc = getLocationByRouteIndex(currentStepIndex);
		if (loc == null) return currentStepIndex + 1;
		boolean showingNext = false;
		if (playerWp != null && !isWithinProximity(playerWp, loc.getWorldPoint()))
		{
			boolean completed = loc.isVisited();
			if (!completed)
			{
				int varp = loc.getStateValue(client);
				if (varp == BirdhouseData.VARPLAYER_VALUE_OCCUPIED_FULL
					&& (loc.isFilledThisRun() || loc.isHadNeedsFilling()))
					completed = true;
			}
			if (completed) showingNext = true;
		}
		else if (loc.getStateValue(client) == BirdhouseData.VARPLAYER_VALUE_OCCUPIED_FULL
			&& (loc.isFilledThisRun() || loc.isHadNeedsFilling()))
			showingNext = true;
		int oneBased = showingNext ? currentStepIndex + 2 : currentStepIndex + 1;
		return Math.min(oneBased, getTotalSteps());
	}

	private String getInstructionForStep(int routeIndex, WorldPoint playerWp)
	{
		final int maxIndex = getLocationsInRouteOrder().size();
		if (routeIndex < 0 || routeIndex >= maxIndex) return null;

		while (routeIndex < maxIndex)
		{
			// On first step, only show "Gather the required items" when not yet at the first location.
			// Once at the first location (e.g. after building), show normal step instruction (fill seeds, etc.).
			if (routeIndex == 0 && !hasRequiredItems())
			{
				BirdhouseLocation first = getLocationByRouteIndex(0);
				if (first != null && (playerWp == null || !isWithinProximity(playerWp, first.getWorldPoint())))
					return "Gather the required items";
			}
			BirdhouseLocation loc = getLocationByRouteIndex(routeIndex);
			if (loc == null) return null;
			if (playerWp != null && !isWithinProximity(playerWp, loc.getWorldPoint()))
			{
				boolean completed = loc.isVisited();
				if (!completed)
				{
					int varp = loc.getStateValue(client);
					if (varp == BirdhouseData.VARPLAYER_VALUE_OCCUPIED_FULL
						&& (loc.isFilledThisRun() || loc.isHadNeedsFilling()))
						completed = true;
				}
				if (completed)
				{
					routeIndex++;
					continue;
				}
				return "Go to " + loc.getName();
			}
			int varp = loc.getStateValue(client);
			if (varp == BirdhouseData.VARPLAYER_VALUE_OCCUPIED_FULL)
			{
				if (loc.isFilledThisRun() || loc.isHadNeedsFilling())
				{
					routeIndex++;
					continue;
				}
				return "Empty the birdhouse to harvest it";
			}
			if (varp == BirdhouseData.VARPLAYER_VALUE_UNOCCUPIED)
				return "Click the patch to build a new birdhouse";
			if (varp == BirdhouseData.VARPLAYER_VALUE_OCCUPIED_EMPTY)
			{
				loc.setHadNeedsFilling(true);
				return "Click seeds, then click the birdhouse to fill it";
			}
			return "Visit " + loc.getName();
		}
		return null;
	}

	/** True if the player's inventory satisfies all required items for this task. */
	private boolean hasRequiredItems()
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		if (inv == null) return false;
		for (RequiredItem ri : getRequiredItems())
		{
			int have = countItemsInContainer(inv, ri.getItemIds());
			if (have < ri.getQuantity()) return false;
		}
		return true;
	}

	private static int countItemsInContainer(ItemContainer container, List<Integer> itemIds)
	{
		if (container == null || itemIds == null) return 0;
		int count = 0;
		for (Item item : container.getItems())
			if (itemIds.contains(item.getId())) count += item.getQuantity();
		return count;
	}

	private boolean isWithinProximity(WorldPoint playerWp, WorldPoint locWp)
	{
		if (playerWp.getPlane() != locWp.getPlane()) return false;
		int dx = Math.abs(playerWp.getX() - locWp.getX());
		int dy = Math.abs(playerWp.getY() - locWp.getY());
		return Math.max(dx, dy) <= PROXIMITY_TILES;
	}

	private BirdhouseLocation getLocationByRouteIndex(int routeIndex)
	{
		List<BirdhouseLocation> ordered = getLocationsInRouteOrder();
		if (routeIndex < 0 || routeIndex >= ordered.size()) return null;
		return ordered.get(routeIndex);
	}

	private void buildStepSequence()
	{
		steps.clear();
		for (BirdhouseLocation loc : getLocationsInRouteOrder())
			steps.add(new TaskStep("Visit " + loc.getName(), loc.getWorldPoint(), Collections.emptyList(), () -> loc.isVisited()));
	}

	/** Plan §7.3: Detect Take (harvest), Build (place birdhouse). Advance on Fill is detected by polling state in checkProximityAndAdvance. */
	public void onMenuOptionClicked(String option, int objectId, int param0, int param1)
	{
		if (!active || locations == null) return;
		boolean take = "Take".equals(option);
		boolean build = "Build".equals(option);
		if (!take && !build) return;
		LocalPoint lp = LocalPoint.fromScene(param0, param1);
		if (lp == null) return;
		WorldPoint wp = WorldPoint.fromLocal(client, lp);
		for (BirdhouseLocation loc : locations)
		{
			if (loc.getPatchObjectId() == objectId && loc.getWorldPoint().equals(wp))
			{
				if (take)
				{
					loc.setFilledThisRun(false);
					loc.setHadNeedsFilling(false);
				}
				taskManager.notifyTaskStateChanged(this);
				break;
			}
		}
	}

	private void advanceToNextStep()
	{
		int completed = getCompletedSteps();
		if (completed < steps.size()) currentStepIndex = completed;
	}

	/** When the player is within this many tiles of the current patch, "Go to X" is suppressed and step instruction is shown. */
	private static final int PROXIMITY_TILES = 10;

	/**
	 * Polls state from object definition (VarPlayer/Varbit per patch). When current step's location
	 * has state 21 and hadNeedsFilling (or prev 19), mark filled and advance. Called every GameTick.
	 */
	public void checkProximityAndAdvance()
	{
		if (!active || locations == null) return;
		List<BirdhouseLocation> ordered = getLocationsInRouteOrder();
		if (currentStepIndex < 0 || currentStepIndex >= ordered.size()) return;
		BirdhouseLocation loc = ordered.get(currentStepIndex);
		if (loc.isVisited()) return;
		int value = loc.getStateValue(client);
		loc.setLastVarplayerValue(value);
		if (value == BirdhouseData.VARPLAYER_VALUE_OCCUPIED_EMPTY)
			loc.setHadNeedsFilling(true);
		else if (value == BirdhouseData.VARPLAYER_VALUE_OCCUPIED_FULL && loc.isHadNeedsFilling())
		{
			loc.setHadNeedsFilling(false);
			loc.setFilledThisRun(true);
			loc.setVisited(true);
			loc.setCompleted(true);
			advanceToNextStep();
			taskManager.notifyTaskStateChanged(this);
			if (isComplete())
			{
				if (onCompleteCallback != null)
				{
					onCompleteCallback.run();
					return;
				}
			}
		}
		else if (value != BirdhouseData.VARPLAYER_VALUE_OCCUPIED_FULL)
		{
			loc.setFilledThisRun(false);
			loc.setHadNeedsFilling(false);
		}
		// Ensure run auto-stops when all locations are completed (fallback if last step didn't fire callback)
		if (isComplete() && onCompleteCallback != null)
		{
			onCompleteCallback.run();
		}
	}
}
