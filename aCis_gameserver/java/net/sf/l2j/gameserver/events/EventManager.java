package net.sf.l2j.gameserver.events;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.events.enums.EventState;
import net.sf.l2j.gameserver.events.holder.EventHolder;
import net.sf.l2j.gameserver.events.holder.NpcHolder;
import net.sf.l2j.gameserver.events.holder.PlayerHolder;
import net.sf.l2j.gameserver.events.holder.RewardHolder;
import net.sf.l2j.gameserver.events.holder.TeamHolder;
import net.sf.l2j.gameserver.events.model.AbstractEvent;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;

import org.w3c.dom.Document;

/**
 * @author StinkyMadness
 */
public class EventManager implements IXmlReader, Runnable
{
	private final List<AbstractEvent> _events = new ArrayList<>();
	
	private final Set<Player> _participants = ConcurrentHashMap.newKeySet();
	
	private EventState _state = EventState.INACTIVE;
	private AbstractEvent _currentEvent = null;
	private int _timer = 0;
	
	private ScheduledFuture<?> _task;
	
	public EventManager()
	{
		load();
		selectNextEvent();
	}
	
	@Override
	public void load()
	{
		parseFile("./data/xml/custom/events.xml");
		LOGGER.info("Loaded {} events.", _events.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path p)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "event", eventNode ->
		{
			final String path = parseString(eventNode.getAttributes(), "class");
			if (path == null)
			{
				LOGGER.warn("One of the event path isn't defined.");
				return;
			}
			
			if (!parseBoolean(eventNode.getAttributes(), "load"))
				return;
			
			final StatSet set = new StatSet();
			forEach(eventNode, "settings", setNode -> set.putAll(parseAttributes(setNode)));
			
			final List<TeamHolder> teams = new ArrayList<>();
			forEach(eventNode, "team", teamsNode ->
			{
				final List<Location> spawns = new ArrayList<>();
				forEach(teamsNode, "spawn", spawnsNode ->
				{
					final StatSet spawn = parseAttributes(spawnsNode);
					spawns.add(new Location(spawn.getInteger("x"), spawn.getInteger("y"), spawn.getInteger("z")));
				});
				teams.add(new TeamHolder(parseAttributes(teamsNode), spawns));
			});
			
			final List<NpcHolder> npcs = new ArrayList<>();
			forEach(eventNode, "npc", npcNode ->
			{
				final List<SpawnLocation> spawns = new ArrayList<>();
				forEach(npcNode, "spawn", spawnsNode ->
				{
					final StatSet spawn = parseAttributes(spawnsNode);
					spawns.add(new SpawnLocation(spawn.getInteger("x"), spawn.getInteger("y"), spawn.getInteger("z"), spawn.getInteger("h", 0)));
				});
				npcs.add(new NpcHolder(parseAttributes(npcNode), spawns));
			});
			
			final List<RewardHolder> rewards = new ArrayList<>();
			forEach(eventNode, "reward", rewardNode -> rewards.add(new RewardHolder(parseAttributes(rewardNode))));
			
			try
			{
				// Create the event.
				final AbstractEvent event = (AbstractEvent) Class.forName("net.sf.l2j.gameserver.events.model." + path.replace(".java", "")).getDeclaredConstructor().newInstance();
				event.setHolder(new EventHolder(set, teams, npcs, rewards));
				
				// Add event and settings on map
				_events.add(event);
			}
			catch (Exception e)
			{
				LOGGER.error("Event '{}' is missing.", e, path);
			}
		}));
	}
	
	@Override
	public void run()
	{
		switch (_state)
		{
			case REGISTER:
				switch (_timer)
				{
					case 900:
					case 600:
					case 300:
					case 240:
					case 180:
					case 120:
					case 60:
						announceToAll(String.format("%s minute(s) avant la fin des Inscriptions.", _timer / 60));
						break;
					case 45:
					case 30:
					case 15:
					case 5:
						announceToAll(String.format("%s second(s) avant la fin des Inscriptions.", _timer));
						break;
					case 0:
						announceToAll("Les inscriptions sont fini.");
						if (_participants.size() < _currentEvent.getHolder().getCapacityLimit().getId())
						{
							announceToAll("Aucun joueur ne participe a l'event.");
							set(EventState.INACTIVE, 0); // Time to inactive till Prochain Event.
						}
						else
						{
							announceToAll(String.format("L'Event va commencer avec %s Joueur(s).", _participants.size()));
							set(EventState.TELEPORT, 11); // 10 seconds till the players will be teleported.
						}
						break;
				}
				break;
			case TELEPORT:
				switch (_timer)
				{
					case 10:
					case 3:
					case 2:
					case 1:
						sendMessage("Teleportation dans %s second(s).", _timer);
						break;
					case 0:
						_currentEvent.onTeleport();
						_currentEvent.getPlayers().forEach(PlayerHolder::freeze);
						set(EventState.STARTING, 11); // 10 seconds till the event start.
						break;
				}
				break;
			case STARTING:
				switch (_timer)
				{
					case 10:
					case 3:
					case 2:
					case 1:
						sendMessage("Debut de l'Event dans %s second(s).", _timer);
						break;
					case 0:
						_currentEvent.getPlayers().forEach(PlayerHolder::unfreeze);
						sendMessage("%s Aito! Hadjmeh.", _currentEvent.getHolder().getName());
						_currentEvent.onStarting();
						set(EventState.PROGRESS, _currentEvent.getHolder().getDuration() + 1); // Time that event will run.
						break;
				}
				break;
			case PROGRESS:
				_currentEvent.onProgress(_timer);
				_currentEvent.broadcastInfo();
				switch (_timer)
				{
					case 1200:
					case 600:
					case 300:
					case 240:
					case 180:
					case 120:
					case 60:
						sendMessage("%s minute(s) avant la fin de l'Event.", _timer / 60);
						break;
					case 30:
					case 15:
					case 10:
					case 3:
					case 2:
					case 1:
						sendMessage("%s second(s) avant la fin de l'Event.", _timer);
						break;
					case 0:
						sendMessage("%s vient de se terminer.", _currentEvent.getHolder().getName());
						_currentEvent.getPlayers().forEach(PlayerHolder::freeze);
						_currentEvent.calculateRewards();
						set(EventState.FINISH, 11); // 10 seconds till the event be inactive.
						break;
				}
				break;
			case FINISH:
				_currentEvent.onFinish(_timer);
				switch (_timer)
				{
					case 10:
					case 3:
					case 2:
					case 1:
						sendMessage("%s second(s) avant teleportation.", _timer);
						break;
					case 0:
						set(EventState.INACTIVE, 0);
						break;
				}
				break;
		}
		_timer--;
	}
	
	public void set(EventState state, int timer)
	{
		_state = state;
		_timer = timer;
		
		if (state == EventState.INACTIVE)
		{
			_currentEvent.cleanMe();
			_currentEvent = null;
			_participants.clear();
			
			if (_task != null)
			{
				_task.cancel(false);
				_task = null;
				selectNextEvent();
			}
		}
	}
	
	public void selectNextEvent()
	{
		if (_events.isEmpty())
			return;
		
		_currentEvent = _events.stream().sorted(Comparator.comparing(e -> e.getHolder().getNextSchedule())).findFirst().orElse(null);
		
		final long period = _currentEvent.getHolder().getNextSchedule();
		ThreadPool.schedule(() -> startNextEvent(), period);
		World.announceToOnlinePlayers(String.format("Event [ %s ] Time: %s", _currentEvent.getHolder().getName(), new SimpleDateFormat("dd-MM HH:mm").format(new Date(period + System.currentTimeMillis()))), true);
	}
	
	public void startNextEvent()
	{
		announceToAll("Les inscriptions sont ouvert!");
		announceToAll("Pour participer, taper la command: (.join)");
		announceToAll("Maururu et bon courage.");
		set(EventState.REGISTER, _currentEvent.getHolder().getRegistration());
		_task = ThreadPool.scheduleAtFixedRate(this, 0L, 1000L);
	}
	
	public void onLogin(Player player)
	{
		if (_state == EventState.INACTIVE || _currentEvent == null)
			return;
		
		_currentEvent.onLogin(player);
	}
	
	public AbstractEvent getCurrentEvent()
	{
		return _currentEvent;
	}
	
	public String getTimer()
	{
		if (_state == EventState.TELEPORT || _state == EventState.STARTING || _state == EventState.FINISH)
			return "00:00";
		
		return String.format("%02d:%02d", _timer / 60, _timer % 60);
	}
	
	public void register(Player player)
	{
		if (player == null)
			return;
		
		if (_currentEvent == null)
		{
			player.sendMessage("Aucun event en cours.");
			return;
		}
		
		if (_state != EventState.REGISTER)
		{
			player.sendMessage("Fin des inscriptions.");
			return;
		}
		
		if (_participants.contains(player))
		{
			player.sendMessage("Tu es deja inscrit.");
			return;
		}
		
		// HWID can be used here.
		
		if (_participants.size() >= _currentEvent.getHolder().getCapacityLimit().getValue())
		{
			player.sendMessage("L'Event est complet.");
			return;
		}
		
		final EventHolder info = _currentEvent.getHolder();
		if (info == null)
			return;
		
		if (info.isRestrictedLevel(player.getStatus().getLevel()))
		{
			player.sendMessage("Niveau " + info.getLevelLimit().getId() + " a " + info.getLevelLimit().getValue() + " requis.");
			return;
		}
		
		if (info.isRestrictedClass(player.getActiveClass()))
		{
			player.sendMessage("Ta class ne te permet pas de pouvoir participer a l'Event.");
			return;
		}
		
		_participants.add(player);
		player.sendMessage("Inscription reussi.");
		player.sendMessage("Pour quitter l'Event command: (.leave)");
		player.sendMessage("Maururu!");
	}
	
	public void unregister(Player player)
	{
		if (player == null)
			return;
		
		if (_currentEvent == null)
		{
			player.sendMessage("Aucun event en cours.");
			return;
		}
		
		if (_state != EventState.REGISTER)
		{
			player.sendMessage("Fin des inscriptions.");
			return;
		}
		
		if (!_participants.contains(player))
		{
			player.sendMessage("Tu n'es pas inscrit.");
			return;
		}
		
		_participants.remove(player);
		player.sendMessage("A la prochaine!");
	}
	
	public void announceToAll(String message)
	{
		World.announceToOnlinePlayers(String.format("%s: %s", _currentEvent.getHolder().getName(), message), true);
	}
	
	public void sendMessage(String message, Object... args)
	{
		_participants.forEach(player -> player.sendMessage("QoA_Event: " + String.format(message, args)));
	}
	
	public boolean isRegistred(Player player)
	{
		return _participants.contains(player);
	}
	
	public Set<Player> getParticipants()
	{
		return _participants;
	}
	
	public boolean is(EventState... states)
	{
		return Stream.of(states).anyMatch(state -> state == _state);
	}
	
	public static final EventManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EventManager INSTANCE = new EventManager();
	}
}