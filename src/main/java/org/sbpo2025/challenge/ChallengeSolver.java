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
    protected List<Integer> orders_size;
    protected List<Map<Integer, Integer>> aisles;
    protected List<Integer> aisles_size;
    protected int nItems;
    protected int waveSizeLB;
    protected int waveSizeUB;
    protected Random r = new Random();
    protected ChallengeSolution finalSolution;

    public ChallengeSolver(
            List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB,
            int waveSizeUB) {
        this.nItems = nItems;
        this.orders = orders;
        this.aisles = aisles;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
        this.finalSolution = null;

        this.orders_size = new ArrayList<Integer>();
        for (Map<Integer, Integer> order : orders)
            orders_size.add(sumMapValues(order));
        this.aisles_size = new ArrayList<Integer>();
        for (Map<Integer, Integer> aisle : aisles)
            aisles_size.add(sumMapValues(aisle));
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

    boolean checkHaveNeed(Map<Integer, Integer> items_h, Map<Integer, Integer> items_n) {
        for (Map.Entry<Integer, Integer> item : items_n.entrySet()) {
            if (item.getValue() < items_h.getOrDefault(item.getKey(), 0))
                return false;
        }
        return true;
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

    void removeMapValues(Map<Integer, Integer> a, Map<Integer, Integer> b) {
        for (Map.Entry<Integer, Integer> entry : b.entrySet()) {
            a.put(entry.getKey(), a.getOrDefault(entry.getKey(), 0) - entry.getValue());
        }
    }

    int sumMapValues(Map<Integer, Integer> a) {
        return a.values().stream().mapToInt(d -> d).sum();
    }

    boolean canGetOrder(int order_id, Map<Integer, Integer> items_h, Map<Integer, Integer> items_n, int prev_total) {
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

    boolean canRemoveAisle(int aisle_id, Map<Integer, Integer> items_h, Map<Integer, Integer> items_n, int prev_total) {
        if (Config.VERBOSE) {
            System.out.printf("Trying remove aisle %d\n", aisle_id);
        }
        Map<Integer, Integer> aisle = aisles.get(aisle_id);

        boolean ok = true;
        for (Map.Entry<Integer, Integer> entry : aisle.entrySet()) {
            if (Config.VERBOSE) {
                System.out.print("Check item: ");
                System.out.println(entry);
                System.out.print("Items N: ");
                System.out.println(items_n);
                System.out.print("Items H: ");
                System.out.println(items_h);
            }

            if (items_n.getOrDefault(entry.getKey(), 0) > items_h.getOrDefault(entry.getKey(), 0) - entry.getValue()) {
                ok = false;
                break;
            }
        }

        if (Config.VERBOSE) {
            System.out.printf("Final flag %d\n", ok ? 1 : 0);
        }

        return ok;
    }

    public ChallengeSolution bestBySequenceOrderAisle(List<Integer> seq_order, List<Integer> seq_aisle) {
        ChallengeSolution solution = new ChallengeSolution(null, null);

        Set<Integer> orders_ids = new HashSet<Integer>();
        Set<Integer> aisles_ids = new HashSet<Integer>();
        int total = 0;
        Map<Integer, Integer> items_h = new HashMap<Integer, Integer>(); // Aisle
        Map<Integer, Integer> items_n = new HashMap<Integer, Integer>(); // Orders

        int id_seq_aisle = 0;
        for (int id_seq_order = 0; id_seq_order < seq_order.size(); id_seq_order++) {
            Map<Integer, Integer> order_cur = orders.get(seq_order.get(id_seq_order));
            addMapValues(items_n, order_cur);
            total += sumMapValues(order_cur);
            orders_ids.add(seq_order.get(id_seq_order));
            if (total < waveSizeLB)
                continue;
            if (total > waveSizeUB)
                break;

            while (id_seq_aisle < seq_aisle.size() && !checkHaveNeed(items_h, items_n)) {
                addMapValues(items_h, aisles.get(seq_aisle.get(id_seq_aisle)));
                aisles_ids.add(seq_aisle.get(id_seq_aisle));
                id_seq_aisle++;

            }

            if (checkHaveNeed(items_h, items_n)) {
                ChallengeSolution n_solution = new ChallengeSolution(orders_ids, aisles_ids);
                if (solution == null || (computeObjectiveFunction(solution) < computeObjectiveFunction(n_solution))) {
                    solution = n_solution;
                }
            }
        }

        return solution;
    }

    /**
     * WARNING O(n2)
     * 
     * @return ChallengeSolution solution
     */
    public ChallengeSolution bestBySequenceAisleOrder(List<Integer> seq_order, List<Integer> seq_aisle) {
        ChallengeSolution solution = null;

        Set<Integer> orders_ids = new HashSet<Integer>();
        Set<Integer> aisles_ids = new HashSet<Integer>();
        int total = 0;
        Map<Integer, Integer> items_h = new HashMap<Integer, Integer>(); // Aisle
        Map<Integer, Integer> items_n = new HashMap<Integer, Integer>(); // Orders

        for (int id_seq_aisle = 0; id_seq_aisle < seq_aisle.size(); id_seq_aisle++) {
            int aisle_id = seq_aisle.get(id_seq_aisle);

            addMapValues(items_h, aisles.get(aisle_id));
            aisles_ids.add(aisle_id);

            for (int id_seq_order = 0; id_seq_order < seq_order.size(); id_seq_order++) {
                int order_id = seq_order.get(id_seq_order);
                if (orders_ids.contains(order_id))
                    continue;

                Map<Integer, Integer> order = orders.get(order_id);

                if (canGetOrder(order_id, items_h, items_n, total)) {
                    total += sumMapValues(order);
                    addMapValues(items_n, order);
                    orders_ids.add(order_id);

                    if (total >= waveSizeLB) {
                        ChallengeSolution n_solution = new ChallengeSolution(orders_ids, aisles_ids);
                        if (solution == null
                                || (computeObjectiveFunction(solution) < computeObjectiveFunction(n_solution))) {
                            solution = n_solution;
                        }
                    }
                }
            }
        }
        return solution;
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

        if (Config.VERBOSE) {
            System.out.println("Starting a search...");
            System.out.print("Previus selected:");
            System.out.println(new ChallengeSolution(orders_ids, aisles_ids));
            System.out.println("--------------------------------------");
        }

        // Try add orders
        List<Integer> options = complementOrdersIdsList(orders_ids);
        Collections.shuffle(options);
        for (int option : options) {
            if (canGetOrder(option, items_h, items_n, total)) {
                addMapValues(items_n, orders.get(option));
                total += sumMapValues(orders.get(option));
                orders_ids.add(option);

                if (total >= waveSizeLB)
                    if (r.nextDouble() < Config.STOP_ADD_CHANCE)
                        break;

            }
        }

        // Try remove aisle
        options = new ArrayList<Integer>(aisles_ids);
        Collections.shuffle(options);
        for (int option : options) {
            if (canRemoveAisle(option, items_h, items_n, total)) {
                removeMapValues(items_h, aisles.get(option));
                total -= sumMapValues(aisles.get(option));
                aisles_ids.remove(option);

                if (r.nextDouble() < Config.STOP_REMOVE_CHANCE)
                    break;
            }
        }

        return new ChallengeSolution(orders_ids, aisles_ids);
    }

    public List<ChallengeSolution> CreateInitialPopulation(StopWatch stopWatch) {
        List<ChallengeSolution> population = new ArrayList<ChallengeSolution>();

        List<Integer> seq_order = new ArrayList<Integer>();
        List<Integer> seq_aisle = new ArrayList<Integer>();

        for (int i = 0; i < orders.size(); i++)
            seq_order.add(i);
        for (int i = 0; i < aisles.size(); i++)
            seq_aisle.add(i);

        Collections.shuffle(seq_order);
        population.add(bestBySequenceAisleOrder(seq_order, seq_aisle));

        // System.out.println(population.get(population.size() - 1) + " - "
        // + computeObjectiveFunction(population.get(population.size() - 1)));

        // for (int i = 0; i < Config.POPULATION; i++) {
        while (!timeOver(stopWatch)) {
            population.add(dumpSolution());

            population.add(bestBySequenceOrderAisle(seq_order, seq_aisle));

            Collections.shuffle(seq_order);

            // Aisle random
            Collections.shuffle(seq_aisle);
            population.add(bestBySequenceOrderAisle(seq_order, seq_aisle));

            // Aisle by quantity of types of items
            Collections.sort(seq_aisle, (a, b) -> aisles.get(b).size() - aisles.get(a).size());
            population.add(bestBySequenceOrderAisle(seq_order, seq_aisle));
            // Reverse order
            Collections.sort(seq_aisle, (a, b) -> aisles.get(a).size() - aisles.get(b).size());
            population.add(bestBySequenceOrderAisle(seq_order, seq_aisle));

            // Aisle by quantity of items
            Collections.sort(seq_aisle, (a, b) -> aisles_size.get(b) - aisles_size.get(a));
            population.add(bestBySequenceOrderAisle(seq_order, seq_aisle));
            // Reverse order
            Collections.sort(seq_aisle, (a, b) -> aisles_size.get(a) - aisles_size.get(b));
            population.add(bestBySequenceOrderAisle(seq_order, seq_aisle));
            // System.out.println(seq_aisle);
        }

        population.add(bestBySequenceAisleOrder(seq_order, seq_aisle));
        // System.out.println(population.get(population.size() - 1) + " - "
        // + computeObjectiveFunction(population.get(population.size() - 1)));

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
        if (Config.VERBOSE) {
            System.out.println(Config.EXPANSION_SET_SELECTED);
        }
        if (Config.VERBOSE) {
            System.out.printf("Numero de items: %d\n", nItems);
            System.out.printf("waveSizeLB: %d, waveSizeUB: %d\n", waveSizeLB, waveSizeUB);

            imprimirListaDeMapas("orders:", orders);
            imprimirListaDeMapas("aisles:", aisles);
        }
        List<ChallengeSolution> population = CreateInitialPopulation(stopWatch);

        ChallengeSolution best = bestSolution(population);

        if (Config.VERBOSE) {
            System.out.println(best);
        }

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

    protected boolean timeOver(StopWatch stopWatch) {
        return stopWatch.getDuration().toMillis() > Config.MAX_DURATION;
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
