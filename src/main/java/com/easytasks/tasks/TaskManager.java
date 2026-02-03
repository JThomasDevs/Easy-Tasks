package com.easytasks.tasks;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class TaskManager
{
	private static final Logger log = LoggerFactory.getLogger(TaskManager.class);
	private final List<Task> activeTasks = new CopyOnWriteArrayList<>();
	private final List<TaskStateListener> listeners = new CopyOnWriteArrayList<>();

	public void registerTask(Task task)
	{
		if (task == null) return;
		try
		{
			activeTasks.add(task);
			task.start();
			notifyTaskRegistered(task);
		}
		catch (Exception e) { log.warn("Failed to register task", e); }
	}

	public void unregisterTask(Task task)
	{
		if (task == null) return;
		try
		{
			activeTasks.remove(task);
			task.stop();
			notifyTaskUnregistered(task);
		}
		catch (Exception e) { 
			log.warn("Failed to unregister task", e); }
	}

	public List<Task> getActiveTasks()
	{
		return Collections.unmodifiableList(new ArrayList<>(activeTasks));
	}

	@SuppressWarnings("unchecked")
	public <T extends Task> Optional<T> getTaskByType(Class<T> type)
	{
		return activeTasks.stream()
			.filter(type::isInstance)
			.map(t -> (T) t)
			.findFirst();
	}

	public void addTaskStateListener(TaskStateListener listener)
	{
		if (listener != null) listeners.add(listener);
	}

	public void removeTaskStateListener(TaskStateListener listener)
	{
		listeners.remove(listener);
	}

	private void notifyTaskRegistered(Task task)
	{
		for (TaskStateListener l : listeners)
		{
			try { l.onTaskRegistered(task); }
			catch (Exception e) { log.warn("TaskStateListener error", e); }
		}
	}

	private void notifyTaskUnregistered(Task task)
	{
		for (TaskStateListener l : listeners)
		{
			try { l.onTaskUnregistered(task); }
			catch (Exception e) { log.warn("TaskStateListener error", e); }
		}
	}

	public void notifyTaskStateChanged(Task task)
	{
		for (TaskStateListener l : listeners)
		{
			try { l.onTaskStateChanged(task); }
			catch (Exception e) { log.warn("TaskStateListener error", e); }
		}
	}

	public interface TaskStateListener
	{
		default void onTaskRegistered(Task task) {}
		default void onTaskUnregistered(Task task) {}
		default void onTaskStateChanged(Task task) {}
	}
}
