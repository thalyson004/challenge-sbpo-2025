package org.sbpo2025.challenge;

import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ChallengeSolver {
    private final long MAX_RUNTIME = 600000; // milliseconds; 10 minutes

    protected List<Map<Integer, Integer>> orders;
    protected List<Map<Integer, Integer>> aisles;
    protected int nItems;
    protected int waveSizeLB;
    protected int waveSizeUB;
    protected Random r = new Random();

    public ChallengeSolver(
            List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB,
            int waveSizeUB) {
        this.nItems = nItems;
        this.orders = orders;
        this.aisles = aisles;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
    }

    Set<Integer> complement_ids(Set<Integer> check, int mx_index) {
        Set<Integer> complement = new HashSet<Integer>();

        for (int i = 0; i < mx_index; i++)
            complement.add(i);
        for (int i : check)
            complement.remove(i);

        return complement;
    }

    Set<Integer> complementOrdersIds(Set<Integer> check) {
        return complement_ids(check, orders.size());
    }

    List<Integer> complementOrdersIdsList(Set<Integer> check) {
        return new ArrayList<Integer>(complement_ids(check, orders.size()));
    }

    Set<Integer> complementAislesIds(Set<Integer> check) {
        return complement_ids(check, aisles.size());
    }

    //
    /**
     * Add b values into the map a
     * 
     * @param a A Map<Integer, Integer>
     * @param b A Map<Integer, Integer>
     * @return nothing
     */
    void addMapValues(Map<Integer, Integer> a, Map<Integer, Integer> b) {
        for (Map.Entry<Integer, Integer> entry : b.entrySet()) {
            a.put(entry.getKey(), a.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }
    }

    int sumMapValues(Map<Integer, Integer> a) {
        return a.values().stream().mapToInt(d -> d).sum();
    }

    boolean canGet(int order_id, Map<Integer, Integer> items_h, Map<Integer, Integer> items_n, int prev_total) {
        if (Config.VERBOSE) {
            System.out.printf("Trying add order %d\n", order_id);
        }

        int order_total = 0;
        Map<Integer, Integer> order = orders.get(order_id);

        boolean ok = true;
        for (Map.Entry<Integer, Integer> entry : order.entrySet()) {
            if (Config.VERBOSE) {
                System.out.print("Check item: ");
                System.out.println(entry);
                System.out.print("Items N: ");
                System.out.println(items_n);
                System.out.print("Items H: ");
                System.out.println(items_h);
            }
            if (items_n.getOrDefault(entry.getKey(), 0) + entry.getValue() > items_h.getOrDefault(entry.getKey(), 0)) {
                ok = false;
                break;
            }

            order_total += entry.getValue();
        }

        if (prev_total + order_total > waveSizeUB)
            ok = false;

        if (Config.VERBOSE) {
            System.out.printf("Final flag %d\n", ok ? 1 : 0);
        }

        return ok;
    }

    public ChallengeSolution dumpSolution() {
        Set<Integer> orders_ids = new HashSet<Integer>();
        Set<Integer> aisles_ids = new HashSet<Integer>();
        int total = 0;
        Map<Integer, Integer> items_h = new HashMap<Integer, Integer>(); // Aisle
        Map<Integer, Integer> items_n = new HashMap<Integer, Integer>(); // Orders

        for (int i = 0; i < aisles.size(); i++) {
            aisles_ids.add(i);
            addMapValues(items_h, aisles.get(i));
        }

        if (Config.VERBOSE) {
            System.out.println("Will try add orders");
        }

        boolean again = true;
        while (again) { // This dont have any sense, only if I try remove aisle
            if (Config.VERBOSE) {
                System.out.println("Starting a search...");
                System.out.print("Previus selected:");
                System.out.println(new ChallengeSolution(orders_ids, aisles_ids));
                System.out.println("--------------------------------------");
            }

            again = false;

            List<Integer> options = complementOrdersIdsList(orders_ids);
            Collections.shuffle(options);
            for (int option : options) {
                if (canGet(option, items_h, items_n, total)) {
                    addMapValues(items_n, orders.get(option));
                    total += sumMapValues(orders.get(option));
                    orders_ids.add(option);
                    again = true;

                    if (total >= waveSizeLB)
                        if (r.nextDouble() < Config.STOP_CHANCE)
                            break;

                }
            }

            // TODO: Try remove and add aisle
        }

        return new ChallengeSolution(orders_ids, aisles_ids);
    }

    public List<ChallengeSolution> CreateInitialPopulation() {
        List<ChallengeSolution> population = new ArrayList<ChallengeSolution>();

        for (int i = 0; i < Config.POPULATION; i++) {
            population.add(dumpSolution());
        }

        return population;
    }

    public ChallengeSolution bestSolution(List<ChallengeSolution> population) {
        ChallengeSolution best = population.get(0);
        double best_objective = computeObjectiveFunction(best);

        for (int i = 1; i < population.size(); i++) {
            ChallengeSolution solution = population.get(i);
            if (isSolutionFeasible(solution) && best_objective < computeObjectiveFunction(solution)) {
                best = solution;
                best_objective = computeObjectiveFunction(solution);
            }
        }

        return best;
    }

    public ChallengeSolution solve(StopWatch stopWatch) {
        System.out.println(Config.EXPANSION_SET_SELECTED);

        Set<Integer> ans_orders = new HashSet<Integer>();
        Set<Integer> ans_aisles = new HashSet<Integer>();

        System.out.printf("Numero de items: %d\n", nItems);
        System.out.printf("waveSizeLB: %d, waveSizeUB: %d\n", waveSizeLB, waveSizeUB);

        imprimirListaDeMapas("orders:", orders);
        imprimirListaDeMapas("aisles:", aisles);

        List<ChallengeSolution> population = CreateInitialPopulation();

        ChallengeSolution best = bestSolution(population);

        System.out.println(best);

        return best;
    }

    /*
     * Get the remaining time in seconds
     */
    protected long getRemainingTime(StopWatch stopWatch) {
        return Math.max(
                TimeUnit.SECONDS.convert(MAX_RUNTIME - stopWatch.getTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
                0);
    }

    protected boolean isSolutionFeasible(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return false;
        }

        int[] totalUnitsPicked = new int[nItems];
        int[] totalUnitsAvailable = new int[nItems];

        // Calculate total units picked
        for (int order : selectedOrders) {
            for (Map.Entry<Integer, Integer> entry : orders.get(order).entrySet()) {
                totalUnitsPicked[entry.getKey()] += entry.getValue();
            }
        }

        // Calculate total units available
        for (int aisle : visitedAisles) {
            for (Map.Entry<Integer, Integer> entry : aisles.get(aisle).entrySet()) {
                totalUnitsAvailable[entry.getKey()] += entry.getValue();
            }
        }

        // Check if the total units picked are within bounds
        int totalUnits = Arrays.stream(totalUnitsPicked).sum();
        if (totalUnits < waveSizeLB || totalUnits > waveSizeUB) {
            return false;
        }

        // Check if the units picked do not exceed the units available
        for (int i = 0; i < nItems; i++) {
            if (totalUnitsPicked[i] > totalUnitsAvailable[i]) {
                return false;
            }
        }

        return true;
    }

    protected double computeObjectiveFunction(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return 0.0;
        }
        int totalUnitsPicked = 0;

        // Calculate total units picked
        for (int order : selectedOrders) {
            totalUnitsPicked += orders.get(order).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        // Calculate the number of visited aisles
        int numVisitedAisles = visitedAisles.size();

        // Objective function: total units picked / number of visited aisles
        return (double) totalUnitsPicked / numVisitedAisles;
    }

    public static void imprimirListaDeMapas(String title, List<Map<Integer, Integer>> listOfMaps) {
        System.out.println(title);

        for (Map<Integer, Integer> map : listOfMaps) {
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                int key = entry.getKey();
                int value = entry.getValue();

                System.out.print("(" + key + ", " + value + ") ");
            }
            System.out.println();
        }
    }
}
