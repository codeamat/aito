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
package net.sf.l2j.gameserver.model.missions;

import java.util.Calendar;
import java.util.List;

import net.sf.l2j.commons.data.StatSet;

import net.sf.l2j.gameserver.enums.MissionType;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.missions.events.impl.MissionEventType;
import net.sf.l2j.gameserver.model.missions.events.impl.character.player.OnMissionEventLogin;

/**
 * @author Rationale
 */
public class MissionLogin extends AbstractDailyMission<OnMissionEventLogin>
{
	private final int _day;
	private final int _count;

	public MissionLogin(final int id, final String name, final MissionType type, final List<IntIntHolder> items, final StatSet set)
	{
		super(id, name, type, items);

		_day = set.getInteger("day", 0);
		_count = set.getInteger("count", 0);
	}

	@Override
	public void onAsyncEvent(final OnMissionEventLogin event)
	{
		if (_day != 0 && Calendar.getInstance().get(Calendar.DAY_OF_WEEK) != _day)
		{
			return;
		}

		if (_count != 0 && event.getActiveChar().getMission().increaseCounter(getId()) < _count)
		{
			return;
		}

		onComplete(event.getActiveChar());
	}

	@Override
	public MissionEventType getEventType()
	{
		return MissionEventType.ON_PLAYER_LOGIN;
	}

}
