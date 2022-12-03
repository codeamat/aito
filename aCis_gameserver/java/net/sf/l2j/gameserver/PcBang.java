package net.sf.l2j.gameserver;

import java.util.logging.Logger;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;

public class PcBang implements Runnable
{
	Logger _log = Logger.getLogger(PcBang.class.getName());
	private static PcBang _instance;
	
	public static PcBang getInstance()
	
	{
		if(_instance == null)
		{
			_instance = new PcBang();
		}
		
		return _instance;
	}
	
	private PcBang()
	{
		_log.info("Aito Credit point event started.");
	}
	
	@Override
	public void run()
	{
		
		int score = 0;
		for (Player activeChar: World.getInstance().getPlayers())
		{
			
			if(activeChar.getStatus().getLevel() > Config.PCB_MIN_LEVEL )
			{
				score = Rnd.get(Config.PCB_POINT_MIN, Config.PCB_POINT_MAX);
				
				if(Rnd.get(100) <= Config.PCB_CHANCE_DUAL_POINT)
				{
					score *= 2;
					
					activeChar.addPcBangScore(score);
					
					activeChar.sendMessage("Tu as recu plus de credits.");
					activeChar.updatePcBangWnd(score, true, true);
				}
				else
				{
					activeChar.addPcBangScore(score);
					activeChar.sendMessage("Tu as recu des credits.");
					activeChar.updatePcBangWnd(score, true, false);
				}
			}
			
			activeChar = null;
		}
	}
}