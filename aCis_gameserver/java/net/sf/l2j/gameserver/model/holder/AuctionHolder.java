package net.sf.l2j.gameserver.model.holder;

/**
 * @author StinkyMadness
 */
public class AuctionHolder
{
	private final int _id;
	private final int _ownerId;
	private final int _itemId;
	private final int _itemCount;
	private final int _enchant;
	private final int _priceId;
	private final int _priceCount;
	private final long _timer;
	
	public AuctionHolder(int id, int ownerId, int itemId, int itemCount, int enchant, int priceId, int priceCount, long timer)
	{
		_id = id;
		_ownerId = ownerId;
		_itemId = itemId;
		_itemCount = itemCount;
		_enchant = enchant;
		_priceId = priceId;
		_priceCount = priceCount;
		_timer = timer;
	}
	
	public final int getId()
	{
		return _id;
	}
	
	public final int getOwnerId()
	{
		return _ownerId;
	}
	
	public final int getItemId()
	{
		return _itemId;
	}
	
	public final int getItemCount()
	{
		return _itemCount;
	}
	
	public final int getEnchantLevel()
	{
		return _enchant;
	}
	
	public final int getPriceId()
	{
		return _priceId;
	}
	
	public final int getPriceCount()
	{
		return _priceCount;
	}
	
	public final long getTimer()
	{
		return _timer;
	}
}