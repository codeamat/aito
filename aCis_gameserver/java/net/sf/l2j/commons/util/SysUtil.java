package net.sf.l2j.commons.util;

import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.spawn.Spawn;

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

	private static final DateTimeFormatter DAILY_FORMATER = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("H:mm").toFormatter(Locale.ROOT);

	/**
	 * @implNote Allowed format: <BR>
	 *           <li><b>SUN 21:00</b></li>
	 *           <li><b>21:00</b></li>
	 * @param str
	 * @param compare - True will compare the current millisecond with the {@code Calendar} <BR>
	 *                and increase by <b>DAY</b> or <b>WEEK</b> depend on the format.
	 * @return {@value long}
	 */
	public static long parseWeeklyDate(final String str, final boolean compare)
	{
		final Calendar calendar = Calendar.getInstance();

		try
		{
			final TemporalAccessor temporal = DAILY_FORMATER.parse(str.trim());

			final LocalTime time = LocalTime.from(temporal);

			calendar.set(Calendar.HOUR_OF_DAY, time.getHour());
			calendar.set(Calendar.MINUTE, time.getMinute());
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);

			if (compare && calendar.getTimeInMillis() < System.currentTimeMillis())
			{
				calendar.add(Calendar.DAY_OF_MONTH, temporal.isSupported(ChronoField.DAY_OF_WEEK) ? 7 : 1);
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}

		return calendar.getTimeInMillis();
	}

	/**
	 * @param objects
	 * @return {@value true} if any {@code Object} is null
	 */
	public static boolean isAnyNull(final Object... objects)
	{
		if (Objects.isNull(objects))
		{
			return true;
		}

		for (final Object obj : objects)
		{
			if (Objects.isNull(obj))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * @param cast
	 * @param <T>
	 * @param id
	 * @param locational
	 * @return {@code Npc}
	 */
	public static <T> T addSpawn(final Class<T> cast, final int id, final Location locational)
	{
		return addSpawn(cast, id, locational, null);
	}

	/**
	 * @param cast
	 * @param <T>
	 * @param id
	 * @param locational
	 * @param consumer
	 * @return {@code Npc}
	 */
	public static <T> T addSpawn(final Class<T> cast, final int id, final Location locational, final Consumer<T> consumer)
	{
		return addSpawn(cast, id, locational.getX(), locational.getY(), locational.getZ(), 0, consumer);
	}

	/**
	 * @param cast
	 * @param <T>
	 * @param id
	 * @param x
	 * @param y
	 * @param z
	 * @return {@code Npc}
	 */
	public static <T> T addSpawn(final Class<T> cast, final int id, final int x, final int y, final int z)
	{
		return addSpawn(cast, id, x, y, z, 0);
	}

	/**
	 * @param <T>
	 * @param cast
	 * @param id
	 * @param x
	 * @param y
	 * @param z
	 * @param instanceId
	 * @return {@code Npc}
	 */
	public static <T> T addSpawn(final Class<T> cast, final int id, final int x, final int y, final int z, final int instanceId)
	{
		return addSpawn(cast, id, x, y, z, instanceId, null);
	}

	/**
	 * @param cast
	 * @param <T>
	 * @param id
	 * @param x
	 * @param y
	 * @param z
	 * @param consumer
	 * @return {@code Npc}
	 */
	public static <T> T addSpawn(final Class<T> cast, final int id, final int x, final int y, final int z, final Consumer<T> consumer)
	{
		return addSpawn(cast, id, x, y, z, 0, consumer);
	}

	/**
	 * @param cast
	 * @param <T>
	 * @param id
	 * @param x
	 * @param y
	 * @param z
	 * @param instanceId
	 * @param consumer
	 * @return {@code Npc}
	 */
	public static <T> T addSpawn(final Class<T> cast, final int id, final int x, final int y, final int z, final int instanceId, final Consumer<T> consumer)
	{
		try
		{
			final Spawn spawn = new Spawn(id);
			spawn.setLoc(x, y, z, 0);
			spawn.setInstanceId(instanceId);

			final T obj = cast.cast(spawn.doSpawn(false));

			if (Objects.nonNull(consumer))
			{
				consumer.accept(obj);
			}

			return obj;
		}
		catch (final Exception e)
		{

		}

		return null;
	}

	/**
	 * @param <T>
	 * @param element
	 * @param list
	 * @return {@code T}
	 */
	public static <T> T addToList(final T element, final List<T> list)
	{
		Objects.requireNonNull(list);

		if (Objects.nonNull(element))
		{
			list.add(element);
		}

		return element;
	}

	/**
	 * @param <T>
	 * @param element
	 * @param list
	 * @param consumer
	 * @return {@code T}
	 */
	public static <T> T addToList(final T element, final List<T> list, final Consumer<T> consumer)
	{
		if (Objects.nonNull(consumer))
		{
			consumer.accept(element);
		}

		return addToList(element, list);
	}

	/**
	 * @param <T>
	 * @param list
	 * @param element
	 * @param predicate
	 * @return {@code List<T>}
	 */
	public static <T> List<T> addIf(final List<T> list, final T element, final Predicate<T> predicate)
	{
		if (Objects.nonNull(list) && Objects.nonNull(element) && predicate.test(element))
		{
			list.add(element);
		}

		return list;
	}

	/**
	 * @param <K>
	 * @param <V>
	 * @param key
	 * @param value
	 * @param map
	 * @return {@code V}
	 */
	public static <K, V> V addToMap(final K key, final V value, final Map<K, V> map)
	{
		Objects.requireNonNull(map);

		if (Objects.nonNull(key) && Objects.nonNull(value))
		{
			map.put(key, value);
		}

		return value;
	}

	/**
	 * @param <K>
	 * @param <V>
	 * @param key
	 * @param value
	 * @param map
	 * @param consumer
	 * @return {@code V}
	 */
	public static <K, V> V addToMap(final K key, final V value, final Map<K, V> map, final Consumer<V> consumer)
	{
		if (Objects.nonNull(consumer))
		{
			consumer.accept(value);
		}

		return addToMap(key, value, map);
	}

	public static String formatSecondsToTime(final int time, final boolean cut)
	{
		final int days = time / 86400;
		final int hours = (time - days * 24 * 3600) / 3600;
		final int minutes = (time - days * 24 * 3600 - hours * 3600) / 60;
		final int seconds = time - days * 24 * 3600 - minutes * 3600;

		String result;

		if (days >= 1)
		{
			if (hours < 1 || cut)
			{
				result = days + " " + declension(days, DeclensionKey.DAYS);
			}
			else
			{
				result = days + " " + declension(days, DeclensionKey.DAYS) + " " + hours + " " + declension(hours, DeclensionKey.HOUR);
			}
		}
		else
		{
			if (hours >= 1)
			{
				if (minutes < 1 || cut)
				{
					result = hours + " " + declension(hours, DeclensionKey.HOUR);
				}
				else
				{
					result = hours + " " + declension(hours, DeclensionKey.HOUR) + " " + minutes + " " + declension(minutes, DeclensionKey.MINUTES);
				}
			}
			else if (minutes >= 1)
			{
				result = minutes + " " + declension(minutes, DeclensionKey.MINUTES);
			}
			else
			{
				result = seconds + " " + declension(seconds, DeclensionKey.SECONDS);
			}
		}

		return result;
	}

	private static String declension(long count, final DeclensionKey word)
	{
		final String declension = switch (word)
				{
					case DAYS -> "Day";
					case HOUR -> "Hour";
					case MINUTES -> "Minute";
					case SECONDS -> "Second";
					case PIECE -> "Piece";
					case POINT -> "Point";
				};

		if (count > 100L)
		{
			count %= 100L;
		}

		if (count > 20L)
		{
			count %= 10L;
		}

		return count == 1 ? declension : declension.concat("s");
	}

	private static enum DeclensionKey
	{
		DAYS, HOUR, MINUTES, SECONDS, PIECE, POINT;
	}

	private static final NumberFormat ADENA_FORMATTER = NumberFormat.getIntegerInstance(Locale.ENGLISH);

	public static String formatAdena(long amount)
	{
		synchronized (ADENA_FORMATTER)
		{
			return ADENA_FORMATTER.format(amount);
		}
	}
}