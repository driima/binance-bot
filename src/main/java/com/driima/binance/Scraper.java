package com.driima.binance;

import com.driima.binance.binance.BinanceContext;
import com.driima.binance.util.RateLimiter;
import com.google.common.collect.Sets;
import org.javacord.api.AccountType;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.user.UserStatus;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Scraper {

    private final Set<String> ignore = Sets.newHashSet("TRY", "BTC");

    private static long time = 0;

    public static void logTime(long time) {
        System.out.println(Scraper.time);
        System.out.println("Time: " + (time - Scraper.time) + "ms");
        Scraper.time = time;
    }

    public Scraper(BinanceContext context, String token) {
        RateLimiter rateLimiter = new RateLimiter(30, TimeUnit.MINUTES);

        new DiscordApiBuilder().setAccountType(AccountType.CLIENT).setToken(token).login().thenAccept(api -> {
            api.updateStatus(UserStatus.DO_NOT_DISTURB);

            api.addMessageCreateListener(messageCreateEvent -> {
                if (messageCreateEvent.getMessage().getContent().startsWith(".")) {
                    return;
                }

                if (messageCreateEvent.getMessageAuthor().isBotUser()) {
                    return;
                }

                if (!(messageCreateEvent.getChannel() instanceof ServerChannel)) {
                    return;
                }

                ServerChannel channel = (ServerChannel) messageCreateEvent.getChannel();

                if (channel.getName().contains("pump-signal")) {
                    Instant creationTimestamp = messageCreateEvent.getMessage().getCreationTimestamp();
                    time = creationTimestamp.toEpochMilli();
                    String content = messageCreateEvent.getMessage().getContent().toUpperCase();

                    System.out.println(messageCreateEvent.getMessage().getContent());

                    Arrays.stream(content.replace("\n", " ").replace("\r", " ").replaceAll("[^a-zA-Z$# ]", " ").split(" "))
                            .sorted((o1, o2) -> o1.equals(o2) ? 0 : (o1.contains("$") || o1.contains("#") ? -1 : 1))
                            .map(word -> word.replaceAll("[^a-zA-Z ]", ""))
                            .filter(context::hasBalance)
                            .filter(word -> !ignore.contains(word))
                            .findFirst()
                            .ifPresent(coin -> {
                                if (rateLimiter.execute(coin)) {
                                    System.out.println(coin + " is being pumped.");
                                    context.pump(coin, "BTC");

                                    try {
                                        Desktop.getDesktop().browse(URI.create("https://www.binance.com/en/trade/" + coin + "_BTC?layout=pro"));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

//                                if (content.contains("PUMP")) {
//                                    if (rateLimiter.execute()) {
//                                        context.pump(coin, "BTC");
//                                    }
//                                } else if (content.startsWith("CHECK")) {
//                                    context.check(coin, "BTC");
//                                }
                            });
                }
            });
        });


    }
}
