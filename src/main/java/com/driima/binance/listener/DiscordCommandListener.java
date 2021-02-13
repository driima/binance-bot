package com.driima.binance.listener;

import com.binance.api.client.BinanceApiRestClient;
import com.driima.binance.wrapper.BinanceWrapper;
import com.driima.foxen.CommandHandler;
import com.driima.foxen.UsageFormat;
import com.driima.foxen.parsing.ResponseConsumer;
import com.driima.foxen.parsing.SuppliableArguments;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.util.*;

public class DiscordCommandListener extends CommandHandler<Message> implements MessageCreateListener {

    private final Map<String, List<String>> permissions = new HashMap<>();

    private DiscordApi api;
    private BinanceApiRestClient client;
    private BinanceWrapper context;

    public DiscordCommandListener(DiscordApi api, BinanceApiRestClient client, BinanceWrapper context) {
        super(UsageFormat.builder()
                .commandPrefix(".")
                .optionalArgsAffixes("[]")
                .requiredArgsAffixes("{}")
                .build()
        );

        this.api = api;
        this.client = client;
        this.context = context;
    }

    /**
     * Gets a map which contains all set permissions.
     * The map's key is the user id, the value is a list with all permissions of this user.
     *
     * @return A map which contains all set permissions.
     */
    public Map<String, List<String>> getPermissions() {
        return permissions;
    }

    /**
     * Adds a permission for the user.
     *
     * @param user       The user.
     * @param permission The permission to add.
     */
    public void addPermission(User user, String permission) {
        addPermission(String.valueOf(user.getId()), permission);
    }

    /**
     * Checks if the user has the required permission.
     *
     * @param user       The user.
     * @param permission The permission to check.
     * @return If the user has the given permission.
     */
    public boolean hasPermission(User user, String permission) {
        return hasPermission(String.valueOf(user.getId()), permission);
    }


    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        Message message = event.getMessage();

        if (message.getUserAuthor().map(User::isYourself).orElse(false)) {
            return;
        }

        // TODO: Not essential for personal use, but this needs some work
//        if (command == null) {
//            if (splitMessage.length > 1) {
//                command = getCommands().get(splitMessage[1].toLowerCase());
//
//                if (command == null) {
//                    return;
//                }
//
//                DiscordCommand discordCommand = command.getMethod().getAnnotation(DiscordCommand.class);
//
//                if (discordCommand != null) {
//                    if (discordCommand.requiresMention()) {
//                        splitMessage = Arrays.copyOfRange(splitMessage, 1, splitMessage.length);
//                    }
//                }
//            } else {
//                return;
//            }
//        }
//
//        Command commandAnnotation = command.getAnnotation();
//        DiscordCommand discordCommand = command.getMethod().getAnnotation(DiscordCommand.class);
//
//        if (discordCommand != null) {
//            if (discordCommand.requiresMention() && !commandString.equals(api.getYourself().getMentionTag())) {
//                return;
//            }
//
//            if (message.getPrivateChannel().isPresent() && !discordCommand.privateMessages()) {
//                return;
//            }
//
//            if (!message.getPrivateChannel().isPresent() && !discordCommand.channelMessages()) {
//                return;
//            }
//        }
//
//        if (!hasPermission(message.getUserAuthor().map(User::getId).map(String::valueOf).orElse("-1"), commandAnnotation.requiredPermission())) {
//            if (CommandError.MISSING_PERMISSIONS.toString() != null) {
//                message.getChannel().sendMessage(CommandError.MISSING_PERMISSIONS.toString());
//            }
//
//            return;
//        }

        process(message, message.getContent());
    }

    @Override
    public SuppliableArguments getSuppliableArguments(Message supplier) {
        return new SuppliableArguments()
                .supply(BinanceApiRestClient.class, () -> client)
                .supply(BinanceWrapper.class, () -> context)
                .supply(Message.class, () -> supplier)
                .supply(DiscordApi.class, () -> api)
                .supply(Channel.class, () -> supplier != null ? supplier.getChannel() : null)
                .supply(GroupChannel.class, () -> supplier != null ? supplier.getChannel().asGroupChannel().orElse(null) : null)
                .supply(PrivateChannel.class, () -> supplier != null ? supplier.getChannel().asPrivateChannel().orElse(null) : null)
                .supply(ServerChannel.class, () -> supplier != null ? supplier.getChannel().asServerChannel().orElse(null) : null)
                .supply(ServerTextChannel.class, () -> supplier != null ? supplier.getChannel().asServerTextChannel().orElse(null) : null)
                .supply(TextChannel.class, () -> supplier != null ? supplier.getChannel().asTextChannel().orElse(null) : null)
                .supply(User.class, () -> supplier != null ? supplier.getUserAuthor().orElse(null) : null)
                .supply(MessageAuthor.class, () -> supplier != null ? supplier.getAuthor() : null)
                .supply(Server.class, () -> supplier != null ? supplier.getServerTextChannel()
                        .map(ServerChannel::getServer).orElse(null) : null);
    }

    @Override
    public ResponseConsumer getResponseConsumer(Message supplier) {
        return new ResponseConsumer()
                .consume(String.class, supplier.getChannel()::sendMessage)
                .consume(EmbedBuilder.class, supplier.getChannel()::sendMessage)
                .consume(List.class, list -> {
                    String result = String.join("\n", (List<String>) list);

                    supplier.getChannel().sendMessage(result);
                });
    }

    /**
     * Adds a permission for the user with the given id.
     *
     * @param userId     The id of the user.
     * @param permission The permission to add.
     */
    private void addPermission(String userId, String permission) {
        this.permissions.computeIfAbsent(userId, u -> new ArrayList<>()).add(permission);
    }

    /**
     * Checks if the user with the given id has the required permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission to check.
     * @return If the user has the given permission.
     */
    private boolean hasPermission(String userId, String permission) {
        if (permission.equalsIgnoreCase("none") || permission.equals("")) {
            return true;
        }

        List<String> permissions = this.permissions.get(userId);

        if (permissions == null) {
            return false;
        }

        for (String perm : permissions) {
            if (checkPermission(perm, permission)) {
                return true;
            }
        }

        return false;
    }

    private boolean checkPermission(String permission, String required) {
        String[] splitPermission = permission.split("\\.");
        String[] splitRequired = required.split("\\.");

        int lower = splitPermission.length > splitRequired.length
                ? splitRequired.length
                : splitPermission.length;

        for (int i = 0; i < lower; i++) {
            if (!splitPermission[i].equalsIgnoreCase(splitRequired[i])) {
                return splitPermission[i].equals("*");
            }
        }

        return splitPermission.length == splitRequired.length;
    }
}
