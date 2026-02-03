package com.easytasks.infoboxes;

import com.easytasks.tasks.TaskStep;
import com.easytasks.tasks.birdhouse.BirdhouseTask;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Plan §6.2: Shows current step instruction and progress (Step X/Y: [Action]).
 */
public class StepGuidanceInfoBox extends InfoBox
{
	private final BirdhouseTask task;
	private final Client client;

	public StepGuidanceInfoBox(BufferedImage image, Plugin plugin, BirdhouseTask task, Client client)
	{
		super(image, plugin);
		this.task = task;
		this.client = client;
		setTooltip("Current step in birdhouse run");
	}

	@Override
	public String getText()
	{
		if (task == null || !task.isActive()) return "";
		TaskStep step = task.getCurrentStep();
		if (step == null) return "";
		WorldPoint playerWp = client.getLocalPlayer() != null ? client.getLocalPlayer().getWorldLocation() : null;
		int current = task.getCompletedSteps() + 1;
		int total = task.getTotalSteps();
		String instruction = task.getCurrentStepInstruction(playerWp);
		if (instruction == null) instruction = step.getDescription();
		return current + "/" + total + ": " + instruction;
	}

	@Override
	public Color getTextColor()
	{
		return Color.WHITE;
	}

	@Override
	public boolean render()
	{
		return task != null && task.isActive();
	}

	/** Called when task state changes (e.g. after varbit change). */
	public void update()
	{
		if (task == null) return;
		WorldPoint playerWp = client.getLocalPlayer() != null ? client.getLocalPlayer().getWorldLocation() : null;
		String instruction = task.getCurrentStepInstruction(playerWp);
		setTooltip(instruction != null ? instruction : "Progress: " + task.getProgress() + "%");
	}}
