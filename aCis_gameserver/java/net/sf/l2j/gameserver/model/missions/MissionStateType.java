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

import net.sf.l2j.gameserver.model.PlayerMission;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author Rationale
 */
public enum MissionStateType
{
	ACTIVE
	{
		@Override
		public boolean checkCondition(final Player activeChar, final AbstractDailyMission<?> mission)
		{
			return activeChar.getMission().hasMission(mission);
		}
	},
	NOT_QUALIFIED
	{
		@Override
		public boolean checkCondition(final Player activeChar, final AbstractDailyMission<?> mission)
		{
			return !mission.isAllowed(activeChar);
		}
	},
	NOT_READY
	{
		@Override
		public boolean checkCondition(final Player activeChar, final AbstractDailyMission<?> mission)
		{
			return switch (mission.getType())
			{
				case SINGLE -> false;
				default -> activeChar.getMission().getReuseDelay(mission.getId()) > System.currentTimeMillis();
			};
		}
	},
	CANNOT_TAKE_AGAIN
	{
		@Override
		public boolean checkCondition(final Player activeChar, final AbstractDailyMission<?> mission)
		{
			return switch (mission.getType())
			{
				case SINGLE -> activeChar.getMission().getReuseDelay(mission.getId()) == PlayerMission.SINGLE_COMPLETE;
				default -> false;
			};
		}
	},
	AVAILABLE
	{
		@Override
		public boolean checkCondition(final Player activeChar, final AbstractDailyMission<?> mission)
		{
			return false;
		}
	};

	public abstract boolean checkCondition(final Player activeChar, final AbstractDailyMission<?> mission);
}
