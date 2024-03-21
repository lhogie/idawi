import java.util.*;

class ShortestPathTree {

    static class Edge {
        Vertex destination;
        int weight;

        public Edge(Vertex destination, int weight) {
            this.destination = destination;
            this.weight = weight;
        }
    }

    static class Vertex {
        int id;
        List<Edge> neighbors;

        public Vertex(int id) {
            this.id = id;
            this.neighbors = new ArrayList<>();
        }

        public void addEdge(Vertex destination, int weight) {
            neighbors.add(new Edge(destination, weight));
            destination.neighbors.add(new Edge(this, weight));
        }
    }

    static class Graph {
        List<Vertex> vertices;
        Map<Vertex, Vertex> parentMap;

        public Graph() {
            this.vertices = new ArrayList<>();
            this.parentMap = new HashMap<>();
        }

        public void addVertex(Vertex vertex) {
            vertices.add(vertex);
        }

        public Map<Vertex, Vertex> dijkstra(Vertex source) {
            Map<Vertex, Integer> distanceMap = new HashMap<>();
            for (Vertex vertex : vertices) {
                distanceMap.put(vertex, Integer.MAX_VALUE);
            }
            distanceMap.put(source, 0);

            PriorityQueue<Edge> minHeap = new PriorityQueue<>((a, b) -> Integer.compare(a.weight, b.weight));
            minHeap.offer(new Edge(source, 0));

            while (!minHeap.isEmpty()) {
                Edge current = minHeap.poll();
                Vertex currentVertex = current.destination;

                for (Edge neighbor : currentVertex.neighbors) {
                    int newDistance = distanceMap.get(currentVertex) + neighbor.weight;

                    if (newDistance < distanceMap.get(neighbor.destination)) {
                        distanceMap.put(neighbor.destination, newDistance);
                        parentMap.put(neighbor.destination, currentVertex);
                        minHeap.offer(new Edge(neighbor.destination, newDistance));
                    }
                }
            }

            return parentMap;
        }

        public void printShortestPathTree(Map<Vertex, Vertex> parentMap, Vertex source) {
            System.out.println("Shortest Path Tree from source vertex " + source.id + ":");
            for (Vertex vertex : parentMap.keySet()) {
                Vertex parent = parentMap.get(vertex);
                if (parent != null) {
                    System.out.println("Edge: " + parent.id + " -> " + vertex.id);
                }
            }
        }
    }

    public static void main(String[] args) {
        Graph graph = new Graph();

        Vertex v0 = new Vertex(0);
        Vertex v1 = new Vertex(1);
        Vertex v2 = new Vertex(2);
        Vertex v3 = new Vertex(3);
        Vertex v4 = new Vertex(4);

        v0.addEdge(v1, 2);
        v0.addEdge(v3, 1);
        v1.addEdge(v2, 3);
        v1.addEdge(v3, 2);
        v2.addEdge(v4, 1);
        v3.addEdge(v4, 4);

        graph.addVertex(v0);
        graph.addVertex(v1);
        graph.addVertex(v2);
        graph.addVertex(v3);
        graph.addVertex(v4);

        Vertex sourceVertex = v0;
        Map<Vertex, Vertex> parentMap = graph.dijkstra(sourceVertex);

        graph.printShortestPathTree(parentMap, sourceVertex);
    }
}
