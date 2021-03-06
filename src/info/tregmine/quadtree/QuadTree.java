package info.tregmine.quadtree;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;

/**
 * A datastructure for efficiently assigning a rectangle
 * in space to a specific value, that allows quick lookup
 * for any point. Any lookup can be accomplished in O(log(n))
 * where n is the size of the space covered by the tree.
 *
 * This tree heavily trades insert performance for lookup
 * performance. Lookups can be made in a few microseconds,
 * while inserts take on the order of 50 microseconds.
 *
 * Delete is currently unsupported, although it wouldn't
 * be very hard to add.
 */
public class QuadTree<V>
{
    public static class Node<V>
    {
        // The rectangle covered by this node.
        Rectangle nodeRect;
        
        // the sub rectangle that is associated
        // with the value. might span multiple nodes.
        Rectangle valueRect;
        // The corner of an rectangle that lies within
        // this node. Used by insert when subdividing
        // space further.
        Point valuePoint;
        // All values present in this node. Several 
        // rectangles can overlap a single node without
        // ever overlapping each other, which forces us
        // to keep track of several possible values.
        Map<Rectangle, V> values;
        
        // top left, top right
        Node<V> tl, tr;
        // bottom left, bottom right
        Node<V> bl, br;
        
        // Indicates whether or not this node has been further
        // divided or not. If this value is true, this node will
        // never contain a value itself, so it's used as a criteria
        // for continuing searching.
        boolean split;
        
        // Flag used to indicate to the visualizer that this Node 
        // contains an assigned value.
        boolean color = false;
        
        public Node(Rectangle rect)
        {
            this.nodeRect = rect;
            
            this.valuePoint = null;
            this.valueRect = null;
            this.values = new HashMap<Rectangle, V>();
            
            this.tl = null;
            this.tr = null;
            this.bl = null;
            this.br = null;
            
            this.split = false;
        }
        
        /**
         * Checks whether or not a point is contained in this node.
         */
        public boolean contains(Point p)
        {
            return nodeRect.contains(p);
        }
        
        /**
         * Checks whether or not a rectangle intersects this node.
         */
        public boolean intersects(Rectangle rect)
        {
            return nodeRect.intersects(rect);
        }
        
        /**
         * Recursively find the matching rectangle of a point.
         * No longer used in favor of a faster iterative approach.
         */
        public V find(Point p)
        {
            if (!split) {
                for (Map.Entry<Rectangle, V> entry : values.entrySet()) {
                    Rectangle rect = entry.getKey();
                    if (rect.contains(p)) {
                        return entry.getValue();
                    }
                }
                
                return null;
            }
        
            if (tl.contains(p)) {
                return tl.find(p);
            } else if (tr.contains(p)) {
                return tr.find(p);
            } else if (bl.contains(p)) {
                return bl.find(p);
            } else if (br.contains(p)) {
                return br.find(p);
            } else {
                return null;
            }
        }
        
        /**
         * Insert a new rectangle and corresponding value. Recursively
         * splits space as needed.
         */
        public void insert(Point p, Rectangle rect, V v, int depth)
        throws IntersectionException
        {
            // this node doesn't matche the point
            if (!nodeRect.contains(p)) {
                //System.out.println(nodeRect + " does not match " + p);
                return;
            }
            
            // no value has been assigned to this
            // node, so we don't have to split it
            if (this.valuePoint == null && !split) {
                values.put(rect, v);
                valueRect = rect;
                valuePoint = p;
                return;
            }
            
            if (p.equals(valuePoint)) {
                throw new IntersectionException("Specified point " + p + 
                    " is already in use.");
            }

            // initialize subnodes
            if (!split) {
                //System.out.println("splitting");
            
                long totalWidth = nodeRect.getWidth();
                long totalHeight = nodeRect.getHeight();
                int w1 = (int)(totalWidth / 2);
                int h1 = (int)(totalHeight / 2);
                int w2 = (int)(totalWidth - w1);
                int h2 = (int)(totalHeight - h1);
                
                //System.out.println("w: " + w);
                //System.out.println("h: " + h);
                
                tl = new Node<V>(new Rectangle(nodeRect.x1, nodeRect.y1, 
                    nodeRect.x1 + w1, nodeRect.y1 - h1));
                //System.out.println(tl.nodeRect);
                tr = new Node<V>(new Rectangle(nodeRect.x1 + w1, nodeRect.y1, 
                    nodeRect.x1 + w1 + w2, nodeRect.y1 - h1));
                //System.out.println(tr.nodeRect);
                bl = new Node<V>(new Rectangle(nodeRect.x1, nodeRect.y1 - h1,
                    nodeRect.x1 + w1, nodeRect.y1 - h1 - h2));
                //System.out.println(bl.nodeRect);
                br = new Node<V>(new Rectangle(nodeRect.x1 + w1, nodeRect.y1 - h1,
                    nodeRect.x1 + w1 + w2, nodeRect.y1 - h1 - h2));
                //System.out.println(br.nodeRect);
                    
                this.split = true;

                // move current value to subsection
                V value = values.get(valueRect);
                if (tl.contains(valuePoint)) {
                    tl.insert(this.valuePoint, this.valueRect, value, depth+1);
                } else if (tr.contains(valuePoint)) {
                    tr.insert(this.valuePoint, this.valueRect, value, depth+1);
                } else if (bl.contains(valuePoint)) {
                    bl.insert(this.valuePoint, this.valueRect, value, depth+1);
                } else if (br.contains(valuePoint)) {
                    br.insert(this.valuePoint, this.valueRect, value, depth+1);
                } else {
                    throw new IllegalStateException(this.valuePoint + " is not " + 
                        "in " + tl.nodeRect + ";\n" + 
                        "or " + tr.nodeRect + ";\n" + 
                        "or " + bl.nodeRect + ";\n" + 
                        "or " + br.nodeRect);
                }
                
                // transfer all rectangles that intersect this node and
                // their corresponding values to the new subnodes
                for (Map.Entry<Rectangle, V> entry : values.entrySet()) {
                    Rectangle candidate = entry.getKey();
                    if (tl.intersects(candidate)) {
                        tl.values.put(candidate, entry.getValue());
                        tl.color = true;
                    } 
                    if (tr.intersects(candidate)) {
                        tr.values.put(candidate, entry.getValue());
                        tr.color = true;
                    } 
                    if (bl.intersects(candidate)) {
                        bl.values.put(candidate, entry.getValue());
                        bl.color = true;
                    } 
                    if (br.intersects(candidate)) {
                        br.values.put(candidate, entry.getValue());
                        br.color = true;
                    }
                }
            }
            
            // insert new value
            if (tl.contains(p)) {
                tl.insert(p, rect, v, depth+1);
            } else if (tr.contains(p)) {
                tr.insert(p, rect, v, depth+1);
            } else if (bl.contains(p)) {
                bl.insert(p, rect, v, depth+1);
            } else if (br.contains(p)) {
                br.insert(p, rect, v, depth+1);
            } else {
                throw new IllegalStateException(p + " is not in " + tl.nodeRect + ";\n" + 
                    "or " + tr.nodeRect + ";\n" + 
                    "or " + bl.nodeRect + ";\n" + 
                    "or " + br.nodeRect);
            }
            
            values.clear();
            valueRect = null;
            valuePoint = null;
        }
        
        /**
         * Find out if any rectangle currently in the tree intersects
         * the passed rectangle. Used by insert() to prevent overlapping
         * rectangles from being inserted.
         */
        public boolean findIntersections(Rectangle rect)
        {
            if (!split) {
                for (Map.Entry<Rectangle, V> entry : values.entrySet()) {
                    Rectangle candidate = entry.getKey();
                    if (candidate.intersects(rect)) {
                        return true;
                    }
                }
                return false;
            }
        
            if (tl.intersects(rect) && tl.findIntersections(rect)) {
                return true;
            } else if (tr.intersects(rect) && tr.findIntersections(rect)) {
                return true;
            } else if (bl.intersects(rect) && bl.findIntersections(rect)) {
                return true;
            } else if (br.intersects(rect) && br.findIntersections(rect)) {
                return true;
            }
            
            return false;
        }
        
        /**
         * Find all nodes/nodes within a specified rectangle, and assign
         * a specified value to them. This is used to speed up lookup for
         * nodes that completely lies inside a rectangle, and thus won't
         * have their value set by Node.insert().
         */
        public void assign(Rectangle rect, V value)
        {
            if (!split) {
                values.put(rect, value);
                color = true;
                return;
            }
        
            if (tl.intersects(rect)) {
                tl.assign(rect, value);
            } 
            if (tr.intersects(rect)) {
                tr.assign(rect, value);
            } 
            if (bl.intersects(rect)) {
                bl.assign(rect, value);
            } 
            if (br.intersects(rect)) {
                br.assign(rect, value);
            }
        }
    }

    Node<V> root;
    
    /**
     * Construct a tree covering the entirety of integer 2d space.
     */
    public QuadTree()
    {
        this(0);
    }
    
    /**
     * Construct a tree covering a specified sub space defined by
     * -size < x < size, -size < y < size.
     */
    public QuadTree(int size)
    {
        if (size == 0) {
            this.root = new Node<V>(new Rectangle(Integer.MIN_VALUE, Integer.MAX_VALUE, 
                Integer.MAX_VALUE, Integer.MIN_VALUE));
        } else {
            this.root = new Node<V>(new Rectangle(-size, size, size, -size));
        }
    }
    
    /**
     * Insert a rectangle and a value into space.
     */
    public void insert(Rectangle rect, V value)
    throws IntersectionException
    {
        // look for an intersecting rectangle, and complain
        // loudly if one is found.
        if (root.findIntersections(rect)) {
            throw new IntersectionException();
        }
    
        // insert all corners of the rectangle into the grid.
        for (Point p : rect.getPoints()) {
            root.insert(p, rect, value, 0);
        }
        
        // assign the current value to all nodes that lie within
        // or intersect the current rectangle.
        root.assign(rect, value);
    }
    
    /**
     * Find the value corresponding to a point in space.
     *
     * @return The value if found, otherwise null.
     */
    public V find(Point p)
    {
        // Find the target node by iteration rather
        // than recursion, saving a lot of time by
        // eliminating method calls and stack allocations.
        Node<V> current = root;
        while (current.split) {
            if (!current.contains(p)) {
                return null;
            }
        
            // This approach reduces the worst case number of 
            // comparisons needed from 16 to 2, which has to
            // be considered as a rather nice optimization. :)
            Rectangle rect = current.nodeRect;
            if (p.x > rect.centerX) {
                if (p.y > rect.centerY) {
                    current = current.tr;
                } else {
                    current = current.br;
                }
            } else {
                if (p.y > rect.centerY) {
                    current = current.tl;
                } else {
                    current = current.bl;
                }            
            }
        
            /*if (current.tl.contains(p)) {
                current = current.tl;
            } else if (current.tr.contains(p)) {
                current = current.tr;
            } else if (current.bl.contains(p)) {
                current = current.bl;
            } else if (current.br.contains(p)) {
                current = current.br;
            } else {
                return null;
            }*/
        }
        
        // Figure out which of the rectangles that intersect
        // this node contains the requested value.
        for (Map.Entry<Rectangle, V> entry : current.values.entrySet()) {
            Rectangle rect = entry.getKey();
            if (rect.contains(p)) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Linear search used for speed comparison.
     */
    private static Integer linearSearch(Map<Rectangle, Integer> data, Point p)
    {
        for (Map.Entry<Rectangle, Integer> entry : data.entrySet()) {
            Rectangle rect = entry.getKey();
            if (rect.contains(p)) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Perform a stress test of the tree by randomly generating rectangles,
     * inserting them into the tree and then looking up random points known
     * to lie within the rectangles.
     */
    public static void main(String[] args)
    throws Exception
    {
        int size = 0;
        int points = 10000;
        QuadTree<Integer> tree = new QuadTree<Integer>(size);
        
        if (size == 0) {
            size = Integer.MAX_VALUE/2;
        }
        
        // Generate random rectangles and values
        Map<Rectangle, Integer> values = new HashMap<Rectangle, Integer>();
        Random rand = new Random();
        float avgInsertTime = 0.0f;
        for (int i = 0; i < points; i++) {
            int x1 = rand.nextInt(2*size) - size;
            int y1 = rand.nextInt(2*size) - size;
            int w = Math.abs(rand.nextInt(size/1000));
            int h = Math.abs(rand.nextInt(size/1000));
            
            Rectangle rect = new Rectangle(x1, y1, x1 + w, y1 - h);
            if (rect.getLeft() < -size || rect.getRight() > size ||
                rect.getBottom() < -size || rect.getTop() > size) {
            
                i--;
                continue;
            }
            
            Integer value = rand.nextInt();
            
            try {
                long t = System.nanoTime();
                tree.insert(rect, value);
                t = System.nanoTime() - t;
                avgInsertTime += t;
                values.put(rect, value);
            } catch (IntersectionException e) {
                //System.out.println("Discarding intersecting rectangle.");
                i--;
            }
        }
        
        avgInsertTime /= points;
        System.out.println(String.format("Average insert time: %.2f ns", avgInsertTime));
        
        if (points != values.size()) {
            System.out.println("Some inserts failed.");
        }
        
        // Perform quad tree lookups and linear searches for points
        // that lie within the previously generated rectangles.
        float avgFindTime = 0.0f, avgLinearSearchTime = 0.0f;
        for (Map.Entry<Rectangle, Integer> entry : values.entrySet()) {
            Rectangle rect = entry.getKey();
            int value = entry.getValue();
            
            int w = Math.abs(rect.getRight() - rect.getLeft());
            int h = Math.abs(rect.getTop() - rect.getBottom());
            
            int x = rect.getLeft() + rand.nextInt(w);
            int y = rect.getBottom() + rand.nextInt(h);
            
            Point p = new Point(x, y);
            
            long t = System.nanoTime();
            Integer cmp = tree.find(p);
            t = System.nanoTime() - t;
            
            if (cmp == null || cmp != value) {
                System.out.println("Missmatch for " + p + " that should match " + rect);
            } else {
                //System.out.println("Matched in " + t + " ns.");
                avgFindTime += t;
                
                t = System.nanoTime();
                value = linearSearch(values, p);
                if (cmp != value) {
                    System.out.println("Linear search result does not match find.");
                }
                t = System.nanoTime() - t;
                
                avgLinearSearchTime += t;
            }
        }
        
        avgFindTime /= values.size();
        System.out.println(String.format("Average find time: %.2f ns", avgFindTime));
        
        avgLinearSearchTime /= values.size();
        System.out.println(String.format("Average linear search time: %.2f ns", avgLinearSearchTime));
        
        // If space is small enough, generate a visualization.
        if (size < 1000) {
            System.out.println("Generating visualization.");
            QuadTreeVisualizer.drawQuadTree(tree, "tree.png");
        }
        
        // Memory benchmark
        System.out.println("total memory: " + Runtime.getRuntime().totalMemory()/1024/1024 + " mb");
        System.out.println("free memory: " + Runtime.getRuntime().freeMemory()/1024/1024 + " mb");
    }
}
