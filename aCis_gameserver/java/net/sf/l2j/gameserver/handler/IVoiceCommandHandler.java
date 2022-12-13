package net.sf.l2j.gameserver.handler;

import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author StinkyMadness
 */
public interface IVoiceCommandHandler
{
	public void useVoiceCommand(Player player, String command);
	
	public String[] getVoiceCommandList();
}