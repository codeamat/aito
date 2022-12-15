package net.sf.l2j.gameserver.handler.chathandlers;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.enums.FloodProtector;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.handler.VoiceCommandHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class ChatAll implements IChatHandler
{
	private static final SayType[] COMMAND_IDS =
	{
		SayType.ALL
	};
	
	@Override
	public void handleChat(SayType type, Player player, String target, String text)
	{
		if (VoiceCommandHandler.getInstance().handleVoicedCommand(player, text))
			return;
		
		if (!player.getClient().performAction(FloodProtector.GLOBAL_CHAT))
			return;
		
		String params = "";
		boolean vcd_used = false;

		if (text.startsWith("."))
		{
			System.out.println("cgahttell#0");

			final StringTokenizer st = new StringTokenizer(text);
			IVoicedCommandHandler vch;
			String command = "";

			if (st.countTokens() > 1)
			{
				command = st.nextToken().substring(1);
				params = text.substring(command.length() + 2);
				vch = VoicedCommandHandler.getInstance().getHandler(command);
			}
			else
			{
				command = text.substring(1);

				vch = VoicedCommandHandler.getInstance().getHandler(command);
			}

			System.out.println("cgahttell#");

			if (vch != null)
			{
				System.out.println("cgahttell#2");
				vch.useVoicedCommand(command, params, player);
				vcd_used = true;
			}
			else
			{
				vcd_used = false;
			}
		}

		if (vcd_used)
		{
			return;
		}

		final CreatureSay cs = new CreatureSay(player, type, text);
		for (Player knownPlayer : player.getKnownTypeInRadius(Player.class, 1250))
			knownPlayer.sendPacket(cs);
		
		player.sendPacket(cs);
	}
	
	@Override
	public SayType[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}