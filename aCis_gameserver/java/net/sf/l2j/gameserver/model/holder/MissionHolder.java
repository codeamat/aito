package net.sf.l2j.gameserver.model.holder;

import java.util.List;

import net.sf.l2j.commons.data.StatSet;

/**
 * @author StinkyMadness
 */
public class MissionHolder
{
	private final int _level;
	private final String _name;
	private final String _icon;
	private final String _description;
	private final int _required;
	private final List<IntIntHolder> _reward;
	
	public MissionHolder(StatSet set)
	{
		_level = set.getInteger("level", 1);
		_name = set.getString("name", "Name");
		_icon = set.getString("icon", "icon.noimage");
		_description = set.getString("desc", "Description");
		_required = set.getInteger("required", 1);
		_reward = set.getIntIntHolderList("reward");
	}
	
	public final int getLevel()
	{
		return _level;
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public final String getIcon()
	{
		return _icon;
	}
	
	public final String getDescription()
	{
		return _description;
	}
	
	public final int getRequired()
	{
		return _required;
	}
	
	public final List<IntIntHolder> getRewards()
	{
		return _reward;
	}
}