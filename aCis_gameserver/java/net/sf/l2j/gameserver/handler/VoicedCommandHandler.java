package net.sf.l2j.gameserver.handler;

import java.util.HashMap;
import java.util.Map;

public class VoicedCommandHandler
{
	private final Map<String, IVoicedCommandHandler> _entries = new HashMap<>();
	
	public void registerHandler(final IVoicedCommandHandler handler)
	{
		for (String type : handler.getVoicedCommandList())
		{
			_entries.put(type, handler);
		}
	}
	
	public IVoicedCommandHandler getHandler(final String command)
	{
		return _entries.get(command);
	}
	
	public int size()
	{
		return _entries.size();
	}
	
	public static VoicedCommandHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final VoicedCommandHandler INSTANCE = new VoicedCommandHandler();
	}
}