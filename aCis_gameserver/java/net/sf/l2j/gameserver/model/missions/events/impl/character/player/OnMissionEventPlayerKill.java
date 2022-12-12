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
package net.sf.l2j.gameserver.model.missions.events.impl.character.player;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.missions.events.impl.IMissionEvent;
import net.sf.l2j.gameserver.model.missions.events.impl.MissionEventType;

/**
 * @author Rationale
 */
public class OnMissionEventPlayerKill implements IMissionEvent
{
	private final Player _activeChar;
	private final Player _target;

	public OnMissionEventPlayerKill(final Player activeChar, final Player target)
	{
		_activeChar = activeChar;
		_target = target;
	}

	public Player getActiveChar()
	{
		return _activeChar;
	}

	public Player getTarget()
	{
		return _target;
	}

	@Override
	public MissionEventType getType()
	{
		return MissionEventType.ON_PLAYER_KILL;
	}
}
