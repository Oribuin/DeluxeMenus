package com.extendedclip.deluxemenus.utils;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

public class StringUtils {

  private static final Pattern GRADIENT_PATTERN = Pattern
      .compile("<(?<type>gradient|g)(?<hex>(:#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})){2,})>",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern STOP = Pattern
      .compile("<(gradient|g)(#(\\d+))?((:#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})){2,})(:(l|L|loop))?>|" +
          "&#([A-Fa-f0-9]){6}|" +
          org.bukkit.ChatColor.COLOR_CHAR, Pattern.CASE_INSENSITIVE
      );

  private final static Pattern HEX_PATTERN = Pattern
      .compile("&(#[a-f0-9]{6})", Pattern.CASE_INSENSITIVE);

  /**
   * Translates the ampersand color codes like '&7' to their section symbol counterparts like '§7'.
   * <br>
   * It also translates hex colors like '&#aaFF00' to their section symbol counterparts like
   * '§x§a§a§F§F§0§0'.
   *
   * @param input The string in which to translate the color codes.
   * @return The string with the translated colors.
   */
  @NotNull
  public static String color(@NotNull String input) {
    //Hex Support for 1.16.1+
    Matcher hexMatcher = HEX_PATTERN.matcher(input);
    if (VersionHelper.IS_HEX_VERSION) {
      input = parseGradient(input);

      while (hexMatcher.find()) {
        input = input.replace(hexMatcher.group(), ChatColor.of(hexMatcher.group(1)).toString());
      }
    }

    return ChatColor.translateAlternateColorCodes('&', input);
  }


  /**
   * Parses a hex code like <gradient:#aaFF00:#FFaa00> into a gradient.
   *
   * @param input The string to parse.
   * @return The parsed string.
   */
  private static String parseGradient(String input) {
    String parsed = input;

    Matcher matcher = GRADIENT_PATTERN.matcher(parsed);
    while (matcher.find()) {
      StringBuilder parsedGradient = new StringBuilder();
      List<Color> hexSteps = Arrays.stream(matcher.group("hex").substring(1).split(":"))
          .map(hex -> hex.length() != 4 ? hex
              : String.format("#%s%s%s%s%s%s", hex.charAt(1), hex.charAt(1), hex.charAt(2),
                  hex.charAt(2), hex.charAt(3), hex.charAt(3)))
          .map(Color::decode)
          .collect(Collectors.toList());

      int stop = findStop(parsed, matcher.end());
      String content = parsed.substring(matcher.end(), stop);
      int contentLength = content.length();
      char[] chars = content.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "KkLlMmNnOoRr".indexOf(chars[i + 1]) > -1) {
                contentLength -= 2;
            }
        }

      Gradient gradient = new Gradient(hexSteps, contentLength);

      String compoundedFormat = ""; // Carry the format codes through the gradient
      for (int i = 0; i < chars.length; i++) {
        char c = chars[i];
        if (c == '&' && i + 1 < chars.length) {
          char next = chars[i + 1];
          org.bukkit.ChatColor color = org.bukkit.ChatColor.getByChar(next);
          if (color != null && color.isFormat()) {
            compoundedFormat += String.valueOf(ChatColor.COLOR_CHAR) + next;
            i++; // Skip next character
            continue;
          }
        }
        parsedGradient.append(gradient.nextChatColor()).append(compoundedFormat).append(c);
      }

      String before = parsed.substring(0, matcher.start());
      String after = parsed.substring(stop);
      parsed = before + parsedGradient + after;
      matcher = GRADIENT_PATTERN.matcher(parsed);
    }

    return parsed;
  }
  /**
   * Finds the closest hex or ChatColor value as the hex string
   *
   * @param hex The hex color
   * @return The closest ChatColor value
   */
  public static ChatColor translateHex(String hex) {
    if (VersionHelper.IS_HEX_VERSION)
      return ChatColor.of(hex);
    return translateHex(Color.decode(hex));
  }

  public static ChatColor translateHex(Color color) {
    if (VersionHelper.IS_HEX_VERSION)
      return ChatColor.of(color);

    int minDist = Integer.MAX_VALUE;
    ChatColor legacy = ChatColor.WHITE;
    for (ChatColorHexMapping mapping : ChatColorHexMapping.values()) {
      int r = mapping.getRed() - color.getRed();
      int g = mapping.getGreen() - color.getGreen();
      int b = mapping.getBlue() - color.getBlue();
      int dist = r * r + g * g + b * b;
      if (dist < minDist) {
        minDist = dist;
        legacy = mapping.getChatColor();
      }
    }

    return legacy;
  }

  /**
   * Maps hex codes to ChatColors
   */
  public enum ChatColorHexMapping {

    BLACK(0x000000, ChatColor.BLACK),
    DARK_BLUE(0x0000AA, ChatColor.DARK_BLUE),
    DARK_GREEN(0x00AA00, ChatColor.DARK_GREEN),
    DARK_AQUA(0x00AAAA, ChatColor.DARK_AQUA),
    DARK_RED(0xAA0000, ChatColor.DARK_RED),
    DARK_PURPLE(0xAA00AA, ChatColor.DARK_PURPLE),
    GOLD(0xFFAA00, ChatColor.GOLD),
    GRAY(0xAAAAAA, ChatColor.GRAY),
    DARK_GRAY(0x555555, ChatColor.DARK_GRAY),
    BLUE(0x5555FF, ChatColor.BLUE),
    GREEN(0x55FF55, ChatColor.GREEN),
    AQUA(0x55FFFF, ChatColor.AQUA),
    RED(0xFF5555, ChatColor.RED),
    LIGHT_PURPLE(0xFF55FF, ChatColor.LIGHT_PURPLE),
    YELLOW(0xFFFF55, ChatColor.YELLOW),
    WHITE(0xFFFFFF, ChatColor.WHITE);

    private final int r, g, b;
    private final ChatColor chatColor;

    ChatColorHexMapping(int hex, ChatColor chatColor) {
      this.r = (hex >> 16) & 0xFF;
      this.g = (hex >> 8) & 0xFF;
      this.b = hex & 0xFF;
      this.chatColor = chatColor;
    }

    public int getRed() {
      return this.r;
    }

    public int getGreen() {
      return this.g;
    }

    public int getBlue() {
      return this.b;
    }

    public ChatColor getChatColor() {
      return this.chatColor;
    }

  }
  /**
   * Returns the index before the color changes
   *
   * @param content     The content to search through
   * @param searchAfter The index at which to search after
   * @return the index of the color stop, or the end of the string index if none is found
   */
  private static int findStop(String content, int searchAfter) {
    Matcher matcher = STOP.matcher(content);
    while (matcher.find()) {
        if (matcher.start() > searchAfter) {
            return matcher.start();
        }
    }
    return content.length();
  }

}
