package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.model.DressMe;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

public class DressMeData implements IXmlReader
{
	private final List<DressMe> _entries = new ArrayList<>();
	
	public DressMeData()
	{
		load();
	}
	
	public void reload()
	{
		_entries.clear();
		load();
	}
	
	@Override
	public void load()
	{
		parseFile("./data/xml/dressme.xml");
		LOGGER.info("Loaded {} dressme templates.", _entries.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "dressme", enchantNode ->
		{
			NamedNodeMap attrs = enchantNode.getAttributes();
			
			final int itemId = Integer.valueOf(attrs.getNamedItem("itemId").getNodeValue());
			final int chest = Integer.valueOf(attrs.getNamedItem("chest").getNodeValue());
			final int legs = Integer.valueOf(attrs.getNamedItem("legs").getNodeValue());
			final int hair = Integer.valueOf(attrs.getNamedItem("hair").getNodeValue());
			final int gloves = Integer.valueOf(attrs.getNamedItem("gloves").getNodeValue());
			final int feet = Integer.valueOf(attrs.getNamedItem("feet").getNodeValue());
			
			_entries.add(new DressMe(itemId, chest, legs, hair, gloves, feet));
		}));
	}
	
	public DressMe getItemId(int itemId)
	{
		return _entries.stream().filter(x -> x.getItemId() == itemId).findFirst().orElse(null);
	}
	
	public static DressMeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DressMeData INSTANCE = new DressMeData();
	}
}