package com.easytasks;

import com.easytasks.tasks.Task;
import com.easytasks.tasks.TaskManager;
import com.easytasks.tasks.birdhouse.BirdhouseTask;
import com.easytasks.ui.BirdhouseSidePanel;
import com.easytasks.overlays.BirdhouseHighlightOverlay;
import com.easytasks.overlays.SeedHighlightOverlay;
import com.easytasks.overlays.StepGuidanceOverlay;
import com.easytasks.overlays.TeleportHighlightOverlay;
import com.google.inject.Provides;
import javax.inject.Provider;
import com.easytasks.tasks.birdhouse.BirdhouseMode;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@PluginDescriptor(
	name = "Easy Tasks",
	description = "Assists with birdhouse runs on Fossil Island",
	tags = {"birdhouse", "hunter", "fossil island", "skilling"}
)
public class BirdhousePlugin extends Plugin implements TaskManager.TaskStateListener
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BirdhouseConfig config;

	@Inject
	private TaskManager taskManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BirdhouseHighlightOverlay birdhouseHighlightOverlay;

	@Inject
	private SeedHighlightOverlay seedHighlightOverlay;

	@Inject
	private TeleportHighlightOverlay teleportHighlightOverlay;

	@Inject
	private StepGuidanceOverlay stepGuidanceOverlay;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private BirdhouseSidePanel sidePanel;

	@Inject
	private Provider<BirdhouseTask> birdhouseTaskProvider;

	@Inject
	private InfoBoxManager infoBoxManager;

	private NavigationButton navButton;
	private List<com.easytasks.infoboxes.ItemTrackingInfoBox> itemTrackingInfoBoxes = new ArrayList<>();

	@Provides
	BirdhouseConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BirdhouseConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(birdhouseHighlightOverlay);
		overlayManager.add(seedHighlightOverlay);
		overlayManager.add(teleportHighlightOverlay);
		overlayManager.add(stepGuidanceOverlay);
		BufferedImage icon = createNavIcon();
		navButton = NavigationButton.builder()
			.icon(icon)
			.panel(sidePanel)
			.tooltip("Easy Tasks")
			.build();
		clientToolbar.addNavigation(navButton);

		sidePanel.setOnStartRequested(this::startBirdhouseRun);
		sidePanel.setOnStopRequested(this::stopBirdhouseRun);
		sidePanel.setOnItemListRefreshRequested(this::refreshItemTrackingInfoBox);
		taskManager.addTaskStateListener(this);

		log.debug("Easy Tasks started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		taskManager.removeTaskStateListener(this);
		stopBirdhouseRun();
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(stepGuidanceOverlay);
		overlayManager.remove(teleportHighlightOverlay);
		overlayManager.remove(seedHighlightOverlay);
		overlayManager.remove(birdhouseHighlightOverlay);
		log.debug("Easy Tasks stopped!");
	}

	private void startBirdhouseRun()
	{
		int[] routeOrder = sidePanel.getRouteOrder();
		BirdhouseMode mode = config.birdhouseMode();
		clientThread.invokeLater(() -> {
			BirdhouseTask task = birdhouseTaskProvider.get();
			task.setRouteOrder(routeOrder);
			task.setMode(mode);
			task.setOnCompleteCallback(this::onTaskComplete);
			taskManager.registerTask(task);

			removeBirdhouseInfoBoxes();
			if (config.showItemTracking())
			{
				itemTrackingInfoBoxes = new ArrayList<>();
				for (Task.RequiredItem ri : task.getRequiredItems())
				{
					BufferedImage itemBoxImage = itemManager.getImage(ri.getItemId());
					if (itemBoxImage == null) itemBoxImage = createNavIcon();
					com.easytasks.infoboxes.ItemTrackingInfoBox box = new com.easytasks.infoboxes.ItemTrackingInfoBox(itemBoxImage, this, ri, task, client, itemManager);
					infoBoxManager.addInfoBox(box);
					itemTrackingInfoBoxes.add(box);
				}
			}
			else
				itemTrackingInfoBoxes = new ArrayList<>();
		});
	}

	private void stopBirdhouseRun()
	{
		removeBirdhouseInfoBoxes();
		taskManager.getTaskByType(BirdhouseTask.class).ifPresent(task -> {
			taskManager.unregisterTask(task);
			clientThread.invokeLater(() ->
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Easy Tasks: Run stopped.", null));
		});
	}

	/** Called when the final location completes; stops the run (unregisters task, removes infoboxes). */
	private void onTaskComplete()
	{
		removeBirdhouseInfoBoxes();
		taskManager.getTaskByType(BirdhouseTask.class).ifPresent(task -> {
			taskManager.unregisterTask(task);
		});
		// Ensure side panel button and progress reset to "Start Birdhouse Run".
		sidePanel.setRunIdle();
	}

	private void applyConfigToTask(BirdhouseTask task)
	{
		task.setMode(config.birdhouseMode());
		task.setRouteOrder(sidePanel.getRouteOrder());
	}

	private void refreshItemTrackingInfoBox()
	{
		for (com.easytasks.infoboxes.ItemTrackingInfoBox box : itemTrackingInfoBoxes)
			box.update();
	}

	private void removeBirdhouseInfoBoxes()
	{
		for (com.easytasks.infoboxes.ItemTrackingInfoBox box : itemTrackingInfoBoxes)
			infoBoxManager.removeInfoBox(box);
		itemTrackingInfoBoxes.clear();
	}

	private static BufferedImage createNavIcon()
	{
		BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(70, 130, 180));
		g.fillRoundRect(0, 0, 24, 24, 4, 4);
		g.setColor(Color.WHITE);
		g.setFont(new Font("SansSerif", Font.BOLD, 12));
		g.drawString("ET", 4, 17);
		g.dispose();
		return ImageUtil.resizeImage(img, 24, 24);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			stopBirdhouseRun();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		taskManager.getTaskByType(BirdhouseTask.class).ifPresent(BirdhouseTask::checkProximityAndAdvance);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		taskManager.getTaskByType(BirdhouseTask.class).ifPresent(task ->
			task.onMenuOptionClicked(event.getMenuOption(), event.getId(), event.getParam0(), event.getParam1()));
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != net.runelite.api.InventoryID.INVENTORY.getId())
			return;
		for (com.easytasks.infoboxes.ItemTrackingInfoBox box : itemTrackingInfoBoxes)
			box.update();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"birdhouse".equals(event.getGroup())) return;
		sidePanel.refreshFromConfig();
		taskManager.getTaskByType(BirdhouseTask.class).ifPresent(this::applyConfigToTask);
		sidePanel.refreshItemList();
	}

	@Override
	public void onTaskStateChanged(Task task)
	{
		// Step guidance is shown only via StepGuidanceOverlay, not as an infobox.
	}
}
