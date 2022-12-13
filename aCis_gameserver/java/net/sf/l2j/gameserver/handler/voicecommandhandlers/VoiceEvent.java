package net.sf.l2j.gameserver.handler.voicecommandhandlers;

import net.sf.l2j.gameserver.events.EventManager;
import net.sf.l2j.gameserver.handler.IVoiceCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author StinkyMadness
 */
public class VoiceEvent implements IVoiceCommandHandler
{
	private static final String[] VOICE_COMMANDS =
	{
		"join",
		"leave"
	};
	
	@Override
	public void useVoiceCommand(Player player, String command)
	{
		if (command.startsWith("join"))
			EventManager.getInstance().register(player);
		else if (command.startsWith("leave"))
			EventManager.getInstance().unregister(player);
	}
	
	@Override
	public String[] getVoiceCommandList()
	{
		return VOICE_COMMANDS;
	}
}