package net.sf.l2j.gameserver.data.manager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.SysUtil;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.model.Broadcast;
import net.sf.l2j.gameserver.model.Race;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.type.subtype.ZoneType;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Rationale
 */
public class RaceData extends StatSet implements IXmlReader, IVoicedCommandHandler
{
	private static final long serialVersionUID = 1L;

	private static final List<RaceTemplate> TEMPLATE = new ArrayList<>();
	private static final List<IntIntHolder> REWARD = new ArrayList<>();

	private static final AtomicReference<Race> RACE = new AtomicReference<>();
	
	public RaceData()
	{
		load();
		
		setNext();
						
		VoicedCommandHandler.getInstance().registerHandler(this);
	}
	
	@Override
	public void load()
	{
		TEMPLATE.clear();

		parseFile("data/xml/custom/race.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + TEMPLATE.size() + " templates");
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
			else if ("rewards".equalsIgnoreCase(node.getNodeName()))
			{
				for (Node sub_node = node.getFirstChild(); sub_node != null; sub_node = sub_node.getNextSibling())
				{
					if ("item".equalsIgnoreCase(sub_node.getNodeName()))
					{
						REWARD.add(new IntIntHolder(parseInteger(sub_node.getAttributes(), "id"), parseInteger(sub_node.getAttributes(), "count", 1)));
					}
				}
			}
			else if ("template".equalsIgnoreCase(node.getNodeName()))
			{
				for (Node sub_node = node.getFirstChild(); sub_node != null; sub_node = sub_node.getNextSibling())
				{
					if ("race".equalsIgnoreCase(sub_node.getNodeName()))
					{
						final int mountId = parseInteger(sub_node.getAttributes(), "mountId");
						
						final String startRange = parseString(sub_node.getAttributes(), "start_range").replaceAll("\\s", "");
						final String endRange = parseString(sub_node.getAttributes(), "start_range").replaceAll("\\s", "");
						
						final List<Location> list = new ArrayList<>();
						
						for (Node checkpoint_node = sub_node.getFirstChild(); checkpoint_node != null; checkpoint_node = checkpoint_node.getNextSibling())
						{
							if ("checkpoint".equalsIgnoreCase(checkpoint_node.getNodeName()))
							{
								list.add(new Location(parseInteger(checkpoint_node.getAttributes(), "x"), parseInteger(checkpoint_node.getAttributes(), "y"), parseInteger(checkpoint_node.getAttributes(), "z")));
							}
						}
						
						final Location startRangeLocation = new Location(Integer.parseInt(startRange.split(",")[0]), Integer.parseInt(startRange.split(",")[1]), Integer.parseInt(startRange.split(",")[2]));
						final Location endRangeLocation = new Location(Integer.parseInt(endRange.split(",")[0]), Integer.parseInt(endRange.split(",")[1]), Integer.parseInt(endRange.split(",")[2]));
						
						TEMPLATE.add(new RaceTemplate(new Location[] {startRangeLocation, endRangeLocation}, list, mountId));
					}
				}
			}
		}
	}
	
	public void onZoneEnter(final Player player, final ZoneType type)
	{
		if (Objects.nonNull(RACE.get()) && RACE.get().isActive())
		{
			RACE.get().onZoneEnter(player, type);
		}
	}
	
	public void setNext()
	{		
		if (Objects.nonNull(RACE.get()))
		{
			RACE.get().finish();
		}
		
		long closest = 0;

		for (final String date : getString("schedule", "").replaceAll("\\s", "").split(";"))
		{
			final long scheduled = SysUtil.parseWeeklyDate(date, true);

			if (closest <= System.currentTimeMillis() || closest > scheduled)
			{
				closest = scheduled;
			}
		}
		
		ThreadPool.schedule(() -> setRace(new Race(TEMPLATE.get(Rnd.get(TEMPLATE.size())))), closest - System.currentTimeMillis());
		
		System.out.println(closest - System.currentTimeMillis());
		Broadcast.toAllOnlinePlayers("[Te Arii Race] Prochaine course dans: " + SysUtil.formatMillisToTime(closest - System.currentTimeMillis()));
	}
	
	public void setRace(final Race race)
	{
		RACE.set(race);
	}
	
	public List<IntIntHolder> getRewards()
	{
		return REWARD;
	}
	
	private static final String[] VOICED_COMMAND = { "402" };
	
	@Override
	public void useVoicedCommand(String command, final String params, Player player)
	{
		System.out.println("test");
		if (Objects.isNull(RACE.get()))
		{
			player.sendMessage("Il n'y a aucune course.");
		}
		else if (!RACE.get().isRegistering())
		{
			player.sendMessage("La course a deja commence.");
		}
		else if (RACE.get().isParticipant(player))
		{
			RACE.get().removePlayer(player);
		}
		else
		{
			RACE.get().registerPlayer(player);
		}
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMAND;
	}

	//@formatter:off
	public static record RaceTemplate (Location[] getStartingLocation, List<Location> getLocations, int getMountNpcId) {}
	//@formatter:on
	
	public static RaceData getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final RaceData _instance = new RaceData();
	}
}
