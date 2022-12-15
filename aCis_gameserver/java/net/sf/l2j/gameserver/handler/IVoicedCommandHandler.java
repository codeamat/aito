package net.sf.l2j.gameserver.handler;

import net.sf.l2j.gameserver.model.actor.Player;

public interface IVoicedCommandHandler
{		
	public void useVoicedCommand(String command, String params, Player player);
	
	public String[] getVoicedCommandList();
}