package net.sf.l2j.gameserver.events.holder;

import java.util.List;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.location.SpawnLocation;

/**
 * @author StinkyMadness
 */
public class NpcHolder
{
	private final int _id;
	private final List<SpawnLocation> _spawns;
	
	public NpcHolder(StatSet set, List<SpawnLocation> spawns)
	{
		_id = set.getInteger("id");
		_spawns = spawns;
	}
	
	public final int getId()
	{
		return _id;
	}
	
	public final List<SpawnLocation> getSpawns()
	{
		return _spawns;
	}
	
	public final SpawnLocation getRndSpawn()
	{
		return Rnd.get(_spawns);
	}
}