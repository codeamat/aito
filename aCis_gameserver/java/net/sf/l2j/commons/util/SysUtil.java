package net.sf.l2j.commons.util;

import net.sf.l2j.commons.lang.StringUtil;

/**
 * A class holding system oriented methods.
 */
public class SysUtil
{
	private static final int MEBIOCTET = 1024 * 1024;
	
	/**
	 * @return the used amount of memory the JVM is using.
	 */
	public static long getUsedMemory()
	{
		return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / MEBIOCTET;
	}
	
	/**
	 * @return the maximum amount of memory the JVM can use.
	 */
	public static long getMaxMemory()
	{
		return Runtime.getRuntime().maxMemory() / MEBIOCTET;
	}
	
	/**
	 * @param str - the string whose first letter to capitalize
	 * @return a string with the first letter of the {@code str} capitalized
	 */
	public static String capitalizeFirst(final String str)
	{
		if (str == null || str.isEmpty())
		{
			return str;
		}

		final char[] arr = str.toCharArray();
		final char c = arr[0];

		if (Character.isLetter(c))
		{
			arr[0] = Character.toUpperCase(c);
		}

		return new String(arr);
	}
	
	/**
	 * @param ms
	 * @return formatted time
	 */
	public static String formatMillisToTime(final long ms)
	{
		final int seconds = (int) (ms / 1000) % 60;
		final int minutes = (int) (ms / (1000 * 60) % 60);
		final int hours = (int) (ms / (1000 * 60 * 60) % 24);

		return hours + "h " + minutes + "m " + seconds + "s(s)";
	}
	
	/**
	 * Gets the HTML representation of HP gauge.
	 *
	 * @param width               the width
	 * @param current             the current value
	 * @param max                 the max value
	 * @param displayAsPercentage if {@code true} the text in middle will be displayed as percent else it will be displayed as "current / max"
	 * @return the HTML
	 */
	public static String getHpGauge(final int width, final long current, final long max, final boolean displayAsPercentage)
	{
		return getGauge(width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_HP_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_HP_Center", 17, -13);
	}
	
	/**
	 * Gets the HTML representation of a gauge.
	 *
	 * @param width               the width
	 * @param current             the current value
	 * @param max                 the max value
	 * @param displayAsPercentage if {@code true} the text in middle will be displayed as percent else it will be displayed as "current / max"
	 * @param backgroundImage     the background image
	 * @param image               the foreground image
	 * @param imageHeight         the image height
	 * @param top                 the top adjustment
	 * @return the HTML
	 */
	private static String getGauge(final int width, long current, final long max, final boolean displayAsPercentage, final String backgroundImage, final String image, final long imageHeight, final long top)
	{
		current = Math.min(current, max);

		final StringBuilder sb = new StringBuilder();
		StringUtil.append(sb, "<table width=", String.valueOf(width), " cellpadding=0 cellspacing=0><tr><td background=\"" + backgroundImage + "\">");
		StringUtil.append(sb, "<img src=\"" + image + "\" width=", String.valueOf((long) ((double) current / max * width)), " height=", String.valueOf(imageHeight), ">");
		StringUtil.append(sb, "</td></tr><tr><td align=center><table cellpadding=0 cellspacing=", String.valueOf(top), "><tr><td>");

		if (displayAsPercentage)
		{
			StringUtil.append(sb, "<table cellpadding=0 cellspacing=2><tr><td>", String.format("%.2f%%", (double) current / max * 100), "</td></tr></table>");
		}
		else
		{
			final String tdWidth = String.valueOf((width - 10) / 2);
			StringUtil.append(sb, "<table cellpadding=0 cellspacing=0><tr><td width=" + tdWidth + " align=right>", String.valueOf(current), "</td>");
			StringUtil.append(sb, "<td width=10 align=center>/</td><td width=" + tdWidth + ">", String.valueOf(max), "</td></tr></table>");
		}

		StringUtil.append(sb, "</td></tr></table></td></tr></table>");
		return sb.toString();
	}
}