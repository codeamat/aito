package net.sf.l2j.gameserver.events.holder;

import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * @author StinkyMadness
 */
public class PlayerHolder
{
	private final Player _player;
	private final String _title;
	private final int _colorTitle;
	private final int _colorName;
	private int _score;
	
	public PlayerHolder(Player player)
	{
		_player = player;
		_title = player.getTitle();
		_colorTitle = player.getAppearance().getTitleColor();
		_colorName = player.getAppearance().getNameColor();
		_score = 0;
	}
	
	public void clean()
	{
		_player.setTitle(_title);
		_player.getAppearance().setTitleColor(_colorTitle);
		_player.getAppearance().setNameColor(_colorName);
		_player.teleportTo(82872, 148600, -3464, 150);
		unfreeze();
	}
	
	public void increaseScore()
	{
		score(_score + 1);
	}
	
	public void score(int val)
	{
		_score = val;
	}
	
	public int score()
	{
		return _score;
	}
	
	public void sendPacket(L2GameServerPacket packet)
	{
		_player.sendPacket(packet);
	}
	
	public void freeze()
	{
		if (_player.isDead())
			_player.doRevive();
		
		_player.abortAll(false);
		_player.setIsParalyzed(true);
		_player.setInvul(true);
		_player.startAbnormalEffect(AbnormalEffect.ROOT);
	}
	
	public void unfreeze()
	{
		_player.setIsParalyzed(false);
		_player.setInvul(false);
		_player.stopAbnormalEffect(AbnormalEffect.ROOT);
	}
	
	public Player getPlayer()
	{
		return _player;
	}
}