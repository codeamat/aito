package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.gameserver.data.xml.TeleportData;
import net.sf.l2j.gameserver.enums.TeleportType;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

/**
 * Spawn Gatekeepers at Lilith/Anakim deaths (after a 10sec delay).<BR>
 * Despawn them after 15 minutes.
 */
public class QoA_VIP_Teleporter extends Quest {
    private static final int VIP_TELEPORTER = 40000;

    public QoA_VIP_Teleporter() {
        super(-1, "Teleporter");

        addFirstTalkId(VIP_TELEPORTER);
        addTalkId(VIP_TELEPORTER);

    }

    @Override
    public String onFirstTalk(Npc npc, Player player) {


            return "40000.htm";

    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player)
    {
        TeleportData.getInstance().showTeleportList(player, npc, TeleportType.valueOf(event.toUpperCase()));

        return super.onAdvEvent(event, npc, player);
    }

    @Override
    public String onTalk(Npc npc, Player player)
    {
        return (player.isVip()) ? "vip.htm" : "vipteleporter-no.htm";
    }

}