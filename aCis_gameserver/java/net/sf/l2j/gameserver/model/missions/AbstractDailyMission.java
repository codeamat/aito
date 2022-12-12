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

import net.sf.l2j.gameserver.data.manager.DailyMissionData;
import net.sf.l2j.gameserver.enums.MissionType;
import net.sf.l2j.gameserver.model.PlayerMission;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.missions.events.impl.IMissionEvent;
import net.sf.l2j.gameserver.model.missions.events.impl.MissionEventType;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;

/**
 * @author Rationale
 * @param <T>
 */
public abstract class AbstractDailyMission<T extends IMissionEvent>
{
	private final int _id;
	private final String _name;

	private final MissionType _type;

	private final List<IntIntHolder> _items;

	public AbstractDailyMission(final int id, final String name, final MissionType type, final List<IntIntHolder> items)
	{
		_id = id;
		_name = name;
		_type = type;
		_items = items;
	}

	public int getId()
	{
		return _id;
	}

	public String getName()
	{
		return _name;
	}

	public MissionType getType()
	{
		return _type;
	}

	public List<IntIntHolder> getItems()
	{
		return _items;
	}

	public boolean isAllowed(final Player activeChar)
	{
		return true;
	}

	public int getStatus()
	{
		return 1;
	}

	protected void onComplete(final Player activeChar)
	{
		activeChar.getMission().removeMission(getId());
		activeChar.getMission().removeCounter(getId());

		activeChar.getMission().setReuseDelay(getId(), PlayerMission.PENDING_REWARD);

		if (DailyMissionData.getInstance().getBool("soundEffectEnabled", false))
		{
			activeChar.sendPacket(new PlaySound("ItemSound.quest_finish"));
		}

		activeChar.sendMessage("[DailyMission] " + getName() + " is completed. You can receive your reward.");
	}

	public abstract void onAsyncEvent(final T event);

	public abstract MissionEventType getEventType();
}
