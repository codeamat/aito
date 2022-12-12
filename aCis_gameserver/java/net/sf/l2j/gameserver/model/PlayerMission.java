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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.data.manager.DailyMissionData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.missions.AbstractDailyMission;
import net.sf.l2j.gameserver.model.missions.MissionStateType;
import net.sf.l2j.gameserver.model.missions.events.impl.IMissionEvent;

/**
 * @author Rationale
 */
public class PlayerMission
{
	private static final Logger _log = Logger.getLogger(PlayerMission.class.getName());

	// Queries
	private static final String SELECT_MISSION = "SELECT * FROM character_daily_mission WHERE charId=?";
	private static final String DELETE_MISSION = "DELETE FROM character_daily_mission WHERE charId=?";
	private static final String INSERT_MISSION = "INSERT INTO character_daily_mission (charId, id, counter) VALUES (?,?,?)";

	private static final String SELECT_REUSE = "SELECT * FROM character_daily_mission_reuse WHERE charId=?";
	private static final String DELETE_REUSE = "DELETE FROM character_daily_mission_reuse WHERE charId=?";
	private static final String INSERT_REUSE = "INSERT INTO character_daily_mission_reuse (charId, id, reuse) VALUES (?,?,?)";

	private static final List<MissionStateType> STATE_CONDITION = List.of(MissionStateType.ACTIVE, MissionStateType.CANNOT_TAKE_AGAIN, MissionStateType.NOT_READY, MissionStateType.NOT_QUALIFIED);

	public static final int SINGLE_COMPLETE = -1;
	public static final int PENDING_REWARD = -2;

	private final Player _activeChar;

	private final Map<Integer, Long> _reuseDelay = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _counter = new ConcurrentHashMap<>();
	private final Map<Integer, AbstractDailyMission<IMissionEvent>> _missions = new ConcurrentHashMap<>();

	public PlayerMission(final Player activeChar)
	{
		_activeChar = activeChar;

		restoreMe();
	}

	public void setReuseDelay(final int id, final long val)
	{
		_reuseDelay.put(id, val);
	}

	public void removeReuseDelay(final int id)
	{
		_reuseDelay.remove(id);
	}

	public long getReuseDelay(final int id)
	{
		return _reuseDelay.getOrDefault(id, 0L);
	}

	public int increaseCounter(final int id)
	{
		return _counter.merge(id, 1, Integer::sum);
	}

	public int addCounter(final int id, final int val)
	{
		return _counter.merge(id, val, Integer::sum);
	}

	public int getCounter(final int id)
	{
		return _counter.getOrDefault(id, 0);
	}

	public void removeCounter(final int id)
	{
		_counter.remove(id);
	}

	public void addMission(final AbstractDailyMission<IMissionEvent> mission)
	{
		_missions.put(mission.getId(), mission);
	}

	public void removeMission(final int id)
	{
		_missions.remove(id);
	}

	/**
	 * @param mission
	 * @return {@value true} if {@code L2PcInstance} has {@code AbstractDailyMission}
	 */
	public boolean hasMission(final AbstractDailyMission<?> mission)
	{
		return Objects.nonNull(mission) && _missions.containsKey(mission.getId());
	}

	/**
	 * @return {@code Collections} of all {@code AbstractDailyMission}
	 */
	public Collection<AbstractDailyMission<IMissionEvent>> getMissions()
	{
		return _missions.values();
	}

	/**
	 * @param mission
	 * @return {@code MissionStateType}
	 */
	public MissionStateType getMissionState(final AbstractDailyMission<?> mission)
	{
		return STATE_CONDITION.stream().filter(s -> s.checkCondition(_activeChar, mission)).findFirst().orElse(MissionStateType.AVAILABLE);
	}

	public boolean storeMe()
	{
		try (Connection con = ConnectionPool.getConnection())
		{
			try (PreparedStatement st = con.prepareStatement(DELETE_MISSION))
			{
				st.setInt(1, _activeChar.getObjectId());
				st.execute();
			}

			try (PreparedStatement st = con.prepareStatement(INSERT_MISSION))
			{
				for (final AbstractDailyMission<?> mission : getMissions())
				{
					st.setInt(1, _activeChar.getObjectId());
					st.setInt(2, mission.getId());
					st.setLong(3, getCounter(mission.getId()));
					st.addBatch();
				}

				st.executeBatch();
			}

			try (PreparedStatement st = con.prepareStatement(DELETE_REUSE))
			{
				st.setInt(1, _activeChar.getObjectId());
				st.execute();
			}

			try (PreparedStatement st = con.prepareStatement(INSERT_REUSE))
			{
				for (final Entry<Integer, Long> entry : _reuseDelay.entrySet())
				{
					st.setInt(1, _activeChar.getObjectId());
					st.setInt(2, entry.getKey());
					st.setLong(3, entry.getValue());
					st.addBatch();
				}

				st.executeBatch();
			}
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Failed to store mission for player: " + _activeChar, e);
			return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean restoreMe()
	{
		try (Connection con = ConnectionPool.getConnection())
		{
			try (PreparedStatement st = con.prepareStatement(SELECT_MISSION))
			{
				st.setInt(1, _activeChar.getObjectId());

				try (ResultSet rs = st.executeQuery())
				{
					while (rs.next())
					{
						final AbstractDailyMission<?> mission = DailyMissionData.getInstance().getMission(rs.getInt(2));

						if (Objects.nonNull(mission))
						{
							addMission((AbstractDailyMission<IMissionEvent>) mission);
							addCounter(mission.getId(), rs.getInt(3));
						}
					}
				}
			}

			try (PreparedStatement st = con.prepareStatement(SELECT_REUSE))
			{
				st.setInt(1, _activeChar.getObjectId());

				try (ResultSet rs = st.executeQuery())
				{
					while (rs.next())
					{
						setReuseDelay(rs.getInt(2), rs.getLong(3));
					}
				}
			}
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Failed to restore mission for player: " + _activeChar, e);
			return false;
		}

		return true;
	}
}
