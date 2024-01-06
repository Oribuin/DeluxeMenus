package com.extendedclip.deluxemenus.utils;

import static com.extendedclip.deluxemenus.utils.StringUtils.translateHex;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.ChatColor;


/**
 * Allows generation of a multi-part gradient with a defined number of steps
 */
public class Gradient {

  private final List<TwoStopGradient> gradients;
  private final int steps;
  protected long step;

  public Gradient(List<Color> colors, int steps) {
    if (colors.size() < 2) {
      throw new IllegalArgumentException("Must provide at least 2 colors");
    }

    this.gradients = new ArrayList<>();
    this.steps = steps;
    this.step = 0;

    float increment = (float) (this.steps - 1) / (colors.size() - 1);
    for (int i = 0; i < colors.size() - 1; i++) {
      this.gradients.add(new TwoStopGradient(colors.get(i), colors.get(i + 1), increment * i,
          increment * (i + 1)));
    }
  }

  public ChatColor nextChatColor() {
    // Gradients will use the first color if the entire spectrum won't be available to preserve prettiness
    if (!VersionHelper.IS_HEX_VERSION || this.steps <= 1) {
      return translateHex(this.gradients.get(0).colorAt(0));
    }
    return translateHex(this.nextColor());
  }

  public Color nextColor() {
    // Do some wizardry to get a function that bounces back and forth between 0 and a cap given an increasing input
    // Thanks to BomBardyGamer for assisting with this
    int adjustedStep = (int) Math.round(Math.abs(
        ((2 * Math.asin(Math.sin(this.step * (Math.PI / (2 * this.steps))))) / Math.PI)
            * this.steps));

    Color color;
    if (this.gradients.size() < 2) {
      color = this.gradients.get(0).colorAt(adjustedStep);
    } else {
      float segment = (float) this.steps / this.gradients.size();
      int index = (int) Math.min(Math.floor(adjustedStep / segment), this.gradients.size() - 1);
      color = this.gradients.get(index).colorAt(adjustedStep);
    }

    this.step++;
    return color;
  }

  public static class TwoStopGradient {

    private final Color startColor;
    private final Color endColor;
    private final float lowerRange;
    private final float upperRange;

    private TwoStopGradient(Color startColor, Color endColor, float lowerRange, float upperRange) {
      this.startColor = startColor;
      this.endColor = endColor;
      this.lowerRange = lowerRange;
      this.upperRange = upperRange;
    }

    /**
     * Gets the color of this gradient at the given step
     *
     * @param step The step
     * @return The color of this gradient at the given step
     */
    public Color colorAt(int step) {
      return new Color(
          this.calculateHexPiece(step, this.startColor.getRed(), this.endColor.getRed()),
          this.calculateHexPiece(step, this.startColor.getGreen(), this.endColor.getGreen()),
          this.calculateHexPiece(step, this.startColor.getBlue(), this.endColor.getBlue())
      );
    }

    private int calculateHexPiece(int step, int channelStart, int channelEnd) {
      float range = this.upperRange - this.lowerRange;
      if (range == 0) // No range, don't divide by 0
      {
        return channelStart;
      }
      float interval = (channelEnd - channelStart) / range;
      return Math.min(Math.max(Math.round(interval * (step - this.lowerRange) + channelStart), 0),
          255);
    }

  }

}