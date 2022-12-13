package net.sf.l2j.gameserver.events.holder;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;

/**
 * @author StinkyMadness
 */
public class TeamHolder
{
	private final String _name;
	private final String _color;
	
	private final int[] _npcs;
	private final int[] _doors;
	
	private int _score;
	
	private final List<Location> _locs;
	private final Set<Player> _players = ConcurrentHashMap.newKeySet();
	
	public TeamHolder(StatSet set, List<Location> locs)
	{
		_name = set.getString("name");
		_color = set.getString("color", "FFFFFF");
		_locs = locs;
		_score = 0;
		
		_npcs = set.getIntegerArray("npcs", ArraysUtil.EMPTY_INT_ARRAY);
		_doors = set.getIntegerArray("doors", ArraysUtil.EMPTY_INT_ARRAY);
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public final String getColor()
	{
		return _color;
	}
	
	public final Location getRndLocation()
	{
		return Rnd.get(_locs);
	}
	
	public void increaseScore()
	{
		_score += 1;
	}
	
	public int score()
	{
		return _score;
	}
	
	public void add(Player player)
	{
		if (!_players.contains(player))
			_players.add(player);
	}
	
	public void remove(Player player)
	{
		if (_players.contains(player))
			_players.remove(player);
	}
	
	public Set<Player> getMembers()
	{
		return _players;
	}
	
	public int size()
	{
		return _players.size();
	}
	
	public final int[] getNpcs()
	{
		return _npcs;
	}
	
	public final int[] getDoors()
	{
		return _doors;
	}
	
	public void insertTeamToEvent(EventHolder settings)
	{
		for (Player player : _players)
		{
			// Remove effects if event not allow them.
			if (!settings.allowEffects())
				player.stopAllEffects();
			
			// Remove summons if event not allow them.
			if (!settings.allowSummons() && player.getSummon() != null)
				player.getSummon().unSummon(player);
			
			player.setTitle(_name);
			player.getAppearance().setTitleColor(Integer.decode("0x" + _color));
			player.getAppearance().setNameColor(Integer.decode("0x" + _color));
			player.teleportTo(getRndLocation(), 75);
		}
	}
	
	public void clean()
	{
		_players.clear();
		_score = 0;
	}
}