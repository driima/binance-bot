package com.driima.binance;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.AssetBalance;
import com.dreamburst.sed.dispatchers.DirectDispatcher;
import com.dreamburst.sed.dispatchers.Dispatchers;
import com.driima.binance.binance.BinanceContext;
import com.driima.foxen.parsing.Arguments;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BinanceBot {

    public static void main(String[] args) {
        DirectDispatcher dispatcher = Dispatchers.get(DirectDispatcher.class);

        try (InputStream inputStream = new FileInputStream("config.properties")) {
            // Load API keys / tokens
            Properties properties = new Properties();
            properties.load(inputStream);

            String binanceKey = properties.getProperty("binance.key");

            String binanceSecret = properties.getProperty("binance.secret");
            String discordBotToken = properties.getProperty("discord.bot.token");
            String discordScraperToken = properties.getProperty("discord.scraper.token");

            // Set up Binance API and context used to wrap REST responses
            BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(binanceKey, binanceSecret);
            BinanceApiRestClient client = factory.newRestClient();
            BinanceApiAsyncRestClient binanceApiAsyncRestClient = factory.newAsyncRestClient();
            BinanceContext context = new BinanceContext(client, binanceApiAsyncRestClient);

            Arguments.registerParsable(AssetBalance.class, input -> client.getAccount().getAssetBalance(input.toUpperCase()));

            new DiscordBot(context, discordBotToken);
//        new Scraper(context, discordScraperToken);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
