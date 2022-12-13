package net.sf.l2j.gameserver.events.model;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.enums.items.EtcItemType;
import net.sf.l2j.gameserver.events.EventManager;
import net.sf.l2j.gameserver.events.enums.EventState;
import net.sf.l2j.gameserver.events.enums.RewardType;
import net.sf.l2j.gameserver.events.holder.EventHolder;
import net.sf.l2j.gameserver.events.holder.NpcHolder;
import net.sf.l2j.gameserver.events.holder.PlayerHolder;
import net.sf.l2j.gameserver.events.holder.TeamHolder;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * @author StinkyMadness
 */
public abstract class AbstractEvent
{
	private static final CLogger LOGGER = new CLogger(AbstractEvent.class.getName());
	
	protected Set<Npc> _npcs = ConcurrentHashMap.newKeySet();
	protected Set<TeamHolder> _teams = ConcurrentHashMap.newKeySet();
	protected Set<PlayerHolder> _players = ConcurrentHashMap.newKeySet();
	
	protected final Map<Integer, TeamHolder> _disconnected = new ConcurrentHashMap<>();
	
	protected int _highestTeamScore; // Highest Team Score
	protected int _highestPlayerScore; // Highest Player Score
	
	protected EventHolder _holder;
	
	public void onTeleport()
	{
		if (_holder.getTeams().isEmpty())
		{
			LOGGER.warn("There was no team created for {} event", _holder.getName());
			EventManager.getInstance().set(EventState.INACTIVE, 30); // 30 second to select another event.
			return;
		}
		
		// Create teams of the event.
		_holder.getTeams().forEach(team -> _teams.add(team));
		
		// Split players on teams.
		EventManager.getInstance().getParticipants().stream().sorted(Comparator.comparing(player -> player.getActiveClass())).forEach(player ->
		{
			_players.add(new PlayerHolder(player));
			_teams.stream().min(Comparator.comparingInt(TeamHolder::size)).get().add(player);
		});
		
		// Insert teams to event.
		_teams.forEach(team -> team.insertTeamToEvent(_holder));
		
		_players.forEach(PlayerHolder::freeze);
		spawnAllNpcs();
	}
	
	public void onStarting()
	{
	}
	
	public void onProgress(int timer)
	{
	}
	
	public void onFinish(int timer)
	{
	}
	
	public void cleanMe()
	{
		_npcs.forEach(Npc::decayMe);
		_npcs.clear();
		
		_teams.forEach(TeamHolder::clean);
		_teams.clear();
		
		_players.forEach(PlayerHolder::clean);
		_players.clear();
		
		_highestPlayerScore = 0;
		_highestTeamScore = 0;
	}
	
	public void onKill(Player player, Creature creature)
	{
		if (_holder.hasRewardType(RewardType.KILL))
			_holder.getRewards(RewardType.KILL).forEach(rewardHolder -> rewardHolder.reward(player));
		
		if (creature instanceof Player && _holder.increasePvP())
		{
			player.setPvpKills(player.getPvpKills() + 1);
			player.sendPacket(new UserInfo(player));
		}
	}
	
	public void onDamage(Player player, Creature target, int damage)
	{
	}
	
	public void onDie(Player player, Creature killer)
	{
		ThreadPool.schedule(() -> onRevive(player), _holder.getRevive());
	}
	
	public boolean canAttack(Player player, Creature target)
	{
		return target.getActingPlayer() == null || !player.isEventTeamWith(target);
	}
	
	public boolean onUseSkill(Player player, L2Skill skill)
	{
		if (_holder.isRestrictedSkill(skill.getId()))
		{
			player.sendMessage(String.format("%s is restricted on this event.", skill.getName()));
			return false;
		}
		
		switch (skill.getSkillType())
		{
			case RESURRECT:
			case SUMMON_FRIEND:
			case RECALL:
			case FAKE_DEATH:
				player.sendMessage("You are not able to use that skill on current event.");
				return false;
		}
		return true;
	}
	
	public boolean onUseItem(Player player, ItemInstance item)
	{
		if (_holder.isRestrictedItem(item.getItemId()))
		{
			player.sendMessage(String.format("%s is restricted on this event.", item.getName()));
			return false;
		}
		
		if (item.getItem().getItemType() instanceof EtcItemType)
		{
			switch ((EtcItemType) item.getItem().getItemType())
			{
				case ELIXIR:
				case POTION:
					if (_holder.allowPotions())
						return true;
				case SCROLL:
				case RECIPE:
				case PET_COLLAR:
				case CASTLE_GUARD:
				case SCRL_ENCHANT_WP:
				case SCRL_ENCHANT_AM:
				case BLESS_SCRL_ENCHANT_WP:
				case BLESS_SCRL_ENCHANT_AM:
					player.sendMessage("You are not able to use that item on current event.");
					return false;
			}
		}
		return true;
	}
	
	public boolean onDestroyItem(Player player, ItemInstance item)
	{
		return true;
	}
	
	public boolean onSay(SayType type, Player player, String context)
	{
		return true; // if return false player can't use chat.
	}
	
	public boolean onInteract(Player player, Npc npc)
	{
		return true;
	}
	
	public void onRevive(Player player)
	{
		if (!player.isDead())
			return;
		
		player.doRevive();
		player.getStatus().setMaxCpHpMp();
		player.teleportTo(getPlayerTeam(player).getRndLocation(), 75);
	}
	
	public int onScore(Player player)
	{
		final TeamHolder teamHolder = getPlayerTeam(player);
		teamHolder.increaseScore();
		_highestTeamScore = Math.max(teamHolder.score(), _highestTeamScore);
		
		final PlayerHolder playerHolder = getPlayer(player);
		playerHolder.increaseScore();
		_highestPlayerScore = Math.max(playerHolder.score(), _highestPlayerScore);
		return playerHolder.score();
	}
	
	public void onLogin(Player player)
	{
		if (!_disconnected.containsKey(player.getObjectId()))
			return;
		
		_players.add(new PlayerHolder(player));
		
		final TeamHolder teamHolder = _disconnected.get(player.getObjectId());
		teamHolder.add(player);
		teamHolder.insertTeamToEvent(_holder);
	}
	
	public void onLogout(Player player)
	{
		final TeamHolder teamHolder = getPlayerTeam(player);
		if (teamHolder != null)
		{
			teamHolder.remove(player);
			_disconnected.put(player.getObjectId(), teamHolder);
		}
		
		final PlayerHolder playerHolder = getPlayer(player);
		if (playerHolder != null)
		{
			_players.remove(playerHolder);
			playerHolder.clean();
		}
	}
	
	public void spawnAllNpcs()
	{
		final List<NpcHolder> npcs = _holder.getNpcs();
		if (npcs == null || npcs.isEmpty())
			return;
		
		for (NpcHolder npcHolder : npcs)
		{
			final List<SpawnLocation> spawns = npcHolder.getSpawns();
			if (spawns == null || spawns.isEmpty())
				continue;
			
			for (SpawnLocation spawn : spawns)
			{
				final Npc npc = addSpawn(npcHolder.getId(), spawn);
				if (npc == null)
					continue;
				
				_npcs.add(npc);
			}
		}
	}
	
	public Npc addSpawn(int npcId, SpawnLocation position)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		if (template == null)
			return null;
		
		try
		{
			final Spawn spawn = new Spawn(template);
			spawn.setLoc(position);
			spawn.setRespawnState(false);
			return spawn.doSpawn(false);
		}
		catch (Exception e)
		{
			LOGGER.warn("Something was wrone while event try npc id {}", npcId);
			return null;
		}
	}
	
	public void removeSpawn(Npc npc)
	{
		_npcs.remove(npc);
		npc.scheduleDespawn(100L);
	}
	
	public void openDoors(int[] doors)
	{
		for (int doorId : doors)
		{
			final Door door = DoorData.getInstance().getDoor(doorId);
			if (door != null)
				door.openMe();
		}
	}
	
	public void closeDoors(int[] doors)
	{
		for (int doorId : doors)
		{
			final Door door = DoorData.getInstance().getDoor(doorId);
			if (door != null)
				door.closeMe();
		}
	}
	
	public void rewardMVPs()
	{
		if (_highestPlayerScore == 0 || !_holder.hasRewardType(RewardType.MVP))
			return;
		
		final List<PlayerHolder> MVPs = _players.stream().filter(ph -> ph.score() == _highestPlayerScore).collect(Collectors.toList());
		if (!MVPs.isEmpty())
		{
			announceToAll(MVPs.size() == 1 ? String.format("MVP was %s with %s point(s).", MVPs.get(0).getPlayer().getName(), StringUtil.formatNumber(_highestPlayerScore)) : String.format("%s total MVP's with %s point(s).", MVPs.size(), StringUtil.formatNumber(_highestPlayerScore)));
			MVPs.forEach(ph -> _holder.getRewards(RewardType.MVP).forEach(info -> info.reward(ph.getPlayer())));
		}
	}
	
	public void rewardTeams()
	{
		if (_highestTeamScore == 0)
		{
			announceToAll("There was no winners.");
			return;
		}
		
		if (_holder.hasRewardType(RewardType.TIE) && _teams.stream().allMatch(team -> team.score() == _highestTeamScore))
		{
			announceToAll(String.format("No winner, all teams had %s point(s).", StringUtil.formatNumber(_highestTeamScore)));
			_teams.forEach(team -> team.getMembers().forEach(player -> _holder.getRewards(RewardType.TIE).forEach(rewardHolder -> rewardHolder.reward(player))));
			return;
		}
		
		for (TeamHolder teamHolder : _teams)
		{
			if (_holder.hasRewardType(RewardType.WINNER) && teamHolder.score() == _highestTeamScore)
			{
				announceToAll(String.format("Winner team is '%s' with %s point(s).", teamHolder.getName(), StringUtil.formatNumber(teamHolder.score())));
				teamHolder.getMembers().forEach(player -> _holder.getRewards(RewardType.WINNER).forEach(rewardHolder -> rewardHolder.reward(player)));
			}
			else if (_holder.hasRewardType(RewardType.LOSSER) && teamHolder.score() > 0)
				teamHolder.getMembers().forEach(player -> _holder.getRewards(RewardType.LOSSER).forEach(rewardHolder -> rewardHolder.reward(player)));
		}
	}
	
	public void setHolder(EventHolder holder)
	{
		_holder = holder;
	}
	
	public EventHolder getHolder()
	{
		return _holder;
	}
	
	public Set<TeamHolder> getTeams()
	{
		return _teams;
	}
	
	public TeamHolder getPlayerTeam(Player player)
	{
		return _teams.stream().filter(team -> team.getMembers().contains(player)).findFirst().orElse(null);
	}
	
	public Set<PlayerHolder> getPlayers()
	{
		return _players;
	}
	
	public PlayerHolder getPlayer(Player player)
	{
		return _players.stream().filter(playerHolder -> playerHolder.getPlayer() == player).findFirst().orElse(null);
	}
	
	public void announceToAll(String message)
	{
		World.announceToOnlinePlayers(String.format("%s: %s", _holder.getName(), message), true);
	}
	
	public void announceToTeams(String message)
	{
		final CreatureSay creatureSay = new CreatureSay(0, SayType.CRITICAL_ANNOUNCE, "", String.format("%s: %s", _holder.getName(), message));
		_players.forEach(playerHolder -> playerHolder.sendPacket(creatureSay));
	}
	
	public void announceToTeam(TeamHolder teamHolder, String message)
	{
		final CreatureSay creatureSay = new CreatureSay(0, SayType.CRITICAL_ANNOUNCE, "", String.format("%s: %s", _holder.getName(), message));
		teamHolder.getMembers().forEach(player -> player.sendPacket(creatureSay));
	}
	
	public void broadcastInfo()
	{
		if (getMessage() == null || getMessage().isBlank())
			return;
		
		final ExShowScreenMessage message = new ExShowScreenMessage(1, -1, SMPOS.TOP_RIGHT, false, 1, 0, 0, false, 2000, false, String.format("%sTime: %s", getMessage(), EventManager.getInstance().getTimer()));
		_players.forEach(playerHolder -> playerHolder.sendPacket(message));
	}
	
	public abstract void calculateRewards();
	
	public abstract String getMessage();
}