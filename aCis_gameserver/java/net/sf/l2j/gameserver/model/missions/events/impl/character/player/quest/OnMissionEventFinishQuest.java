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
package net.sf.l2j.gameserver.model.missions.events.impl.character.player.quest;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.missions.events.impl.IMissionEvent;
import net.sf.l2j.gameserver.model.missions.events.impl.MissionEventType;
import net.sf.l2j.gameserver.scripting.Quest;

/**
 * @author Rationale
 */
public class OnMissionEventFinishQuest implements IMissionEvent
{
	private final Player _activeChar;
	private final Quest _quest;

	public OnMissionEventFinishQuest(final Player activeChar, final Quest quest)
	{
		_activeChar = activeChar;
		_quest = quest;
	}

	public Player getActiveChar()
	{
		return _activeChar;
	}

	public Quest getQuest()
	{
		return _quest;
	}

	@Override
	public MissionEventType getType()
	{
		return MissionEventType.ON_PLAYER_QUEST_FINISH;
	}
}
