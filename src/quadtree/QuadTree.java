package quadtree;

public class QuadTree<V>
{
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
        
        public boolean contains(Point p)
        {
            return (p.x >= tl.x && p.x <= br.x) &&
                (p.y <= tl.y && p.y >= br.y);
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
        Rectangle valueRect;
        Point valuePoint;
        V value;
        
        // top left, top right
        Node<V> tl, tr;
        // bottom left, bottom right
        Node<V> bl, br;
        
        boolean split;
        
        public Node(Rectangle rect)
        {
            this.sectorRect = rect;
            
            this.valueRect = null;
            this.valuePoint = null;
            this.value = null;
            
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
        
        public V find(Point p)
        {
            if (!split) {
                return value;
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
                throw new IllegalStateException("No matching subsector.");
            }
        }
        
        public void insert(Point p, Rectangle rect, V v, int depth)
        {
            //System.out.println("inserting " + p + " in " + sectorRect + " at depth " + depth);
        
            // this node doesn't matche the point
            if (!sectorRect.contains(p)) {
                System.out.println(sectorRect + " does not match " + p);
                return;
            }
            
            // no value has been assigned to this
            // node, so we don't have to split it
            if (this.value == null && !split) {
                this.value = v;
                this.valueRect = rect;
                this.valuePoint = p;
                return;
            }
            
            if (p.equals(valuePoint)) {
                throw new IllegalStateException("Specified point " + p + " is already in use.");
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
                if (tl.contains(valuePoint)) {
                    tl.insert(this.valuePoint, this.valueRect, this.value, depth+1);
                } else if (tr.contains(valuePoint)) {
                    tr.insert(this.valuePoint, this.valueRect, this.value, depth+1);
                } else if (bl.contains(valuePoint)) {
                    bl.insert(this.valuePoint, this.valueRect, this.value, depth+1);
                } else if (br.contains(valuePoint)) {
                    br.insert(this.valuePoint, this.valueRect, this.value, depth+1);
                } else {
                    throw new IllegalStateException(this.valuePoint + " is not in " + tl.sectorRect + ";\n" + 
                        "or " + tr.sectorRect + ";\n" + 
                        "or " + bl.sectorRect + ";\n" + 
                        "or " + br.sectorRect);
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
            
            this.value = null;
            this.valueRect = null;
            this.valuePoint = null;
        }
    }

    Node<V> root;
    
    public QuadTree()
    {
        //this.root = new Node<V>(new Rectangle(Integer.MIN_VALUE, Integer.MAX_VALUE, 
        //    Integer.MAX_VALUE, Integer.MIN_VALUE));
            
        this.root = new Node<V>(new Rectangle(-100, 100, 
            100, -100));
    }
    
    public void insert(Rectangle rect, V value)
    {
        for (Point p : rect.getPoints()) {
            root.insert(p, rect, value, 0);
            //System.out.println();
        }
    }
    
    public V find(Point point)
    {
        return root.find(point);
    }
    
    public void delete(Rectangle rect)
    {
    }
    
    public static void main(String[] args)
    throws Exception
    {
        QuadTree<String> tree = new QuadTree<String>();
        
        tree.insert(new Rectangle(-10, -20, 14, 14), "foo");
        //System.out.println();
        //tree.insert(new Rectangle(0, 0, 11, -11), "bar");
        //System.out.println();
        tree.insert(new Rectangle(20, 5, 90, -90), "baz");
        //System.out.println();
        tree.insert(new Rectangle(20, 10, 30, 20), "quux");
        
        tree.insert(new Rectangle(-20, 22, 48, 48), "quuz");
        
        QuadTreeVisualizer.drawQuadTree(tree, "tree.png");
        
        System.out.println(tree.find(new Point(2, 2)));
        System.out.println();
        System.out.println(tree.find(new Point(-2, -2)));
        System.out.println();
        System.out.println(tree.find(new Point(25, 0)));
        System.out.println();
    }
}
