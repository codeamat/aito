package net.sf.l2j.gameserver.events.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.gameserver.enums.Paperdoll;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.events.EventManager;
import net.sf.l2j.gameserver.events.enums.EventState;
import net.sf.l2j.gameserver.events.holder.NpcHolder;
import net.sf.l2j.gameserver.events.holder.TeamHolder;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * @author StinkyMadness
 */
public class CaptureTheFlag extends AbstractEvent
{
	private static final int FLAG_ITEM_ID = 6718;
	
	private Map<Player, Npc> _flags = new ConcurrentHashMap<>();
	private Map<Player, Long> _time = new ConcurrentHashMap<>();
	
	private Map<Player, ItemInstance> _weapon = new ConcurrentHashMap<>();
	private Map<Player, ItemInstance> _shield = new ConcurrentHashMap<>();
	
	@Override
	public void onTeleport()
	{
		super.onTeleport();
		closeDoors(_holder.getDoors());
	}
	
	@Override
	public void onStarting()
	{
		ThreadPool.schedule(() -> _teams.forEach(team -> openDoors(team.getDoors())), 10000L);
	}
	
	@Override
	public void onProgress(int timer)
	{
		if (!_time.isEmpty())
		{
			final long currentTime = System.currentTimeMillis();
			for (Map.Entry<Player, Long> entry : _time.entrySet())
			{
				// Get time left and check.
				final Player player = entry.getKey();
				final long timeLeft = entry.getValue();
				
				// Time is running out.
				if (currentTime > timeLeft)
				{
					removeFlag(player, false, false);
					_time.remove(player);
				}
				// Time almost runned out.
				else if (currentTime > (timeLeft - 15000))
					player.sendMessage(String.format("Ramain %s second(s) to deliver the enemy flag.", (timeLeft - currentTime) / 1000));
			}
		}
	}
	
	@Override
	public boolean onInteract(Player player, Npc npc)
	{
		if (!EventManager.getInstance().is(EventState.PROGRESS))
			return false;
		
		final TeamHolder teamHolder = getNpcTeam(npc);
		if (teamHolder == null)
			return false;
		
		final boolean isHolder = npc.getName().contains("Flag Holder");
		
		// Interact with team NPC's
		if (teamHolder == getPlayerTeam(player))
		{
			// Interact with team flag holder.
			if (isHolder)
			{
				removeFlag(player, false, true);
				return false;
			}
			
			final NpcHolder npcHolder = _holder.getNpc(npc.getNpcId());
			if (npcHolder == null || _flags.containsKey(player))
				return false;
			
			if (npcHolder.getSpawns().stream().anyMatch(spawn -> npc.distance2D(spawn) > 100))
			{
				removeSpawn(npc);
				_npcs.add(addSpawn(npc.getNpcId(), npcHolder.getRndSpawn()));
			}
			return false;
		}
		
		// Interact with enemy flag.
		if (!_flags.containsKey(player) && !isHolder)
			equipFlag(player, npc);
		
		return false;
	}
	
	@Override
	public boolean canAttack(Player player, Creature target)
	{
		return !_flags.containsKey(player) && super.canAttack(player, target);
	}
	
	@Override
	public boolean onUseSkill(Player player, L2Skill skill)
	{
		final WorldObject target = player.getTarget();
		final Player victim = target == null ? null : target.getActingPlayer();
		if (victim != null && getPlayerTeam(player) == getPlayerTeam(victim) && (skill.isOffensive() || skill.isDebuff()))
			return false;
		
		return !_flags.containsKey(player) && super.onUseSkill(player, skill);
	}
	
	@Override
	public boolean onUseItem(Player player, ItemInstance item)
	{
		return !_flags.containsKey(player) && super.onUseItem(player, item);
	}
	
	@Override
	public boolean onDestroyItem(Player player, ItemInstance item)
	{
		return item.getItemId() != FLAG_ITEM_ID;
	}
	
	@Override
	public void onDie(Player player, Creature killer)
	{
		removeFlag(player, true, false);
		super.onDie(player, killer);
	}
	
	public void equipFlag(Player player, Npc npc)
	{
		if (_flags.containsKey(player))
			return;
		
		if (!_time.containsKey(player))
			_time.put(player, System.currentTimeMillis() + 90000L);
		
		_flags.put(player, npc);
		_npcs.remove(npc);
		
		storePreviewesEquipment(player);
		
		removeSpawn(_flags.get(player));
		
		player.disarmWeapon(true);
		
		final ItemInstance item = player.getInventory().addItem("CaptureTheFlag", FLAG_ITEM_ID, 1, player, null);
		player.getInventory().equipItemAndRecord(item);
		player.broadcastPacket(new SocialAction(player, 16));
		
		player.sendPacket(new CreatureSay(player.getObjectId(), SayType.TELL, "Event", "You have 90 second(s) to deliver the enemy flag."));
		player.sendPacket(new ItemList(player, false));
		player.broadcastUserInfo();
	}
	
	public void removeFlag(Player player, boolean onPlayer, boolean increasePoint)
	{
		if (!_flags.containsKey(player))
			return;
		
		if (_time.containsKey(player))
			_time.remove(player);
		
		player.destroyItemByItemId("DestroyFlag", FLAG_ITEM_ID, 1, player, false);
		equipPreviewsEquipment(player);
		player.broadcastUserInfo();
		player.sendPacket(new ItemList(player, false));
		
		if (EventManager.getInstance().is(EventState.PROGRESS))
		{
			final Npc npc = _flags.remove(player);
			if (npc == null)
				return;
			
			final NpcHolder npcHolder = _holder.getNpc(npc.getNpcId());
			if (npcHolder == null)
				return;
			
			_npcs.add(addSpawn(npc.getNpcId(), onPlayer ? player.getPosition() : npcHolder.getRndSpawn()));
			
			if (increasePoint)
			{
				player.setTitle(String.format("%s Flag(s)", onScore(player)));
				player.broadcastTitleInfo();
				respawnTeams();
				announceToTeams(String.format("%s has score for %s team.", player.getName(), getPlayerTeam(player).getName()));
			}
		}
	}
	
	private void storePreviewesEquipment(Player player)
	{
		final ItemInstance weapon = player.getInventory().getItemFrom(Paperdoll.RHAND);
		if (weapon != null && !_weapon.containsKey(player))
			_weapon.put(player, weapon);
		
		final ItemInstance shield = player.getInventory().getItemFrom(Paperdoll.LHAND);
		if (shield != null && !_shield.containsKey(player))
			_shield.put(player, shield);
	}
	
	private void equipPreviewsEquipment(Player player)
	{
		final ItemInstance weapon = _weapon.remove(player);
		if (weapon != null)
			player.getInventory().equipItemAndRecord(weapon);
		
		final ItemInstance shield = _shield.remove(player);
		if (shield != null)
			player.getInventory().equipItemAndRecord(shield);
	}
	
	public TeamHolder getNpcTeam(Npc npc)
	{
		return _teams.stream().filter(team -> ArraysUtil.contains(team.getNpcs(), npc.getNpcId())).findFirst().orElse(null);
	}
	
	public void respawnTeams()
	{
		_time.clear();
		_flags.keySet().stream().forEach(player -> removeFlag(player, false, false));
		_flags.clear();
		
		_weapon.clear();
		_shield.clear();
		
		_teams.forEach(team -> team.getMembers().forEach(member ->
		{
			closeDoors(team.getDoors());
			
			if (member.isDead())
				onRevive(member);
			else
				member.teleportTo(team.getRndLocation(), 75);
			
			ThreadPool.schedule(() -> openDoors(team.getDoors()), 10000L); // 10 seconds to open doors.
		}));
		
		_npcs.forEach(Npc::deleteMe);
		spawnAllNpcs();
	}
	
	@Override
	public void cleanMe()
	{
		if (_flags != null && !_flags.isEmpty())
			_flags.keySet().forEach(player -> removeFlag(player, false, false));
		
		_players.forEach(playerHolder -> playerHolder.getPlayer().destroyItemByItemId("CaptureTheFlag", FLAG_ITEM_ID, 1, playerHolder.getPlayer(), false));
		
		super.cleanMe();
	}
	
	@Override
	public void calculateRewards()
	{
		rewardMVPs();
		rewardTeams();
	}
	
	@Override
	public String getMessage()
	{
		final StringBuilder sb = new StringBuilder();
		getTeams().forEach(team -> sb.append(team.getName() + ": " + team.score() + " | "));
		return sb.toString();
	}
}