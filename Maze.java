import tester.*;
import java.util.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;


// represents a vertex and its properties
class Vertex {
  int x;
  int y;
  Color color;
  boolean topBorder;
  boolean leftBorder;
  boolean bottomBorder;
  boolean rightBorder;
  ArrayList<Vertex> borderingVerticesInMaze;
  Vertex parent; // each vertex's previous vertex, used for retracing the solution path



  Vertex(int x, int y, Color color) {
    this.x = x;
    this.y = y;
    this.color = color;
    this.topBorder = true;
    this.leftBorder = true;
    this.bottomBorder = true;
    this.rightBorder = true;
    this.borderingVerticesInMaze = new ArrayList<Vertex>();
  }


  // draws a singular vertex and its surrounding borders
  void drawVertexAndBorders(WorldScene scene, int size, int width, int height) {

    // drawing the vertices with the proper colors
    WorldImage v = 
        new RectangleImage(size, size, OutlineMode.SOLID, this.color);
    scene.placeImageXY(v, this.x * size + (size / 2), this.y * size + (size / 2));


    // drawing the top border of the vertex if necessary
    if (this.topBorder) {
      WorldImage topBorder = new LineImage(new Posn(size, 0), Color.BLACK);
      scene.placeImageXY(topBorder, this.x * size + (size / 2), this.y * size);
    }

    // drawing the left border of the vertex if necessary
    if (this.leftBorder) {
      WorldImage leftBorder = new LineImage(new Posn(0, size), Color.BLACK);
      scene.placeImageXY(leftBorder, this.x * size, this.y * size + (size / 2));
    }

    // drawing the bottom border of the vertex if necessary
    if (this.bottomBorder) {
      WorldImage bottomBorder = new LineImage(new Posn(size, 0), Color.BLACK);
      scene.placeImageXY(bottomBorder, this.x * size + (size / 2), (this.y + 1) * size);
    }

    // drawing the right border of the vertex if necessary
    if (this.rightBorder) {
      WorldImage rightBorder = new LineImage(new Posn(0, size), Color.BLACK);
      scene.placeImageXY(rightBorder, (this.x + 1) * size, this.y * size + (size / 2));
    }
  }
}





// represents an edge and its properties
class Edge implements Comparable<Edge> {
  Vertex from;
  Vertex to;
  int weight;



  Edge(Vertex from, Vertex to, int weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
  }


  // comparing two edges by weight to determine which comes first when sorting
  public int compareTo(Edge other) {
    return this.weight - other.weight;
  }
}





// represents the maze and its properties
class Maze extends World {
  int width; 
  int height; 
  Random rand;
  ArrayList<ArrayList<Vertex>> vertices; // all of the vertices in the maze
  ArrayList<Edge> allEdges; // all of the possible edges between every vertex in the maze
  ArrayList<Edge> edgesInMaze; // all of the edges in the maze, produced by Kruskal's algorithm
  boolean bfs; // whether or not a bfs is being done on the maze
  boolean dfs; // whether or not a dfs is being done on the maze
  Deque<Vertex> bfsWorklist;  // the worklist for performing a bfs on the maze
  Stack<Vertex> dfsWorklist; // the worklist for performing a dfs on the maze
  ArrayList<Vertex> seen; // the list of elements that have been seen while traversing the maze
  boolean currentlySearchingMaze;
  boolean mazeSolved;
  Vertex currentRetracingVertex; // determines the current vertex that we are starting from 
  // to retrace the solution path


  // size of width and height of the rectangle for each vertex calculated using the width and 
  // height. Math is done to ensure that the height of the maze is 600 px or the width of the maze
  // is 1000 px, while maintaining the square shape of each vertex
  final int vertexSize;


  // constructor for playing the game with an inputed width and height
  Maze(int width, int height) {
    this.width = new Utils().checkRange(width, 2, 100, "Width must be between 2 and 100");
    this.height = new Utils().checkRange(height, 2, 100, "Height must be between 2 and 100");
    this.rand = new Random();
    this.vertexSize = Math.min(600 / this.height, 1000 / this.width);
    this.makeVertices();
    this.makeAllEdges();
    this.generateEdgesOfMaze();
    this.bfs = false;
    this.dfs = false;
    this.bfsWorklist = new ArrayDeque<Vertex>();
    this.dfsWorklist = new Stack<Vertex>();
    this.seen = new ArrayList<Vertex>();
    this.currentlySearchingMaze = false;
    this.mazeSolved = false;
    this.currentRetracingVertex = this.vertices.get(this.width - 1).get(this.height - 1);
  }


  // constructor for testing the game with an inputed width and height and a seeded random
  Maze(int width, int height, Random rand) {
    this.width = new Utils().checkRange(width, 2, 100, "Width must be between 2 and 100");
    this.height = new Utils().checkRange(height, 2, 100, "Height must be between 2 and 100");
    this.rand = rand;
    this.vertexSize = Math.min(600 / this.height, 1000 / this.width);
    this.makeVertices();
    this.makeAllEdges();
    this.generateEdgesOfMaze();
    this.bfs = false;
    this.dfs = false;
    this.bfsWorklist = new ArrayDeque<Vertex>();
    this.dfsWorklist = new Stack<Vertex>();
    this.seen = new ArrayList<Vertex>();
    this.currentlySearchingMaze = false;
    this.mazeSolved = false;
    this.currentRetracingVertex = this.vertices.get(this.width - 1).get(this.height - 1);
  }



  /////////////////////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////// DRAWING METHODS ///////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////


  // draws the world
  public WorldScene makeScene() {
    WorldScene scene = 
        new WorldScene(this.vertexSize * this.width + 5, this.vertexSize * this.height + 5);
    this.drawVerticesAndBorders(scene);
    return scene;
  }


  // draws all of the vertices onto the scene
  void drawVerticesAndBorders(WorldScene scene) {
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        Vertex v = this.vertices.get(i).get(j);
        v.drawVertexAndBorders(scene, this.vertexSize, this.width, this.height);
      }
    }
  }



  /////////////////////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////// BIGBANG METHODS ///////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////


  // determines what happens every tick
  public void onTick() {

    // searches one vertex on every tick with a bfs if the maze isn't already solved
    if (this.bfs && !this.mazeSolved) {
      this.bfs();
    }

    // searches one vertex on every tick with a dfs if the maze isn't already solved
    if (this.dfs && !this.mazeSolved) {
      this.dfs();
    }

    // retraces one vertex of the solution path on every tick once the maze is solved
    if (this.mazeSolved && this.currentlySearchingMaze) {
      this.retraceSolution();
    }
  }


  // determines what happened when a key is pressed
  public void onKeyEvent(String key) {

    // starts performing a bfs if the maze has not already been solved with a dfs
    if (key.equals("b") && !this.dfs) {
      this.currentlySearchingMaze = true;
      this.bfsWorklist.add(this.vertices.get(0).get(0));
      this.bfs = true;
    }

    // starts performing a dfs if the maze has not already been solved with a bfs
    if (key.equals("d") && !this.bfs) {
      this.currentlySearchingMaze = true;
      this.dfsWorklist.add(this.vertices.get(0).get(0));
      this.dfs = true;
    }

    // clears the maze so that it can be solved again with either of the searching algorithms
    if (key.equals("c") && !this.currentlySearchingMaze) {
      this.resetColors();
      this.bfs = false;
      this.dfs = false;
      this.bfsWorklist.clear();
      this.dfsWorklist.clear();
      this.seen.clear();
      this.mazeSolved = false;
      this.currentRetracingVertex = this.vertices.get(this.width - 1).get(this.height - 1);
    }

    // generates a new maze and resets other parameters
    if (key.equals("r") && !this.currentlySearchingMaze) { 
      this.makeVertices();
      this.makeAllEdges();
      this.generateEdgesOfMaze();
      this.bfs = false;
      this.dfs = false;
      this.bfsWorklist.clear();
      this.dfsWorklist.clear();
      this.seen.clear();
      this.mazeSolved = false;
      this.currentRetracingVertex = this.vertices.get(this.width - 1).get(this.height - 1);
    }
  }



  /////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////// MAZE METHODS ////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////


  // generates the vertices needed for the maze based on the height and width
  void makeVertices() {
    ArrayList<ArrayList<Vertex>> vertices = new ArrayList<ArrayList<Vertex>>();
    for (int i = 0; i < width; i++) {
      ArrayList<Vertex> row = new ArrayList<Vertex>();
      for (int j = 0; j < height; j++) {
        Vertex v = new Vertex(i, j, this.determineColor(i, j));
        row.add(v);
      }
      vertices.add(row);
    }
    this.vertices = vertices;
  }


  // determines the color of every vertex based on its x and y in relation to the width and height
  Color determineColor(int x, int y) {
    if (x == 0 && y == 0) {
      return Color.GREEN;
    }

    else if (x == this.width - 1 && y == this.height - 1) {
      return Color.RED;
    }

    else {
      return Color.LIGHT_GRAY;
    }
  }


  // generates all of the edges between the every adjacent pair of vertices in the maze
  void makeAllEdges() {
    ArrayList<Edge> edges = new ArrayList<Edge>();

    // constructing the vertical edges
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height - 1; j++) {
        Vertex from = this.vertices.get(i).get(j);
        Vertex to = this.vertices.get(i).get(j + 1);
        Edge e = new Edge(from, to, this.rand.nextInt(100000));
        edges.add(e);
      }
    }

    // constructing the horizontal edges
    for (int i = 0; i < width - 1; i++) {
      for (int j = 0; j < height; j++) {
        Vertex from = this.vertices.get(i).get(j);
        Vertex to = this.vertices.get(i + 1).get(j);
        Edge e = new Edge(from, to, this.rand.nextInt(100000));
        edges.add(e);
      }
    }

    this.allEdges = edges;
  }


  // resets all of the colors of the current maze to the default before a searching algorithm 
  // is used if they have been changed while performing a searching algorithm on the maze
  void resetColors() {
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        Vertex v = this.vertices.get(i).get(j);
        v.color = this.determineColor(i, j);
      }
    }
  }



  /////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////// KRUSKAL'S ALGORITHM /////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////////////////////////// 


  // generate edges for maze using Kruskal's Algorithm
  void generateEdgesOfMaze() {
    HashMap<Vertex, Vertex> representatives = new HashMap<Vertex, Vertex>();
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    ArrayList<Edge> worklist = new ArrayList<Edge>(this.allEdges);
    Collections.sort(worklist);

    // initialize every node's representative to itself
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        Vertex v = this.vertices.get(i).get(j);
        representatives.put(v, v);
      }
    }

    // constructing the minimum spanning tree
    int totalVertices = this.height * this.width;
    while (edgesInTree.size() < totalVertices - 1) {
      Edge edge = worklist.remove(0);
      Vertex from = edge.from;
      Vertex to = edge.to;
      if (!this.find(representatives, from).equals(this.find(representatives, to))) {
        edgesInTree.add(edge);
        this.updateBorders(from, to);
        this.addBorderingVertices(from, to);
        this.union(representatives, 
            this.find(representatives, from),
            this.find(representatives, to));
      } 
    }

    this.edgesInMaze = edgesInTree;
  } 


  // finds the representative
  <T> T find(HashMap<T, T> representatives, T t) {
    if (representatives.get(t).equals(t)) {
      return t;
    }
    else {
      return this.find(representatives, representatives.get(t));
    }
  }


  // unions the representatives
  <T> void union(HashMap<T, T> representatives, T fromRepresentative, T toRepresentative) {
    representatives.put(fromRepresentative, toRepresentative);
  }


  // updates the borders of the vertices of an edge to be false where the edge exists so that
  // the maze can be properly drawn without borders wherever the edges of the maze exist
  void updateBorders(Vertex from, Vertex to) {

    // if the edge between the two vertices is horizontal
    if (from.x + 1 == to.x) {
      from.rightBorder = false;
      to.leftBorder = false;
    }

    // if the edge between the two vertices is vertical
    if (from.y + 1 == to.y) {
      from.bottomBorder = false;
      to.topBorder = false;
    }
  }


  // adds from and to vertices to each others list of bordering vertices which is important for 
  // the searching algorithms
  void addBorderingVertices(Vertex from, Vertex to) {
    from.borderingVerticesInMaze.add(to);
    to.borderingVerticesInMaze.add(from);
  }



  /////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////// SEARCHING METHODS ///////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////


  // performing a breadth-first-search
  void bfs() {

    // performing one iteration of the bfs if there is an element in the worklist
    if (this.bfsWorklist.size() > 0) {
      Vertex next = this.bfsWorklist.remove();

      // checking to see if this element is the final vertex we are searching for and 
      // updating fields as necessary
      if (next.x == this.width - 1 && next.y == this.height - 1) {
        next.color = new Color(58, 188, 229);
        seen.add(next);
        this.mazeSolved = true;
      }

      // adding this vertex's bordering vertices into the worklist if they are valid
      // and updating fields as necessary
      else {
        next.color = new Color(58, 188, 229);
        seen.add(next);
        for (Vertex v : next.borderingVerticesInMaze) {
          if (!seen.contains(v)) {
            this.bfsWorklist.add(v);
            v.parent = next;
          }
        }
      }
    }
  }


  // performs a depth-first-search
  void dfs() {

    // performing one iteration of the dfs if there is an element in the worklist
    if (this.dfsWorklist.size() > 0) {
      Vertex next = this.dfsWorklist.pop();

      // checking to see if this element is the final vertex we are searching for and 
      // updating fields as necessary
      if (next.x == this.width - 1 && next.y == this.height - 1) {
        next.color = new Color(58, 188, 229);
        seen.add(next);
        this.mazeSolved = true;
      }

      // adding this vertex's bordering vertices into the worklist if they are valid
      // and updating fields as necessary
      else {
        next.color = new Color(58, 188, 229);
        seen.add(next);
        for (Vertex v : next.borderingVerticesInMaze) {
          if (!seen.contains(v)) {
            this.dfsWorklist.push(v);
            v.parent = next;
          }
        }
      }
    }
  }


  // retraces the solution path from the last vertex to the starting one
  void retraceSolution() {

    // if the currentRetracingVertex is the starting vertex, change the searching field to false
    if (this.currentRetracingVertex.x == 0 && this.currentRetracingVertex.y == 0) {
      this.currentRetracingVertex.color = Color.blue;
      this.currentlySearchingMaze = false;
    }

    // change the color of the vertex and update the currentRetracingVertex to be the next one
    else {
      this.currentRetracingVertex.color = Color.blue;
      this.currentRetracingVertex = this.currentRetracingVertex.parent;

    }
  }
}





// class for utility methods
class Utils {

  // checks that the given value within the allowed range, and throws an exception if not
  int checkRange(int val, int minimum, int maximum, String message) {
    if (val >= minimum && val <= maximum) {
      return val;
    } 
    else {
      throw new IllegalArgumentException(message);
    }
  }
}





// examples and tests
class ExamplesMaze {


  /////////////////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////// EXAMPLES AND TESTS FOR VERTEX ////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////


  Vertex vertex1;
  Vertex vertex2;
  Vertex vertex3;
  Vertex vertex4;


  // initial vertex conditions for testing
  void initVertexConditions() {
    vertex1 = new Vertex(0, 0, Color.GREEN);
    vertex2 = new Vertex(1, 0, Color.LIGHT_GRAY);
    vertex3 = new Vertex(0, 1, Color.LIGHT_GRAY);
    vertex4 = new Vertex(1, 1, Color.RED);
  }


  // test the method drawVertexAndBorders
  void testDrawVertexAndBorders(Tester t) {
    this.initVertexConditions();

    // adjusting the borders for easier testing so we don't have to draw as many
    // this makes it so that there are only borders on the outside of the 2 by 2 maze
    vertex1.bottomBorder = false;
    vertex1.rightBorder = false;
    vertex2.leftBorder = false;
    vertex2.bottomBorder = false;
    vertex3.topBorder = false;
    vertex3.rightBorder = false;
    vertex4.topBorder = false;
    vertex4.leftBorder = false;

    // adding the vertices and borders of this vertex manually to a scene and adding it to another
    // scene with drawVertexAndBorder and checking to see if the two scenes are identical
    WorldScene scene = new WorldScene(1000, 1000);
    WorldScene manuallyDrawnScene = new WorldScene(1000, 1000);

    // adding the first vertex (which should be green) and its borders
    vertex1.drawVertexAndBorders(manuallyDrawnScene, 10, 2, 2);
    scene.placeImageXY(new RectangleImage(10, 10, OutlineMode.SOLID, Color.GREEN), 5, 5);
    scene.placeImageXY(new LineImage(new Posn(10, 0), Color.BLACK), 5, 0);
    scene.placeImageXY(new LineImage(new Posn(0, 10), Color.BLACK), 0, 5);
    t.checkExpect(manuallyDrawnScene, scene);

    // adding the second vertex (which should be gray) and its borders
    vertex2.drawVertexAndBorders(manuallyDrawnScene, 10, 2, 2);
    scene.placeImageXY(new RectangleImage(10, 10, OutlineMode.SOLID, Color.LIGHT_GRAY), 15, 5);
    scene.placeImageXY(new LineImage(new Posn(10, 0), Color.BLACK), 15, 0);
    scene.placeImageXY(new LineImage(new Posn(0, 10), Color.BLACK), 20, 5);
    t.checkExpect(manuallyDrawnScene, scene);

    // adding the third vertex (which should be gray) and its borders
    vertex3.drawVertexAndBorders(manuallyDrawnScene, 10, 2, 2);
    scene.placeImageXY(new RectangleImage(10, 10, OutlineMode.SOLID, Color.LIGHT_GRAY), 5, 15);
    scene.placeImageXY(new LineImage(new Posn(10, 0), Color.BLACK), 5, 20);
    scene.placeImageXY(new LineImage(new Posn(0, 10), Color.BLACK), 0, 15);
    t.checkExpect(manuallyDrawnScene, scene);

    // adding the fourth vertex (which should be red) and its borders
    vertex4.drawVertexAndBorders(manuallyDrawnScene, 10, 2, 2);
    scene.placeImageXY(new RectangleImage(10, 10, OutlineMode.SOLID, Color.RED), 15, 15);
    scene.placeImageXY(new LineImage(new Posn(10, 0), Color.BLACK), 15, 20);
    scene.placeImageXY(new LineImage(new Posn(0, 10), Color.BLACK), 20, 15);
    t.checkExpect(manuallyDrawnScene, scene);
  }



  /////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////// EXAMPLES AND TESTS FOR EDGE /////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////


  // test the method compareTo
  void testCompareTo(Tester t) {
    Edge edge1 = new Edge(new Vertex(0, 0, Color.GREEN), new Vertex(1, 1, Color.LIGHT_GRAY), 100);
    Edge edge2 = new Edge(new Vertex(0, 0, Color.GREEN), new Vertex(1, 1, Color.LIGHT_GRAY), 200);
    Edge edge3 = new Edge(new Vertex(0, 0, Color.GREEN), new Vertex(1, 1, Color.LIGHT_GRAY), 400);
    Edge edge4 = new Edge(new Vertex(0, 0, Color.GREEN), new Vertex(1, 1, Color.LIGHT_GRAY), 400);

    t.checkExpect(edge1.compareTo(edge2), -100);
    t.checkExpect(edge2.compareTo(edge1), 100);
    t.checkExpect(edge1.compareTo(edge3), -300);
    t.checkExpect(edge3.compareTo(edge1), 300);
    t.checkExpect(edge2.compareTo(edge3), -200);
    t.checkExpect(edge3.compareTo(edge2), 200);
    t.checkExpect(edge2.compareTo(edge2), 0);
    t.checkExpect(edge3.compareTo(edge4), 0);
  }


  /////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////// EXAMPLES AND TESTS FOR MAZE /////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////


  Maze playingMaze;
  Maze testingMaze1;
  Maze testingMaze2;


  // initial maze conditions for testing
  void initMazeConditions() {
    playingMaze = new Maze(30, 30);
    testingMaze1 = new Maze(2, 2, new Random(1));
    testingMaze2 = new Maze(25, 10, new Random(1));
  }


  // playing the game with a relatively smaller maze
  void testBigBang(Tester t) {
    this.initMazeConditions();

    this.playingMaze.bigBang(this.playingMaze.vertexSize * this.playingMaze.width + 5, 
        this.playingMaze.vertexSize * this.playingMaze.height + 5, 1.0 / 100.0);
  }


  // testing the constructor exception when a width or height that is too small is passed in
  // for both constructors, no random as argument and with random as argument
  void testMazeConstructor(Tester t) {
    t.checkConstructorException(
        new IllegalArgumentException( "Width must be between 2 and 100"), 
        "Maze", 1, 50);

    t.checkConstructorException(
        new IllegalArgumentException( "Width must be between 2 and 100"), 
        "Maze", 1000, 10);

    t.checkConstructorException(
        new IllegalArgumentException( "Height must be between 2 and 100"), 
        "Maze", 20, 1000, new Random(1));

    t.checkConstructorException(
        new IllegalArgumentException( "Height must be between 2 and 100"), 
        "Maze", 20, -45, new Random());
  }


  // test the method makeScene
  void testMakeScene(Tester t) {
    this.initMazeConditions();

    // adding the vertices and borders of the world manually to a scene and checking to 
    // see if the output of makeScene is the same
    WorldScene scene = new WorldScene(605, 605);
    scene.placeImageXY(new RectangleImage(300, 300, OutlineMode.SOLID, Color.GREEN), 150, 150);
    scene.placeImageXY(new RectangleImage(300, 300, OutlineMode.SOLID, Color.LIGHT_GRAY), 150, 450);
    scene.placeImageXY(new RectangleImage(300, 300, OutlineMode.SOLID, Color.LIGHT_GRAY), 450, 150);
    scene.placeImageXY(new RectangleImage(300, 300, OutlineMode.SOLID, Color.RED), 450, 450);
    scene.placeImageXY(new LineImage(new Posn(300, 0), Color.BLACK), 150, 0);
    scene.placeImageXY(new LineImage(new Posn(300, 0), Color.BLACK), 450, 0);
    scene.placeImageXY(new LineImage(new Posn(300, 0), Color.BLACK), 150, 600);
    scene.placeImageXY(new LineImage(new Posn(300, 0), Color.BLACK), 450, 600);
    scene.placeImageXY(new LineImage(new Posn(0, 300), Color.BLACK), 0, 150);
    scene.placeImageXY(new LineImage(new Posn(0, 300), Color.BLACK), 0, 450);
    scene.placeImageXY(new LineImage(new Posn(0, 300), Color.BLACK), 600, 150);
    scene.placeImageXY(new LineImage(new Posn(0, 300), Color.BLACK), 600, 450);
    scene.placeImageXY(new LineImage(new Posn(0, 300), Color.BLACK), 300, 450);
    t.checkExpect(this.testingMaze1.makeScene(), scene);
  }


  // test the method drawVerticesAndBorders 
  void testDrawVerticesAndBorders(Tester t) {
    this.initMazeConditions();

    // adding the vertices and borders of the world manually to a scene and checking to 
    // see if the output of drawVerticesAndBorders is the same
    WorldScene scene = new WorldScene(605, 605);
    scene.placeImageXY(new RectangleImage(300, 300, OutlineMode.SOLID, Color.GREEN), 150, 150);
    scene.placeImageXY(new RectangleImage(300, 300, OutlineMode.SOLID, Color.LIGHT_GRAY), 150, 450);
    scene.placeImageXY(new RectangleImage(300, 300, OutlineMode.SOLID, Color.LIGHT_GRAY), 450, 150);
    scene.placeImageXY(new RectangleImage(300, 300, OutlineMode.SOLID, Color.RED), 450, 450);
    scene.placeImageXY(new LineImage(new Posn(300, 0), Color.BLACK), 150, 0);
    scene.placeImageXY(new LineImage(new Posn(300, 0), Color.BLACK), 450, 0);
    scene.placeImageXY(new LineImage(new Posn(300, 0), Color.BLACK), 150, 600);
    scene.placeImageXY(new LineImage(new Posn(300, 0), Color.BLACK), 450, 600);
    scene.placeImageXY(new LineImage(new Posn(0, 300), Color.BLACK), 0, 150);
    scene.placeImageXY(new LineImage(new Posn(0, 300), Color.BLACK), 0, 450);
    scene.placeImageXY(new LineImage(new Posn(0, 300), Color.BLACK), 600, 150);
    scene.placeImageXY(new LineImage(new Posn(0, 300), Color.BLACK), 600, 450);
    scene.placeImageXY(new LineImage(new Posn(0, 300), Color.BLACK), 300, 450);
    t.checkExpect(this.testingMaze1.makeScene(), scene);
  }


  // test the method makeVertices 
  void testMakeVertices(Tester t) {
    this.initMazeConditions();

    // testing the number of rows and columns of vertices of the 2 by 2 maze
    t.checkExpect(this.testingMaze1.vertices.size(), 2);
    t.checkExpect(this.testingMaze1.vertices.get(0).size(), 2);
    t.checkExpect(this.testingMaze1.vertices.get(1).size(), 2);

    // testing the number of rows and columns of vertices of the 25 by 10 maze
    t.checkExpect(this.testingMaze2.vertices.size(), 25);
    t.checkExpect(this.testingMaze2.vertices.get(0).size(), 10);
    t.checkExpect(this.testingMaze2.vertices.get(1).size(), 10);
    t.checkExpect(this.testingMaze2.vertices.get(4).size(), 10);
    t.checkExpect(this.testingMaze2.vertices.get(6).size(), 10);
    t.checkExpect(this.testingMaze2.vertices.get(9).size(), 10);
  }


  // test the method makeAllEdges 
  void testMakeAllEdges(Tester t) {
    this.initMazeConditions();

    // the total number of edges in a maze should be height * (width - 1) + width * (height - 1)

    // testing the number of edges in a 2 by 2 maze
    t.checkExpect(this.testingMaze1.allEdges.size(), 4);

    // testing the number of edges in a 25 by 10 maze
    t.checkExpect(this.testingMaze2.allEdges.size(), 465);
  }


  // test the method generateEdgesOfMaze
  void testGenerateEdgesOfMaze(Tester t) {
    this.initMazeConditions();

    // making sure that the number of edges in the maze is equal to the number of vertices - 1
    t.checkExpect(this.testingMaze1.edgesInMaze.size(), 3);
    t.checkExpect(this.testingMaze2.edgesInMaze.size(), 249);

    // making sure that all of the edges are correctly sorted by weight in the 2 by 2 maze
    t.checkExpect(this.testingMaze1.edgesInMaze.get(0).weight, 41847);
    t.checkExpect(this.testingMaze1.edgesInMaze.get(1).weight, 48985);
    t.checkExpect(this.testingMaze1.edgesInMaze.get(2).weight, 64588);
  }


  // test the method find
  void testFind(Tester t) {
    this.initMazeConditions();

    // initializing a HashMap with integers for easier testing
    HashMap<Integer, Integer> integerMap = new HashMap<Integer, Integer>();
    for (int i = 0; i < 10; i++) {
      integerMap.put(i, i);
    }

    t.checkExpect(this.testingMaze1.find(integerMap, 1), 1);
    t.checkExpect(this.testingMaze1.find(integerMap, 3), 3);

    // changing the representatives to test if the method follows through the representatives
    integerMap.put(5, 3);
    integerMap.put(3, 8);
    integerMap.put(8, 9);

    t.checkExpect(this.testingMaze1.find(integerMap, 9), 9);
    t.checkExpect(this.testingMaze1.find(integerMap, 8), 9);
    t.checkExpect(this.testingMaze1.find(integerMap, 3), 9);
    t.checkExpect(this.testingMaze1.find(integerMap, 5), 9);
  }


  // test the method union
  void testUnion(Tester t) {
    this.initMazeConditions();

    // initializing a HashMap with integers for easier testing
    HashMap<Integer, Integer> integerMap = new HashMap<Integer, Integer>();
    for (int i = 0; i < 10; i++) {
      integerMap.put(i, i);
    }

    this.testingMaze1.union(integerMap, 1, 2);
    t.checkExpect(integerMap.get(1), 2);

    this.testingMaze1.union(integerMap, 8, 4);
    t.checkExpect(integerMap.get(8), 4);

    this.testingMaze1.union(integerMap, 0, 5);
    t.checkExpect(integerMap.get(0), 5);
  }


  // test the method updateBorders
  void testUpdateBorder(Tester t) {
    this.initMazeConditions();
    this.initVertexConditions();

    // checking to make sure that the borders of two horizontal vertices are updated
    this.vertex1.rightBorder = true;
    this.vertex2.leftBorder = true;
    this.testingMaze1.updateBorders(this.vertex1, this.vertex2);
    t.checkExpect(this.vertex1.rightBorder, false);
    t.checkExpect(this.vertex2.leftBorder, false);

    // checking to make sure that the borders of two vertical vertices are updated
    this.initVertexConditions();
    this.vertex1.bottomBorder = true;
    this.vertex3.topBorder = true;
    this.testingMaze1.updateBorders(this.vertex1, this.vertex3);
    t.checkExpect(this.vertex1.bottomBorder, false);
    t.checkExpect(this.vertex3.topBorder, false);
  }


  // test the method determineColor 
  void testDetermineColor(Tester t) {
    this.initMazeConditions();

    // checking that the method returns the correct colors for the 2 by 2 maze
    t.checkExpect(this.testingMaze1.determineColor(0, 0), Color.GREEN);
    t.checkExpect(this.testingMaze1.determineColor(1, 0), Color.LIGHT_GRAY);
    t.checkExpect(this.testingMaze1.determineColor(0, 1), Color.LIGHT_GRAY);
    t.checkExpect(this.testingMaze1.determineColor(1, 1), Color.RED);
  }


  // test the method resetColors 
  void testResetColors(Tester t) {
    this.initMazeConditions();

    Vertex starting = this.testingMaze1.vertices.get(0).get(0);
    Vertex middleVertex1 = this.testingMaze1.vertices.get(1).get(0);
    Vertex middleVertex2 = this.testingMaze1.vertices.get(0).get(1);
    Vertex ending = this.testingMaze1.vertices.get(1).get(1);

    // changing the colors to black so that we can reset them to what they are for a starting maze
    starting.color = Color.BLACK;
    middleVertex1.color = Color.BLACK;
    middleVertex2.color = Color.BLACK;
    ending.color = Color.BLACK;


    // calling the method resetColors and checking to see if the colors are what they should be now
    this.testingMaze1.resetColors();
    t.checkExpect(starting.color, Color.GREEN);
    t.checkExpect(middleVertex1.color, Color.LIGHT_GRAY);
    t.checkExpect(middleVertex2.color, Color.LIGHT_GRAY);
    t.checkExpect(ending.color, Color.RED);
  }


  // test the method addBorderingVertices
  void testAddBorderingVertices(Tester t) {
    this.initMazeConditions();
    this.initVertexConditions();

    t.checkExpect(this.vertex1.borderingVerticesInMaze.size(), 0);
    t.checkExpect(this.vertex2.borderingVerticesInMaze.size(), 0);
    t.checkExpect(this.vertex3.borderingVerticesInMaze.size(), 0);
    t.checkExpect(this.vertex4.borderingVerticesInMaze.size(), 0);

    this.testingMaze1.addBorderingVertices(vertex1, vertex2);
    t.checkExpect(this.vertex1.borderingVerticesInMaze.size(), 1);
    t.checkExpect(this.vertex2.borderingVerticesInMaze.size(), 1);

    this.testingMaze1.addBorderingVertices(vertex2, vertex3);
    t.checkExpect(this.vertex2.borderingVerticesInMaze.size(), 2);
    t.checkExpect(this.vertex3.borderingVerticesInMaze.size(), 1);

    this.testingMaze1.addBorderingVertices(vertex2, vertex4);
    t.checkExpect(this.vertex2.borderingVerticesInMaze.size(), 3);
    t.checkExpect(this.vertex4.borderingVerticesInMaze.size(), 1);
  }


  // test the method onTick and onKeyEvent together
  void testOnTickAndOnKeyEvent(Tester t) {
    this.initMazeConditions();

    // testing a full bfs with 'b' on the 2 by 2 maze
    t.checkExpect(this.testingMaze1.currentlySearchingMaze , false);
    t.checkExpect(this.testingMaze1.bfs, false);
    t.checkExpect(this.testingMaze1.bfsWorklist.size(), 0);
    t.checkExpect(this.testingMaze1.seen.size(), 0);

    this.testingMaze1.onKeyEvent("b");

    t.checkExpect(this.testingMaze1.currentlySearchingMaze , true);
    t.checkExpect(this.testingMaze1.bfs, true);
    t.checkExpect(this.testingMaze1.bfsWorklist.size(), 1);
    t.checkExpect(this.testingMaze1.seen.size(), 0);
    this.testingMaze1.onTick();
    t.checkExpect(this.testingMaze1.bfsWorklist.size(), 2);
    t.checkExpect(this.testingMaze1.vertices.get(0).get(0).color, new Color(58, 188, 229));
    t.checkExpect(this.testingMaze1.seen.size(), 1);
    this.testingMaze1.onTick();
    t.checkExpect(this.testingMaze1.bfsWorklist.size(), 2);
    t.checkExpect(this.testingMaze1.vertices.get(1).get(0).color, new Color(58, 188, 229));
    t.checkExpect(this.testingMaze1.seen.size(), 2);
    this.testingMaze1.onTick();
    t.checkExpect(this.testingMaze1.bfsWorklist.size(), 1);
    t.checkExpect(this.testingMaze1.vertices.get(0).get(1).color, new Color(58, 188, 229));
    t.checkExpect(this.testingMaze1.seen.size(), 3);
    this.testingMaze1.onTick();
    t.checkExpect(this.testingMaze1.bfsWorklist.size(), 0);
    t.checkExpect(this.testingMaze1.seen.size(), 4);
    t.checkExpect(this.testingMaze1.mazeSolved, true);
    // some more onTick calls to allow the retracing to take place, this will be tested later
    this.testingMaze1.onTick();
    this.testingMaze1.onTick();
    this.testingMaze1.onTick();
    this.testingMaze1.onTick();
    t.checkExpect(this.testingMaze1.currentlySearchingMaze, false);


    // testing the clearing functionality with 'c' on the just solved maze
    this.testingMaze1.onKeyEvent("c");
    t.checkExpect(this.testingMaze1.bfs, false);
    t.checkExpect(this.testingMaze1.seen.size(), 0);
    t.checkExpect(this.testingMaze1.mazeSolved, false);
    t.checkExpect(this.testingMaze1.vertices.get(0).get(0).color, Color.GREEN);


    // testing a full dfs with 'd' on the 2 by 2 maze
    t.checkExpect(this.testingMaze1.dfs, false);
    t.checkExpect(this.testingMaze1.dfsWorklist.size(), 0);

    this.testingMaze1.onKeyEvent("d");

    t.checkExpect(this.testingMaze1.currentlySearchingMaze , true);
    t.checkExpect(this.testingMaze1.dfs, true);
    t.checkExpect(this.testingMaze1.dfsWorklist.size(), 1);
    t.checkExpect(this.testingMaze1.seen.size(), 0);
    this.testingMaze1.onTick();
    t.checkExpect(this.testingMaze1.dfsWorklist.size(), 2);
    t.checkExpect(this.testingMaze1.vertices.get(0).get(0).color, new Color(58, 188, 229));
    t.checkExpect(this.testingMaze1.seen.size(), 1);
    this.testingMaze1.onTick();
    t.checkExpect(this.testingMaze1.dfsWorklist.size(), 1);
    t.checkExpect(this.testingMaze1.vertices.get(0).get(1).color, new Color(58, 188, 229));
    t.checkExpect(this.testingMaze1.seen.size(), 2);
    this.testingMaze1.onTick();
    t.checkExpect(this.testingMaze1.dfsWorklist.size(), 1);
    t.checkExpect(this.testingMaze1.vertices.get(1).get(0).color, new Color(58, 188, 229));
    t.checkExpect(this.testingMaze1.seen.size(), 3);
    this.testingMaze1.onTick();
    t.checkExpect(this.testingMaze1.dfsWorklist.size(), 0);
    t.checkExpect(this.testingMaze1.seen.size(), 4);
    t.checkExpect(this.testingMaze1.mazeSolved, true);


    // testing the retracing of a maze after the dfs has been done
    t.checkExpect(this.testingMaze1.currentlySearchingMaze, true);
    t.checkExpect(this.testingMaze1.currentRetracingVertex, 
        this.testingMaze1.vertices.get(1).get(0));
    t.checkExpect(this.testingMaze1.vertices.get(1).get(1).color, Color.BLUE);
    this.testingMaze1.onTick();
    t.checkExpect(this.testingMaze1.currentRetracingVertex, 
        this.testingMaze1.vertices.get(0).get(0));
    t.checkExpect(this.testingMaze1.vertices.get(1).get(0).color, Color.BLUE);
    this.testingMaze1.onTick();
    t.checkExpect(this.testingMaze1.vertices.get(0).get(0).color, Color.BLUE);
    t.checkExpect(this.testingMaze1.currentlySearchingMaze, false);


    // testing to make sure that there are new vertices when a new maze is generated with 'r'
    // the list of list of vertices should be different after 'r' is pressed so checkFail is used
    ArrayList<ArrayList<Vertex>> oldVertices = this.testingMaze1.vertices;
    this.testingMaze1.onKeyEvent("r");
    ArrayList<ArrayList<Vertex>> newVertices = this.testingMaze1.vertices;
    t.checkFail(oldVertices, newVertices);
  }


  // test the method bfs
  void testBfs(Tester t) {
    this.initMazeConditions();

    // testing the bfs on the 2 by 2 maze

    // making sure that nothing happens when the worklist is empty and bfs is called
    t.checkExpect(this.testingMaze1.bfsWorklist.size(), 0);
    this.testingMaze1.bfs();
    t.checkExpect(this.testingMaze1.bfsWorklist.size(), 0);

    // adding the topLeft vertex to the worklist and iterating through the bfs
    Vertex starting = this.testingMaze1.vertices.get(0).get(0);
    this.testingMaze1.bfsWorklist.add(starting);
    t.checkExpect(this.testingMaze1.bfsWorklist.size(), 1);
    t.checkExpect(this.testingMaze1.seen.size(), 0);
    this.testingMaze1.bfs();
    t.checkExpect(this.testingMaze1.vertices.get(0).get(0).color, new Color(58, 188, 229));
    t.checkExpect(this.testingMaze1.seen.size(), 1);
    this.testingMaze1.bfs();
    t.checkExpect(this.testingMaze1.bfsWorklist.size(), 2);
    t.checkExpect(this.testingMaze1.vertices.get(1).get(0).color, new Color(58, 188, 229));
    t.checkExpect(this.testingMaze1.seen.size(), 2);
    this.testingMaze1.bfs();
    t.checkExpect(this.testingMaze1.bfsWorklist.size(), 1);
    t.checkExpect(this.testingMaze1.vertices.get(0).get(1).color, new Color(58, 188, 229));
    t.checkExpect(this.testingMaze1.seen.size(), 3);
    this.testingMaze1.bfs();
    t.checkExpect(this.testingMaze1.bfsWorklist.size(), 0);
    t.checkExpect(this.testingMaze1.seen.size(), 4);
    t.checkExpect(this.testingMaze1.mazeSolved, true);
  }


  // test the method dfs
  void testDfs(Tester t) {
    this.initMazeConditions();

    // testing the dfs on a 2 by 2 maze

    // making sure that nothing happens when the worklist is empty and dfs is called
    t.checkExpect(this.testingMaze1.dfsWorklist.size(), 0);
    this.testingMaze1.dfs();
    t.checkExpect(this.testingMaze1.dfsWorklist.size(), 0);

    // adding the topLeft vertex to the worklist and iterating through the dfs
    Vertex starting = this.testingMaze1.vertices.get(0).get(0);
    this.testingMaze1.dfsWorklist.add(starting);
    t.checkExpect(this.testingMaze1.dfsWorklist.size(), 1);
    t.checkExpect(this.testingMaze1.seen.size(), 0);
    this.testingMaze1.dfs();
    t.checkExpect(this.testingMaze1.dfsWorklist.size(), 2);
    t.checkExpect(this.testingMaze1.vertices.get(0).get(0).color, new Color(58, 188, 229));
    t.checkExpect(this.testingMaze1.seen.size(), 1);
    this.testingMaze1.dfs();
    t.checkExpect(this.testingMaze1.dfsWorklist.size(), 1);
    t.checkExpect(this.testingMaze1.vertices.get(0).get(1).color, new Color(58, 188, 229));
    t.checkExpect(this.testingMaze1.seen.size(), 2);
    this.testingMaze1.dfs();
    t.checkExpect(this.testingMaze1.dfsWorklist.size(), 1);
    t.checkExpect(this.testingMaze1.vertices.get(1).get(0).color, new Color(58, 188, 229));
    t.checkExpect(this.testingMaze1.seen.size(), 3);
    this.testingMaze1.dfs();
    t.checkExpect(this.testingMaze1.dfsWorklist.size(), 0);
    t.checkExpect(this.testingMaze1.seen.size(), 4);
    t.checkExpect(this.testingMaze1.mazeSolved, true);
  }


  // test the method retraceSolution
  void testRetraceSolution(Tester t) {
    this.initMazeConditions();

    // solving through the maze with a dfs so that we can retrace the solution
    this.testingMaze1.onKeyEvent("d");
    this.testingMaze1.onTick();
    this.testingMaze1.onTick();
    this.testingMaze1.onTick();
    this.testingMaze1.onTick();
    t.checkExpect(this.testingMaze1.mazeSolved, true);


    // testing the retracing of a maze after the dfs has been done
    t.checkExpect(this.testingMaze1.currentlySearchingMaze, true);
    t.checkExpect(this.testingMaze1.currentRetracingVertex, 
        this.testingMaze1.vertices.get(1).get(0));
    t.checkExpect(this.testingMaze1.vertices.get(1).get(1).color, Color.BLUE);
    this.testingMaze1.retraceSolution();
    t.checkExpect(this.testingMaze1.currentRetracingVertex, 
        this.testingMaze1.vertices.get(0).get(0));
    t.checkExpect(this.testingMaze1.vertices.get(1).get(0).color, Color.BLUE);
    this.testingMaze1.retraceSolution();
    t.checkExpect(this.testingMaze1.vertices.get(0).get(0).color, Color.BLUE);
    t.checkExpect(this.testingMaze1.currentlySearchingMaze, false);
  }


  // test the method checkRange
  void testCheckRange(Tester t) {
    Utils u = new Utils();

    t.checkExpect(u.checkRange(10, 0, 20, "Value is too big or small!"), 10);
    t.checkException(new IllegalArgumentException("Value is too big or small!"), 
        u, "checkRange", 10, 50, 100, "Value is too big or small!");
    t.checkException(new IllegalArgumentException("Value is too big or small!"), 
        u, "checkRange", 1000, 50, 100, "Value is too big or small!");
  }
}






