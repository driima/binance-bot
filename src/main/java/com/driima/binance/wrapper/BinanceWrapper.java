package com.driima.binance.wrapper;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.market.TickerPrice;
import com.dreamburst.sed.dispatchers.Dispatchers;
import com.driima.binance.Scraper;
import com.driima.binance.events.PriceChangeEvent;
import com.driima.binance.util.Utils;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BinanceWrapper {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private static final Set<String> validPumpQuotes = Sets.newHashSet("USDT", "BTC");

    private final BinanceApiRestClient client;
    private final BinanceApiAsyncRestClient asyncClient;
    private final Map<String, Balance> balances = Maps.newHashMap();
    private final Map<String, Trade> trades = Maps.newHashMap();
    private final Map<String, Symbol> symbols;

    private final Map<String, CircularFifoQueue<Double>> averageChanges = Maps.newHashMap();
    private final Map<String, PumpTracker> currentPumps = Maps.newHashMap();

    public BinanceWrapper(BinanceApiRestClient client, BinanceApiAsyncRestClient asyncClient) {
        this.client = client;
        this.asyncClient = asyncClient;

        this.symbols = client.getExchangeInfo().getSymbols().stream().map(Symbol::new).collect(Collectors.toMap(s -> s.getSymbolInfo().getSymbol(), s -> s));

        // Retrieve balances:
        scheduler.schedule(() -> asyncClient.getAccount(account -> account.getBalances()
                .forEach(assetBalance -> {
                    if (this.balances.containsKey(assetBalance.getAsset())) {
                        Balance balance = this.balances.get(assetBalance.getAsset());
                        double free = Double.parseDouble(assetBalance.getFree());
                        double locked = Double.parseDouble(assetBalance.getLocked());

                        balance.setFree(free);
                        balance.setLocked(locked);
                    } else {
                        this.balances.put(assetBalance.getAsset(), new Balance(assetBalance));
                    }
                })),2, TimeUnit.SECONDS);

        // Monitor price changes:
        scheduler.scheduleAtFixedRate(() -> asyncClient.getAllPrices(allPrices -> {
            for (TickerPrice tickerPrice : allPrices) {
                if (this.trades.containsKey(tickerPrice.getSymbol())) {
                    // Update the existing price
                    Trade trade = this.trades.get(tickerPrice.getSymbol());
                    Double oldPrice = trade.getPrice();
                    trade.update(tickerPrice);

                    // Ignore any asset without a USDT/BTC pairing
                    if (validPumpQuotes.stream().noneMatch(s -> trade.getSymbol().endsWith(s))) {
                        continue;
                    }

                    Double newPrice = trade.getPrice();

                    double change = Utils.getDifference(oldPrice, newPrice);

                    // Ignore the change if there is no change, unless it's currently being tracked
                    if (oldPrice.equals(newPrice) && !currentPumps.containsKey(tickerPrice.getSymbol())) {
                        continue;
                    }

                    // Normalize any downward price trends to 0
                    if (change < 0 && !currentPumps.containsKey(tickerPrice.getSymbol())) {
                        change = 0;
                    }

                    // Get or create a rolling average queue for this symbol
                    CircularFifoQueue<Double> queue = averageChanges.computeIfAbsent(tickerPrice.getSymbol(), s -> new CircularFifoQueue<>(120));

                    // If the queue is filled
                    if (queue.isAtFullCapacity()) {
                        if (currentPumps.containsKey(tickerPrice.getSymbol())) {
                            PumpTracker pumpTracker = currentPumps.get(tickerPrice.getSymbol());
                            double changeSinceEntry = Utils.getDifference(pumpTracker.getEntry(), newPrice);
                            System.out.println("    " + tickerPrice.getSymbol() + ": " + Utils.percentage(changeSinceEntry));

                            if (changeSinceEntry > pumpTracker.getProfit()) {
                                System.out.println("    " + tickerPrice.getSymbol() + ": Bumping profit margin...");
                                pumpTracker.setProfit(changeSinceEntry);
                                pumpTracker.setTop(newPrice);
                            } else {
                                // TODO: Change this logic
                                if (change < -(pumpTracker.getProfit() / 2)) {
                                    System.out.println("    " + tickerPrice.getSymbol() + ": Leaving pump with " + Utils.percentage(changeSinceEntry) + " profit");
                                    currentPumps.remove(tickerPrice.getSymbol());
                                }
                            }
                        } else {
                            // Find the running average
                            double runningAverage = queue.stream().mapToDouble(s -> s)
                                    .average()
                                    .orElse(0);

                            if (change - runningAverage >= 0.02) {
                                // If the change has increased by at least 2%
                                if (change >= 0.12) {
                                    // If it's a huge increase (12%), it could indicate a pump. We should buy in here.
                                    System.out.println("BIG PRICE INCREASE: " + Utils.percentage(change));
                                } else {
                                    // Monitor and log the change to discord server(s)
                                    double x = getSymbol(tickerPrice.getSymbol()).getMinNotional() / 1000;

                                    // If the price is more than the min notional (possible logic change)
                                    if (Math.abs(newPrice - oldPrice) >= x) {
                                        Dispatchers.dispatch(new PriceChangeEvent(getSymbol(tickerPrice.getSymbol()), oldPrice, newPrice, change));
                                        currentPumps.put(tickerPrice.getSymbol(), new PumpTracker(newPrice));
                                    }
                                }

                            }
                        }
                    }

                    queue.add(change);
                } else {
                    this.trades.put(tickerPrice.getSymbol(), new Trade(tickerPrice));
                }
            }
        }), 0, 250, TimeUnit.MILLISECONDS);
    }

    public Map<String, Balance> getBalances() {
        return this.balances;
    }

    public Collection<Balance> getBalancesAsList() {
        return this.balances.values();
    }

    public Balance getBalance(String coin) {
        return balances.get(coin.toUpperCase());
    }

    public boolean hasBalance(String coin) {
        return balances.containsKey(coin.toUpperCase());
    }

    public boolean tradeExists(String symbol) {
        return this.symbols.keySet().stream().anyMatch(s -> s.startsWith(symbol.toUpperCase()));
    }

    public Set<String> getTradeNames(String coin) {
        return this.symbols.keySet().stream().filter(symbol -> symbol.startsWith(coin.toUpperCase())).collect(Collectors.toSet());
    }

    public Set<Trade> getTrades(String symbol) {
        return this.trades.entrySet().stream().filter(t -> t.getKey().startsWith(symbol.toUpperCase()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    public Set<Trade> getTradesDirectly(String symbol) {
        return client.getAllPrices().stream().filter(t -> t.getSymbol().startsWith(symbol.toUpperCase())).map(Trade::new).collect(Collectors.toSet());
    }

    public Trade getTrade(String symbol) {
        return this.trades.get(symbol.toUpperCase());
    }

    public Trade getTradeDirectly(String symbol) {
        return new Trade(client.getPrice(symbol.toUpperCase()));
    }

    public Symbol getSymbol(String symbol) {
        return this.symbols.get(symbol.toUpperCase());
    }

    public void pump(String coin, String asset) {
        // TODO: Optimise
        String symbol = coin+asset;
        Trade trade = getTrade(symbol);
        Symbol symbolInfo = getSymbol(symbol);
        Balance balance = balances.get(asset);

        double free = balance.getFree();
        double down = free * 0.5;

        if (down < symbolInfo.getMinNotional()) {
            System.out.println("Not enough to buy in with.");
            return;
        }

        String quantity = String.format("%.8f", down);

        System.out.println(quantity);
        System.out.println("Buying... " + trade);

        NewOrder newOrder = NewOrder.marketBuy(symbol, "").quoteOrderQty(quantity).newOrderRespType(NewOrderResponseType.FULL);

        NewOrderResponse response = client.newOrder(newOrder);
        List<com.binance.api.client.domain.account.Trade> buys = response.getFills();

        Scraper.logTime(Instant.now().toEpochMilli());

        double totalBought = 0;
        double price = 0;

        for (com.binance.api.client.domain.account.Trade fill : buys) {
            balance.setFree(balance.getFree() + (Double.parseDouble(fill.getPrice()) * Double.parseDouble(fill.getQty())));
            totalBought += Double.parseDouble(fill.getQty());
            price = Double.parseDouble(fill.getPrice());
            System.out.println(fill);
        }

        String formattedHalf = Utils.reduceDecimals(totalBought*0.449, symbolInfo.getStepSize());
        client.newOrder(NewOrder.limitSell(symbol, TimeInForce.GTC, formattedHalf, String.format("%.8f", price * 1.5)));

        scheduler.schedule(() -> {
            System.out.println("Selling... " + trade);
            client.newOrder(NewOrder.marketSell(symbol, formattedHalf).newOrderRespType(NewOrderResponseType.FULL));

            List<com.binance.api.client.domain.account.Trade> sells = response.getFills();

            for (com.binance.api.client.domain.account.Trade fill : sells) {
                balance.setFree(balance.getFree() - (Double.parseDouble(fill.getPrice()) * Double.parseDouble(fill.getQty())));
                System.out.println(fill);
                System.out.println("New balance: " + balance.getFree());
            }
        },15, TimeUnit.SECONDS);
    }

    public BinanceApiRestClient getClient() {
        return client;
    }
}
