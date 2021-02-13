package com.driima.binance.commands;

import com.binance.api.client.domain.account.*;
import com.binance.api.client.exception.BinanceApiException;
import com.driima.binance.Embed;
import com.driima.binance.EmbedType;
import com.driima.binance.wrapper.Balance;
import com.driima.binance.wrapper.BinanceWrapper;
import com.driima.binance.wrapper.Symbol;
import com.driima.binance.util.Utils;
import com.driima.foxen.Command;
import com.driima.foxen.CommandExecutor;
import com.driima.foxen.Optional;
import com.driima.foxen.util.Template;
import com.google.common.collect.Lists;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CoreCommands implements CommandExecutor {

    private static final String NO_ASSET = "Could not find {asset}";
    private static final String ASSET_BALANCE = "You currently have {free} free {asset} and {locked} locked {asset}";
    private static final String MARKET_BUY_RESPONSE = "Placed an order for {amount}x {asset}: {response}";
    private static final String BUYS_FOR_ASSET = "Buys for {asset} ({limit})";
    private static final String YOUR_ASSET = "Your {asset}";
    private static final String CURRENT_ASSET = "{amount} {asset}";

    @Command
    public String ping() {
        return "pong!";
    }

    @Command
    public String bal(Server server, BinanceWrapper context, @Optional String coin) {
        double gbpUsdt = context.getTrade("GBPUSDT").getPrice();
        double btcGbp = context.getTrade("BTCGBP").getPrice();

        StringBuilder result = new StringBuilder("```");

        List<Balance> balances = coin != null
                ? Lists.newArrayList(context.getBalance(coin))
                : context.getBalancesAsList().stream()
                .filter(balance -> balance.getFree() > 0 || balance.getLocked() > 0)
                .collect(Collectors.toList());

        result.append(Template.get().map(YOUR_ASSET, "asset", "Balances")).append("\n");

        double usdTotal = 0;

        for (Balance balance : balances) {
            double total = balance.getFree() + balance.getLocked();

            com.driima.binance.wrapper.Trade usdt = context.getTrade(balance.getAsset() + "USDT");
            com.driima.binance.wrapper.Trade btc = context.getTrade(balance.getAsset() + "BTC");

            if (usdt == null && btc == null) {
                continue;
            }

            List<String> vals = Lists.newArrayList();

            String dollarFormat;

            if (usdt != null) {
                double dollars = total * usdt.getPrice();
                dollarFormat = Utils.reduceDecimals(dollars, 2);
                vals.add("$" + dollarFormat);
                usdTotal += dollars;
            } else {
                double dollars = ((total * btc.getPrice()) / btcGbp) * gbpUsdt;
                dollarFormat = Utils.reduceDecimals(dollars, 2);
                vals.add("$" + dollarFormat);
                usdTotal += dollars;
            }

            if (Double.parseDouble(dollarFormat) < 1) {
                continue;
            }

            if (btc != null) {
                vals.add(Utils.reduceDecimals(total * btc.getPrice(), 8) + " BTC");
            }

            vals.add(usdt == null
                    ? "£" + Utils.reduceDecimals(((total * btc.getPrice()) / btcGbp), 2)
                    : "£" + Utils.reduceDecimals(((total * usdt.getPrice()) / gbpUsdt), 2));

            String map = Template.get().map(CURRENT_ASSET, "amount", total, "asset", balance.getAsset()) + ": " + String.join(" | ", vals);
            result.append(map).append("\n");
        }

        result.append("\n")
                .append("Total: $").append(Utils.reduceDecimals(usdTotal, 2))
                .append(" | £").append(Utils.reduceDecimals(usdTotal / gbpUsdt, 2));

        return result.append("```").toString();
    }

    @Command
    public EmbedBuilder orders(Server server, BinanceWrapper context, String asset, @Optional int limit) {
        if (limit == 0) {
            limit = 50;
        }

        try {
            EmbedBuilder pack = Embed.get(server, Template.get().map(BUYS_FOR_ASSET, "asset", asset, "limit", limit), EmbedType.INFO).pack();
            List<Trade> trades = context.getClient().getMyTrades(asset.toUpperCase(), limit);
            Symbol symbol = context.getSymbol(asset);

            for (Trade trade : trades) {
                if (trade.isBuyer()) {
                    pack.addField(Utils.reduceDecimals(Double.parseDouble(trade.getQty()), symbol.getStepSize()) + " x " + trade.getPrice(), "");
                }
            }

            return pack;
        } catch (BinanceApiException e) {
            Set<com.driima.binance.wrapper.Trade> trades = context.getTrades(asset.toUpperCase());

            if (trades.isEmpty()) {
                EmbedBuilder pack = Embed.get(server, "Warning", EmbedType.WARNING).pack();
                pack.addInlineField(Template.get().map(NO_ASSET, "asset", asset), "Make sure to include the currency you traded with.");
                return pack;
            }

            Balance balance = context.getBalance(asset);

            EmbedBuilder pack = Embed.get(server, "Warning", EmbedType.INFO).pack();
            pack.addField("Did you mean one of these?", "- " + trades.stream().map(com.driima.binance.wrapper.Trade::getSymbol).collect(Collectors.joining("\n- ")));
            pack.addField("Your " + balance.getAsset() + ":", balance.getFree() + " | " + balance.getLocked());
            return pack;
        }
    }
}
