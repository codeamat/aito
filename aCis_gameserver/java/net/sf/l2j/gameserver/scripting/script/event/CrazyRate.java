package net.sf.l2j.gameserver.scripting.script.event;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.scripting.ScheduledQuest;

public class CrazyRate extends ScheduledQuest
{
	private int _multipler = 3;
	
	public CrazyRate()
	{
		super(-1, "event");
	}
	
	@Override
	protected void onStart()
	{
		LOGGER.info("Crazy Rate: Event ongoing!");
		World.announceToOnlinePlayers("Crazy rate started together let's level up multiplied "+_multipler+"x Rates.", true);
		
		Config.RATE_XP = _multipler * Config.RATE_XP;
		Config.RATE_SP = _multipler * Config.RATE_SP;
		Config.RATE_DROP_ITEMS = _multipler * Config.RATE_DROP_ITEMS;
		Config.RATE_DROP_ITEMS_BY_RAID = _multipler * Config.RATE_DROP_ITEMS_BY_RAID;
		Config.RATE_DROP_ADENA = _multipler * Config.RATE_DROP_ADENA;
	}
	
	@Override
	protected void onEnd()
	{
		LOGGER.info("Crazy Rate: Event end!");
		World.announceToOnlinePlayers("Crazy Rate came to an end rates returned to normal!");
		
		Config.RATE_XP = Config.RATE_XP / _multipler;
		Config.RATE_SP = Config.RATE_SP / _multipler;
		Config.RATE_DROP_ITEMS = Config.RATE_DROP_ITEMS / _multipler;
		Config.RATE_DROP_ITEMS_BY_RAID = Config.RATE_DROP_ITEMS_BY_RAID / _multipler;
		Config.RATE_DROP_ADENA = Config.RATE_DROP_ADENA / _multipler;
	}
}
