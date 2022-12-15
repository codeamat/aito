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

import java.time.temporal.ValueRange;
import java.util.Collections;
import java.util.List;

import net.sf.l2j.commons.data.StatSet;

import net.sf.l2j.gameserver.enums.MissionType;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.missions.events.impl.MissionEventType;
import net.sf.l2j.gameserver.model.missions.events.impl.character.npc.OnMissionEventAttackableKill;

/**
 * @author Rationale
 */
public class MissionAttackableKill extends AbstractDailyMission<OnMissionEventAttackableKill>
{
	private final boolean _isRaid;
	private final int _count;

	private final ValueRange _levelRange;

	private List<Integer> _npcId;

	public MissionAttackableKill(final int id, final String name, final MissionType type, final List<IntIntHolder> items, final StatSet set)
	{
		super(id, name, type, items);

		_isRaid = set.getBool("isRaid", false);
		_count = Math.max(1, set.getInteger("count", Integer.MAX_VALUE));
		_levelRange = ValueRange.of(set.getLong("minLevel", Long.MIN_VALUE), set.getLong("maxLevel", Long.MAX_VALUE));
		//_npcId = set.containsKey("npcId") ? set.getIntegerList("npcId", ",") : Collections.emptyList();
	}

	@Override
	public void onAsyncEvent(final OnMissionEventAttackableKill event)
	{
		if (!_npcId.isEmpty() && !_npcId.contains(event.getAttackable().getNpcId()))
		{
			return;
		}

		if (_isRaid && !event.getAttackable().isRaidBoss())
		{
			return;
		}

		if (!_levelRange.isValidValue(event.getAttackable().getStatus().getLevel()))
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
		return MissionEventType.ON_ATTACKABLE_KILL;
	}

}
