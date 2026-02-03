package com.easytasks.overlays;

import com.easytasks.BirdhouseConfig;
import com.easytasks.tasks.TaskManager;
import com.easytasks.tasks.TaskStep;
import com.easytasks.tasks.birdhouse.BirdhouseTask;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Optional;

/**
 * Text overlay showing current step guidance on screen (like farming-helper's EasyFarmingOverlayInfoBox).
 */
public class StepGuidanceOverlay extends Overlay
{
	private final Client client;
	private final BirdhouseConfig config;
	private final TaskManager taskManager;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	public StepGuidanceOverlay(Client client, BirdhouseConfig config, TaskManager taskManager)
	{
		this.client = client;
		this.config = config;
		this.taskManager = taskManager;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showStepGuidance()) return null;
		Optional<BirdhouseTask> opt = taskManager.getTaskByType(BirdhouseTask.class);
		if (opt.isEmpty() || !opt.get().isActive()) return null;

		BirdhouseTask task = opt.get();
		TaskStep step = task.getCurrentStep();
		if (step == null) return null;

		panelComponent.getChildren().clear();
		WorldPoint playerWp = client.getLocalPlayer() != null ? client.getLocalPlayer().getWorldLocation() : null;
		int current = task.getCompletedSteps() + 1;
		int total = task.getTotalSteps();
		String instruction = task.getCurrentStepInstruction(playerWp);
		if (instruction == null) instruction = step.getDescription();
		String text = current + "/" + total + ": " + instruction;
		panelComponent.getChildren().add(LineComponent.builder().left(text).build());
		return panelComponent.render(graphics);
	}
}
