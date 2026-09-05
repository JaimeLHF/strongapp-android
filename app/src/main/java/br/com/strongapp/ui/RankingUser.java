package br.com.strongapp.ui;

/** Uma linha do ranking da comunidade. */
public class RankingUser {
    public final int position;
    public final String name;
    public final int value;
    public final String subtitle;

    public RankingUser(int position, String name, int value, String subtitle) {
        this.position = position;
        this.name = name;
        this.value = value;
        this.subtitle = subtitle;
    }

    /** Iniciais para o avatar, como na versão web. */
    public String initials() {
        String[] parts = name.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && builder.length() < 2) {
                builder.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return builder.toString();
    }
}
