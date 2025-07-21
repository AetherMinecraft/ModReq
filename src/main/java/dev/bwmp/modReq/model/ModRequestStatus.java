package dev.bwmp.modReq.model;

public enum ModRequestStatus {
  /**
   * Request is open and awaiting attention
   */
  OPEN,

  /**
   * Request has been elevated for admin attention
   */
  ELEVATED,

  /**
   * Request has been completed successfully
   */
  COMPLETED,

  /**
   * Request has been closed without completion
   */
  CLOSED;

  /**
   * Gets a user-friendly display name for this status
   * 
   * @return Display name
   */
  public String getDisplayName() {
    return switch (this) {
      case OPEN -> "Open";
      case ELEVATED -> "Elevated";
      case COMPLETED -> "Completed";
      case CLOSED -> "Closed";
    };
  }

  /**
   * Gets the color code associated with this status
   * 
   * @return Color code for display
   */
  public String getColorCode() {
    return switch (this) {
      case OPEN -> "&a"; // Green
      case ELEVATED -> "&6"; // Gold
      case COMPLETED -> "&2"; // Dark Green
      case CLOSED -> "&7"; // Gray
    };
  }

  /**
   * Gets a colored display name for this status
   * 
   * @return Colored display name
   */
  public String getColoredDisplayName() {
    return getColorCode() + getDisplayName();
  }
}
