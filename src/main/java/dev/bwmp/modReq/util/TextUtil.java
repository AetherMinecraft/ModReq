package dev.bwmp.modReq.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for handling text formatting with both legacy and modern
 * Adventure text
 */
public class TextUtil {

	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
	private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand()
			.toBuilder()
			.character('&')
			.hexColors()
			.useUnusualXRepeatedCharacterHexFormat()
			.build();

	/**
	 * Format text using Adventure MiniMessage format or legacy & codes
	 * Automatically detects the format and converts appropriately
	 * 
	 * @param text The text to format (supports both & codes and MiniMessage)
	 * @return Component for sending to players
	 */
	public static Component format(String text) {
		if (text == null || text.isEmpty()) {
			return Component.empty();
		}

		// Check if text contains MiniMessage tags
		if (text.contains("<") && text.contains(">")) {
			try {
				return MINI_MESSAGE.deserialize(text);
			} catch (Exception e) {
				// If MiniMessage parsing fails, fall back to legacy
				return LEGACY_SERIALIZER.deserialize(text);
			}
		} else {
			// Use legacy & color codes
			return LEGACY_SERIALIZER.deserialize(text);
		}
	}

	/**
	 * Format text with legacy & color codes
	 * 
	 * @param text Text with & color codes
	 * @return Component for sending to players
	 */
	public static Component legacy(String text) {
		return LEGACY_SERIALIZER.deserialize(text);
	}

	/**
	 * Format text with MiniMessage format
	 * 
	 * @param text Text with MiniMessage tags like <green>, <bold>, etc.
	 * @return Component for sending to players
	 */
	public static Component mini(String text) {
		return MINI_MESSAGE.deserialize(text);
	}

	/**
	 * Create a success message (green)
	 */
	public static Component success(String message) {
		return format("&a" + message);
	}

	/**
	 * Create an error message (red)
	 */
	public static Component error(String message) {
		return format("&c" + message);
	}

	/**
	 * Create a warning message (yellow)
	 */
	public static Component warning(String message) {
		return format("&e" + message);
	}

	/**
	 * Create an info message (aqua)
	 */
	public static Component info(String message) {
		return format("&b" + message);
	}

	/**
	 * Create a highlight message (gold)
	 */
	public static Component highlight(String message) {
		return format("&6" + message);
	}

	/**
	 * Create a prefix for ModReq messages
	 */
	public static Component prefix() {
		return format("&6[ModReq] &f");
	}

	/**
	 * Create a ModReq message with prefix
	 */
	public static Component prefixed(String message) {
		return prefix().append(format(message));
	}

	/**
	 * Convert § color codes to & color codes for legacy compatibility
	 */
	public static String convertSectionToAmpersand(String text) {
		return text.replace('§', '&');
	}

	/**
	 * Create a relative time component with hover tooltip showing full date
	 * 
	 * @param dateTime The LocalDateTime to format
	 * @return Component with relative time and hover tooltip
	 */
	public static Component relativeTime(LocalDateTime dateTime) {
		String relativeText = getRelativeTimeString(dateTime);
		String fullDate = dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm:ss"));

		return Component.text(relativeText)
				.color(NamedTextColor.GRAY)
				.hoverEvent(HoverEvent.showText(
						Component.text(fullDate).color(NamedTextColor.WHITE)));
	}

	/**
	 * Create a colored relative time component with hover tooltip
	 * 
	 * @param dateTime The LocalDateTime to format
	 * @param color    The color for the relative time text
	 * @return Component with relative time and hover tooltip
	 */
	public static Component relativeTime(LocalDateTime dateTime, String color) {
		String relativeText = getRelativeTimeString(dateTime);
		String fullDate = dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm:ss"));

		return format(color + relativeText)
				.hoverEvent(HoverEvent.showText(
						Component.text(fullDate).color(NamedTextColor.WHITE)));
	}

	/**
	 * Get a human-readable relative time string
	 * 
	 * @param dateTime The LocalDateTime to format
	 * @return String like "5 minutes ago", "2 hours ago", etc.
	 */
	private static String getRelativeTimeString(LocalDateTime dateTime) {
		LocalDateTime now = LocalDateTime.now();

		long seconds = ChronoUnit.SECONDS.between(dateTime, now);
		long minutes = ChronoUnit.MINUTES.between(dateTime, now);
		long hours = ChronoUnit.HOURS.between(dateTime, now);
		long days = ChronoUnit.DAYS.between(dateTime, now);

		if (seconds < 60) {
			return seconds <= 1 ? "just now" : seconds + " seconds ago";
		} else if (minutes < 60) {
			return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
		} else if (hours < 24) {
			return hours == 1 ? "1 hour ago" : hours + " hours ago";
		} else if (days < 30) {
			return days == 1 ? "1 day ago" : days + " days ago";
		} else if (days < 365) {
			long months = days / 30;
			return months == 1 ? "1 month ago" : months + " months ago";
		} else {
			long years = days / 365;
			return years == 1 ? "1 year ago" : years + " years ago";
		}
	}
}
