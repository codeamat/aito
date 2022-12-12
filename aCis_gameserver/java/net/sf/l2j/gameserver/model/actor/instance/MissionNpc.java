package net.sf.l2j.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import net.sf.l2j.commons.data.Pagination;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.data.xml.MissionData;
import net.sf.l2j.gameserver.enums.actors.MissionType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.holder.MissionHolder;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author StinkyMadness
 */
public class MissionNpc extends Folk
{
	private final static int PAGE_LIMIT = 6;
	
	public MissionNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken(); // skip command
		
		if (command.startsWith("Chat"))
		{
			try
			{
				showChatWindow(player, Integer.parseInt(st.nextToken()));
			}
			catch (Exception e)
			{
				showChatWindow(player, 1);
			}
		}
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath(getNpcId(), 0));
		html.replace("%objectId%", getObjectId());
		html.replace("%list%", getList(player, Math.max(val, 1)));
		player.sendPacket(html);
	}
	
	private String getList(Player player, int page)
	{
		final StringBuilder sb = new StringBuilder();
		final Pagination<MissionType> list = new Pagination<>(player.getMissions().getAvailableTypes().stream(), page, PAGE_LIMIT);
		for (MissionType type : list)
		{
			final IntIntHolder mission = player.getMissions().getMission(type);
			final MissionHolder data = MissionData.getInstance().getMissionByLevel(type, mission.getId() + 1);
			if (data == null)
				continue;
			
			final boolean completed = data.getLevel() == mission.getId();
			sb.append("<table width=296 bgcolor=000000><tr><td width=296 align=center>" + generateBar(286, 4, completed ? data.getRequired() : mission.getValue(), data.getRequired()) + "</td></tr></table><table width=296 bgcolor=000000><tr>");
			sb.append("<td width=40 height=40 align=center><button width=32 height=32 back=" + data.getIcon() + " fore=" + data.getIcon() + "></td>");
			sb.append("<td width=256><font color=LEVEL>Lv " + data.getLevel() + "</font> " + data.getName() + " " + (completed ? "<font color=00FF00>Completed</font>" : "") + "<br1>");
			sb.append("<font color=B09878>" + (completed ? "[" + data.getName() + " achievement completed]" : data.getDescription().replaceAll("%remain%", StringUtil.formatNumber(data.getRequired() - mission.getValue()))) + "</font></td></tr></table><img src=L2UI.SquareGray width=296 height=1>");
		}
		list.generateSpace(sb, "<img height=47>");
		sb.append("<img src=L2UI.SquareWhite width=296 height=1>");
		list.generatePages(sb, "bypass npc_" + getObjectId() + "_Chat %page%");
		return sb.toString();
	}
	
	public String generateBar(int width, int height, int current, int max)
	{
		final StringBuilder sb = new StringBuilder();
		current = current > max ? max : current;
		int bar = Math.max((width * (current * 100 / max) / 100), 0);
		sb.append("<table width=" + width + " cellspacing=0 cellpadding=0><tr><td width=" + bar + " align=center><img src=L2UI_CH3.BR_BAR1_CP width=" + bar + " height=" + height + "/></td>");
		sb.append("<td width=" + (width - bar) + " align=center><img src=L2UI_CH3.BR_BAR1_HP1 width=" + (width - bar) + " height=" + height + "/></td></tr></table>");
		return sb.toString();
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		return "data/html/mods/mission/" + npcId + "" + (val == 0 ? "" : "-" + val) + ".htm";
	}
}