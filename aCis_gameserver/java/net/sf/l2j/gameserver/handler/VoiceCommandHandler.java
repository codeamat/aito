package net.sf.l2j.gameserver.handler;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.handler.voicecommandhandlers.VoiceEvent;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author StinkyMadness
 */
public class VoiceCommandHandler
{
	private final Map<String, IVoiceCommandHandler> _entries = new HashMap<>();
	
	protected VoiceCommandHandler()
	{
		registerHandler(new VoiceEvent());
	}
	
	private void registerHandler(IVoiceCommandHandler handler)
	{
		for (String command : handler.getVoiceCommandList())
			_entries.put(command, handler);
	}
	
	public IVoiceCommandHandler getHandler(String voiceCommand)
	{
		return _entries.get(voiceCommand);
	}
	
	public boolean handleVoicedCommand(Player player, String text)
	{
		if (!text.startsWith("."))
			return false;
		
		String command = text.substring(1);
		final IVoiceCommandHandler vch = getHandler(command.split(" ")[0]);
		if (vch == null)
			return false;
		
		vch.useVoiceCommand(player, command);
		return true;
	}
	
	public int size()
	{
		return _entries.size();
	}
	
	public static VoiceCommandHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final VoiceCommandHandler INSTANCE = new VoiceCommandHandler();
	}
}