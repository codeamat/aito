package net.sf.l2j.gameserver.model;

import java.util.Collection;

import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * @author ProjectX
 *
 */
public class Broadcast
{
	/**
	 * Send a packet to all L2PcInstance present in the world.<BR>
	 * <B><U> Concept</U> :</B><BR>
	 * In order to inform other players of state modification on the L2Character, server just need to go through _allPlayers to send Server->Client Packet<BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packet to this L2Character (to do this use method toSelfAndKnownPlayers)</B></FONT><BR>
	 *
	 * @param packet
	 */
	public static void toAllOnlinePlayers(final L2GameServerPacket packet)
	{
		for (final Player player : World.getInstance().getPlayers())
		{
			if (player.isOnline())
			{
				player.sendPacket(packet);
			}
		}
	}

	public static void toAllOnlinePlayers(final String text)
	{
		toAllOnlinePlayers(text, false);
	}

	public static void toAllOnlinePlayers(final String text, final boolean isCritical)
	{
		toAllOnlinePlayers(new CreatureSay(0, isCritical ? SayType.CRITICAL_ANNOUNCE : SayType.ANNOUNCEMENT, "", text));
	}

	public static void toPlayersInInstance(final L2GameServerPacket packet, final int instanceId)
	{
		for (final Player player : World.getInstance().getPlayers())
		{
			if (player.isOnline() && player.getInstanceId() == instanceId)
			{
				player.sendPacket(packet);
			}
		}
	}

	public static void toAllOnlinePlayersOnScreen(final String text)
	{
		toAllOnlinePlayers(new ExShowScreenMessage(text, 10000));
	}

	public static void toPlayers(final Collection<Player> collection, final String text)
	{
		toPlayers(collection, text, false);
	}

	public static void toPlayers(final Collection<Player> collection, final String text, final boolean isCritical)
	{
		toPlayers(collection, new CreatureSay(0, isCritical ? SayType.CRITICAL_ANNOUNCE : SayType.ANNOUNCEMENT, "", text));
	}

	public static void toPlayers(final Collection<Player> collection, final L2GameServerPacket packet)
	{
		for (final Player player : collection)
		{
			if (player.isOnline())
			{
				player.sendPacket(packet);
			}
		}
	}
}
