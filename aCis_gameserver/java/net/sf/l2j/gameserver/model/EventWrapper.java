/*
 * Copyright (C) 2004-2021 L2J Server
 * This file is part of L2J Server.
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.enums.WrapperType;

/**
 * @author Rationale
 */
public class EventWrapper
{
	private static final Logger LOGGER = Logger.getLogger(EventWrapper.class.getName());

	private final Map<WrapperType, EventWrapperBuilder> _wrappers = new ConcurrentHashMap<>();
	private final Map<String, List<Future<?>>> _async = new ConcurrentHashMap<>();

	private WrapperType _wrapperType = WrapperType.PREPARE;

	protected final void addWrapper(final WrapperType type, final EventWrapperBuilder builder)
	{
		_wrappers.put(type, builder);
	}

	protected final void removeWrapper(final WrapperType type)
	{
		_wrappers.remove(type);
	}

	protected final WrapperType getWrapperType()
	{
		return _wrapperType;
	}

	protected final void setWrapperType(final WrapperType type)
	{
		_wrapperType = Objects.requireNonNullElse(type, WrapperType.PREPARE);
	}

	/**
	 * @param type
	 * @return {@code EventWrapperBuilder}
	 */
	protected final EventWrapperBuilder getWrapper(final WrapperType type)
	{
		return _wrappers.get(type);
	}

	protected void executeSyncWrapper(final WrapperType type)
	{
		try
		{
			if (_wrappers.containsKey(type))
			{
				_wrappers.get(type).GET().forEach(s -> _async.computeIfAbsent(type.name(), k -> new ArrayList<>()).add(s));
			}
		}
		catch (final Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to execute. " + e);

			onExecuteFailure();
		}

		setWrapperType(type);
	}

	protected void onExecuteFailure()
	{

	}

	/**
	 * Schedules an a-sync task to be executed after the given {@code Duration}.<BR>
	 * In case {@code Duration} is zero it will execute the task instantly.
	 *
	 * @param name
	 * @param task
	 * @param duration
	 */
	protected final void scheduleAsync(final String name, final Runnable task, final Duration duration)
	{
		scheduleAsync(name, task, duration, true);
	}

	/**
	 * Schedules an a-sync task to be executed after the given {@code Duration}.<BR>
	 * In case {@code Duration} is zero it will execute the task instantly.
	 *
	 * @param name
	 * @param task
	 * @param duration
	 * @param cancelPreviousAsync
	 */
	protected final void scheduleAsync(final String name, final Runnable task, final Duration duration, final boolean cancelPreviousAsync)
	{
		if (cancelPreviousAsync)
		{
			cancelAsync(name);
		}

		if (!duration.isZero())
		{
			_async.computeIfAbsent(name, k -> new ArrayList<>()).add(ThreadPool.schedule(task, duration.toMillis()));
		}
		else
		{
			ThreadPool.execute(task);
		}
	}

	/**
	 * Schedules an a-sync task to be executed after the given {@code Duration}.<BR>
	 * Repeat the task every {@code Duration}
	 *
	 * @param name
	 * @param task
	 * @param duration
	 */
	protected final void scheduleAsyncAtFixedRate(final String name, final Runnable task, final Duration duration)
	{
		_async.computeIfAbsent(name, k -> new ArrayList<>()).add(ThreadPool.scheduleAtFixedRate(task, duration.toMillis(), duration.toMillis()));
	}

	/**
	 * Cancel all given scheduled tasks associated with given {@code name}
	 *
	 * @param name
	 */
	protected final void cancelAsync(final String name)
	{
		_async.getOrDefault(name, Collections.emptyList()).stream().filter(Objects::nonNull).forEach(s -> s.cancel(true));

		_async.remove(name);
	}

	protected final void cancelAllAsync()
	{
		_async.keySet().forEach(this::cancelAsync);
	}
}
