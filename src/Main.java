import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    // Problem specifications

    // For our maze problem, we have a passability map that indicates whether some state
    // is a wall (a wall is false, a non-wall is true)
    static Boolean[][] passableMap = { {true, true, true, true},
                                       {true, false, true, true},
                                       {true, true, true, true} };

    // The reward map must have the same size as the passability map and sets a reward at
    // the corresponding state
    static Double[][] rewardMap = { {0.0, 0.0, 0.0, 1.0},
                                    {0.0, 0.0, 0.0, -1.0},
                                    {0.0, 0.0, 0.0, 0.0} };

    // This is a set of actions where the action our agent intends to take is the row and
    // the action our agent actually takes is the column (e.g. take action UP but the agent
    // actually goes RIGHT)
                                // UP    DOWN  RIGHT LEFT (actual action)
    static double actions[][] = { {0.8,  0  ,  0.1,  0.1},   // UP (taken action)
                                  {0  ,  0.8,  0.1,  0.1},   // DOWN
                                  {0.1,  0.1,  0.8,  0  },   // RIGHT
                                  {0.1,  0.1,  0  ,  0.8} }; // LEFT

    // This is a set of observations where the true value is the row and the observed value is the column
                                    // 1    2    end observation
    static double observables[][] = { {0.9, 0.1, 0  }, // 1 wall states
                                      {0.1, 0.9, 0  }, // 2 wall states
                                      {0  , 0  , 1  }};// terminal states


    static int width = passableMap[0].length;
    static int height = passableMap.length;

    public static void main(String[] args) {
        // Initialize the states according to the specifications above
        State[][] stateMap = new State[width][height];
        List<State> stateList = new ArrayList<>();
        initializeStates(stateMap, stateList);

        // Specify initial belief state as a map
        Double[][] beliefStateMap = { {0.0, 1.0, 0.0, 0.0},
                                      {0.0, 0.0, 0.0, 0.0},
                                      {0.0, 0.0, 0.0, 0.0} };

        // Specify actions as a list of integers
        // An action is an integer where 0 is UP, 1 is DOWN, 2 is RIGHT, 3 is LEFT
        int[] actions = {2, 2, 0};

        // Specify observations as a list of integers
        // An observation is an integer where 0 is 1 WALL, 1 is 2 WALL, and 2 is TERMINAL
        int[] observations = {0, 0, 2};

        computeBeliefState(beliefStateMap, actions, observations, stateMap, stateList);
    }


    static class State {
        int i; // row
        int j; // column
        int id; // index of stateList
        double reward;

        State(int i, int j, int id) {
            this.i = i;
            this.j = j;
            this.id = id;
            neighbours = new State[actions.length];
            transition = new double[width*height][actions.length];
            observable = new double[observables.length];
        }

        State[] neighbours; // Neighbours of the state with respect to the action taken
                            // (e.g. neighbour at index 0 is the state above this one)
        double[][] transition; // Such that transition[S'][A] = P(S' | this state, A)
        double[] observable;   // Such that observable[e] = P(e | this state)
    }

    static <T> T get(T[][] map, int i, int j) {
        return map[map.length-1-j][i];
    }

    static void initializeStates(State[][] stateMap, List<State> stateList) {

        // Initializing states
        for (int i = 0; i < width; i++)  {
            for (int j = 0; j < height; j++) {
                // Passable map is converted into a state board/matrix, also have a copy
                // represented as a list of states
                if (get(passableMap, i, j)) {
                    State state = new State(i, j, stateList.size());
                    stateList.add(state);
                    stateMap[i][j] = state;
                    // Set up rewards
                    state.reward = get(rewardMap, i, j);
                }
            }
        }

        // Setting up adjacencies/neighbours
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                State state = stateMap[i][j];
                if (state != null) {
                    // Get the neighbours of a state according to the actions (e.g. 0 is UP)
                    if (state.j+1 < height) state.neighbours[0] = stateMap[state.i][state.j+1]; // UP
                    if (state.j-1 >= 0) state.neighbours[1] = stateMap[state.i][state.j-1]; // DOWN
                    if (state.i+1 < width) state.neighbours[2] = stateMap[state.i+1][state.j]; // RIGHT
                    if (state.i-1 >= 0) state.neighbours[3] = stateMap[state.i-1][state.j]; // LEFT
                }
            }
        }

        // Setting up transition model
        for (State state : stateList) {
            // Transition
            if (state.reward != 0) { // If this is a terminal state
                for (int action = 0; action < actions.length; action++) {
                    state.transition[state.id][action] = 1; // No action can move the agent from the terminal state
                }
            } else { // Non-terminal
                // Determine P(S' | state, A)
                for (int actionThatOccurs = 0; actionThatOccurs < actions.length; actionThatOccurs++) {
                    for (int actionTaken = 0; actionTaken < actions[actionThatOccurs].length; actionTaken++) {
                        State neighbour = state.neighbours[actionThatOccurs];
                        if (neighbour != null) {
                            // No wall means add probability to neighbour states
                            state.transition[neighbour.id][actionTaken] += actions[actionThatOccurs][actionTaken];
                        } else {
                            // If hit a wall, add probability to this state itself
                            state.transition[state.id][actionTaken] += actions[actionThatOccurs][actionTaken];
                        }
                    }
                }
            }

            // Setting up observation model
            if (state.reward != 0) { // If terminal state, set P(e | state) to terminal observable
                state.observable = observables[2].clone();
            } else {
                int numWalls = 0;
                for (State neighbour : state.neighbours) {
                    if (neighbour == null) numWalls++;
                }
                if (numWalls == 1) { // If 1 wall, set P(e | state) to 1 wall observable
                    state.observable = observables[0].clone();
                } else if (numWalls == 2) { // If 2 wall, set P(e | state) to 2 wall observable
                    state.observable = observables[1].clone();
                } else {
                    throw new RuntimeException("Impossible");
                }
            }
        }
    }

    static void computeBeliefState(Double[][] beliefStateMap, int[] actions, int[] observations,
                                          State[][] stateMap, List<State> stateList) {
        // Build beliefStateList from beliefStateMap
        List<Double> beliefStateList = Arrays.asList(new Double[stateList.size()]);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (stateMap[i][j] != null) beliefStateList.set(stateMap[i][j].id, get(beliefStateMap, i, j));
            }
        }

        // Update belief
        for (int iteration = 0; iteration < actions.length; iteration++) {
            List<Double> beliefStateListPrime = new ArrayList<>(beliefStateList); // This is b'
            double normalizationFactor = 0; // This is 1/alpha
            int a = actions[iteration]; // This is a
            int e = observations[iteration]; // This is e

            for (int sPrime = 0; sPrime < stateList.size(); sPrime++) {
                double sum = 0; // sum_s P(s' | a, s)b(s)
                for (int s = 0; s < stateList.size(); s++) {
                    sum += stateList.get(s).transition[sPrime][a] * beliefStateList.get(s);
                }
                // b'(s') = P(e | s') * sum
                double bOfSPrime = stateList.get(sPrime).observable[e] * sum;
                normalizationFactor += bOfSPrime;
                beliefStateListPrime.set(sPrime, bOfSPrime);
            }
            // Normalize using alpha (normalizationFactor)
            for (int i = 0; i < beliefStateListPrime.size(); i++) {
                double unNormalized = beliefStateListPrime.get(i);
                // b'(s') * normalizationFactor
                beliefStateListPrime.set(i, unNormalized/normalizationFactor);
            }

            // Repeat process for next action, observation pair
            beliefStateList = beliefStateListPrime;
        }

        // Build beliefStateMap
        beliefStateMap = new Double[width][height];
        for (int i = 0; i < beliefStateList.size(); i++) {
            State state = stateList.get(i);
            beliefStateMap[state.i][state.j] = beliefStateList.get(i);
        }

        // Visualize beliefState
        printBeliefState(beliefStateMap);
    }

    static void printBeliefState(Double[][] beliefStateMap) {
        System.out.println("");
        for (int j = height-1; j >= 0; j--) {
            String row = "";
            for (int i = 0; i < width; i++) {
                if (beliefStateMap[i][j] == null) {
                    row += "WALL \t\t";
                } else {
                    row += String.format("%.3f", beliefStateMap[i][j]) + "\t\t";
                }
            }
            System.out.println(row + "\n");
        }
    }
}