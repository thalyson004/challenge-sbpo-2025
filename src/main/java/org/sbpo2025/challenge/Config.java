package org.sbpo2025.challenge;

public class Config {
    static String[] OPERATIONS = { "ADD", "REMOVE", "SWAP" };
    static boolean[] OPERATIONS_ENABLED = { true, true, false };

    static int MAX_UNIT = 2; // Mx of operations by mutation

    static int EXPANSION_SET = 50; // Each expansion, select the best EXPANSION_SET
    static int EXPANSION_SET_SELECTED = 10; // Randomly select the bests into the pre-select expansion set

    static int POPULATION = 100; // Each generation, select the best POPULATION
    static int POPULATION_SELECTED = 50; // Randomly select the bests into the pre-select population

    static double CROSSOVER_RATE = 0.2;

    static boolean PRESERVE_BEST = false; // If true, preserve the best solution for the next generation
}
