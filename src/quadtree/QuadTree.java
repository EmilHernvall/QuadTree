package quadtree;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;

public class QuadTree<V>
{
    public static class IntersectionException extends Exception
    {
        public IntersectionException(String message)
        {
            super(message);
        }
        
        public IntersectionException()
        {
            super();
        }
    }

    public static class Point
    {
        public int x, y;
        
        public Point(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public String toString()
        {
            return String.format("%d,%d", x, y);
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof Point)) {
                return false;
            }
            
            Point b = (Point)obj;
            return b != null && b.x == x && b.y == y;
        }
        
        @Override
        public int hashCode()
        {
            int code = 17;
            code = 31 * code + x;
            code = 31 * code + y;
            
            return code;
        }
    }

    public static class Rectangle
    {
        // top left, top right
        Point tl, tr;
        // bottom left, bottom right
        Point bl, br;
    
        public Rectangle(int x1, int y1, int x2, int y2)
        {
            int tmp;
            
            // swap values so that we can guarantee the
            // relative position of the points
            if (x1 > x2) {
                tmp = x2;
                x2 = x1;
                x1 = tmp;
            }
            
            if (y1 < y2) {
                tmp = y2;
                y2 = y1;
                y1 = tmp;
            }
        
            this.tl = new Point(x1, y1);
            this.tr = new Point(x2, y1);
            this.bl = new Point(x1, y2);
            this.br = new Point(x2, y2);
        }
        
        public int getLeft() { return tl.x; }
        public int getRight() { return tr.x; }
        public int getTop() { return tl.y; }
        public int getBottom() { return bl.y; }
        
        public boolean contains(Point p)
        {
            return (p.x >= tl.x && p.x <= br.x) &&
                (p.y <= tl.y && p.y >= br.y);
        }
        
        public boolean intersects(Rectangle rect)
        {
            return !(rect.getLeft() > getRight() ||
                rect.getRight() < getLeft() ||
                rect.getTop() < getBottom() ||
                rect.getBottom() > getTop());
        }
        
        public Point[] getPoints()
        {
            return new Point[] { tl, tr, bl, br };
        }
        
        @Override
        public String toString()
        {
            return String.format("(%s),(%s),(%s),(%s)", tl, tr, bl, br);
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof Rectangle)) {
                return false;
            }
            
            Rectangle b = (Rectangle)obj;
            return tl.equals(b.tl) && br.equals(b.br);
        }
        
        @Override
        public int hashCode()
        {
            int code = 17;
            code = 31 * code + tl.hashCode();
            code = 31 * code + br.hashCode();
            
            return code;
        }
    }

    public static class Node<V>
    {
        Rectangle sectorRect;
        
        // the sub rectangle that is associated
        // with the value. might span multiple sectors.
        Point valuePoint;
        Rectangle valueRect;
        Map<Rectangle, V> values;
        
        // top left, top right
        Node<V> tl, tr;
        // bottom left, bottom right
        Node<V> bl, br;
        
        boolean split;
        boolean color = false;
        
        public Node(Rectangle rect)
        {
            this.sectorRect = rect;
            
            this.valuePoint = null;
            this.valueRect = null;
            this.values = new HashMap<Rectangle, V>();
            
            this.tl = null;
            this.tr = null;
            this.bl = null;
            this.br = null;
            
            this.split = false;
        }
        
        public boolean contains(Point p)
        {
            return sectorRect.contains(p);
        }
        
        public boolean intersects(Rectangle rect)
        {
            return sectorRect.intersects(rect);
        }
        
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
        
        public void insert(Point p, Rectangle rect, V v, int depth)
        throws IntersectionException
        {
            //System.out.println("inserting " + p + " in " + sectorRect + " at depth " + depth);
        
            // this node doesn't matche the point
            if (!sectorRect.contains(p)) {
                //System.out.println(sectorRect + " does not match " + p);
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
                throw new IntersectionException("Specified point " + p + " is already in use.");
            }

            // initialize subsectors
            if (!split) {
                //System.out.println("splitting");
            
                long totalWidth = (long)sectorRect.tr.x - (long)sectorRect.tl.x;
                long totalHeight = (long)sectorRect.tl.y - (long)sectorRect.bl.y;
                int w1 = (int)(totalWidth / 2);
                int h1 = (int)(totalHeight / 2);
                int w2 = (int)(totalWidth - w1);
                int h2 = (int)(totalHeight - h1);
                
                //System.out.println("w: " + w);
                //System.out.println("h: " + h);
                
                tl = new Node<V>(new Rectangle(sectorRect.tl.x, sectorRect.tl.y, 
                    sectorRect.tl.x + w1, sectorRect.tl.y - h1));
                //System.out.println(tl.sectorRect);
                tr = new Node<V>(new Rectangle(sectorRect.tl.x + w1, sectorRect.tl.y, 
                    sectorRect.tl.x + w1 + w2, sectorRect.tl.y - h1));
                //System.out.println(tr.sectorRect);
                bl = new Node<V>(new Rectangle(sectorRect.tl.x, sectorRect.tl.y - h1,
                    sectorRect.tl.x + w1, sectorRect.tl.y - h1 - h2));
                //System.out.println(bl.sectorRect);
                br = new Node<V>(new Rectangle(sectorRect.tl.x + w1, sectorRect.tl.y - h1,
                    sectorRect.tl.x + w1 + w2, sectorRect.tl.y - h1 - h2));
                //System.out.println(br.sectorRect);
                    
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
                    throw new IllegalStateException(this.valuePoint + " is not in " + tl.sectorRect + ";\n" + 
                        "or " + tr.sectorRect + ";\n" + 
                        "or " + bl.sectorRect + ";\n" + 
                        "or " + br.sectorRect);
                }
                
                // transfer all rectangles that intersect this sector and
                // their corresponding values to the new subsectors
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
                throw new IllegalStateException(p + " is not in " + tl.sectorRect + ";\n" + 
                    "or " + tr.sectorRect + ";\n" + 
                    "or " + bl.sectorRect + ";\n" + 
                    "or " + br.sectorRect);
            }
            
            values.clear();
            valueRect = null;
            valuePoint = null;
        }
        
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
    
    public QuadTree(int size)
    {
        if (size == 0) {
            this.root = new Node<V>(new Rectangle(Integer.MIN_VALUE, Integer.MAX_VALUE, 
                Integer.MAX_VALUE, Integer.MIN_VALUE));
        } else {
            this.root = new Node<V>(new Rectangle(-size, size, size, -size));
        }
    }
    
    public void insert(Rectangle rect, V value)
    throws IntersectionException
    {
        // look for an intersecting rectangle
        if (root.findIntersections(rect)) {
            throw new IntersectionException();
        }
    
        for (Point p : rect.getPoints()) {
            root.insert(p, rect, value, 0);
        }
        root.assign(rect, value);
    }
    
    public V find(Point point)
    {
        return root.find(point);
    }
    
    public void delete(Rectangle rect)
    {
        throw new UnsupportedOperationException();
    }
    
    private static void timedFind(QuadTree<String> tree, Point p)
    {
        long t = System.nanoTime();
        String v = tree.find(p);
        t = System.nanoTime() - t;
        System.out.println(v);
        System.out.println("done in " + t + " ns");
    }
    
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
    
    public static void main(String[] args)
    throws Exception
    {
        int size = 0;
        int points = 10000;
        QuadTree<Integer> tree = new QuadTree<Integer>(size);
        
        if (size == 0) {
            size = Integer.MAX_VALUE/2;
        }
        
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
        
        if (size < 1000) {
            System.out.println("Generating visualization.");
            QuadTreeVisualizer.drawQuadTree(tree, "tree.png");
        }
        
        System.out.println("total memory: " + Runtime.getRuntime().totalMemory()/1024/1024 + " mb");
        System.out.println("free memory: " + Runtime.getRuntime().freeMemory()/1024/1024 + " mb");
        
        /*
        QuadTree<String> tree = new QuadTree<String>();
        
        tree.insert(new Rectangle(-10, -20, 14, 14), "foo");
        //tree.insert(new Rectangle(0, 0, 11, -11), "bar");
        tree.insert(new Rectangle(20, 5, 90, -90), "baz");
        tree.insert(new Rectangle(20, 10, 30, 20), "quux");
        tree.insert(new Rectangle(-20, 22, 48, 48), "quuz");
        
        //QuadTreeVisualizer.drawQuadTree(tree, "tree.png");
        
        timedFind(tree, new Point(2, 2));
        timedFind(tree, new Point(-2, -2));
        timedFind(tree, new Point(25, 0));
        timedFind(tree, new Point(49, -10));
        timedFind(tree, new Point(30, 15));
        timedFind(tree, new Point(30, -24));
        timedFind(tree, new Point(30, 30));
        timedFind(tree, new Point(18, 12));*/
    }
}
