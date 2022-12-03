package net.sf.l2j.gameserver.handler.itemhandlers;

import java.util.Calendar;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;

/**
 * 
 * @author Sarada
 *
 */
public class VipCoin implements IItemHandler
{
	private static final int ITEM_IDS[] = { Config.VIP_COIN_ID1,  Config.VIP_COIN_ID2, Config.VIP_COIN_ID3};
	
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		Player activeChar = (Player)playable;
		
		int itemId = item.getItemId();
		
		if (itemId == Config.VIP_COIN_ID1)
		{
			if (activeChar.isInOlympiadMode())
			{
				activeChar.sendMessage("This item cannot be used on Olympiad Games.");
				return;
			}
			else if (activeChar.isVip())
			{
				activeChar.sendMessage("Tu es deja VIP!.");
				return;
			}
			if (activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
			{
				if (activeChar.isVip())
				{
					long daysleft = (activeChar.getVipEndTime() - Calendar.getInstance().getTimeInMillis()) / 86400000L;
					activeChar.setEndTime("vip", (int)(daysleft + Config.VIP_DAYS_ID1));
					activeChar.sendMessage("Felicitation, tu viens de recevoir un autre " + Config.VIP_DAYS_ID1 + " jour de VIP.");
				}
				else
				{
					activeChar.setVip(true);
					activeChar.setEndTime("vip", Config.VIP_DAYS_ID1);
					activeChar.sendMessage("Felicitation, tu viens de devenir VIP pour " + Config.VIP_DAYS_ID1 + " jour.");
				}
				
				if (Config.ALLOW_VIP_NCOLOR && activeChar.isVip())
					activeChar.getAppearance().setNameColor(Config.VIP_NCOLOR);
				
				if (Config.ALLOW_VIP_TCOLOR && activeChar.isVip()) 
					activeChar.getAppearance().setTitleColor(Config.VIP_TCOLOR);
				
				activeChar.broadcastUserInfo();
				activeChar.sendPacket(new EtcStatusUpdate(activeChar));
			}
		}
		
		if (itemId == Config.VIP_COIN_ID2)
		{
			if (activeChar.isInOlympiadMode())
			{
				activeChar.sendMessage("This item cannot be used on Olympiad Games.");
				return;
			}
			else if (activeChar.isVip())
			{
				activeChar.sendMessage("Tu es deja VIP!.");
				return;
			}
			if (activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
			{
				if (activeChar.isVip())
				{
					long daysleft = (activeChar.getVipEndTime() - Calendar.getInstance().getTimeInMillis()) / 86400000L;
					activeChar.setEndTime("vip", (int)(daysleft + Config.VIP_DAYS_ID2));
					activeChar.sendMessage("Felicitation, tu viens de recevoir un autre " + Config.VIP_DAYS_ID2 + " jour de VIP.");
				}
				else
				{
					activeChar.setVip(true);
					activeChar.setEndTime("vip", Config.VIP_DAYS_ID2);
					activeChar.sendMessage("Felicitation, tu viens de devenir VIP pour " + Config.VIP_DAYS_ID2 + " jours.");
				}
				
				if (Config.ALLOW_VIP_NCOLOR && activeChar.isVip())
					activeChar.getAppearance().setNameColor(Config.VIP_NCOLOR);
				
				if (Config.ALLOW_VIP_TCOLOR && activeChar.isVip()) 
					activeChar.getAppearance().setTitleColor(Config.VIP_TCOLOR);
				
				activeChar.broadcastUserInfo();
				activeChar.sendPacket(new EtcStatusUpdate(activeChar));
			}
		}
		
		if (itemId == Config.VIP_COIN_ID3)
		{
			if (activeChar.isInOlympiadMode())
			{
				activeChar.sendMessage("This item cannot be used on Olympiad Games.");
				return;
			}
			else if (activeChar.isVip())
			{
				activeChar.sendMessage("Tu es deja VIP!.");
				return;
			}
			if (activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
			{
				if (activeChar.isVip())
				{
					long daysleft = (activeChar.getVipEndTime() - Calendar.getInstance().getTimeInMillis()) / 86400000L;
					activeChar.setEndTime("vip", (int)(daysleft + Config.VIP_DAYS_ID3));
					activeChar.sendMessage("Felicitation, tu viens de recevoir un autre " + Config.VIP_DAYS_ID3 + "jour de VIP.");
				}
				else
				{
					activeChar.setVip(true);
					activeChar.setEndTime("vip", Config.VIP_DAYS_ID3);
					activeChar.sendMessage("Felicitation, tu viens de devenir VIP pour " + Config.VIP_DAYS_ID3 + " jours.");
				}
				
				if (Config.ALLOW_VIP_NCOLOR && activeChar.isVip())
					activeChar.getAppearance().setNameColor(Config.VIP_NCOLOR);
				
				if (Config.ALLOW_VIP_TCOLOR && activeChar.isVip()) 
					activeChar.getAppearance().setTitleColor(Config.VIP_TCOLOR);
				
				activeChar.broadcastUserInfo();
				activeChar.sendPacket(new EtcStatusUpdate(activeChar));
			}
		}
	}
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
