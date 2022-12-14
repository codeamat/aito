/*
 * Copyright (C) 2004-2020 L2J Server
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
package net.sf.l2j.gameserver.enums;

/**
 * @author UnAfraid
 */
public enum PlayerAction
{
	ADMIN_COMMAND, USER_ENGAGE, EVENT_RECONNECT, EVENT_REGISTER, EVENT_OBSERVE, DONATE_SERVICE, DELETE_PROFIL, PARTY_INVITE, SELL_BUFF, GENERATE_COLLECTOR, CONSUME_COLLECTOR, DELETE_TWITCH, CANCEL_DAILY_MISSION, UNLOCK_CRAFT;

	private final int _mask;

	private PlayerAction()
	{
		_mask = 1 << ordinal();
	}

	public int getMask()
	{
		return _mask;
	}
}
