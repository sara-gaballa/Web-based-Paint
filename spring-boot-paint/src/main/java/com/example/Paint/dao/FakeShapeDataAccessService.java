package com.example.Paint.dao;

import com.example.Paint.Service.ShapePrototype;
import com.example.Paint.input.ShapeData;
import com.example.Paint.model.*;
import com.google.gson.Gson;
import org.springframework.stereotype.Component;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;


@Component
public class FakeShapeDataAccessService implements ShapeDAO {
    // List<Shape> DB = new ArrayList<>();
    private Map<Integer, Shape> DB = new HashMap<>();
    int MAX = 10000; // maximum number of shapes to be saved in database

    //stores top of undo stack
    private Point action;
    //stores all forward actions
    private Stack<Point> undo = new Stack<Point>();
    //stores all backward actions
    private Stack<Point> redo = new Stack<Point>();

    private Stack<ArrayList<Integer>> clearedIdsUndo = new Stack<ArrayList<Integer>>();

    private Stack<ArrayList<Integer>> clearedIdsRedo = new Stack<ArrayList<Integer>>();

    @Override
    public Map<Integer, Shape> getAllShapes() {
        return DB;
    }

    @Override
    public Shape addShape(Shape shape) { //action added redo done
        int id = new Random().nextInt(MAX);
        // check for id uniqueness
        while (DB.containsKey(id)) {
            id = new Random().nextInt(MAX);
        }
        shape.setId(id);
        DB.put(id, shape);
        undo.add(new Point(id, shape));
        redo.clear();
        clearedIdsRedo.clear();
        return shape;
    }

    @Override
    public Shape addCopy(int id) { //action added, redo done
        if (!DB.containsKey(id))
            return null;

        Shape copiedShape = ShapePrototype.getClone(DB.get(id));
        undo.add(new Point(copiedShape.getId(), copiedShape));
        redo.clear();
        clearedIdsRedo.clear();
        return addShape(copiedShape);
    }

    @Override
    public Shape updateShape(int id, ShapeData shapeData) { //action added redo done
        if (!DB.containsKey(id))
            return null;
        Shape oldShape = DB.get(id);
        //shape = ShapeFactory.getShape(shapeData);
        redo.clear();
        clearedIdsRedo.clear();
        return setAttributes(oldShape, shapeData);
    }

    @Override
    public void deleteShape(int id) { //action added redo done
        DB.remove(id);
        undo.add(new Point(id, null));
        redo.clear();
        clearedIdsRedo.clear();
    }

    @Override
    public void deleteAll() { //action added redo done
        this.DB.clear();
        this.undo.add(new Point(-1, null)); //means all clearedIds
        this.formClearAction();
        redo.clear();
        clearedIdsRedo.clear();
    }

    private void formClearAction() {
        for (Entry<Integer, Shape> entry : DB.entrySet()) {
            clearedIdsUndo.peek().add(entry.getKey());
        }
    }

    @Override
    public Map<Integer, Shape> undo() {
        if (undo.isEmpty())
            return null;
        action = new Point(undo.peek().getKey(), undo.peek().getShape());
        undo.pop();
        redo.push(action);
        formAllShapesOnUndo(action);
        return this.getAllShapes();
    }

    /*
     * Actions can be:
     * (+ve int, shape) //adding or updating shape
     * (-ve int, null) // clear all forward
     * (+ve int, null) // deleting shape
     * */
    private void formAllShapesOnUndo(Point action) {
        if (action.getKey() > 0 && action.getShape() == null) { //means last action was deleting that shape
            Point index = this.contiansKey(action.getKey());
            if (index != null)  //key found and so restore shape
                DB.put(action.getKey(), undo.get(index.getKey()).getShape());
        } else if (action.getKey() < 0) { //means last action was to clear all
            this.restoreAllUndo();
        } else if (action.getKey() > 0) { // means last action was either adding new shape or updating shape
            Point index = contiansKey(action.getKey());
            if (index != null) { //shape found means it was updated
                DB.replace(action.getKey(), undo.get(index.getKey()).getShape());
            } else {
                DB.remove(index);
            }
        }
    }

    private void restoreAllUndo() {
        ArrayList<Integer> indexes = this.clearedIdsUndo.peek();
        for (int i = 0; i < indexes.size(); i++) {
            Point ind = this.contiansKey(indexes.get(i));
            DB.put(ind.getKey(), ind.getShape());
        }
        //all shapes restored, peek accessed then poped on redone when clear all action is in redo
        clearedIdsRedo.add(clearedIdsUndo.pop());
    }

    private Point contiansKey(int id) {
        for (int i = undo.size() - 1; i >= 0; i--) {
            if (id == undo.get(i).getKey() && undo.get(i).getShape() != null)
                return new Point(undo.get(i).getKey(), undo.get(i).getShape());
        }
        return null;
    }

    @Override
    public Map<Integer, Shape> redo() {
        if (redo.isEmpty())
            return null;
        action = new Point(redo.peek().getKey(), redo.peek().getShape());
        redo.pop();
        undo.push(action);
        this.formAllShapesOnRedo(action);
        return this.getAllShapes();
    }

    /*Actions can be:
     * (+ve, shape)
     * (-ve, null) delete all
     * (+ve, null) delete it
     * */
    private void formAllShapesOnRedo(Point action) {
        if (redo.peek().getKey() < 0) { //delete all
            this.DB.clear();
            this.undo.add(new Point(-1, null)); //means all clearedIds
            this.formClearAction();
        } else if (redo.peek().getKey() > 0 && redo.peek().getShape() == null) { //delete shape
            DB.remove(redo.peek().getKey());
            undo.add(new Point(redo.peek().getKey(), null));
        } else if (redo.peek().getKey() > 0 && redo.peek().getShape() != null) { //either add or modify
            Point index = contiansKey(action.getKey());
            if (index != null) { //shape found means it was updated
                DB.replace(action.getKey(), undo.get(index.getKey()).getShape());
            } else {
                assert false;
                DB.put(index.getKey(), index.getShape());
            }
        }
    }

    @Override
    public void save(String fileName, String extension) throws IOException {
        if (extension.equals(".json")) {
            try {
                BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName.concat(extension)));

                // create Gson instance
                Gson gson = new Gson();

                // write JSON to file
                gson.toJson(DB, writer);

                //close the writer
                writer.close();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            try {
                FileOutputStream fos = new FileOutputStream(fileName.concat(extension));
                XMLEncoder encoder = new XMLEncoder(fos);
                encoder.writeObject(DB);
                encoder.close();
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Map<Integer, Shape> load(String fileName, String extension) throws IOException {
        if (extension.equals(".json")) {
            try {
                // create Gson instance
                Gson gson = new Gson();

                // create a reader
                Reader reader = Files.newBufferedReader(Paths.get(fileName.concat(extension)));

                // convert JSON file to map
                DB = gson.fromJson(reader, Map.class);

                // print map entries
                for (Map.Entry<Integer, Shape> entry : DB.entrySet()) {
                    System.out.println(entry.getKey() + "=" + entry.getValue());
                }

                // close reader
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

            try {
                FileInputStream fis = new FileInputStream(fileName.concat(extension));
                XMLDecoder decoder = new XMLDecoder(fis);
                DB = (Map<Integer, Shape>) decoder.readObject();
                decoder.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return DB;
    }

    private Shape setAttributes(Shape shape, ShapeData shapeData) {
        shape.setStartX(shapeData.x);
        shape.setStartY(shapeData.y);
        shape.setFill(shapeData.fill);
        shape.setRotation(shapeData.rotation);
        shape.setStroke(shapeData.stroke);
        shape.setStrokeWidth(shapeData.strokeWidth);
        String type = shape.getType().toLowerCase();
        switch (type) {
            case "circle":
                ((Circle) shape).setRadius(shapeData.radius);
                break;
            case "ellipse":
                ((Ellipse) shape).setHeight(shapeData.height);
                ((Ellipse) shape).setWidth(shapeData.width);
                break;
            case "triangle":
                ((Triangle) shape).setHeight(shapeData.height);
                ((Triangle) shape).setWidth(shapeData.width);
                break;
            case "rectangle":
                ((Rectangle) shape).setLength(shapeData.length);
                ((Rectangle) shape).setWidth(shapeData.width);
                break;
            case "square":
                ((Square) shape).setLength(shapeData.length);
                break;
            case "line":
                ((Line) shape).setEndX(shapeData.endX);
                ((Line) shape).setEndY(shapeData.endY);
                break;
        }
        undo.add(new Point(shape.getId(), shape));
        return shape;
    }
}
