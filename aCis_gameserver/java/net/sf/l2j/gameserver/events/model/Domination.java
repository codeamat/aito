package net.sf.l2j.gameserver.events.model;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * @author StinkyMadness
 */
public class Domination extends AbstractEvent
{
    @Override
    public void onProgress(int timer)
    {
        if (_npcs.isEmpty())
            return;

        _npcs.stream().filter(npc -> npc.getNpcId() == 36006).forEach(npc ->
        {
            for (Player player : npc.getKnownTypeInRadius(Player.class, 250))
            {
                if (player == null || player.isDead() || !player.isInEvent())
                    continue;

                final int score = onScore(player);
                player.setTitle(String.format("%s point(s)", score));
                player.broadcastTitleInfo();
            }
        });
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
        StringBuilder sb = new StringBuilder();
        getTeams().forEach(team -> sb.append(team.getName() + ": " + StringUtil.formatNumber(team.score()) + " Points | "));
        return sb.toString();
    }
}