package net.sf.l2j.gameserver.model.actor.instance;

import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.util.SysUtil;

import net.sf.l2j.gameserver.data.manager.DailyMissionData;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.enums.MissionType;
import net.sf.l2j.gameserver.enums.PlayerAction;
import net.sf.l2j.gameserver.model.PlayerMission;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.missions.AbstractDailyMission;
import net.sf.l2j.gameserver.model.missions.MissionStateType;
import net.sf.l2j.gameserver.model.missions.events.impl.IMissionEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Rationale
 */
public class DailyMissionManager extends Folk
{
	private static final Logger _log = Logger.getLogger(DailyMissionManager.class.getName());

	private static final int RESULT_PER_PAGE = 6;

	private static final String DEFAULT_HTML = "index.htm";

	public DailyMissionManager(final int objectId, final NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void showChatWindow(final Player activeChar, final int val)
	{
		handleRedirect(activeChar, DEFAULT_HTML, 0);
	}

	@Override
	public void onBypassFeedback(final Player activeChar, final String command)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();

		switch (actualCommand)
		{
			case "redirect" ->
			{
				final String html = st.hasMoreTokens() ? st.nextToken() : DEFAULT_HTML;
				final int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;

				final String[] parameters = new String[st.countTokens()];

				for (int i = 0; i < parameters.length; i++)
				{
					parameters[i] = st.nextToken();
				}

				handleRedirect(activeChar, html, page, parameters);
			}
			case "mission" -> handleMission(activeChar, st);
		}
	}

	@SuppressWarnings("unchecked")
	public void handleMission(final Player activeChar, final StringTokenizer st)
	{
		if (!st.hasMoreTokens())
		{
			return;
		}

		switch (st.nextToken())
		{
			case "reward" ->
			{
				if (st.hasMoreTokens())
				{
					final AbstractDailyMission<?> mission = DailyMissionData.getInstance().getMission(Integer.parseInt(st.nextToken()));

					if (Objects.isNull(mission))
					{
						_log.log(Level.WARNING, getClass().getSimpleName() + ": Failed to find mission for " + activeChar);
						return;
					}

					if (activeChar.getMission().getReuseDelay(mission.getId()) == PlayerMission.PENDING_REWARD)
					{
						mission.getItems().forEach(s -> activeChar.addItem(getClass().getSimpleName(), s.getId(), s.getValue(), activeChar, true));
					}

					switch (mission.getType())
					{
						case DAILY -> activeChar.getMission().setReuseDelay(mission.getId(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
						case WEEKLY -> activeChar.getMission().setReuseDelay(mission.getId(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7));
						case SINGLE -> activeChar.getMission().setReuseDelay(mission.getId(), PlayerMission.SINGLE_COMPLETE);
						default -> activeChar.getMission().removeReuseDelay(mission.getId());
					}

					handleRedirect(activeChar, "mission.htm", 0, String.valueOf(mission.getId()));
				}
			}
			case "accept" ->
			{
				if (st.hasMoreTokens())
				{
					final AbstractDailyMission<?> mission = DailyMissionData.getInstance().getMission(Integer.parseInt(st.nextToken()));

					if (Objects.isNull(mission))
					{
						_log.log(Level.WARNING, getClass().getSimpleName() + ": Failed to find mission for " + activeChar);
						return;
					}

					final int maxMission = DailyMissionData.getInstance().getInteger("maxMissionPerInstance", 0);

					if (maxMission > 0 && activeChar.getMission().getMissions().size() >= maxMission)
					{
						activeChar.sendPacket(SystemMessage.sendString("You can take up to " + maxMission + " mission at once"));

						handleRedirect(activeChar, "mission.htm", 0, String.valueOf(mission.getId()));
					}
					else
					{
						switch (activeChar.getMission().getMissionState(mission))
						{
							case AVAILABLE ->
							{
								activeChar.getMission().addMission((AbstractDailyMission<IMissionEvent>) mission);

								if (DailyMissionData.getInstance().getBool("soundEffectEnabled", false))
								{
									activeChar.sendPacket(new PlaySound("ItemSound.quest_accept"));
								}

								activeChar.sendPacket(SystemMessage.sendString("Mission " + mission.getName() + " is now active."));
							}
						}

						handleRedirect(activeChar, "mission.htm", 0, String.valueOf(mission.getId()));
					}
				}
			}
			case "cancel" ->
			{
				if (st.hasMoreTokens())
				{
					final AbstractDailyMission<?> mission = DailyMissionData.getInstance().getMission(Integer.parseInt(st.nextToken()));

					if (Objects.isNull(mission))
					{
						_log.log(Level.WARNING, getClass().getSimpleName() + ": Failed to find mission for player: " + activeChar);
						return;
					}

					activeChar.getMemos().set("daily_mission_id", mission.getId());
					activeChar.addAction(PlayerAction.CANCEL_DAILY_MISSION);
					activeChar.sendPacket(new ConfirmDlg(SystemMessageId.S1).addString("Do you want to cancel this mission? Any progress will be permanently deleted.").addTime(12000));
				}
			}
		}
	}

	/**
	 * Redirect to html and replace parameters if any
	 *
	 * @param activeChar
	 * @param htmRedirect
	 * @param page
	 * @param parameters
	 */
	public void handleRedirect(final Player activeChar, final String htmRedirect, final int page, final String... parameters)
	{
		if (parameters == null)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Null parameters in handleRedirect for player: " + activeChar);
			return;
		}

		if (!htmRedirect.endsWith(".htm") && !htmRedirect.endsWith(".html"))
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Failed to find html redirect with name: " + htmRedirect + " for player: " + activeChar);
			return;
		}

		final NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setFile("data/html/mods/DailyMission/" + htmRedirect);
		htm.replace("%objectId%", String.valueOf(getObjectId()));

		final String filteredHtmRedirect = htmRedirect.substring(0, htmRedirect.lastIndexOf("."));

		switch (filteredHtmRedirect)
		{
			case "index" ->
			{
				final List<AbstractDailyMission<?>> list = DailyMissionData.getInstance().getMissions();

				final int starting = page * RESULT_PER_PAGE;
				final int ending = Math.min(list.size(), (page + 1) * RESULT_PER_PAGE);

				final StringBuilder sb = new StringBuilder();

				for (int i = starting; i < ending; i++)
				{
					final AbstractDailyMission<?> mission = list.get(i);

					sb.append("<table width=300 height=25 bgcolor=000000>");
					sb.append("<tr>");
					sb.append("<td align=center width=45><font color=" + getColor(mission.getType()) + ">" + mission.getType().name() + "</font></td>");
					sb.append("<td align=left fixwidth=165><font color=808080>" + mission.getName() + "</font></td>");
					sb.append("<td align=center fixwidth=40>" + getIcon(activeChar, mission) + "</td>");
					sb.append("<td fixwidth=40><button value=\"\" action=\"bypass -h npc_" + getObjectId() + "_redirect mission.htm 0 " + mission.getId() + "\" width=16 height=16 back=L2UI_CH3.shortcut_next_Down fore=L2UI_CH3.shortcut_next></td>");
					sb.append("</tr>");
					sb.append("</table>");
				}

				sb.append("<br>");
				sb.append("<table width=90>");
				sb.append("<tr>");

				final int maxPages = (list.size() - 1) / RESULT_PER_PAGE;

				if (page > 0)
				{
					sb.append("<td><button action=\"bypass -h npc_" + getObjectId() + "_redirect index.htm " + Math.max(0, page - 1) + "\" width=15 height=15 back=L2UI_CH3.Button.prev1_down fore=L2UI_CH3.Button.prev1></td>");
				}

				sb.append("<td align=center>Page " + (page + 1) + "</td>");

				if (maxPages > page)
				{
					sb.append("<td><button action=\"bypass -h npc_" + getObjectId() + "_redirect index.htm " + Math.min(maxPages, page + 1) + "\" width=15 height=15 back=L2UI_CH3.Button.next1_down fore=L2UI_CH3.Button.next1></td>");
				}

				sb.append("</tr>");
				sb.append("</table>");

				htm.replace("%list%", sb.toString());
			}
			case "mission" ->
			{
				if (parameters.length == 1)
				{
					final AbstractDailyMission<?> mission = DailyMissionData.getInstance().getMission(Integer.parseInt(parameters[0]));

					if (Objects.isNull(mission))
					{
						_log.log(Level.WARNING, getClass().getSimpleName() + ": Failed to find mission with id: " + Integer.parseInt(parameters[0]) + " for " + activeChar);
						return;
					}

					final StringBuilder sb = new StringBuilder();

					for (final IntIntHolder holder : mission.getItems())
					{
						sb.append("<tr>");
						sb.append("<td align=center><font color=B09878>" + ItemData.getInstance().getTemplate(holder.getId()).getName() + " " + holder.getValue() + "</font></td>");
						sb.append("</tr>");
					}

					htm.replace("%name%", mission.getName());
					htm.replace("%type%", mission.getType().toString());
					htm.replace("%progress%", SysUtil.getHpGauge(180, Math.min(mission.getStatus(), activeChar.getMission().getCounter(mission.getId())), mission.getStatus(), false));
					htm.replace("%time_left%", activeChar.getMission().getMissionState(mission) == MissionStateType.NOT_READY ? SysUtil.formatMillisToTime(activeChar.getMission().getReuseDelay(mission.getId()) - System.currentTimeMillis()) : "-");
					htm.replace("%button%", getButton(activeChar, mission));
					htm.replace("%items%", sb.toString());
				}
			}
		}

		activeChar.sendPacket(htm);
	}

	/**
	 * @param type
	 * @return {@code String}
	 */
	private static String getColor(final MissionType type)
	{
		return switch (type)
		{
			case SINGLE -> "F9DA8E";
			case DAILY -> "8AFCF9";
			case WEEKLY -> "FCA28A";
			case REPEAT -> "8AFCD1";
		};
	}

	/**
	 * @param activeChar
	 * @param mission
	 * @return {@code String}
	 */
	private static String getIcon(final Player activeChar, final AbstractDailyMission<?> mission)
	{
		if (activeChar.getMission().getReuseDelay(mission.getId()) == PlayerMission.PENDING_REWARD)
		{
			return "<img src=L2UI_CT1.Icon_DF_Common_Weight width=16 height=16";
		}

		return switch (activeChar.getMission().getMissionState(mission))
		{
			case NOT_QUALIFIED, CANNOT_TAKE_AGAIN -> "<img src=L2UI_CT1.PostWnd_DF_Icon_SafetyTrade width=16 height=16>";
			case NOT_READY -> "<img src=L2UI_CH3.fishing_clockicon width=16 height=16>";
			case AVAILABLE -> "<img src=L2UI_CH3.minimap_party width=16 height=16>";
			default -> SysUtil.getHpGauge(35, Math.min(mission.getStatus(), activeChar.getMission().getCounter(mission.getId())), mission.getStatus(), false);
		};
	}

	/**
	 * @param activeChar
	 * @param mission
	 * @return {@code String}
	 */
	private String getButton(final Player activeChar, final AbstractDailyMission<?> mission)
	{
		if (activeChar.getMission().getReuseDelay(mission.getId()) == PlayerMission.PENDING_REWARD)
		{
			return "<button value=\"Receive Reward\" action=\"bypass -h npc_" + getObjectId() + "_mission reward " + mission.getId() + "\" width=82 height=22 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\">";
		}

		return switch (activeChar.getMission().getMissionState(mission))
		{
			case ACTIVE -> "<button value=\"Cancel Mission\" action=\"bypass -h npc_" + getObjectId() + "_mission cancel " + mission.getId() + "\" width=82 height=22 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\">";
			case NOT_QUALIFIED, NOT_READY -> "<button value=\"Mission Unavailable\" action=\"bypass -h npc_" + getObjectId() + "_redirect mission.htm 0 " + mission.getId() + "\" width=82 height=22 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\">";
			case CANNOT_TAKE_AGAIN -> "<button value=\"Mission Completed\" action=\"bypass -h npc_" + getObjectId() + "_redirect mission.htm 0 " + mission.getId() + "\" width=82 height=22 back=\"aoq.DevTeamBtnsByAllInOne.LoginBTN_ani002\" fore=\"L2UI.DefaultButton\">";
			default -> "<button align=center value=\"Accept Mission\"  action=\"bypass -h npc_" + getObjectId() + "_mission accept " + mission.getId() + "\" width=128 height=64 back=\"aoq.DevTeamBtnsByAllInOne.LoginBTN_ani0022\" fore=\"aoq.DevTeamBtnsByAllInOne.LoginBTN_ani0021\">";
		};
	}
}