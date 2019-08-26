package me.pietelite.einsteinsworkshopedu.tools.chat;

import java.net.MalformedURLException;
import java.net.URL;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

public class ClickableMessage {

  private Text text;

  private ClickableMessage(Text text) {
    this.text = text;
  }

  public Text getText() {
    return text;
  }

  public static Builder builder(Text messageBody) {
    return new Builder(messageBody);
  }

  public static class Builder {

    private Text.Builder builder;

    private Builder(Text messageBody) {
      builder = Text.builder().append(messageBody);
    }

    /**
     * To the end of the message under construction, adds a piece of text which can automatically
     * run a command with the clicker as the source once clicked.
     *
     * @param name         The text to display in the message
     * @param command      The command to run once clicked
     * @param hoverMessage The text to display once a cursor hovers over the clickable text
     * @return The builder, which can be used to continue building
     */
    public Builder addClickableCommand(String name, String command, Text hoverMessage) {
      Text clickable = Text.builder()
          .append(Text.of(TextColors.GOLD, TextStyles.ITALIC, " [",
              Text.of(TextColors.GRAY, name), "]"))
          .onClick(TextActions.runCommand(command))
          .onHover(TextActions.showText(hoverMessage))
          .build();
      builder.append(clickable);
      return this;
    }

    /**
     * To the end of the message under construction, adds a piece of text which can automatically
     * send the user to a designated URL once clicked.
     *
     * @param name The text to display in the message
     * @param url  The url to which the user will be sent
     * @return The builder, which can be used to continue building a ClickableMessage
     */
    public Builder addClickableUrl(String name, String url) throws MalformedURLException {
      Text clickable = Text.builder()
          .append(Text.of(TextColors.GOLD, TextStyles.ITALIC, " [",
              Text.of(TextColors.GRAY, name), "]"))
          .onClick(TextActions.openUrl(new URL(url)))
          .onHover(TextActions.showText(Text.of(TextColors.LIGHT_PURPLE, url)))
          .build();
      builder.append(clickable);
      return this;
    }

    public ClickableMessage build() {
      return new ClickableMessage(builder.build());
    }

  }

}