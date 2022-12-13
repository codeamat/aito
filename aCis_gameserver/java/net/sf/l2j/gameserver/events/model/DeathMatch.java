package net.sf.l2j.gameserver.events.model;

import java.util.Comparator;

import net.sf.l2j.gameserver.events.enums.RewardType;
import net.sf.l2j.gameserver.events.holder.PlayerHolder;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author StinkyMadness
 */
public class DeathMatch extends AbstractEvent
{
	@Override
	public void onKill(Player player, Creature creature)
	{
		if (creature instanceof Player)
		{
			final int score = onScore(player);
			player.setTitle(String.format("Kills : %s", score));
			player.broadcastTitleInfo();
		}
		super.onKill(player, creature);
	}
	
	@Override
	public boolean canAttack(Player player, Creature target)
	{
		return true;
	}
	
	@Override
	public void calculateRewards()
	{
		if (_highestPlayerScore == 0)
		{
			announceToAll("There was no winner.");
			return;
		}
		
		if (_players.stream().allMatch(playerHolder -> playerHolder.score() == _highestPlayerScore))
		{
			_players.forEach(playerHolder -> _holder.getRewards(RewardType.TIE).forEach(rewardHolder -> rewardHolder.reward(playerHolder.getPlayer())));
			return;
		}
		
		for (PlayerHolder playerHolder : _players)
		{
			if (_holder.hasRewardType(RewardType.WINNER) && playerHolder.score() == _highestPlayerScore)
				_holder.getRewards(RewardType.WINNER).forEach(rewardHolder -> rewardHolder.reward(playerHolder.getPlayer()));
			else if (_holder.hasRewardType(RewardType.LOSSER))
				_holder.getRewards(RewardType.LOSSER).forEach(rewardHolder -> rewardHolder.reward(playerHolder.getPlayer()));
		}
	}
	
	@Override
	public String getMessage()
	{
		final PlayerHolder playerHolder = _players.stream().max(Comparator.comparingInt(PlayerHolder::score)).get();
		return (playerHolder == null || playerHolder.score() < 1) ? "Still nobody have score | " : playerHolder.getPlayer().getName() + " have " + playerHolder.score() + " kill(s) | ";
	}
}