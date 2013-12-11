package hudson.plugins.cigame.model;

/**
 * @author hasna
 * @since 1.0.128-SNAPSHOT
 */
public class ScoreLevel {
    private String name;
    private String description;
    private String imageUrl;
    private int level;

    public ScoreLevel(String name, String description, String imageUrl, int level) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getLevel() {
        return level;
    }
}
