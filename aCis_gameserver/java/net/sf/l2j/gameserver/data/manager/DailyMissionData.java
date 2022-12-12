/*
 * Copyright (C) 2004-2019 L2J Server
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
package net.sf.l2j.gameserver.data.manager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.enums.MissionType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.missions.AbstractDailyMission;
import net.sf.l2j.gameserver.model.missions.MissionStateType;
import net.sf.l2j.gameserver.model.missions.events.impl.IMissionEvent;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Rationale
 */
public class DailyMissionData extends StatSet implements IXmlReader
{
	private static final long serialVersionUID = 1L;
	
	private static final List<AbstractDailyMission<?>> MISSION = new ArrayList<>();

	public DailyMissionData()
	{
		load();
	}

	@Override
	public void load()
	{
		MISSION.clear();

		parseFile("./data/xml/custom/dailyMission.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded: " + MISSION.size() + " mission holders.");
	}

	@Override
	public void parseDocument(final Document doc, final Path path)
	{
		for (Node node = doc.getFirstChild().getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ("settings".equalsIgnoreCase(node.getNodeName()))
			{
				for (Node sub_node = node.getFirstChild(); sub_node != null; sub_node = sub_node.getNextSibling())
				{
					if ("set".equalsIgnoreCase(sub_node.getNodeName()))
					{
						set(sub_node.getAttributes().getNamedItem("name").getNodeValue().trim(), sub_node.getAttributes().getNamedItem("val").getNodeValue().trim());
					}
				}
			}
			else if ("missions".equalsIgnoreCase(node.getNodeName()))
			{
				for (Node mission_node = node.getFirstChild(); mission_node != null; mission_node = mission_node.getNextSibling())
				{
					if ("mission".equalsIgnoreCase(mission_node.getNodeName()))
					{
						final Integer id = parseInteger(mission_node.getAttributes(), "id");

						if (Objects.isNull(id))
						{
							LOGGER.warn(getClass().getSimpleName() + ": Failed to find id tag for mission. Skipping.");
							continue;
						}

						final String name = parseString(mission_node.getAttributes(), "name");

						if (Objects.isNull(name))
						{
							LOGGER.warn(getClass().getSimpleName() + ": Failed to find name tag for mission. Skipping.");
							continue;
						}

						final StatSet set = new StatSet();

						for (Node parameter_node = mission_node.getFirstChild(); parameter_node != null; parameter_node = parameter_node.getNextSibling())
						{
							if ("parameter".equalsIgnoreCase(parameter_node.getNodeName()))
							{
								for (Node sub_parameter_node = parameter_node.getFirstChild(); sub_parameter_node != null; sub_parameter_node = sub_parameter_node.getNextSibling())
								{
									if ("param".equalsIgnoreCase(sub_parameter_node.getNodeName()))
									{
										set.set(sub_parameter_node.getAttributes().getNamedItem("name").getNodeValue().trim(), sub_parameter_node.getAttributes().getNamedItem("val").getNodeValue().trim());
									}
								}
							}
						}

						final List<IntIntHolder> list = new ArrayList<>();

						for (Node item_node = mission_node.getFirstChild(); item_node != null; item_node = item_node.getNextSibling())
						{
							if ("items".equalsIgnoreCase(item_node.getNodeName()))
							{
								for (Node sub_item_node = item_node.getFirstChild(); sub_item_node != null; sub_item_node = sub_item_node.getNextSibling())
								{
									if ("item".equalsIgnoreCase(sub_item_node.getNodeName()))
									{
										list.add(new IntIntHolder(parseInteger(sub_item_node.getAttributes(), "id"), parseInteger(sub_item_node.getAttributes(), "count", 1)));
									}
								}
							}
						}

						try
						{
							MISSION.add((AbstractDailyMission<?>) Class.forName("net.sf.l2j.gameserver.model.missions.Mission" + parseString(mission_node.getAttributes(), "script")).getConstructor(int.class, String.class, MissionType.class, List.class, StatSet.class).newInstance(id, name, parseEnum(mission_node.getAttributes(), MissionType.class, "type", MissionType.SINGLE), list, set));
						}
						catch (final Exception e)
						{
							LOGGER.warn(getClass().getSimpleName() + ": Failed to initialize mission of id: " + id + ". Skipping.");
						}
					}
				}
			}
		}
	}

	/**
	 * @param event
	 * @param activeChar
	 */
	public void notifyEventAsync(final IMissionEvent event, final Player activeChar)
	{
		ThreadPool.execute(() -> notifyEventSync(event, activeChar));
	}

	/**
	 * @param event
	 * @param activeChar
	 */
	public void notifyEventSync(final IMissionEvent event, final Player activeChar)
	{
		activeChar.getMission().getMissions().stream().filter(s -> s.getEventType() == event.getType()).filter(s -> activeChar.getMission().getMissionState(s) == MissionStateType.ACTIVE).forEach(s -> s.onAsyncEvent(event));
	}

	/**
	 * @param id
	 * @return {@code AbstractDailyMission}
	 */
	public AbstractDailyMission<?> getMission(final int id)
	{
		return MISSION.stream().filter(s -> s.getId() == id).findFirst().orElse(null);
	}

	/**
	 * @return {@code list} of all {@code AbstractDailyMission}
	 */
	public List<AbstractDailyMission<?>> getMissions()
	{
		return MISSION;
	}

	public static DailyMissionData getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		private static DailyMissionData _instance = new DailyMissionData();
	}
}
