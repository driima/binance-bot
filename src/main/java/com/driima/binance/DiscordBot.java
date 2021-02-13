package com.driima.binance;

import com.dreamburst.sed.dispatchers.DirectDispatcher;
import com.dreamburst.sed.dispatchers.Dispatchers;
import com.driima.binance.binance.BinanceContext;
import com.driima.binance.commands.CoreCommands;
import com.driima.binance.events.PriceChangeEvent;
import com.driima.binance.listener.DiscordCommandListener;
import com.driima.binance.util.RateLimiter;
import com.driima.binance.util.Utils;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.util.logging.ExceptionLogger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DiscordBot {

    private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss:SSS");

    public DiscordBot(BinanceContext context, String token) {
        RateLimiter rateLimiter = new RateLimiter(2, TimeUnit.SECONDS);

        new DiscordApiBuilder().setToken(token).login().thenAccept(api -> {
            api.updateStatus(UserStatus.DO_NOT_DISTURB);

            DiscordCommandListener commandListener = new DiscordCommandListener(api, context.getClient(), context);
            CoreCommands commands = new CoreCommands();

            commandListener.registerCommandExecutor(commands);

            Dispatchers.get(DirectDispatcher.class).register(PriceChangeEvent.class, event -> {
                String oldPrice = Utils.reduceDecimals(event.getOldPrice(), 8);
                String newPrice = Utils.reduceDecimals(event.getNewPrice(), 8);

                DiscordWebhook webhook = new DiscordWebhook("https://discord.com/api/webhooks/808471528605483038/7nlgVK5S7sS1zep9cHIOiXyPBSjKovRkkYw6uSiNXmfC__AJ2ITDhSExey8xI5WIANAP");
                webhook.setUsername("Sudden Price Change");
                webhook.addEmbed(new DiscordWebhook.EmbedObject()
                        .setTitle("Price Increase")
                        .setDescription("[" + event.getSymbol().getSymbolInfo().getSymbol() + "](https://www.binance.com/en/trade/" + event.getSymbol().getSymbolInfo().getSymbol() + "?layout=pro) - Binance Link")
                        .addField("Old Price", oldPrice, true)
                        .addField("New Price", newPrice, true)
                        .addField("Change", "+" + String.format("%.2f", event.getChange()) + "%", true)
                        .setFooter("Time: " + format.format(Date.from(Instant.now())), "")
                );
//                webhook.setContent("[" + event.getSymbol().getSymbolInfo().getSymbol() + "](https://www.binance.com/en/trade/" + event.getSymbol().getSymbolInfo().getSymbol() + "?layout=pro): " + oldPrice + " ⟶ " + newPrice + " (+" + String.format("%.2f", event.getChange()) + "%) @ " + format.format(Date.from(Instant.now())));
                try {
                    webhook.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                api.getChannelById(808015658108583956L).ifPresent(channel -> {
                    if (channel instanceof TextChannel) {
                        TextChannel textChannel = (TextChannel) channel;
                        textChannel.sendMessage(Embed.get(api.getServerById(335324522712399884L).get(), "Pump Alert", "[" + event.getSymbol().getSymbolInfo().getSymbol() + "](https://www.binance.com/en/trade/" + event.getSymbol().getSymbolInfo().getSymbol() + "?layout=pro)")
                                .addInlineField("Old Price", "{old}", "old", oldPrice)
                                .addInlineField("New Price", "{new}", "new", newPrice)
                                .addInlineField("Change", "+{change}%", "change", String.format("%.2f", event.getChange()))
                                .pack().setFooter("Time: " + format.format(Date.from(Instant.now())))
                        );
                    }
                });
            });

//            api.getChannelById(808015658108583956L).ifPresent(channel -> {
//                if (channel instanceof TextChannel) {
//                    TextChannel textChannel = (TextChannel) channel;
//                    textChannel.sendMessage(Embed.get(api.getServerById(335324522712399884L).get(), "Pump Alert", "[ABCDEFG](https://www.binance.com/en/trade/ABCDEFG?layout=pro)")
//                            .addInlineField("Old Price", "{old}", "old", String.format("%.8f", 0.00000000))
//                            .addInlineField("New Price", "{new}", "new", String.format("%.8f", 0.00001111))
//                            .addInlineField("Change", "+{change}%", "change", String.format("%.2f", 3.2145))
//                            .pack().setFooter(format.format(Date.from(Instant.now())))
//                    );
//                }
//            });

            api.addMessageCreateListener(commandListener);
        }).exceptionally(ExceptionLogger.get());
    }
}