package codeescape.core.model;

public class PlayerProfile {
    public String username;
    public String email;
    public String character = "Boy";
    public int outfitVariant = 0; // 0..3
    public int selectedTop = 1;
    public int selectedBottom = 1;
    public int selectedShoes = 1;
    public int selectedAccessory = 1;
    public int savedXp = 0;
    public int lifetimeXp = 0;

    public PlayerProfile() {}
    public PlayerProfile(String username, String email) {
        this.username = username;
        this.email = email;
    }
}
