package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.LinkedList;

public class LinkStateDatabase {

  //linkID => LSAInstance
  HashMap<String, LSA> _store = new HashMap<String, LSA>();

  private RouterDescription rd = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    rd = routerDescription;
    LSA l = initLinkStateDatabase();
    _store.put(l.linkStateID, l);
  }

  /**
   * output the shortest path from this router to the destination with the given IP address
   * <p/>
   * This method should use Dijkstra's algorithm with link weights (not hop count) to find the shortest path.
   * The weights are stored in the weight field of LinkDescription objects in the LSA entries.
   * <p/>
   * format: source ip address -> ip address -> ... -> destination ip
   *
   * @param destinationIP the simulated IP address of the destination router
   * @return the shortest path as a string, or null if no path exists
   */
  String getShortestPath(String destinationIP) {
    List<String> path = getShortestPathRouterIDs(destinationIP);

    if (path == null) {
      return "Path not found";
    }

    StringBuilder result = new StringBuilder("Path found: ");

    for (int i = 0; i < path.size(); i++) {
        String routerID = path.get(i);

        result.append(routerID);

        if (i < path.size() - 1) {
            String nextRouterID = path.get(i + 1);

            for (LinkDescription link : _store.get(routerID).links) {
                if (link.linkID.equals(nextRouterID)) {
                    result.append(" -> (")
                          .append(link.weight)
                          .append(") ");
                    break;
                }
            }
        }
    }

    return result.toString();
  }

  List<String> getShortestPathRouterIDs(String destinationIP) {
    HashMap<String, Integer> dist = new HashMap<>();
    HashMap<String, String> prev = new HashMap<>();
    PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));

    String sourceIP = rd.simulatedIPAddress;

    dist.put(sourceIP, 0);

    for (String routerID : _store.keySet()) {
      if (!routerID.equals(sourceIP)) {
        dist.put(routerID, Integer.MAX_VALUE);
      }
      prev.put(routerID, null);
    }

    pq.offer(Map.entry(sourceIP, 0));

    while (!pq.isEmpty()){
      Map.Entry<String, Integer> current = pq.poll();

      String currentRouterID = current.getKey();
      int distance = current.getValue();

      if (distance > dist.get(currentRouterID)) {
        continue;
      }

      // If we have reached the destination, we can break out of the loop. We don't need to continue processing other nodes.
      if (currentRouterID.equals(destinationIP)) {
        break;
      }

      // Preventing NullPointerException by checking if the current router ID exists in the _store before accessing its links
      if (_store.get(currentRouterID) == null) {
        continue;
      }

      for (LinkDescription link : _store.get(currentRouterID).links) {
        String neighborID = link.linkID;
        int weight = link.weight;
        
        int newDist = distance + weight;

        if (newDist < dist.getOrDefault(neighborID, Integer.MAX_VALUE)) {
          dist.put(neighborID, newDist);
          prev.put(neighborID, currentRouterID);
          pq.offer(Map.entry(neighborID, newDist));
        }
      }
    }

    if (dist.getOrDefault(destinationIP, Integer.MAX_VALUE) == Integer.MAX_VALUE) {
      return null;
    }

    String curr = destinationIP;
    LinkedList<String> path = new LinkedList<>();

    while (curr != null) {
      path.addFirst(curr);
      curr = prev.get(curr);
    }

    return path;
  }

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA();
    lsa.linkStateID = rd.simulatedIPAddress;
    lsa.lsaSeqNumber = Integer.MIN_VALUE;
    LinkDescription ld = new LinkDescription();
    ld.linkID = rd.simulatedIPAddress;
    ld.portNum = -1;
    ld.tosMetrics = 0;
    ld.weight = 0; //self-link has weight 0
    lsa.links.add(ld);
    return lsa;
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LSA lsa: _store.values()) {
      sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
      for (LinkDescription ld : lsa.links) {
        sb.append(ld.linkID).append(",").append(ld.portNum).append(",").
                append(ld.weight).append("\t");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

}
