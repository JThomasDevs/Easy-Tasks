package com.easytasks.ui;

import com.easytasks.BirdhouseConfig;
import com.easytasks.tasks.Task;
import com.easytasks.tasks.TaskManager;
import com.easytasks.tasks.birdhouse.BirdhouseTask;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.Optional;

public class BirdhouseSidePanel extends PluginPanel implements TaskManager.TaskStateListener
{
	private static final String[] LOCATION_NAMES = {"Verdant Valley (North)", "Verdant Valley (South)", "Mushroom Forest", "Tar Swamp"};

	private static final Color BG_DARK = new Color(0x2b2b2b);
	private static final Color BG_PANEL = new Color(0x1e1e1e);
	private static final Color FG_LIGHT = new Color(0xe0e0e0);
	private static final Color BUTTON_BG = new Color(0x1a1a1a);
	private static final Color BUTTON_FG = Color.WHITE;
	private static final Color PROGRESS_BG = new Color(0x333333);
	private static final Color PROGRESS_FG = new Color(0x4a9eff);

	private final BirdhouseConfig config;
	private final TaskManager taskManager;
	private final ConfigManager configManager;

	private final JButton startStopButton;
	private final JProgressBar progressBar;
	private final JLabel progressLabel;
	private final JList<Integer> routeList;
	private final DefaultListModel<Integer> routeModel;

	private Runnable onStartRequested;
	private Runnable onStopRequested;
	private Runnable onItemListRefreshRequested;

	@Inject
	public BirdhouseSidePanel(BirdhouseConfig config, TaskManager taskManager, ConfigManager configManager)
	{
		this.config = config;
		this.taskManager = taskManager;
		this.configManager = configManager;

		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setBackground(BG_DARK);

		JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		header.setBackground(BG_DARK);
		JLabel title = new JLabel("Pick a birdhouse run:");
		title.setFont(title.getFont().deriveFont(Font.PLAIN, 13f));
		title.setForeground(FG_LIGHT);
		header.add(title);
		add(header, BorderLayout.NORTH);

		JPanel center = new JPanel();
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
		center.setBackground(BG_DARK);

		startStopButton = new JButton("Start Birdhouse Run");
		startStopButton.addActionListener(e -> toggleRun());
		styleButton(startStopButton);
		center.add(startStopButton);
		center.add(Box.createVerticalStrut(10));

		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setPreferredSize(new Dimension(0, 14));
		progressBar.setBackground(PROGRESS_BG);
		progressBar.setForeground(PROGRESS_FG);
		center.add(progressBar);
		progressLabel = new JLabel("0/4 locations completed");
		progressLabel.setForeground(FG_LIGHT);
		progressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		center.add(progressLabel);
		center.add(Box.createVerticalStrut(12));

		JLabel routeTitle = new JLabel("Route order (drag to reorder)");
		routeTitle.setForeground(FG_LIGHT);
		routeTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		center.add(routeTitle);
		center.add(Box.createVerticalStrut(4));
		routeModel = new DefaultListModel<>();
		routeList = new JList<>(routeModel);
		routeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		routeList.setDragEnabled(true);
		routeList.setDropMode(DropMode.INSERT);
		routeList.setTransferHandler(new RouteOrderTransferHandler());
		routeList.setBackground(BG_PANEL);
		routeList.setForeground(FG_LIGHT);
		routeList.setSelectionBackground(new Color(0x404040));
		routeList.setSelectionForeground(FG_LIGHT);
		routeList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
			String display = getRouteDisplayName(value);
			JLabel l = new JLabel((index + 1) + ". " + display);
			l.setOpaque(true);
			l.setBackground(isSelected ? list.getSelectionBackground() : BG_PANEL);
			l.setForeground(isSelected ? list.getSelectionForeground() : FG_LIGHT);
			return l;
		});
		routeList.setVisibleRowCount(4);
		routeList.setPreferredSize(new Dimension(0, 80));
		JScrollPane routeScroll = new JScrollPane(routeList);
		routeScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		routeScroll.setBackground(BG_DARK);
		routeScroll.getViewport().setBackground(BG_PANEL);
		center.add(routeScroll);

		JScrollPane scroll = new JScrollPane(center);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		scroll.getViewport().setBackground(BG_DARK);
		add(scroll, BorderLayout.CENTER);
	}

	@Override
	public void addNotify()
	{
		super.addNotify();
		taskManager.addTaskStateListener(this);
		refreshRouteOrderFromConfig();
	}

	@Override
	public void removeNotify()
	{
		taskManager.removeTaskStateListener(this);
		super.removeNotify();
	}

	private static void styleButton(JButton btn)
	{
		btn.setBackground(BUTTON_BG);
		btn.setForeground(BUTTON_FG);
		btn.setFocusPainted(false);
		btn.setBorder(BorderFactory.createLineBorder(new Color(0x444444)));
		btn.setPreferredSize(new Dimension(0, 28));
		btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
	}

	@SuppressWarnings("boxing")
	private static String getRouteDisplayName(Object value)
	{
		if (value == null || !(value instanceof Integer))
			return value == null ? "Unknown" : String.valueOf(value);
		int idx = (Integer) value;
		if (idx < 0 || idx >= LOCATION_NAMES.length)
			return "Unknown";
		return LOCATION_NAMES[idx];
	}

	private void toggleRun()
	{
		Optional<BirdhouseTask> existing = taskManager.getTaskByType(BirdhouseTask.class);
		if (existing.isPresent())
		{
			if (onStopRequested != null) onStopRequested.run();
		}
		else
		{
			if (onStartRequested != null) onStartRequested.run();
		}
	}

	public void setOnStartRequested(Runnable r) { onStartRequested = r; }
	public void setOnStopRequested(Runnable r) { onStopRequested = r; }
	public void setOnItemListRefreshRequested(Runnable r) { onItemListRefreshRequested = r; }

	/** Refreshes the item checklist (e.g. required-items infobox). Called when config changes. */
	public void refreshItemList()
	{
		if (onItemListRefreshRequested != null)
			onItemListRefreshRequested.run();
	}

	@Override
	public void onTaskRegistered(Task task)
	{
		SwingUtilities.invokeLater(() -> updateForTask(task));
	}

	@Override
	public void onTaskUnregistered(Task task)
	{
		SwingUtilities.invokeLater(this::updateIdle);
	}

	@Override
	public void onTaskStateChanged(Task task)
	{
		SwingUtilities.invokeLater(() -> updateForTask(task));
	}

	private void updateForTask(Task task)
	{
		if (!(task instanceof BirdhouseTask)) return;
		BirdhouseTask bt = (BirdhouseTask) task;
		startStopButton.setText("Stop Birdhouse Run");
		progressBar.setValue(bt.getProgress());
		progressLabel.setText(bt.getCompletedSteps() + "/" + bt.getTotalSteps() + " locations completed");
	}

	private void updateIdle()
	{
		startStopButton.setText("Start Birdhouse Run");
		progressBar.setValue(0);
		progressLabel.setText("0/4 locations completed");
	}

	/** Called when the run completes or stops so the button and progress reset. Safe to call from any thread. */
	public void setRunIdle()
	{
		SwingUtilities.invokeLater(this::updateIdle);
	}

	private void refreshRouteOrderFromConfig()
	{
		routeModel.clear();
		String order = configManager.getConfiguration("birdhouse", "routeOrder");
		if (order == null || order.isBlank()) order = "0,1,2,3";
		java.util.Set<Integer> added = new java.util.HashSet<>();
		for (String idxStr : order.split(","))
		{
			try
			{
				int i = Integer.parseInt(idxStr.trim());
				if (i >= 0 && i < 4 && added.add(i)) routeModel.addElement(i);
			}
			catch (NumberFormatException ignored) {}
		}
		for (int i = 0; i < 4; i++)
			if (!added.contains(i)) routeModel.addElement(i);
	}

	private void persistRouteOrder()
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < routeModel.getSize(); i++)
		{
			if (i > 0) sb.append(",");
			sb.append(routeModel.get(i));
		}
		configManager.setConfiguration("birdhouse", "routeOrder", sb.toString());
	}

	/** Plan §4.1: Drag-and-drop reorder; persists routeOrder config immediately. */
	private class RouteOrderTransferHandler extends TransferHandler
	{
		private int sourceIndex = -1;

		@Override
		protected Transferable createTransferable(JComponent c)
		{
			if (!(c instanceof JList)) return null;
			@SuppressWarnings("unchecked")
			JList<Integer> list = (JList<Integer>) c;
			sourceIndex = list.getSelectedIndex();
			if (sourceIndex < 0 || sourceIndex >= routeModel.getSize()) return null;
			return new StringSelection(sourceIndex + ":" + routeModel.get(sourceIndex));
		}

		@Override
		public int getSourceActions(JComponent c) { return MOVE; }

		@Override
		public boolean canImport(TransferHandler.TransferSupport support)
		{
			if (!support.isDrop() || !support.isDataFlavorSupported(DataFlavor.stringFlavor)) return false;
			JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
			return dl.getIndex() >= 0 && dl.getIndex() <= routeModel.getSize();
		}

		@Override
		public boolean importData(TransferHandler.TransferSupport support)
		{
			if (!canImport(support)) return false;
			try
			{
				String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
				String[] parts = data.split(":");
				if (parts.length != 2) return false;
				int from = Integer.parseInt(parts[0]);
				int value = Integer.parseInt(parts[1]);
				int dropIndex = ((JList.DropLocation) support.getDropLocation()).getIndex();
				if (from == dropIndex || from == dropIndex - 1) return true;
				routeModel.remove(from);
				int insert = dropIndex > from ? dropIndex - 1 : dropIndex;
				routeModel.add(insert, value);
				persistRouteOrder();
				return true;
			}
			catch (Exception e) { return false; }
		}

		@Override
		protected void exportDone(JComponent source, Transferable data, int action)
		{
			sourceIndex = -1;
		}
	}

	public void refreshFromConfig() { SwingUtilities.invokeLater(this::refreshRouteOrderFromConfig); }

	/** Returns current route order (indices 0–3) for the task. Used when starting a run. */
	public int[] getRouteOrder()
	{
		int n = routeModel.getSize();
		int[] order = new int[4];
		for (int i = 0; i < 4 && i < n; i++)
			order[i] = routeModel.get(i);
		for (int i = n; i < 4; i++)
			order[i] = i;
		return order;
	}
}
