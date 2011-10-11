package info.tregmine.quadtree;

import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;

import static info.tregmine.quadtree.QuadTree.Node;

public class QuadTreeVisualizer
{
    public static <V> void drawQuadTree(QuadTree<V> tree, String file)
    throws IOException
    {
        Node<V> root = tree.root;
        Rectangle rect = root.nodeRect;
        
        int s = 5;
        int w = s*(int)rect.getWidth();
        int h = s*(int)rect.getHeight();
        
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        
        Set<Rectangle> inserted = drawSector(root, image, w, h, s, 0);
        
        Graphics2D graphics = image.createGraphics();
        /*graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, w, h);*/
        
        graphics.setColor(Color.BLUE);
        graphics.drawLine(w/2, 0, w/2, h);
        graphics.drawLine(0, h/2, w, h/2);
        
        for (Rectangle valueRect : inserted) {
            //System.out.println(valueRect);
            graphics.setColor(Color.RED);
            graphics.drawRect(w/2 + s*valueRect.x1, h/2 - s*valueRect.y1,
                s*(int)valueRect.getWidth(), s*(int)valueRect.getHeight());
                
            /*graphics.drawString(valueRect.tl.toString(), 
                w/2 + s*(valueRect.tl.x - 7), h/2 - s*(valueRect.tl.y - 1));
            graphics.drawString(valueRect.tr.toString(), 
                w/2 + s*(valueRect.tr.x + 1), h/2 - s*(valueRect.tr.y - 1));
                
            graphics.drawString(valueRect.bl.toString(), 
                w/2 + s*(valueRect.bl.x - 7), h/2 - s*(valueRect.bl.y - 1));
            graphics.drawString(valueRect.br.toString(), 
                w/2 + s*(valueRect.br.x + 1), h/2 - s*(valueRect.br.y - 1));*/
        }
        
        graphics.dispose();
        
        ImageIO.write(image, "png", new File(file));
    }
    
    public static <V> Set<Rectangle> drawSector(Node<V> node, BufferedImage image, int w, int h, int s, int d)
    {
        Set<Rectangle> result = new HashSet<Rectangle>();
    
        Graphics2D graphics = image.createGraphics();
        
        Rectangle rect = node.nodeRect;
        if (node.color) {
            graphics.setColor(new Color(0xFF-d*20,0xFF-d*20,0x00));
        } else {
            graphics.setColor(new Color(0xFF-d*20,0xFF-d*20,0xFF-d*20));
        }
        graphics.fillRect(w/2 + s*rect.x1, h/2 - s*rect.y1,
            s*(int)rect.getWidth(), s*(int)rect.getHeight());
        
        graphics.setColor(new Color(d*20,d*20,d*20));
        graphics.drawRect(w/2 + s*rect.x1, h/2 - s*rect.y1,
            s*(int)rect.getWidth(), s*(int)rect.getHeight());
            
        Rectangle valueRect = node.valueRect;
        if (valueRect != null) {  
            result.add(valueRect);
        }
        
        graphics.dispose();
        
        if (node.split) {
            result.addAll(drawSector(node.tl, image, w, h, s, d+1));
            result.addAll(drawSector(node.tr, image, w, h, s, d+1));
            result.addAll(drawSector(node.bl, image, w, h, s, d+1));
            result.addAll(drawSector(node.br, image, w, h, s, d+1));
        }
        
        return result;
    }
}
