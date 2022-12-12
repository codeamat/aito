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

import java.util.List;

import net.sf.l2j.commons.data.StatSet;

import net.sf.l2j.gameserver.enums.MissionType;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.missions.events.impl.MissionEventType;
import net.sf.l2j.gameserver.model.missions.events.impl.character.player.OnMissionEventPlayerKill;

/**
 * @author Rationale
 */
public class MissionPlayerKill extends AbstractDailyMission<OnMissionEventPlayerKill>
{
	private final boolean _isHero;
	private final boolean _isPk;
	private final boolean _isPvP;
	private final boolean _allowSelf;

	private final int _count;

	public MissionPlayerKill(final int id, final String name, final MissionType type, final List<IntIntHolder> items, final StatSet set)
	{
		super(id, name, type, items);

		_isHero = set.getBool("isHero", false);
		_isPk = set.getBool("isPk", false);
		_isPvP = set.getBool("isPvP", false);
		_allowSelf = set.getBool("allowSelf", false);
		_count = Math.max(1, set.getInteger("count", Integer.MAX_VALUE));
		//_zoneId = set.containsKey("zoneId") ? set.getIntegerList("zoneId", ",") : Collections.emptyList();
	}

	@Override
	public void onAsyncEvent(final OnMissionEventPlayerKill event)
	{
		if (_isHero && !event.getTarget().isHero())
		{
			return;
		}

		if (_isPk && event.getTarget().getKarma() <= 0 || _isPvP && event.getTarget().getPvpFlag() == 0)
		{
			return;
		}

		if (!_allowSelf && event.getActiveChar().getObjectId() == event.getTarget().getObjectId())
		{
			return;
		}

		if (event.getActiveChar().getMission().increaseCounter(getId()) >= _count)
		{
			onComplete(event.getActiveChar());
		}
	}

	@Override
	public int getStatus()
	{
		return _count;
	}

	@Override
	public MissionEventType getEventType()
	{
		return MissionEventType.ON_PLAYER_KILL;
	}

}
