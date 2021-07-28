/* *****************************************************************************
 *  Name:
 *  Date:
 *  Description:
 **************************************************************************** */

import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.StdOut;

import java.util.ArrayList;

public class BaseballElimination {

    private final ArrayList<String> teams = new ArrayList<>();
    private final int[] wins;
    private final int[] losses;
    private final int[] remaining;
    private final int[][] against;
    private int maxPossibleFlow; // From s in specific flow network.

    // create a baseball division from given filename in format specified below
    public BaseballElimination(String filename) {
        In in = new In(filename);
        int total = in.readInt();
        wins = new int[total];
        losses = new int[total];
        remaining = new int[total];
        against = new int[total][total];

        for (int i = 0; i < total; i++) {
            teams.add(in.readString());
            wins[i] = in.readInt();
            losses[i] = in.readInt();
            remaining[i] = in.readInt();
            for (int j = 0; j < total; j++) {
                against[i][j] = in.readInt();
            }
        }

    }

    // number of teams
    public int numberOfTeams() {
        return teams.size();
    }

    // all teams
    public Iterable<String> teams() {
        return teams;
    }

    // number of wins for given team
    public int wins(String team) {
        checkValidTeam(team);
        return wins[teams.indexOf(team)];
    }

    // number of losses for given team
    public int losses(String team) {
        checkValidTeam(team);
        return losses[teams.indexOf(team)];
    }

    // number of remaining games for given team
    public int remaining(String team) {
        checkValidTeam(team);
        return remaining[teams.indexOf(team)];
    }

    // number of remaining games between team1 and team2
    public int against(String team1, String team2) {
        checkValidTeam(team1);
        checkValidTeam(team2);
        return against[teams.indexOf(team1)][teams.indexOf(team2)];
    }

    // is given team eliminated?
    public boolean isEliminated(String team) {
        checkValidTeam(team);

        // Trivial Elimination
        if (!trivialElimination(teams.indexOf(team)).isEmpty()) {
            return true;
        }

        // Non-trivial Elimination using max flow
        maxPossibleFlow = 0;
        FlowNetwork network = createFlowNetwork(teams.indexOf(team));
        FordFulkerson fordFulkerson = new FordFulkerson(network, 0, network.V() - 1);
        return fordFulkerson.value() != maxPossibleFlow;
    }

    // subset R of teams that eliminates given team; null if not eliminated
    public Iterable<String> certificateOfElimination(String team) {
        checkValidTeam(team);

        // Trivial Elimination
        ArrayList<String> trivialSubset = trivialElimination(teams.indexOf(team));
        if (!trivialSubset.isEmpty()) {
            return trivialSubset;
        }

        // Non-trivial Elimination using max flow
        maxPossibleFlow = 0;
        FlowNetwork network = createFlowNetwork(teams.indexOf(team));
        FordFulkerson fordFulkerson = new FordFulkerson(network, 0, network.V() - 1);
        ArrayList<String> subset = new ArrayList<>();

        if (fordFulkerson.value() == maxPossibleFlow) return null; // not eliminated

        for (int i = 0; i < teams.size(); i++) {
            if (fordFulkerson.inCut(teamVertex(i))) {
                subset.add(teams.get(i));
            }
        }

        return subset;
    }

    private ArrayList<String> trivialElimination(int selectedTeam) {
        ArrayList<String> subset = new ArrayList<>();
        for (int i = 0; i < teams.size(); i++) {
            if (i == selectedTeam) {
                continue;
            }
            if (wins[i] - wins[selectedTeam] - remaining[selectedTeam] > 0) {
                subset.add(teams.get(i));
            }
        }
        return subset;
    }

    private FlowNetwork createFlowNetwork(int selectedTeam) {
        // Count = 2 (s & t), team vertices, game vertices (combination).
        int noOfTeams = teams.size();
        int vertices = 2 + noOfTeams * (noOfTeams - 1) / 2 + noOfTeams;
        int s = 0, t = vertices - 1;
        FlowNetwork network = new FlowNetwork(vertices);

        for (int i = 0; i < noOfTeams - 1; i++) {
            for (int j = i + 1; j < noOfTeams; j++) {
                // Important: Any game/team vertex linked to selectedTeam is isolated.
                if (i == selectedTeam || j == selectedTeam) continue;

                network.addEdge(new FlowEdge(s, gameVertex(i, j), against[i][j]));
                maxPossibleFlow += against[i][j];
                network.addEdge(
                        new FlowEdge(gameVertex(i, j), teamVertex(i), Double.POSITIVE_INFINITY));
                network.addEdge(
                        new FlowEdge(gameVertex(i, j), teamVertex(j), Double.POSITIVE_INFINITY));
            }
        }

        for (int i = 0; i < noOfTeams; i++) {
            int capacity = wins[selectedTeam] + remaining[selectedTeam] - wins[i];
            network.addEdge(new FlowEdge(teamVertex(i), t, capacity));
        }

        return network;
    }

    private int gameVertex(int i, int j) {
        int index = 0;
        int adder = teams.size() - 1;
        for (int x = 0; x < i; x++) {
            index += adder;
            adder--;
        }
        index = index + j - i;
        return index;
    }

    private int teamVertex(int i) {
        int noOfTeams = teams.size();
        int combi = noOfTeams * (noOfTeams - 1) / 2;
        return combi + i + 1;
    }

    private void checkValidTeam(String team) {
        if (teams.indexOf(team) < 0) throw new IllegalArgumentException();
    }

    public static void main(String[] args) {
        BaseballElimination division = new BaseballElimination(args[0]);
        for (String team : division.teams()) {
            if (division.isEliminated(team)) {
                StdOut.print(team + " is eliminated by the subset R = { ");
                for (String t : division.certificateOfElimination(team)) {
                    StdOut.print(t + " ");
                }
                StdOut.println("}");
            }
            else {
                StdOut.println(team + " is not eliminated");
            }
        }
    }
}
