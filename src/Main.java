import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    static boolean passableMap[][] = { {true, true, true},
                                       {true, false, true},
                                       {true, true, true},
                                       {true, true, true} };
    static double rewardMap[][] = { {0, 0, 0},
                                    {0, 0, 0},
                                    {0, 0, 0},
                                    {0, -1, 1} };
//    float beliefState[][] = new float[3][4];
//    for (int i=0; i<3; i++){
//
//    }
                                // U    D    R    L (actual action)
    static double actions[][] = { {0.8, 0  , 0.1, 0.1},   // U (desired action)
                                  {0  , 0.8, 0.1, 0.1},   // D
                                  {0.1, 0.1, 0.8, 0  },   // R
                                  {0.1, 0.1, 0  , 0.8} }; // L

                                    // 1    2    end
    static double observables[][] = { {0.9, 0.1, 0  }, // 1 wall
                                      {0.1, 0.9, 0  }, // 2 wall
                                      {0  , 0  , 1  }};// terminal

//    static double observables[][] = { { 1, 1, 1 }, // 1 wall
//                                      {1, 1, 1 }, // 2 wall
//                                      {1, 1, 1 }};// terminal

    private static class State {
        int i;
        int j;
        int id;
        double reward;

        State(int i, int j, int id) {
            this.i = i;
            this.j = j;
            this.id = id;
            neighbours = new State[4];
            transition = new double[11][4];
            observable = new double[3];
        }

        public void printState(List<State> stateList) {
            System.out.println("==========================");
            System.out.println("My value: " + i + ", " + j);
            System.out.println("\tNeighbours:");
            for (State state : neighbours) {
                if (state != null) {
                    System.out.println("\t\t" + state.i + ", " + state.j);
                } else {
                    System.out.println("\t\tWall");
                }
            }
            System.out.println("\tTransition:");
            System.out.println("\t\t\tU   D   R   L");
            for (int i = 0; i < transition.length; i++) {
                State state = stateList.get(i);
                String output = "\t(" + state.i + ", " + state.j + ")";
                for (double probability : transition[i]) {
                    output += " " + probability;
                }
                System.out.println(output);
            }
//            System.out.println("\tObservable:");
//            System.out.println("\t 1   2  end");
//            String output = "\t";
//            for (double probability : observable) {
//                output += probability + " ";
//            }
//            System.out.println(output);
            System.out.println("==========================");
        }

        State[] neighbours;
        double[][] transition;
        double[] observable;
    }


    public static void main(String[] args) {
        State[][] stateMap = new State[4][3];
        List<State> stateList = new ArrayList<>();

        // Initializing states
        for (int i = 0; i < passableMap.length; i++)  {
            for (int j = 0; j < passableMap[i].length; j++) {
                // Passable map is inverted
                if (passableMap[i][j]) {
                    State state = new State(i, j, stateList.size());
                    stateList.add(state);
                    stateMap[i][j] = state;
                    // Set up rewards
                    state.reward = rewardMap[i][j];
                }
            }
        }

        // Setting up adjacencies
        for (int i = 0; i < stateMap.length; i++) {
            for (int j = 0; j < stateMap[i].length; j++) {
                State state = stateMap[i][j];
                if (state != null) {
                    if (state.j+1 < passableMap[i].length) state.neighbours[0] = stateMap[state.i][state.j+1]; // UP
                    if (state.j-1 >= 0) state.neighbours[1] = stateMap[state.i][state.j-1]; // DOWN
                    if (state.i+1 < passableMap.length) state.neighbours[2] = stateMap[state.i+1][state.j]; // RIGHT
                    if (state.i-1 >= 0) state.neighbours[3] = stateMap[state.i-1][state.j]; // LEFT
                }
            }
        }

        // Setting up probabilities
        for (State state : stateList) {
            // Transition
            if (state.reward != 0) { // Terminal
                for (int action = 0; action < actions.length; action++) {
                    state.transition[state.id][action] = 1;
                }
            } else { // Non-terminal
                for (int actionThatOccurs = 0; actionThatOccurs < actions.length; actionThatOccurs++) {
                    for (int actionTaken = 0; actionTaken < actions[actionThatOccurs].length; actionTaken++) {
                        State neighbour = state.neighbours[actionThatOccurs];
                        if (neighbour != null) {
                            // No wall means update neighbour
                            state.transition[neighbour.id][actionTaken] += actions[actionThatOccurs][actionTaken];
                        } else {
                            // If hit a wall, add to our own probability
                            state.transition[state.id][actionTaken] += actions[actionThatOccurs][actionTaken];
                        }
                    }
                }
            }

            // Observable
            if (state.reward != 0) { // Terminal
                state.observable = observables[2].clone();
            } else {
                int numWalls = 0;
                for (State neighbour : state.neighbours) {
                    if (neighbour == null) numWalls++;
                }
                if (numWalls == 1) { // 1 wall
                    state.observable = observables[0].clone();
                } else if (numWalls == 2) { // 2 wall
                    state.observable = observables[1].clone();
                } else {
                    throw new RuntimeException("Impossible");
                }
            }
        }

//        for (State state : stateList) {
//            state.printState(stateList);
//        }

//        double[][] beliefStateMap = { {0.1111, 0.1111, 0.1111},
//                                      {0.1111, 0,      0.1111},
//                                      {0.1111, 0.1111, 0.1111},
//                                      {0.1111, 0,      0} };
        double[][] beliefStateMap = { {1, 0, 0},
                                      {0, 0, 0},
                                      {0, 0, 0},
                                      {0, 0, 0} };
        int[] actions = {0, 2, 2, 2};
        int[] observations = {1, 1, 0, 0};
        computeBeliefState(beliefStateMap, actions, observations, stateMap, stateList);
    }

    // An action is an integer where 0 is UP, 1 is DOWN, 2 is RIGHT, 3 is LEFT
    // An observation is an integer where 0 is 1 WALL, 1 is 2 WALL, and 3 is TERMINAL
    public static void computeBeliefState(double[][] beliefStateMap, int[] actions, int[] observations,
                                          State[][] stateMap, List<State> stateList) {
        // Build beliefStateList
        List<Double> beliefStateList = Arrays.asList(new Double[stateList.size()]);
        for (int i = 0; i < beliefStateMap.length; i++) {
            for (int j = 0; j < beliefStateMap[i].length; j++) {
                if (stateMap[i][j] != null) beliefStateList.set(stateMap[i][j].id, beliefStateMap[i][j]);
            }
        }

        // Update belief
        for (int iteration = 0; iteration < actions.length; iteration++) {
            List<Double> beliefStatePrime = new ArrayList<>(beliefStateList);
            double normalizationFactor = 0;
            int a = actions[iteration];
            int e = observations[iteration];

            for (int sPrime = 0; sPrime < stateList.size(); sPrime++) {
                double sum = 0;
                for (int s = 0; s < stateList.size(); s++) {
                    sum += stateList.get(s).transition[sPrime][a] * beliefStateList.get(s);
                }
                double bOfSPrime = stateList.get(sPrime).observable[e] * sum;
                normalizationFactor += bOfSPrime;
                State statePrime = stateList.get(sPrime);
//                System.out.println("Iteration: " + iteration + " | ( " + statePrime.i + ", " + statePrime.j + ") = "
//                                    + bOfSPrime);
                beliefStatePrime.set(sPrime, bOfSPrime);
            }
            // Normalize
            for (int i = 0; i < beliefStatePrime.size(); i++) {
                double unNormalized = beliefStatePrime.get(i);
                beliefStatePrime.set(i, unNormalized/normalizationFactor);
            }

            beliefStateList = beliefStatePrime;
        }

        // Build beliefStateMap
        for (int i = 0; i < beliefStateList.size(); i++) {
            State state = stateList.get(i);
            beliefStateMap[state.i][state.j] = beliefStateList.get(i);
        }

        printBeliefState(beliefStateMap);
    }

    public static void printBeliefState(double[][] beliefStateMap) {
        System.out.println("");
        for (int j = beliefStateMap[0].length-1; j >= 0; j--) {
            String row = "";
            for (int i = 0; i < beliefStateMap.length; i++) {
                row += String.format("%.3f", beliefStateMap[i][j]) + "\t\t";
            }
            System.out.println(row + "\n");
        }
    }
}
