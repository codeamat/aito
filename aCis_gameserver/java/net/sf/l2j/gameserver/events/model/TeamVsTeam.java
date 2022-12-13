package net.sf.l2j.gameserver.events.model;

import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * @author StinkyMadness
 */
public class TeamVsTeam extends AbstractEvent
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
	public boolean onUseSkill(Player player, L2Skill skill)
	{
		final WorldObject target = player.getTarget();
		final Player victim = target == null ? null : target.getActingPlayer();
		if (victim != null && getPlayerTeam(player) == getPlayerTeam(victim) && (skill.isOffensive() || skill.isDebuff()))
			return false;
		
		return super.onUseSkill(player, skill);
	}
	
	@Override
	public void calculateRewards()
	{
		rewardMVPs();
		rewardTeams();
	}
	
	@Override
	public String getMessage()
	{
		final StringBuilder sb = new StringBuilder();
		getTeams().forEach(team -> sb.append(team.getName() + ": " + team.score() + " | "));
		return sb.toString();
	}
}
