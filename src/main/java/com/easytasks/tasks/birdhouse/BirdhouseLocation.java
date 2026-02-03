package com.easytasks.tasks.birdhouse;

import com.easytasks.tasks.TaskLocation;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

public class BirdhouseLocation extends TaskLocation
{
	private final int patchObjectId;

	/** Previous varp value for this patch (to detect 19→21 = just filled). */
	private int lastVarplayerValue = -1;
	/** True when varp transitioned 19→21 this run (newly seeded); don't show "harvest" until they harvest. */
	private boolean filledThisRun;
	/** True when we've seen varp 19 at this location this step (needs filling); next varp 21 = just filled, advance. */
	private boolean hadNeedsFilling;

	public BirdhouseLocation(String name, WorldPoint worldPoint,
	                         List<Integer> objectIds, List<Integer> teleportObjectIds,
	                         int patchObjectId)
	{
		super(name, worldPoint, objectIds, teleportObjectIds);
		this.patchObjectId = patchObjectId;
	}

	/** Object ID for this patch (same whether empty or occupied; state from object definition). */
	public int getPatchObjectId() { return patchObjectId; }

	/** Read state value (0, 19, 21) from the patch object definition (VarPlayer or Varbit). */
	public int getStateValue(Client client)
	{
		ObjectComposition comp = client.getObjectDefinition(patchObjectId);
		if (comp == null) return -1;
		int varpId = comp.getVarPlayerId();
		if (varpId != -1) return client.getVarpValue(varpId);
		int varbitId = comp.getVarbitId();
		if (varbitId != -1) return client.getVarbitValue(varbitId);
		return -1;
	}

	public int getLastVarplayerValue() { return lastVarplayerValue; }
	public void setLastVarplayerValue(int lastVarplayerValue) { this.lastVarplayerValue = lastVarplayerValue; }
	public boolean isFilledThisRun() { return filledThisRun; }
	public void setFilledThisRun(boolean filledThisRun) { this.filledThisRun = filledThisRun; }
	public boolean isHadNeedsFilling() { return hadNeedsFilling; }
	public void setHadNeedsFilling(boolean hadNeedsFilling) { this.hadNeedsFilling = hadNeedsFilling; }

	public boolean isEmpty(Client client)
	{
		return getStateValue(client) == BirdhouseData.VARPLAYER_VALUE_UNOCCUPIED;
	}
	public boolean isPlaced(Client client)
	{
		return getStateValue(client) == BirdhouseData.VARPLAYER_VALUE_OCCUPIED_EMPTY;
	}
	public boolean isOccupied(Client client)
	{
		return getStateValue(client) == BirdhouseData.VARPLAYER_VALUE_OCCUPIED_FULL;
	}
}
