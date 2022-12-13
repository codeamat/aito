package net.sf.l2j.gameserver.events.holder;

import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.gameserver.events.enums.RewardType;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;

/**
 * @author StinkyMadness
 */
public class EventHolder
{
	private final String _name;
	private final String _schedule;
	private final int _registration;
	private final int _duration;
	private final IntIntHolder _level;
	private final IntIntHolder _capacity;
	private final boolean _allowEffects;
	private final boolean _allowPotions;
	private final boolean _allowSummons;
	private final boolean _increasePvP;
	private final int _revive;
	private final int[] _restrictedItems;
	private final int[] _restrictedClass;
	private final int[] _restrictedSkill;
	private final int[] _doors;
	
	private final List<NpcHolder> _npcs;
	private final List<TeamHolder> _teams;
	private final List<RewardHolder> _rewards;
	
	public EventHolder(StatSet set, List<TeamHolder> teams, List<NpcHolder> npcs, List<RewardHolder> rewards)
	{
		_name = set.getString("name", "No Name");
		_schedule = set.getString("schedule", "");
		_registration = set.getInteger("registration", 300);
		_duration = set.getInteger("duration", 900);
		_level = set.getIntIntHolder("level");
		_capacity = set.getIntIntHolder("capacity");
		_allowEffects = set.getBool("allowEffects", true);
		_allowPotions = set.getBool("allowPotions", false);
		_allowSummons = set.getBool("allowSummons", true);
		_increasePvP = set.getBool("increasePvP", false);
		_revive = set.getInteger("revive", 7) * 1000;
		_restrictedItems = set.getIntegerArray("restrictedItems", ArraysUtil.EMPTY_INT_ARRAY);
		_restrictedClass = set.getIntegerArray("restrictedClass", ArraysUtil.EMPTY_INT_ARRAY);
		_restrictedSkill = set.getIntegerArray("restrictedSkill", ArraysUtil.EMPTY_INT_ARRAY);
		_doors = set.getIntegerArray("doors", ArraysUtil.EMPTY_INT_ARRAY);
		
		_npcs = npcs;
		_teams = teams;
		_rewards = rewards;
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public final long getNextSchedule()
	{
		return Stream.of(_schedule.split(" ")).mapToLong(v -> calculateNextCalendar(v)).min().getAsLong();
	}
	
	public final int getRegistration()
	{
		return _registration;
	}
	
	public final int getDuration()
	{
		return _duration;
	}
	
	public final IntIntHolder getLevelLimit()
	{
		return _level;
	}
	
	public final boolean isRestrictedLevel(int level)
	{
		return level < _level.getId() && level > _level.getValue();
	}
	
	public final IntIntHolder getCapacityLimit()
	{
		return _capacity;
	}
	
	public final boolean allowEffects()
	{
		return _allowEffects;
	}
	
	public final boolean allowPotions()
	{
		return _allowPotions;
	}
	
	public final boolean allowSummons()
	{
		return _allowSummons;
	}
	
	public final boolean increasePvP()
	{
		return _increasePvP;
	}
	
	public final int getRevive()
	{
		return _revive;
	}
	
	public final boolean isRestrictedItem(int itemId)
	{
		return ArraysUtil.contains(_restrictedItems, itemId);
	}
	
	public final boolean isRestrictedClass(int classId)
	{
		return ArraysUtil.contains(_restrictedClass, classId);
	}
	
	public final boolean isRestrictedSkill(int skillId)
	{
		return ArraysUtil.contains(_restrictedSkill, skillId);
	}
	
	public final int[] getDoors()
	{
		return _doors;
	}
	
	public final List<NpcHolder> getNpcs()
	{
		return _npcs;
	}
	
	public final NpcHolder getNpc(int npcId)
	{
		return _npcs.stream().filter(npc -> npc.getId() == npcId).findFirst().orElse(null);
	}
	
	public final List<TeamHolder> getTeams()
	{
		return _teams;
	}
	
	public final List<RewardHolder> getRewards(RewardType type)
	{
		return _rewards.stream().filter(reward -> reward.getType() == type).collect(Collectors.toList());
	}
	
	public final boolean hasRewardType(RewardType type)
	{
		return _rewards.stream().anyMatch(reward -> reward.getType() == type);
	}
	
	public static long calculateNextCalendar(String param)
	{
		final StringTokenizer st = new StringTokenizer(param, ":");
		final Calendar calendar = Calendar.getInstance();
		// set hour, minute and second
		calendar.set(Calendar.HOUR_OF_DAY, st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1);
		calendar.set(Calendar.MINUTE, st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0);
		calendar.set(Calendar.SECOND, st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		if (calendar.getTimeInMillis() <= System.currentTimeMillis())
			calendar.add(Calendar.DAY_OF_WEEK, 1);
		
		return (calendar.getTimeInMillis() - System.currentTimeMillis());
	}
}