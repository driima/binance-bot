package com.driima.binance;

import com.driima.foxen.CommandError;
import com.driima.foxen.util.Template;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

public class Embed {

    public static final char ZERO_WIDTH_SPACE = '\u200B';

    private final EmbedBuilder builder = new EmbedBuilder();

    private final Server server;

    private String title;
    private String description;
    private boolean icon = true;
    private EmbedType type = EmbedType.STANDARD;

    private Embed(Server server, String title, String description, EmbedType type) {
        this.server = server;
        this.title = title;
        this.description = description;
        this.type = type;
    }

    private Embed(Server server) {
        this.server = server;
    }

    private Embed(Server server, String title) {
        this(server, title, EmbedType.STANDARD);
    }

    private Embed(Server server, String title, String description) {
        this(server, title, description, EmbedType.STANDARD);
    }

    private Embed(Server server, String title, EmbedType type) {
        this(server, title, null, type);
    }

    public Embed noIcon() {
        this.icon = false;

        return this;
    }

    public Embed setType(EmbedType type) {
        this.type = type;

        return this;
    }

    public Embed setTitle(String title) {
        this.title = title;

        return this;
    }

    public Embed setTitle(String title, Object... replacements) {
        this.title = Template.get().map(title, replacements);

        return this;
    }

    public Embed setDescription(String description) {
        this.description = description;

        return this;
    }

    public Embed setDescription(String description, Object... replacements) {
        this.description = Template.get().map(description, replacements);

        return this;
    }

    public Embed addField(String name, String value) {
        this.builder.addField(name, value);

        return this;
    }

    public Embed addField(String name, String value, Object... replacements) {
        this.builder.addField(name, Template.get().map(value, replacements));

        return this;
    }

    public Embed addInlineField(String name, String value) {
        this.builder.addInlineField(name, value);

        return this;
    }

    public Embed addInlineField(String name, String value, Object... replacements) {
        this.builder.addInlineField(name, Template.get().map(value, replacements));

        return this;
    }

    public EmbedBuilder pack() {
        if (icon && server.getIcon().isPresent()) {
            builder.setAuthor(title, "", server.getIcon().get());
        }

        if (description != null) {
            builder.setDescription(description);
        }

        builder.setColor(type.getColor());

        return builder;
    }

    public static Embed get(Server server, String title, String description, EmbedType type) {
        return new Embed(server, title, description, type);
    }

    public static Embed get(Server server, String title, EmbedType type) {
        return new Embed(server, title, type);
    }

    public static Embed get(Server server, String title, String description) {
        return new Embed(server, title, description);
    }

    public static Embed get(Server server, String title) {
        return new Embed(server, title);
    }

    public static Embed get(Server server) {
        return new Embed(server);
    }

    public static Embed get(Server server, Exception e) {
        return new Embed(server, CommandError.ERROR.toString(), TextUtils.asCode(e.getMessage()), EmbedType.ERROR);
    }
}
