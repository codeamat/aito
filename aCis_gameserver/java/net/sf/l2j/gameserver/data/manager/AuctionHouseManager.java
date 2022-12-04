package net.sf.l2j.gameserver.data.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.AuctionHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

/**
 * @author StinkyMadness
 */
public class AuctionHouseManager implements Runnable
{
	private static final CLogger LOGGER = new CLogger(AuctionHouseManager.class.getName());
	
	private static final String SELECT_AUCTION = "SELECT * FROM auction_house";
	private static final String INSERT_AUCTION = "INSERT INTO auction_house VALUES (?,?,?,?,?,?,?,?)";
	private static final String DELETE_AUCTION = "DELETE FROM auction_house WHERE id = ?";
	
	private Map<Integer, AuctionHolder> _items = new ConcurrentHashMap<>();
	private int _lastId = 0;
	
	public AuctionHouseManager()
	{
		// Run task each second.
		ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
		
		// Load items from database.
		load();
	}
	
	@Override
	public void run()
	{
		if (_items.isEmpty())
			return;
		
		for (Entry<Integer, AuctionHolder> temp : getItems().entrySet())
		{
			AuctionHolder auctionItem = _items.get(temp.getKey());
			
			if (auctionItem.getTimer() > System.currentTimeMillis())
				continue;
			
			final Player owner = World.getInstance().getPlayer(auctionItem.getOwnerId());
			if (owner == null)
				sendItemToOffline(auctionItem.getOwnerId(), auctionItem.getItemId(), auctionItem.getItemCount(), auctionItem.getEnchantLevel());
			else
			{
				owner.sendMessage("For action item " + ItemData.getInstance().getTemplate(auctionItem.getItemId()).getName() + " remain time over.");
				ItemInstance item = owner.addItem("auction", auctionItem.getItemId(), auctionItem.getItemCount(), owner, true);
				if (auctionItem.getEnchantLevel() > 0)
					item.setEnchantLevel(auctionItem.getEnchantLevel());
			}
			
			deleteAuction(auctionItem);
		}
	}
	
	private void load()
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_AUCTION);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				int id = rs.getInt("id");
				
				_items.put(id, new AuctionHolder(id, rs.getInt("ownerId"), rs.getInt("itemId"), rs.getInt("itemCount"), rs.getInt("enchant"), rs.getInt("priceId"), rs.getInt("priceCount"), rs.getLong("timer")));
				
				_lastId = id > _lastId ? id : _lastId;
			}
			rs.close();
			ps.close();
		}
		catch (Exception e)
		{
			LOGGER.warn("Coundn't load() AuctionManager items.");
		}
		
		LOGGER.info("Loaded {} total auction items.", _items.size());
	}
	
	public void addAuction(AuctionHolder action)
	{
		_items.put(action.getId(), action);
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_AUCTION))
		{
			ps.setInt(1, action.getId());
			ps.setInt(2, action.getOwnerId());
			ps.setInt(3, action.getItemId());
			ps.setInt(4, action.getItemCount());
			ps.setInt(5, action.getEnchantLevel());
			ps.setInt(6, action.getPriceId());
			ps.setInt(7, action.getPriceCount());
			ps.setLong(8, action.getTimer());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warn("Coundn't addOrUpdateItem() to AuctionManager.");
		}
	}
	
	public void deleteAuction(AuctionHolder action)
	{
		_items.remove(action.getId());
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_AUCTION))
		{
			ps.setInt(1, action.getId());
			ps.execute();
			ps.close();
		}
		catch (Exception e)
		{
			LOGGER.warn("Coundn't deleteItem() from auction manager item list.");
		}
	}
	
	public void sendItemToOffline(int playerId, int itemId, int count, int enchant)
	{
		final Item item = ItemData.getInstance().getTemplate(itemId);
		if (item == null)
		{
			LOGGER.warn("Try to send NULL itemId={} to player objId={}", itemId, playerId);
			return;
		}
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement select = con.prepareStatement("SELECT count FROM items WHERE owner_id=? AND item_id=?"))
		{
			select.setInt(1, playerId);
			select.setInt(2, itemId);
			try (ResultSet rs = select.executeQuery())
			{
				if (rs.next() && item.isStackable())
				{
					try (PreparedStatement update = con.prepareStatement("UPDATE items SET count=? WHERE owner_id=? AND item_id=?"))
					{
						update.setInt(1, rs.getInt("count") + count);
						update.setInt(2, playerId);
						update.setInt(3, itemId);
						update.execute();
					}
				}
				else
				{
					try (PreparedStatement insert = con.prepareStatement("INSERT INTO items VALUES (?,?,?,?,?,?,?,?,?,?,?)"))
					{
						insert.setInt(1, playerId);
						insert.setInt(2, IdFactory.getInstance().getNextId());
						insert.setInt(3, itemId);
						insert.setInt(4, count);
						insert.setInt(5, enchant);
						insert.setString(6, "INVENTORY");
						insert.setInt(7, 0);
						insert.setInt(8, 0);
						insert.setInt(9, 0);
						insert.setInt(10, -60);
						insert.setLong(11, System.currentTimeMillis());
						insert.execute();
					}
				}
				rs.close();
			}
			select.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public AuctionHolder getItem(int id)
	{
		return _items.get(id);
	}
	
	public Map<Integer, AuctionHolder> getItems()
	{
		return _items;
	}
	
	public List<AuctionHolder> getAuctions(Player player, String search, boolean self)
	{
		List<AuctionHolder> list = new ArrayList<>();
		for (Map.Entry<Integer, AuctionHolder> temp : _items.entrySet())
		{
			final Item item = ItemData.getInstance().getTemplate(temp.getValue().getItemId());
			if (!self && temp.getValue().getOwnerId() == player.getObjectId())
				continue;
			
			if (self && temp.getValue().getOwnerId() != player.getObjectId())
				continue;
			
			if (search != null && !item.getName().contains(search))
				continue;
			
			list.add(temp.getValue());
		}
		return list;
	}
	
	public int getNextId()
	{
		return _lastId++;
	}
	
	public long getStoreTimer()
	{
		return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7);
	}
	
	public static final AuctionHouseManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AuctionHouseManager INSTANCE = new AuctionHouseManager();
	}
}