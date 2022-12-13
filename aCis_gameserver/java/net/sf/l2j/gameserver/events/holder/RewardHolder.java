package net.sf.l2j.gameserver.events.holder;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.events.enums.RewardType;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author StinkyMadness
 */
public class RewardHolder
{
	private final RewardType _type;
	private final int _itemId;
	private final int _minimum;
	private final int _maximum;
	private final int _chance;
	
	public RewardHolder(StatSet set)
	{
		_type = RewardType.valueOf(set.getString("type"));
		_itemId = set.getInteger("itemId");
		_minimum = set.getInteger("min", 1);
		_maximum = set.getInteger("max", 1);
		_chance = set.getInteger("chance", 100);
	}
	
	public final RewardType getType()
	{
		return _type;
	}
	
	public void reward(Player player)
	{
		if (player == null || Rnd.get(100) >= _chance)
			return;
		
		player.addItem("RewardHolder#reward", _itemId, (_minimum >= _maximum ? _minimum : Rnd.get(_minimum, _maximum)), player, true);
	}
}