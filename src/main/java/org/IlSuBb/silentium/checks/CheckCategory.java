package org.IlSuBb.silentium.checks;

public enum CheckCategory {
    BLATANT("Blatant", "<red>"),
    GHOST("Ghost", "<yellow>"),
    ANARCHY("Anarchy", "<gold>");

    private final String displayName;
    private final String color;

    CheckCategory(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
}
