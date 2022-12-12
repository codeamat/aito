package net.sf.l2j.gameserver.data.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.pool.ConnectionPool;

/**
 * @author ProjectX
 *
 */
public class IconData
{
	private static final Map<Integer, String> ICON = new HashMap<>();
	
	public IconData()
	{
		restoreMe();
	}
	
	public void restoreMe()
	{
		try(Connection con = ConnectionPool.getConnection(); PreparedStatement st = con.prepareStatement("SELECT * FROM icons"); ResultSet rs = st.executeQuery())
		{
			while(rs.next())
			{
				ICON.put(rs.getInt(1), rs.getString(2));
			}
		}
		catch(final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public String getIcon(final int itemId)
	{
		return ICON.getOrDefault(itemId, "");
	}
	
	public static IconData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		static final IconData INSTANCE = new IconData();
	}
}
