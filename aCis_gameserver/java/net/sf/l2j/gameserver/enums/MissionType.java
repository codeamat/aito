package net.sf.l2j.gameserver.enums;

import net.sf.l2j.commons.util.SysUtil;

/**
 * @author Rationale
 */
public enum MissionType
{
	SINGLE, DAILY, WEEKLY, REPEAT;

	@Override
	public String toString()
	{
		return SysUtil.capitalizeFirst(name().toLowerCase());
	}
}
