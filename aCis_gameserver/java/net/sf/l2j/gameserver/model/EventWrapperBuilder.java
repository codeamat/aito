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
import java.util.List;
import java.util.concurrent.Future;

import net.sf.l2j.commons.pool.ThreadPool;

/**
 * @author Rationale
 */
public class EventWrapperBuilder
{
	private final List<WrapperHolder> _wrappers = new ArrayList<>();

	public EventWrapperBuilder addSync(final Runnable task)
	{
		_wrappers.add(new WrapperHolder(task, Duration.ZERO));
		return this;
	}

	public EventWrapperBuilder addAsync(final Runnable task, final Duration duration)
	{
		_wrappers.add(new WrapperHolder(task, duration));
		return this;
	}

	/**
	 * @return {@code List<Future<?>>} of all {@code Future}
	 */
	public List<Future<?>> GET()
	{
		final List<Future<?>> list = new ArrayList<>();

		for (final WrapperHolder wrapper : getWrappers())
		{
			if (!wrapper.getDuration().isZero())
			{
				list.add(ThreadPool.schedule(wrapper.getTask(), wrapper.getDuration().toMillis()));
			}
			else
			{
				wrapper.getTask().run();
			}
		}

		return list;
	}

	/**
	 * @param builder
	 * @return {@code EventWrapperBuilder}
	 */
	public EventWrapperBuilder COMBINE(final EventWrapperBuilder builder)
	{
		_wrappers.addAll(builder.getWrappers());
		return this;
	}

	/**
	 * @return {@code List<WrapperHolder>} of all {@code WrapperHolder}
	 */
	public List<WrapperHolder> getWrappers()
	{
		return _wrappers;
	}

	//@formatter:off
	private static record WrapperHolder (Runnable getTask, Duration getDuration) {}
	//@formatter:on

	public static EventWrapperBuilder of()
	{
		return new EventWrapperBuilder();
	}
}
