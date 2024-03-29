package me.pietelite.einsteinsworkshopedu.tools.storage;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;

public class StorageLine {

  private static final String DATA_DELIMITER = ";";
  private static final String COMMENT_DELIMITER = "#";
  private String line;
  private String comment = "";

  private StorageLine(List<String> tokens) {
    this.line = String.join(DATA_DELIMITER, tokens);
  }

  StorageLine(String line) {
    this.line = line;
  }

  public static StorageLine.Builder builder() {
    return new StorageLine.Builder();
  }

  public String toString() {
    return line + COMMENT_DELIMITER + comment;
  }

  public String getLine() {
    return this.toString();
  }

  /**
   * Get all individual pieces of data saved in the storage line.
   *
   * @return An array of String-type objects from the storage line
   */
  public String[] getTokens() {
    String[] macroTokens = line.split(COMMENT_DELIMITER);
    String data = macroTokens[0];
    return data.split(DATA_DELIMITER);
  }

  /**
   * Gets the comment portion of the storage line.
   *
   * @return The comment of the line
   */
  @SuppressWarnings("unused")
  public String getComment() {
    String[] tokens = line.split(COMMENT_DELIMITER, 2);
    if (tokens.length == 2) {
      return tokens[1];
    } else {
      return "";
    }
  }

  boolean hasData() {
    for (String token : getTokens()) {
      if (!token.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  void setComment(@Nonnull String comment) {
    this.comment = comment;
  }

  public static class Builder {

    private List<String> items;

    private Builder() {
      items = new LinkedList<>();
    }

    public StorageLine.Builder addItem(String item) {
      items.add(removeDelimiters(item));
      return this;
    }

    public StorageLine.Builder addItem(double item) {
      return addItem(String.valueOf(item));
    }

    public StorageLine.Builder addItem(boolean item) {
      return addItem(String.valueOf(item));
    }

    public StorageLine build() {
      return new StorageLine(items);
    }

    private String removeDelimiters(String line) {
      return line.replaceAll(DATA_DELIMITER, "").replaceAll(COMMENT_DELIMITER, "");
    }


  }

}
