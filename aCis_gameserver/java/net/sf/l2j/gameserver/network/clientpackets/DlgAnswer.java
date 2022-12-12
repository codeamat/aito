package net.sf.l2j.gameserver.network.clientpackets;

import java.util.Objects;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.DailyMissionData;
import net.sf.l2j.gameserver.enums.PlayerAction;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.missions.AbstractDailyMission;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class DlgAnswer extends L2GameClientPacket
{
	private int _messageId;
	private int _answer;
	private int _requesterId;
	
	@Override
	protected void readImpl()
	{
		_messageId = readD();
		_answer = readD();
		_requesterId = readD();
	}
	
	@Override
	public void runImpl()
	{
		final Player player = getClient().getPlayer();
	
		if (player == null)
			return;
		
		if (_messageId == SystemMessageId.S1.getId())
		{
			if (player.removeAction(PlayerAction.CANCEL_DAILY_MISSION))
			{
				if (_answer == 1)
				{
					final AbstractDailyMission<?> mission = DailyMissionData.getInstance().getMission(player.getMemos().getInteger("daily_mission_id", 0));

					if (Objects.nonNull(mission))
					{
						switch (player.getMission().getMissionState(mission))
						{
							case ACTIVE ->
							{
								player.getMission().removeMission(mission.getId());
								player.getMission().removeCounter(mission.getId());
								player.getMission().removeReuseDelay(mission.getId());

								if (DailyMissionData.getInstance().getBool("soundEffectEnabled", false))
								{
									player.sendPacket(new PlaySound("ItemSound.quest_giveup"));
								}

								player.sendPacket(SystemMessage.sendString("Mission " + mission.getName() + " has been canceled."));
							}
						}
					}
				}
			}
		}
		else if (_messageId == SystemMessageId.RESSURECTION_REQUEST_BY_S1.getId() || _messageId == SystemMessageId.DO_YOU_WANT_TO_BE_RESTORED.getId())
			player.reviveAnswer(_answer);
		else if (_messageId == SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId())
			player.teleportAnswer(_answer, _requesterId);
		else if (_messageId == 1983 && Config.ALLOW_WEDDING)
			player.engageAnswer(_answer);
		else if (_messageId == SystemMessageId.WOULD_YOU_LIKE_TO_OPEN_THE_GATE.getId())
			player.activateGate(_answer, 1);
		else if (_messageId == SystemMessageId.WOULD_YOU_LIKE_TO_CLOSE_THE_GATE.getId())
			player.activateGate(_answer, 0);
	}
}