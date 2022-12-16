package net.sf.l2j.gameserver.model;

public class DressMe
{
	private final int _itemId;
	private final int _hair;
	private final int _chest;
	private final int _legs;
	private final int _gloves;
	private final int _feet;
	
	public DressMe(int itemId, int chest, int legs, int hair, int gloves, int feet)
	{
		_itemId = itemId;
		_chest = chest;
		_legs = legs;
		_hair = hair;
		_gloves = gloves;
		_feet = feet;
	}
	
	public final int getItemId()
	{
		return _itemId;
	}

	public int getChest()
	{
		return _chest;
	}
	
	public int getLegs()
	{
		return _legs;
	}

	public int getHair()
	{
		return _hair;
	}
	
	public int getGloves()
	{
		return _gloves;
	}
	
	public int getFeet()
	{
		return _feet;
	}
}