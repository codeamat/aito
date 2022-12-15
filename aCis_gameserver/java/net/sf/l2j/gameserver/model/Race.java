package net.sf.l2j.gameserver.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.commons.util.SysUtil;

import net.sf.l2j.gameserver.data.manager.RaceData;
import net.sf.l2j.gameserver.data.manager.RaceData.RaceTemplate;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.enums.WrapperType;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.form.ZoneCylinder;
import net.sf.l2j.gameserver.model.zone.type.RaceZone;
import net.sf.l2j.gameserver.model.zone.type.subtype.ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author Rationale
 */
public class Race extends EventWrapper
{
	private final RaceTemplate _template;
	
	private final List<Player> _players = new CopyOnWriteArrayList<>();
	private final List<Npc> _npcs = new CopyOnWriteArrayList<>();
	private final List<RaceZoneHolder> _zones = new ArrayList<>();
	
	private final Map<Integer, Integer> _checkPoint = new ConcurrentHashMap<>();
	
	private final int _instanceId;
	
	public Race(final RaceTemplate template)
	{
		_template = template;
		_instanceId = IdFactory.getInstance().getNextId();
		
		//@formatter:off
		addWrapper(WrapperType.CHECK, EventWrapperBuilder.of().
			addSync(this::checkRegistration));

		addWrapper(WrapperType.START, EventWrapperBuilder.of().addSync(this::start));

		addWrapper(WrapperType.FINISH, EventWrapperBuilder.of().addSync(this::finish));
		//@formatter:on

		Broadcast.toAllOnlinePlayers("[Te Arii Race] Iaorana! Debut des inscriptions pour la course!");
		Broadcast.toAllOnlinePlayers("[Te Arii Race] Command: (.402)");
		Broadcast.toAllOnlinePlayers("[Te Arii Race] Parcours:");
		Broadcast.toAllOnlinePlayers("1 ere Etape: Papeete - Taravao");
		Broadcast.toAllOnlinePlayers("2 eme Etape: Taravao - Papeete");

		scheduleAsync("CHECK", () -> executeSyncWrapper(WrapperType.CHECK), Duration.ofSeconds(RaceData.getInstance().getInteger("registrationInterval", 60)));	
	}
	
	public void start()
	{
		Broadcast.toPlayers(getPlayers(), "[Te Arii Race] AITO MA ! FAATOITO !", true);

		for (final Player player : getPlayers())
		{
			player.setIsImmobilized(false);
			player.disableAllSkills();
			
			showNextCheckPoint(player);
		}
	}
	
	public void finish()
	{
		cancelAllAsync();
		
		RaceData.getInstance().setRace(null);
		RaceData.getInstance().setNext();
		
		if (isRegistering())
		{
			Broadcast.toPlayers(getPlayers(), "[Te Arii Race] Evenement annule!", true);
		}
		else
		{
			Broadcast.toPlayers(getPlayers(), "[Te Arii Race] Fin de la course!!!", true);
			
			for (final Player player : getPlayers())
			{
				player.getRadarList().removeAllMarkers();
				
				player.setTeam(TeamType.NONE);
				player.broadcastUserInfo();
				
				player.sendMessage("[Te Arii Race] Teleportation dans 5 second(s).");
			}
			
			scheduleAsync("TELE_BACK", this::teleportAllBack, Duration.ofSeconds(5));
		}
		
		_npcs.forEach(s -> s.deleteMe());
		_npcs.clear();
		
		IdFactory.getInstance().releaseId(getInstanceId());
		
		scheduleAsync("CLEAN", () ->
		{
			for (final RaceZoneHolder holder : _zones)
			{
				ZoneManager.getInstance().removeZone(holder.getZone());
				
				for (WorldRegion[] regions : World.getInstance().getWorldRegions())
				{
					for (WorldRegion region : regions)
					{
						for (final ZoneType type : region.getZones())
						{
							if (type.getId() == holder.getZone().getId())
							{
								region.removeZone(type);
							}
						}
					}
				}
			}
		}, Duration.ofSeconds(5));
	}
	
	public void teleportAllBack()
	{
		for (final Player player : getPlayers()) 
		{
			player.setInstanceId(0);
			/*player.teleportTo(TeleportType.TOWN);*/
		Location loc = new Location(-5401,-43447,-3969);
			player.teleportTo(loc, 20);
			
			player.enableAllSkills();
			player.dismount();
			
			player.setIsImmobilized(false);
		}
	}
	
	public void registerPlayer(final Player player)
	{
		if (player.isInOlympiadMode())
		{
			player.sendMessage("[Te Arii Race] Tu ne peux pas participer en etant en Olympiad.");
			return;
		}
		
		getPlayers().add(player);
		
		player.sendMessage("[Te Arii Race] Tu es maintenant inscrit pour la course.");
	}
	
	public void removePlayer(final Player player)
	{
		getPlayers().remove(player);
		
		player.sendMessage("[Te Arii Race] Ta participation a bien ete retire.");
	}
	
	public boolean isParticipant(final Player player)
	{
		return getPlayers().contains(player);
	}
	
	public void checkRegistration()
	{
		if (getPlayers().size() < RaceData.getInstance().getInteger("minPlayers", 0))
		{
			Broadcast.toAllOnlinePlayers("[Te Arii Race] Aucun participant.");
			
			RaceData.getInstance().setRace(null);
			RaceData.getInstance().setNext();
			return;
		}
		
		for (final Location location : getTemplate().getLocations())
		{
			final RaceZone zone = new RaceZone(IdFactory.getInstance().getNextId());
			zone.setZone(new ZoneCylinder(location.getX(), location.getY(), location.getZ() - 50, location.getZ() + 50, RaceData.getInstance().getInteger("zoneRadius", 100)));
			_zones.add(new RaceZoneHolder(zone, location));
			
			ZoneManager.getInstance().addZone(zone.getId(), zone);
			
			final WorldRegion[][] regions = World.getInstance().getWorldRegions();
			
			for (int x = 0; x < regions.length; x++)
			{
				final int xLoc = World.getRegionX(x);
				final int xLoc2 = World.getRegionX(x + 1);
				
				for (int y = 0; y < regions[x].length; y++)
				{
					if (zone.getZone().intersectsRectangle(xLoc, xLoc2, World.getRegionY(y), World.getRegionY(y + 1)))
					{
						regions[x][y].addZone(zone);
					}
				}
			}
			
			_npcs.add(SysUtil.addSpawn(Npc.class, RaceData.getInstance().getInteger("npcId", 0), location.getX(), location.getY(), location.getZ(), getInstanceId()));
		}
		
		Broadcast.toAllOnlinePlayers("[Te Arii Race] Les inscriptions sont clos.");
		Broadcast.toPlayers(getPlayers(), "[Te Arii Race] La course va commencer dans " + RaceData.getInstance().getInteger("prepareInterval", 0) + " second(s).", true);
		
		scheduleAsync("START", () -> executeSyncWrapper(WrapperType.START), Duration.ofSeconds(RaceData.getInstance().getInteger("prepareInterval", 10)));
		
		final List<Location> list = getLocationsBetween(getTemplate().getStartingLocation()[0], getTemplate().getStartingLocation()[1], getPlayers().size());
		
		for (int i = 0; i < getPlayers().size(); i ++)
		{
			final Player player = getPlayers().get(i);
			final Location location = list.get(i);
			
			if (player.isMounted())
			{
				player.dismount();
			}
			
			player.getCast().stop();
			player.getAttack().stop();
			player.getMove().stop();
			
			player.mount(getTemplate().getMountNpcId(), 0);
			
			player.setIsImmobilized(true);
			player.setInstanceId(getInstanceId());
			player.teleToLocation(location);
			
			player.setTeam(TeamType.BLUE);
			player.broadcastUserInfo();
		}	
	}
	
	public void showNextCheckPoint(final Player player)
	{
		player.getRadarList().removeAllMarkers();
		
		final int index = _checkPoint.getOrDefault(player.getObjectId(), 0);
		
		if (index > _zones.size())
		{
			return;
		}
				
		player.getRadarList().addMarker(_zones.get(index).getLocation());
	}
	
	public void onZoneEnter(final Player player, final ZoneType zone)
	{
		if (!getPlayers().contains(player) || !isActive())
		{
			return;
		}
		
		final int index = _checkPoint.getOrDefault(player.getObjectId(), 0);
				
		final RaceZoneHolder holder = _zones.get(index);
		
		if (holder.getZone() != zone)
		{
			return;
		}
		
		final int newIndex = _checkPoint.merge(player.getObjectId(), 1, Integer::sum);
		
		if (newIndex >= _zones.size())
		{
			Broadcast.toPlayers(getPlayers(), "[Te Arii Race] "+"Vainqueur de la course: " +player.getName(), true);
			
			finish();
			
			RaceData.getInstance().getRewards().forEach(s -> player.addItem(getClass().getSimpleName(), s.getId(), s.getValue(), player, true));
		}
		else
		{
			showNextCheckPoint(player);
		
			player.sendPacket(new ExShowScreenMessage((index + 1) + " Maohi checked", 3000));
		}
	}
	
	public boolean isActive()
	{
		return getWrapperType() == WrapperType.START;
	}
	
	public boolean isRegistering()
	{
		return getWrapperType() == WrapperType.PREPARE;
	}
		
	public List<Player> getPlayers()
	{
		return _players;
	}
	
	public RaceTemplate getTemplate()
	{
		return _template;
	}
	
	public int getInstanceId()
	{
		return _instanceId;
	}
	
	/**
	 * Calculate locations between 2 locations.
	 *
	 * @param loc1 - First location.
	 * @param loc2 - Second location.
	 * @param size - Maximum location size
	 * @return {@code List} with all location between the two locations.
	 */
	private static List<Location> getLocationsBetween(final Location loc1, final Location loc2, final int size)
	{
		final int diffX = loc2.getX() - loc1.getX();
		final int diffY = loc2.getY() - loc1.getY();

		final int intervalX = diffX / (size + 1);
		final int intervalY = diffY / (size + 1);

		final List<Location> list = new ArrayList<>();

		for (int i = 0; i <= size; i++)
		{
			list.add(new Location(loc1.getX() + intervalX * i, loc1.getY() + intervalY * i, loc1.getZ()));
		}

		return list;
	}
	
	//@formatter:off
	public static record RaceZoneHolder (ZoneType getZone, Location getLocation) {}
	//@formatter::n
}
