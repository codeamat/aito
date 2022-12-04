package net.sf.l2j.gameserver.model.actor.instance;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.math.MathUtil;

import net.sf.l2j.gameserver.data.manager.AuctionHouseManager;
import net.sf.l2j.gameserver.data.sql.PlayerInfoTable;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.AuctionHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author StinkyMadness
 */
public class AuctionHouse extends Folk
{
	private final Map<Player, Integer> _lastPage = new ConcurrentHashMap<>();
	
	private final static int PAGE_LIMIT = 6;
	private final static int MAX_ITEM_PER_PLAYER = 5;
	
	public AuctionHouse(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken(); // skip command
		
		if (command.startsWith("Chat"))
			showChatWindow(player, Integer.parseInt(st.nextToken()));
		else if (command.startsWith("page"))
		{
			final int page = Integer.parseInt(st.nextToken());
			final String search = st.hasMoreTokens() ? st.nextToken() : null;
			showAuctionList(player, page, search);
			_lastPage.put(player, page);
		}
		else if (command.startsWith("selectAuction"))
			showAuctionItem(player, Integer.parseInt(st.nextToken()));
		else if (command.startsWith("buyAuction"))
		{
			int auctionId = Integer.parseInt(command.substring(11));
			final AuctionHolder auction = AuctionHouseManager.getInstance().getItem(auctionId);
			if (auction == null)
			{
				player.sendMessage("Warning: Something goes wrong..");
				showAuctionList(player, _lastPage.get(player), null);
				return;
			}
			
			if (!player.destroyItemByItemId("Buy", auction.getPriceId(), auction.getPriceCount(), player, true))
				return;
			
			final Player seller = World.getInstance().getPlayer(auction.getOwnerId());
			if (seller != null && seller.isOnline())
			{
				seller.addItem("auction", auction.getPriceId(), auction.getPriceCount(), null, true);
				seller.sendMessage("You have sold an item in the Auction House.");
			}
			else
				AuctionHouseManager.getInstance().sendItemToOffline(auction.getOwnerId(), auction.getPriceId(), auction.getPriceCount(), auction.getEnchantLevel());
			
			AuctionHouseManager.getInstance().deleteAuction(auction);
			
			final ItemInstance item = player.addItem("auction", auction.getItemId(), auction.getItemCount(), this, true);
			if (auction.getEnchantLevel() > 0)
				item.setEnchantLevel(auction.getEnchantLevel());
			
			player.sendPacket(new ItemList(player, true));
			player.sendMessage("You have purchased an item from the Auction House.");
			showAuctionList(player, _lastPage.get(player), null);
		}
		else if (command.startsWith("mypage"))
		{
			final int page = Integer.parseInt(st.nextToken());
			final String search = st.hasMoreTokens() ? st.nextToken() : null;
			showMyAuctionList(player, page, search);
			_lastPage.put(player, page);
		}
		else if (command.startsWith("selectMyAuction"))
			showMyAuctionItem(player, Integer.parseInt(st.nextToken()));
		else if (command.startsWith("removeMyAuction"))
		{
			final AuctionHolder auction = AuctionHouseManager.getInstance().getItem(Integer.parseInt(st.nextToken()));
			if (auction == null)
			{
				player.sendMessage("Warning: Something goes wrong..");
				showAuctionList(player, _lastPage.get(player), null);
				return;
			}
			
			AuctionHouseManager.getInstance().deleteAuction(auction);
			
			ItemInstance item = player.addItem("auction", auction.getItemId(), auction.getItemCount(), this, true);
			if (auction.getEnchantLevel() > 0)
				item.setEnchantLevel(auction.getEnchantLevel());
			
			player.sendPacket(new InventoryUpdate());
			player.sendMessage("You have removed an item from the Auction House.");
			showMyAuctionList(player, _lastPage.get(player), null);
		}
		else if (command.startsWith("inventory"))
		{
			int page = Integer.parseInt(st.nextToken());
			showInventory(player, page);
			_lastPage.put(player, page);
		}
		else if (command.startsWith("selectItem"))
			showInventoryItem(player, Integer.parseInt(st.nextToken()));
		else if (command.startsWith("addItem"))
		{
			if (AuctionHouseManager.getInstance().getItems().values().stream().filter(x -> x.getOwnerId() == player.getObjectId()).count() >= MAX_ITEM_PER_PLAYER)
			{
				player.sendMessage("Warning: You can't add more then " + MAX_ITEM_PER_PLAYER + " items.");
				return;
			}
			
			if (st.countTokens() != 4)
			{
				player.sendMessage("Warning: Something goes wrong, contact admin.");
				return;
			}
			
			int itemId = Integer.parseInt(st.nextToken());
			int itemCount = Integer.parseInt(st.nextToken());
			String priceItem = st.nextToken();
			int priceEach = Integer.parseInt(st.nextToken());
			
			int priceId = 0;
			if (priceItem.equals("Adena"))
				priceId = 57;
			else if (priceItem.equals("GoldBar"))
				priceId = 3470;
			
			int enchant = player.getInventory().getItemByItemId(itemId).getEnchantLevel();
			
			if (!player.destroyItemByItemId("AddAuctionItem", itemId, itemCount, player, true))
				return;
			
			final long timeRemain = AuctionHouseManager.getInstance().getStoreTimer();
			AuctionHouseManager.getInstance().addAuction(new AuctionHolder(AuctionHouseManager.getInstance().getNextId(), player.getObjectId(), itemId, itemCount, enchant, priceId, priceEach, timeRemain));
			player.sendPacket(new InventoryUpdate());
			showInventory(player, _lastPage.get(player));
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	public void showAuctionList(Player player, int page, String search)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath(getNpcId(), 1));
		html.replace("%objectId%", getObjectId());
		
		// Retrieve the entire types list based on group type.
		List<AuctionHolder> list = AuctionHouseManager.getInstance().getAuctions(player, search, false);
		
		// Calculate page number.
		final int max = MathUtil.countPagesNumber(list.size(), PAGE_LIMIT);
		page = page > max ? max : page < 1 ? 1 : page;
		
		// Cut skills list up to page number.
		if (!list.isEmpty())
			list = list.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, list.size()));
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<img src=L2UI.SquareGray width=296 height=1>");
		
		int row = 0;
		for (AuctionHolder temp : list)
		{
			Item item = ItemData.getInstance().getTemplate(temp.getItemId());
			String name = item.getName();
			if (name.length() >= 40)
				name = name.substring(0, 37) + "...";
			
			sb.append("<table width=296 bgcolor=000000><tr>");
			sb.append("<td height=40 width=40><button action=\"bypass npc_" + getObjectId() + "_selectAuction " + temp.getId() + "\" width=32 height=32 back=" + item.getIcon() + " fore=" + item.getIcon() + "></td>");
			sb.append("<td width=256>" + name + " " + (item.isStackable() ? "(" + StringUtil.formatNumber(temp.getItemCount()) + ")" : temp.getEnchantLevel() > 0 ? "<font color=LEVEL>+" + temp.getEnchantLevel() + "</font>" : "") + "<br1>");
			sb.append("<font color=B09878>" + (item.isStackable() ? "Total Cost:" : "Cost:") + " " + StringUtil.formatNumber(temp.getPriceCount()) + " " + ItemData.getInstance().getTemplate(temp.getPriceId()).getName() + "</font></td>");
			sb.append("</tr></table>");
			sb.append("<img src=L2UI.SquareGray width=296 height=1>");
			row++;
		}
		
		for (int i = PAGE_LIMIT; i > row; i--)
			sb.append("<img height=41>");
		
		// Build page footer.
		sb.append("<img height=4><img src=L2UI.SquareGray width=296 height=1><table width=296 bgcolor=000000><tr>");
		sb.append("<td align=left width=75><button value=\"Back\" action=\"bypass npc_" + getObjectId() + "_Chat 0\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2></td>");
		sb.append("<td align=center width=75>" + (page > 1 ? "<button value=\"< PREV\" action=\"bypass npc_" + getObjectId() + "_page " + (page - 1) + "" + (search != null ? " " + search : "") + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("<td align=center width=71>Page " + (page < 1 ? 1 : page) + "</td>");
		sb.append("<td align=right width=75>" + (page < max ? "<button value=\"NEXT >\" action=\"bypass npc_" + getObjectId() + "_page " + (page + 1) + "" + (search != null ? " " + search : "") + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("</tr></table>");
		
		html.replace("%list%", sb.toString());
		player.sendPacket(html);
	}
	
	public void showAuctionItem(Player player, int id)
	{
		final AuctionHolder auction = AuctionHouseManager.getInstance().getItem(id);
		if (auction == null)
		{
			showAuctionList(player, 1, null);
			return;
		}
		
		final Item item = ItemData.getInstance().getTemplate(auction.getItemId());
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath(getNpcId(), 2));
		html.replace("%objectId%", getObjectId());
		html.replace("%auctionId%", auction.getId());
		html.replace("%last_page%", _lastPage.get(player));
		
		String name = item.getName();
		if (name.length() >= 40)
			name = name.substring(0, 37) + "...";
		
		html.replace("%item_icon%", item.getIcon());
		html.replace("%item_name%", name);
		html.replace("%item_enchant%", auction.getEnchantLevel() > 0 ? "<font color=LEVEL>+" + auction.getEnchantLevel() + "</font>" : "");
		html.replace("%item_count%", item.isStackable() ? " (" + StringUtil.formatNumber(auction.getItemCount()) + ")" : "");
		html.replace("%item_price%", StringUtil.formatNumber(auction.getPriceCount()) + " " + ItemData.getInstance().getTemplate(auction.getPriceId()).getName());
		
		html.replace("%owner_name%", PlayerInfoTable.getInstance().getPlayerName(auction.getOwnerId()));
		html.replace("%item_duration%", displayTime(auction.getTimer()));
		player.sendPacket(html);
	}
	
	public void showMyAuctionList(Player player, int page, String search)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath(getNpcId(), 3));
		html.replace("%objectId%", getObjectId());
		
		// Retrieve the entire types list based on group type.
		List<AuctionHolder> list = AuctionHouseManager.getInstance().getAuctions(player, search, true);
		
		// Calculate page number.
		final int max = MathUtil.countPagesNumber(list.size(), PAGE_LIMIT);
		page = page > max ? max : page < 1 ? 1 : page;
		
		// Cut skills list up to page number.
		if (!list.isEmpty())
			list = list.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, list.size()));
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<img src=L2UI.SquareGray width=296 height=1>");
		
		int row = 0;
		for (AuctionHolder temp : list)
		{
			Item item = ItemData.getInstance().getTemplate(temp.getItemId());
			String name = item.getName();
			if (name.length() >= 40)
				name = name.substring(0, 37) + "...";
			
			sb.append("<table width=296 bgcolor=000000><tr>");
			sb.append("<td height=40 width=40><button action=\"bypass npc_" + getObjectId() + "_selectMyAuction " + temp.getId() + "\" width=32 height=32 back=" + item.getIcon() + " fore=" + item.getIcon() + "></td>");
			sb.append("<td width=256>" + name + " " + (item.isStackable() ? "(" + StringUtil.formatNumber(temp.getItemCount()) + ")" : temp.getEnchantLevel() > 0 ? "<font color=LEVEL>+" + temp.getEnchantLevel() + "</font>" : "") + "<br1>");
			sb.append("<font color=B09878>" + (item.isStackable() ? "Total Cost:" : "Cost:") + " " + StringUtil.formatNumber(temp.getPriceCount()) + " " + ItemData.getInstance().getTemplate(temp.getPriceId()).getName() + "</font></td>");
			sb.append("</tr></table>");
			sb.append("<img src=L2UI.SquareGray width=296 height=1>");
			row++;
		}
		
		for (int i = PAGE_LIMIT; i > row; i--)
			sb.append("<img height=41>");
		
		// Build page footer.
		sb.append("<img height=4><img src=L2UI.SquareGray width=296 height=1><table width=296 bgcolor=000000><tr>");
		sb.append("<td align=left width=75><button value=\"Back\" action=\"bypass npc_" + getObjectId() + "_Chat 0\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2></td>");
		sb.append("<td align=center width=75>" + (page > 1 ? "<button value=\"< PREV\" action=\"bypass npc_" + getObjectId() + "_mypage " + (page - 1) + "" + (search != null ? " " + search : "") + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("<td align=center width=71>Page " + (page < 1 ? 1 : page) + "</td>");
		sb.append("<td align=right width=75>" + (page < max ? "<button value=\"NEXT >\" action=\"bypass npc_" + getObjectId() + "_mypage " + (page + 1) + "" + (search != null ? " " + search : "") + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("</tr></table>");
		
		html.replace("%list%", sb.toString());
		player.sendPacket(html);
	}
	
	public void showMyAuctionItem(Player player, int id)
	{
		final AuctionHolder auction = AuctionHouseManager.getInstance().getItem(id);
		final Item item = ItemData.getInstance().getTemplate(auction.getItemId());
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath(getNpcId(), 4));
		html.replace("%objectId%", getObjectId());
		html.replace("%auctionId%", auction.getId());
		html.replace("%last_page%", _lastPage.get(player));
		
		String name = item.getName();
		if (name.length() >= 40)
			name = name.substring(0, 37) + "...";
		
		html.replace("%item_icon%", item.getIcon());
		html.replace("%item_name%", name);
		html.replace("%item_enchant%", auction.getEnchantLevel() > 0 ? "<font color=LEVEL>+" + auction.getEnchantLevel() + "</font>" : "");
		html.replace("%item_count%", item.isStackable() ? " (" + StringUtil.formatNumber(auction.getItemCount()) + ")" : "");
		html.replace("%item_price%", StringUtil.formatNumber(auction.getPriceCount()) + " " + ItemData.getInstance().getTemplate(auction.getPriceId()).getName());
		
		html.replace("%owner_name%", PlayerInfoTable.getInstance().getPlayerName(auction.getOwnerId()));
		html.replace("%item_duration%", displayTime(auction.getTimer()));
		player.sendPacket(html);
	}
	
	public void showInventory(Player player, int page)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath(getNpcId(), 5));
		html.replace("%objectId%", getObjectId());
		
		// Retrieve the entire types list based on group type.
		List<ItemInstance> list = player.getInventory().getItems().stream().filter(x -> x.isAuctionItem() && x.isTradable() && !x.isEquipped()).collect(Collectors.toList());
		
		// Calculate page number.
		final int max = MathUtil.countPagesNumber(list.size(), PAGE_LIMIT);
		page = page > max ? max : page < 1 ? 1 : page;
		
		// Cut skills list up to page number.
		if (!list.isEmpty())
			list = list.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, list.size()));
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<img src=L2UI.SquareGray width=296 height=1>");
		
		int row = 0;
		for (ItemInstance temp : list)
		{
			Item item = ItemData.getInstance().getTemplate(temp.getItemId());
			String name = item.getName();
			if (name.length() >= 40)
				name = name.substring(0, 37) + "...";
			
			sb.append("<table width=296 bgcolor=000000><tr>");
			sb.append("<td height=40 width=40><button action=\"bypass npc_" + getObjectId() + "_selectItem " + item.getItemId() + "\" width=32 height=32 back=" + item.getIcon() + " fore=" + item.getIcon() + "></td>");
			sb.append("<td width=256>" + name + " " + (item.isStackable() ? "(" + StringUtil.formatNumber(temp.getCount()) + ")" : temp.getEnchantLevel() > 0 ? "<font color=LEVEL>+" + temp.getEnchantLevel() + "</font>" : "") + "<br1>");
			sb.append("<font color=B09878>Sore it for 7 day(s)</font></td>");
			sb.append("</tr></table>");
			sb.append("<img src=L2UI.SquareGray width=296 height=1>");
			row++;
		}
		
		for (int i = PAGE_LIMIT; i > row; i--)
			sb.append("<img height=41>");
		
		// Build page footer.
		sb.append("<img height=4><img src=L2UI.SquareGray width=296 height=1><table width=296 bgcolor=000000><tr>");
		sb.append("<td align=left width=75><button value=\"Back\" action=\"bypass npc_" + getObjectId() + "_Chat 0\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2></td>");
		sb.append("<td align=center width=75>" + (page > 1 ? "<button value=\"< PREV\" action=\"bypass npc_" + getObjectId() + "_inventory " + (page - 1) + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("<td align=center width=71>Page " + (page < 1 ? 1 : page) + "</td>");
		sb.append("<td align=right width=75>" + (page < max ? "<button value=\"NEXT >\" action=\"bypass npc_" + getObjectId() + "_inventory " + (page + 1) + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("</tr></table>");
		
		html.replace("%list%", sb.toString());
		player.sendPacket(html);
	}
	
	public void showInventoryItem(Player player, int itemId)
	{
		final ItemInstance item = player.getInventory().getItemByItemId(itemId);
		final StringBuilder sb = new StringBuilder();
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath(getNpcId(), 6));
		html.replace("%objectId%", getObjectId());
		
		String name = item.getName();
		if (name.length() >= 40)
			name = name.substring(0, 37) + "...";
		
		html.replace("%item_name%", name);
		html.replace("%icon%", ItemData.getInstance().getTemplate(itemId).getIcon());
		html.replace("%item_enchant%", item.getEnchantLevel() > 0 ? "<font color=LEVEL>+" + item.getEnchantLevel() + "</font>" : "");
		html.replace("%item_count%", item.isStackable() ? " (" + StringUtil.formatNumber(item.getCount()) + ")" : "");
		html.replace("%last_page%", _lastPage.get(player));
		
		if (item.isStackable())
		{
			sb.append("<img src=L2UI.SquareGray width=296 height=1>");
			sb.append("<table width=296 height=28 cellspacing=5 bgcolor=000000><tr>");
			sb.append("<td width=88><font color=B09878>Quanity:</font></td>");
			sb.append("<td width=188><edit var=itemCount type=number width=160 height=12></td>");
			sb.append("<td width=20></td>");
			sb.append("</tr></table>");
		}
		else
			sb.append("<img height=29>");
		
		sb.append("<img src=L2UI.SquareGray width=296 height=1>");
		sb.append("<table width=296 height=28 cellspacing=5 bgcolor=000000><tr>");
		sb.append("<td width=88><font color=B09878>" + (item.isStackable() ? "Total Price" : "Price") + " :</font></td>");
		sb.append("<td width=188><edit var=priceEach type=number width=160 height=12></td>");
		sb.append("<td width=20></td>");
		sb.append("</tr></table>");
		sb.append("<img src=L2UI.SquareGray width=296 height=1>");
		sb.append("<table width=296 height=28 cellspacing=5 bgcolor=000000><tr>");
		sb.append("<td width=88><font color=B09878>Type :</font></td>");
		sb.append("<td width=188><combobox var=priceItem list=Adena;GoldBar; width=160></td>");
		sb.append("<td width=20></td>");
		sb.append("</tr></table>");
		sb.append("<img src=L2UI.SquareGray width=296 height=1>");
		sb.append("<img height=10>");
		sb.append("<img src=L2UI.SquareGray width=270 height=1>");
		sb.append("<table width=270 bgcolor=000000><tr><td width=270 align=center><font color=B09878>Do you want to continue?</font></td></tr></table>");
		sb.append("<table width=270 bgcolor=000000><tr>");
		sb.append("<td width=134 align=right><button value=\"Confirm\" action=\"bypass npc_" + getObjectId() + "_addItem " + itemId + " " + (item.isStackable() ? "$itemCount" : "1") + " $priceItem $priceEach\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2></td>");
		sb.append("<td width=134 align=left><button value=\"Cancel\" action=\"bypass npc_" + getObjectId() + "_inventory " + _lastPage.get(player) + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2></td>");
		sb.append("</tr></table>");
		sb.append("<img src=L2UI.SquareGray width=270 height=1>");
		html.replace("%panel%", sb.toString());
		player.sendPacket(html);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath(getNpcId(), val));
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
		
		if (_lastPage.containsKey(player))
			_lastPage.remove(player);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		return "data/html/auctionhouse/" + npcId + (val == 0 ? "" : "-" + val) + ".htm";
	}
	
	private static String displayTime(long time)
	{
		final long remainingTime = (time - System.currentTimeMillis()) / 1000;
		final int days = (int) (remainingTime / 3600) / 24;
		final int hours = (int) (remainingTime / 3600);
		final int minutes = (int) ((remainingTime % 3600) / 60);
		final int seconds = (int) ((remainingTime % 3600) % 60);
		
		return days > 0 ? days + " day(s)" : hours > 0 ? hours + " hour(s)" : minutes > 0 ? minutes + " minute(s)" : seconds + " second(s)";
	}
}