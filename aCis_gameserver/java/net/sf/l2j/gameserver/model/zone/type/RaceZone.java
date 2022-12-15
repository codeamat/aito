package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.data.manager.RaceData;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.zone.type.subtype.ZoneType;

public class RaceZone extends ZoneType
{
	public RaceZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(Creature character)
	{
		System.out.println(character);
		
		if (character instanceof Player player)
		{
			RaceData.getInstance().onZoneEnter(player, this);
		}
	}
	
	@Override
	protected void onExit(Creature character)
	{
		
	}
}